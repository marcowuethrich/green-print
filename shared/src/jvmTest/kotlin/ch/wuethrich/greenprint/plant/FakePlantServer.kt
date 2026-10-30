package ch.wuethrich.greenprint.plant

import ch.wuethrich.greenprint.model.ApiError
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.RecordWateringRequest
import ch.wuethrich.greenprint.model.UpdatePlantPositionRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

/**
 * The plant endpoints the repository calls, backed by a map; offline, every call fails to connect.
 * A test can hold a request's answer back ([holdNextList], [holdNextWrite]) to play a slow network,
 * and have the server turn down every watering and move ([writeError]).
 */
internal class FakePlantServer(private val dispatcher: CoroutineDispatcher) {
    var online = true
    val plants = linkedMapOf<String, Plant>()
    val positionUpdates = mutableListOf<UpdatePlantPositionRequest>()
    val created = mutableListOf<CreatePlantRequest>()
    val waterings = mutableListOf<RecordWateringRequest>()

    /** How many waterings and moves reached the server, a resent one each time, a turned-down one too. */
    var writeRequests = 0

    /** The next room listing is read when it arrives and answered once this completes. */
    var holdNextList: CompletableDeferred<Unit>? = null

    /** The next watering or move is taken in once this completes. */
    var holdNextWrite: CompletableDeferred<Unit>? = null

    /** The status (and error code) every watering and move is answered with instead of being stored. */
    var writeError: Pair<HttpStatusCode, String?>? = null

    private val json = Json { ignoreUnknownKeys = true }

    // The engine runs on the test's dispatcher, so advanceUntilIdle() also waits for the "network".
    fun client(): HttpClient = HttpClient(
        MockEngine(
            MockEngineConfig().apply {
                dispatcher = this@FakePlantServer.dispatcher
                addHandler { request -> handle(request) }
            },
        ),
    ) {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(DefaultRequest) { contentType(ContentType.Application.Json) }
    }

    private suspend fun MockRequestHandleScope.handle(request: HttpRequestData): HttpResponseData {
        if (!online) throw IOException("offline")
        val path = request.url.encodedPath
        return when {
            request.method == HttpMethod.Get && path.endsWith("/plants") -> list()
            request.method == HttpMethod.Put && path.endsWith("/position") -> write { move(path, request) }
            request.method == HttpMethod.Post && path.endsWith("/waterings") -> write { water(path, request) }
            request.method == HttpMethod.Post && path.endsWith("/plants") -> create(path, request)
            else -> respondError(HttpStatusCode.NotFound)
        }
    }

    // The listing is the state when the request arrived, however late the answer comes.
    private suspend fun MockRequestHandleScope.list(): HttpResponseData {
        val snapshot = plants.values.toList()
        holdNextList?.also { holdNextList = null }?.await()
        return respondJson(snapshot)
    }

    private suspend fun MockRequestHandleScope.write(
        store: MockRequestHandleScope.() -> HttpResponseData,
    ): HttpResponseData {
        writeRequests++
        holdNextWrite?.also { holdNextWrite = null }?.await()
        val (status, code) = writeError ?: return store()
        return if (code == null) respondError(status) else respondJson(ApiError(code), status)
    }

    private fun MockRequestHandleScope.move(path: String, request: HttpRequestData): HttpResponseData {
        val id = path.removePrefix("/plants/").removeSuffix("/position")
        val move = json.decodeFromString<UpdatePlantPositionRequest>(request.bodyText())
        positionUpdates += move
        return update(id) { it.copy(x = move.x, y = move.y) }
    }

    // Like the server, a resent watering (same id) is recorded once.
    private fun MockRequestHandleScope.water(path: String, request: HttpRequestData): HttpResponseData {
        val id = path.removePrefix("/plants/").removeSuffix("/waterings")
        val watering = json.decodeFromString<RecordWateringRequest>(request.bodyText())
        if (waterings.none { it.id == watering.id }) waterings += watering
        return update(id) { it.copy(lastWateredDate = watering.timestamp) }
    }

    private fun MockRequestHandleScope.create(path: String, request: HttpRequestData): HttpResponseData {
        val create = json.decodeFromString<CreatePlantRequest>(request.bodyText())
        created += create
        val plant = Plant(
            id = "plant-${plants.size + 1}",
            roomId = path.removePrefix("/rooms/").removeSuffix("/plants"),
            name = create.name,
            wateringIntervalDays = create.wateringIntervalDays,
            x = create.x,
            y = create.y,
        )
        plants[plant.id] = plant
        return respondJson(plant, HttpStatusCode.Created)
    }

    private fun MockRequestHandleScope.update(id: String, change: (Plant) -> Plant): HttpResponseData {
        val updated = plants[id]?.let(change) ?: return respondError(HttpStatusCode.NotFound)
        plants[id] = updated
        return respondJson(updated)
    }

    private inline fun <reified T> MockRequestHandleScope.respondJson(
        body: T,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(
        content = json.encodeToString(body),
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private fun HttpRequestData.bodyText(): String = (body as TextContent).text
}

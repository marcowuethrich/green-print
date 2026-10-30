package ch.wuethrich.greenprint.plant

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ch.wuethrich.greenprint.db.GreenPrintDb
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.db.PlantEntity
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.SyncStatus
import ch.wuethrich.greenprint.model.UpdatePlantPositionRequest
import ch.wuethrich.greenprint.repository.PlantRepositoryImpl
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * [PlantViewModel] over the real [PlantRepositoryImpl] and an in-memory SQLite cache, with the
 * server played by a Ktor [MockEngine]: blueprint moves are written through to the device first
 * and survive being offline (spatial_blueprint.md §2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlantViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakePlantServer(dispatcher)
    private val queries = inMemoryQueries()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server.plants[FERN.id] = FERN
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an online move is saved on the device and on the server`() = runTest(dispatcher) {
        val viewModel = openRoom()

        viewModel.movePlant(FERN.id, 0.2f, 0.8f)
        advanceUntilIdle()

        assertEquals(UpdatePlantPositionRequest(0.2f, 0.8f), server.positionUpdates.single())
        assertEquals(0.2f to 0.8f, localFern().position())
        assertEquals(SyncStatus.SYNCED, localFern().syncStatus)
        assertEquals(0.2f to 0.8f, shownFern(viewModel).x to shownFern(viewModel).y)
    }

    @Test
    fun `an offline move stays on the device and is reported`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val effects = collectEffects(viewModel)
        server.online = false

        viewModel.movePlant(FERN.id, 0.2f, 0.8f)
        advanceUntilIdle()

        assertEquals(0.2f to 0.8f, localFern().position())
        assertEquals(SyncStatus.ERROR, localFern().syncStatus)
        assertEquals(0.2f, shownFern(viewModel).x)
        assertEquals(listOf<PlantUiEffect>(PlantUiEffect.PositionSavedOffline), effects)
        assertEquals(0.5f, server.plants.getValue(FERN.id).x)
    }

    @Test
    fun `an offline move is sent on the next refresh and wins over the server's copy`() = runTest(dispatcher) {
        val viewModel = openRoom()
        server.online = false
        viewModel.movePlant(FERN.id, 0.2f, 0.8f)
        advanceUntilIdle()

        server.online = true
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(0.2f to 0.8f, server.plants.getValue(FERN.id).let { it.x to it.y })
        assertEquals(SyncStatus.SYNCED, localFern().syncStatus)
        assertEquals(0.2f to 0.8f, shownFern(viewModel).x to shownFern(viewModel).y)
    }

    @Test
    fun `an offline move survives a restart`() = runTest(dispatcher) {
        val viewModel = openRoom()
        server.online = false
        viewModel.movePlant(FERN.id, 0.2f, 0.8f)
        advanceUntilIdle()

        // A new repository and view model over the same database, as after relaunching the app.
        val restarted = openRoom()

        assertEquals(0.2f to 0.8f, shownFern(restarted).x to shownFern(restarted).y)
    }

    @Test
    fun `a move is clamped into the room`() = runTest(dispatcher) {
        val viewModel = openRoom()

        viewModel.movePlant(FERN.id, 1.4f, -0.3f)
        advanceUntilIdle()

        assertEquals(UpdatePlantPositionRequest(1f, 0f), server.positionUpdates.single())
    }

    @Test
    fun `a new plant is placed apart from the plants in the room`() = runTest(dispatcher) {
        val viewModel = openRoom()

        viewModel.createPlant(ROOM_ID, "Ivy", species = null, intervalDays = 7)
        advanceUntilIdle()

        val created = server.created.single()
        assertNotEquals(FERN.x to FERN.y, created.x to created.y)
        assertTrue(created.x in 0f..1f && created.y in 0f..1f)
    }

    private fun TestScope.openRoom(): PlantViewModel {
        val repository = PlantRepositoryImpl(server.client(), BASE_URL, queries, dbDispatcher = dispatcher)
        return PlantViewModel(repository).also {
            it.loadPlants(ROOM_ID)
            advanceUntilIdle()
        }
    }

    private fun TestScope.collectEffects(viewModel: PlantViewModel): List<PlantUiEffect> {
        val effects = mutableListOf<PlantUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffect.toList(effects) }
        return effects
    }

    private fun localFern(): PlantEntity = queries.selectPlantById(FERN.id).executeAsOne()

    private fun PlantEntity.position(): Pair<Float, Float> = x.toFloat() to y.toFloat()

    private fun shownFern(viewModel: PlantViewModel): Plant =
        assertIs<PlantUiState.Success>(viewModel.uiState.value).plants.single { it.id == FERN.id }

    private fun inMemoryQueries(): GreenPrintDbQueries {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GreenPrintDb.Schema.create(driver)
        return GreenPrintDb(driver).greenPrintDbQueries.apply {
            insertSpace("space-1", "Home", null, SyncStatus.SYNCED, 0L)
            insertRoom(ROOM_ID, "space-1", "Living Room", null, SyncStatus.SYNCED, 0L)
        }
    }

    private companion object {
        const val BASE_URL = "http://localhost:8080"
        const val ROOM_ID = "room-1"
        val FERN = Plant(id = "plant-1", roomId = ROOM_ID, name = "Fern", wateringIntervalDays = 5, x = 0.5f, y = 0.5f)
    }
}

/** The plant endpoints the repository calls, backed by a map; offline, every call fails to connect. */
private class FakePlantServer(private val dispatcher: CoroutineDispatcher) {
    var online = true
    val plants = linkedMapOf<String, Plant>()
    val positionUpdates = mutableListOf<UpdatePlantPositionRequest>()
    val created = mutableListOf<CreatePlantRequest>()

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

    private fun MockRequestHandleScope.handle(request: HttpRequestData): HttpResponseData {
        if (!online) throw IOException("offline")
        val path = request.url.encodedPath
        return when {
            request.method == HttpMethod.Get && path.endsWith("/plants") -> respondJson(plants.values.toList())

            request.method == HttpMethod.Put && path.endsWith("/position") -> {
                val id = path.removePrefix("/plants/").removeSuffix("/position")
                val move = json.decodeFromString<UpdatePlantPositionRequest>(request.bodyText())
                positionUpdates += move
                val moved = plants[id]?.copy(x = move.x, y = move.y) ?: return respondError(HttpStatusCode.NotFound)
                plants[id] = moved
                respondJson(moved)
            }

            request.method == HttpMethod.Post && path.endsWith("/plants") -> {
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
                respondJson(plant, HttpStatusCode.Created)
            }

            else -> respondError(HttpStatusCode.NotFound)
        }
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

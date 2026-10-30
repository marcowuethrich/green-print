package ch.wuethrich.greenprint.plant

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ch.wuethrich.greenprint.db.GreenPrintDb
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.db.PlantEntity
import ch.wuethrich.greenprint.db.WateringEventEntity
import ch.wuethrich.greenprint.model.ErrorReason
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.SyncStatus
import ch.wuethrich.greenprint.model.UpdatePlantPositionRequest
import ch.wuethrich.greenprint.repository.PlantRepositoryImpl
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * [PlantViewModel] over the real [PlantRepositoryImpl] and an in-memory SQLite cache, with the
 * server played by a Ktor [MockEngine]: blueprint moves and waterings are written through to the
 * device first and survive being offline (spatial_blueprint.md §2, inventory.md §5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlantViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakePlantServer(dispatcher)
    private val queries = inMemoryQueries()
    private val clock = TestClock()

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

    @Test
    fun `an online watering is logged on the device and on the server`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val effects = collectEffects(viewModel)

        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()

        val sent = server.waterings.single()
        assertEquals(NOW.toString(), sent.timestamp)
        assertEquals(NOW.toString(), server.plants.getValue(FERN.id).lastWateredDate)
        assertEquals(SyncStatus.SYNCED, localWaterings().single().syncStatus)
        assertEquals(sent.id, localWaterings().single().id)
        assertEquals(NOW.toString(), shownFern(viewModel).lastWateredDate)
        assertEquals(1f, PlantCareUtils.calculateHydration(shownFern(viewModel), now = NOW))
        assertEquals(listOf<PlantUiEffect>(PlantUiEffect.Saved(PlantChange.WATERED)), effects)
    }

    @Test
    fun `an offline watering fills the hydration bar at once and is reported`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val effects = collectEffects(viewModel)
        server.online = false

        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()

        assertEquals(NOW.toString(), shownFern(viewModel).lastWateredDate)
        assertEquals(1f, PlantCareUtils.calculateHydration(shownFern(viewModel), now = NOW))
        assertEquals(SyncStatus.ERROR, localWaterings().single().syncStatus)
        assertEquals(listOf<PlantUiEffect>(PlantUiEffect.WateringSavedOffline), effects)
        assertEquals(null, server.plants.getValue(FERN.id).lastWateredDate)
    }

    @Test
    fun `an offline watering is sent once on the next refresh and kept over the server's copy`() = runTest(dispatcher) {
        val viewModel = openRoom()
        server.online = false
        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()
        val wateringId = localWaterings().single().id

        server.online = true
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(wateringId), server.waterings.map { it.id })
        assertEquals(NOW.toString(), server.plants.getValue(FERN.id).lastWateredDate)
        assertEquals(SyncStatus.SYNCED, localWaterings().single().syncStatus)
        assertEquals(NOW.toString(), shownFern(viewModel).lastWateredDate)
    }

    @Test
    fun `an offline watering survives a restart`() = runTest(dispatcher) {
        val viewModel = openRoom()
        server.online = false
        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()

        val restarted = openRoom()

        assertEquals(NOW.toString(), shownFern(restarted).lastWateredDate)
    }

    @Test
    fun `a watering confirmed while a refresh is on the way is not undone by the refresh`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val listing = CompletableDeferred<Unit>()
        server.holdNextList = listing
        viewModel.retry()
        advanceUntilIdle()

        // The server lists the room before the watering, and answers only after it confirmed the watering.
        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()
        listing.complete(Unit)
        advanceUntilIdle()

        assertEquals(SyncStatus.SYNCED, localWaterings().single().syncStatus)
        assertEquals(NOW.toString(), localFern().lastWateredDate)
        assertEquals(NOW.toString(), shownFern(viewModel).lastWateredDate)
    }

    @Test
    fun `a move confirmed while a refresh is on the way is not undone by the refresh`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val listing = CompletableDeferred<Unit>()
        server.holdNextList = listing
        viewModel.retry()
        advanceUntilIdle()

        viewModel.movePlant(FERN.id, 0.2f, 0.8f)
        advanceUntilIdle()
        listing.complete(Unit)
        advanceUntilIdle()

        assertEquals(SyncStatus.SYNCED, localFern().syncStatus)
        assertEquals(0.2f to 0.8f, localFern().position())
        assertEquals(0.2f to 0.8f, shownFern(viewModel).x to shownFern(viewModel).y)
    }

    @Test
    fun `a watering the server turns down is dropped and the plant shows the server's date again`() =
        runTest(dispatcher) {
            val viewModel = openRoom()
            server.online = false
            viewModel.waterPlant(FERN.id)
            advanceUntilIdle()

            // A device clock far ahead: the server answers every send with 400 timestamp_invalid.
            server.online = true
            server.writeError = HttpStatusCode.BadRequest to "timestamp_invalid"
            viewModel.retry()
            advanceUntilIdle()
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(1, server.writeRequests)
            assertTrue(localWaterings().isEmpty())
            assertEquals(null, shownFern(viewModel).lastWateredDate)
        }

    @Test
    fun `a watering turned down online is reported, dropped, and the plant refreshed`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val effects = collectEffects(viewModel)
        server.writeError = HttpStatusCode.BadRequest to "timestamp_invalid"

        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()

        assertEquals(listOf<PlantUiEffect>(PlantUiEffect.Failed(ErrorReason.INVALID_INPUT)), effects)
        assertTrue(localWaterings().isEmpty())
        assertEquals(null, shownFern(viewModel).lastWateredDate)
    }

    @Test
    fun `a watering answered with a conflict never names a plant name`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val effects = collectEffects(viewModel)
        server.writeError = HttpStatusCode.Conflict to "watering_id_taken"

        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()

        assertEquals(listOf<PlantUiEffect>(PlantUiEffect.Failed(ErrorReason.UNKNOWN)), effects)
        assertTrue(localWaterings().isEmpty())
    }

    @Test
    fun `a watering that met a server fault is kept and sent again`() = runTest(dispatcher) {
        val viewModel = openRoom()
        server.writeError = HttpStatusCode.ServiceUnavailable to null
        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()
        assertEquals(SyncStatus.ERROR, localWaterings().single().syncStatus)

        server.writeError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(SyncStatus.SYNCED, localWaterings().single().syncStatus)
        assertEquals(NOW.toString(), server.plants.getValue(FERN.id).lastWateredDate)
    }

    @Test
    fun `a move the server turns down is given up and the plant goes back to the server's spot`() =
        runTest(dispatcher) {
            val viewModel = openRoom()
            server.online = false
            viewModel.movePlant(FERN.id, 0.2f, 0.8f)
            advanceUntilIdle()

            server.online = true
            server.writeError = HttpStatusCode.Forbidden to null
            viewModel.retry()
            advanceUntilIdle()
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(1, server.writeRequests)
            assertEquals(SyncStatus.SYNCED, localFern().syncStatus)
            assertEquals(FERN.x to FERN.y, shownFern(viewModel).x to shownFern(viewModel).y)
        }

    @Test
    fun `a watering on its way is not sent again by a refresh`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val accepted = CompletableDeferred<Unit>()
        server.holdNextWrite = accepted

        viewModel.waterPlant(FERN.id)
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()
        accepted.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, server.writeRequests)
        assertEquals(SyncStatus.SYNCED, localWaterings().single().syncStatus)
    }

    @Test
    fun `moves reach the server one at a time and the latest wins`() = runTest(dispatcher) {
        val viewModel = openRoom()
        val firstAccepted = CompletableDeferred<Unit>()
        server.holdNextWrite = firstAccepted

        viewModel.movePlant(FERN.id, 0.2f, 0.8f)
        advanceUntilIdle()
        clock.now += 1.seconds
        viewModel.movePlant(FERN.id, 0.7f, 0.3f)
        viewModel.retry()
        advanceUntilIdle()
        firstAccepted.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            listOf(UpdatePlantPositionRequest(0.2f, 0.8f), UpdatePlantPositionRequest(0.7f, 0.3f)),
            server.positionUpdates,
        )
        assertEquals(0.7f to 0.3f, server.plants.getValue(FERN.id).let { it.x to it.y })
        assertEquals(SyncStatus.SYNCED, localFern().syncStatus)
        assertEquals(0.7f to 0.3f, shownFern(viewModel).x to shownFern(viewModel).y)
    }

    private fun TestScope.openRoom(): PlantViewModel {
        val repository = PlantRepositoryImpl(
            httpClient = server.client(),
            baseUrl = BASE_URL,
            queries = queries,
            dbDispatcher = dispatcher,
            clock = clock,
        )
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

    private suspend fun localFern(): PlantEntity = queries.selectPlantById(FERN.id).awaitAsOne()

    private suspend fun localWaterings(): List<WateringEventEntity> =
        queries.selectWateringEventsByPlantId(FERN.id).awaitAsList()

    private fun PlantEntity.position(): Pair<Float, Float> = x.toFloat() to y.toFloat()

    private fun shownFern(viewModel: PlantViewModel): Plant =
        assertIs<PlantUiState.Success>(viewModel.uiState.value).plants.single { it.id == FERN.id }

    private fun inMemoryQueries(): GreenPrintDbQueries {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GreenPrintDb.Schema.synchronous().create(driver)
        return GreenPrintDb(driver).greenPrintDbQueries.apply {
            // The JDBC driver answers at once, so the suspend queries never actually suspend here.
            runBlocking {
                insertSpace("space-1", "Home", null, SyncStatus.SYNCED, 0L)
                insertRoom(ROOM_ID, "space-1", "Living Room", null, SyncStatus.SYNCED, 0L)
            }
        }
    }

    private companion object {
        const val BASE_URL = "http://localhost:8080"
        const val ROOM_ID = "room-1"
        val FERN = Plant(id = "plant-1", roomId = ROOM_ID, name = "Fern", wateringIntervalDays = 5, x = 0.5f, y = 0.5f)
        val NOW: Instant = Instant.parse("2026-10-30T10:00:00Z")
    }

    private class TestClock : Clock {
        var now: Instant = NOW

        override fun now(): Instant = now
    }
}

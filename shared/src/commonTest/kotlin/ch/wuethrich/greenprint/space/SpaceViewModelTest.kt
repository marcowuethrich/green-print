package ch.wuethrich.greenprint.space

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.model.*
import ch.wuethrich.greenprint.repository.PlantRepository
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeQueries = GreenPrintDbQueries(FakeSqlDriver())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading then Empty when no spaces exist`() = runTest {
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = emptyList()),
            roomRepository = FakeRoomRepository(),
            plantRepository = FakePlantRepository(),
            queries = fakeQueries
        )

        assertEquals(SpaceUiState.Loading, viewModel.uiState.first())
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SpaceUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `initial state becomes HasSpace when spaces exist`() = runTest {
        val space = Space(id = "1", name = "My Home")
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = listOf(space)),
            roomRepository = FakeRoomRepository(),
            plantRepository = FakePlantRepository(),
            queries = fakeQueries
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<SpaceUiState.HasSpace>(viewModel.uiState.value).also {
            assertEquals(space, it.space)
        }
    }

    @Test
    fun `initial state becomes Error when loading fails`() = runTest {
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(error = NetworkError.Connectivity),
            roomRepository = FakeRoomRepository(),
            plantRepository = FakePlantRepository(),
            queries = fakeQueries
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<SpaceUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `createSpace transitions Empty to HasSpace`() = runTest {
        val created = Space(id = "2", name = "Garden Flat")
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = emptyList(), createResult = created),
            roomRepository = FakeRoomRepository(),
            plantRepository = FakePlantRepository(),
            queries = fakeQueries
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SpaceUiState.Empty, viewModel.uiState.value)

        viewModel.createSpace("Garden Flat")
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<SpaceUiState.HasSpace>(viewModel.uiState.value).also {
            assertEquals(created, it.space)
        }
    }

    @Test
    fun `createSpace with blank name does nothing`() = runTest {
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = emptyList()),
            roomRepository = FakeRoomRepository(),
            plantRepository = FakePlantRepository(),
            queries = fakeQueries
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createSpace("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SpaceUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `deleteSpace transitions HasSpace to Empty`() = runTest {
        val space = Space(id = "3", name = "Studio")
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = listOf(space)),
            roomRepository = FakeRoomRepository(),
            plantRepository = FakePlantRepository(),
            queries = fakeQueries
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteSpace(space.id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SpaceUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `retry reloads space after error`() = runTest {
        var shouldFail = true
        val space = Space(id = "4", name = "Loft")
        val repo = object : FakeSpaceRepository(spaces = listOf(space)) {
            override fun observeSpaces(): Flow<NetworkResult<List<Space>>> {
                return if (shouldFail) flowOf(NetworkResult.Failure(NetworkError.ServerError)) else flowOf(NetworkResult.Success(spaces))
            }
        }
        val viewModel = SpaceViewModel(
            spaceRepository = repo,
            roomRepository = FakeRoomRepository(),
            plantRepository = FakePlantRepository(),
            queries = fakeQueries
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<SpaceUiState.Error>(viewModel.uiState.value)

        shouldFail = false
        viewModel.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<SpaceUiState.HasSpace>(viewModel.uiState.value)
    }
}

// --- Test doubles ---

private class FakeSqlDriver : SqlDriver {
    override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> = QueryResult.Value(0L)
    override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> QueryResult<R>, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<R> = mapper(FakeSqlCursor())
    override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.Value(FakeTransaction())
    override fun currentTransaction(): Transacter.Transaction? = null
    override fun addListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {}
    override fun removeListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {}
    override fun notifyListeners(vararg queryKeys: String) {}
    override fun close() {}
}

private class FakeSqlCursor : SqlCursor {
    override fun next(): QueryResult.Value<Boolean> = QueryResult.Value(false)
    override fun getString(index: Int): String? = null
    override fun getLong(index: Int): Long? = null
    override fun getBytes(index: Int): ByteArray? = null
    override fun getDouble(index: Int): Double? = null
    override fun getBoolean(index: Int): Boolean? = null
}

private class FakeTransaction : Transacter.Transaction() {
    override val enclosingTransaction: Transacter.Transaction? = null
    override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.Value(Unit)
}

private open class FakeSpaceRepository(
    protected val spaces: List<Space> = emptyList(),
    private val error: NetworkError? = null,
    private val createResult: Space? = null,
) : SpaceRepository {

    override fun observeSpaces(): Flow<NetworkResult<List<Space>>> {
        if (error != null) return flowOf(NetworkResult.Failure(error))
        return flowOf(NetworkResult.Success(spaces))
    }

    override suspend fun getSpace(id: String): NetworkResult<Space> = 
        NetworkResult.Success(spaces.first { it.id == id })

    override suspend fun createSpace(name: String, icon: String?): NetworkResult<Space> =
        NetworkResult.Success(createResult ?: Space(id = "new", name = name, icon = icon))

    override suspend fun updateSpace(id: String, name: String, icon: String?): NetworkResult<Space> =
        NetworkResult.Success(spaces.first { it.id == id }.copy(name = name, icon = icon))

    override suspend fun deleteSpace(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

private class FakeRoomRepository : RoomRepository {
    override suspend fun createRoom(spaceId: String, name: String, icon: String?): NetworkResult<Room> =
        NetworkResult.Success(Room(id = "new_room", spaceId = spaceId, name = name, icon = icon))

    override suspend fun updateRoom(id: String, name: String, icon: String?): NetworkResult<Room> =
        NetworkResult.Success(Room(id = id, spaceId = "unknown", name = name, icon = icon))

    override suspend fun deleteRoom(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

private class FakePlantRepository : PlantRepository {
    override fun observePlants(roomId: String): Flow<NetworkResult<List<Plant>>> = flowOf(NetworkResult.Success(emptyList()))
    override suspend fun createPlant(roomId: String, request: CreatePlantRequest): NetworkResult<Plant> = NetworkResult.Failure(NetworkError.NotFound)
    override suspend fun updatePlant(id: String, request: UpdatePlantRequest): NetworkResult<Plant> = NetworkResult.Failure(NetworkError.NotFound)
    override suspend fun archivePlant(id: String, request: ArchivePlantRequest): NetworkResult<Plant> = NetworkResult.Failure(NetworkError.NotFound)
    override suspend fun deletePlant(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

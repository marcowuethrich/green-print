package ch.wuethrich.greenprint.space

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.model.ErrorReason
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
            queries = fakeQueries,
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
            queries = fakeQueries,
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
            queries = fakeQueries,
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
            queries = fakeQueries,
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
            queries = fakeQueries,
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
            queries = fakeQueries,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteSpace(space.id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SpaceUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a room the server refuses to delete opens the room-not-empty dialog`() = runTest {
        val space = Space(id = "5", name = "Flat")
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = listOf(space)),
            roomRepository = FakeRoomRepository(deleteResult = NetworkResult.Failure(NetworkError.Conflict)),
            queries = fakeQueries,
        )
        val effects = mutableListOf<SpaceUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffect.toList(effects) }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteRoom("room-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf<SpaceUiEffect>(SpaceUiEffect.RoomHasPlants("room-1", "")), effects)
        assertIs<SpaceUiState.HasSpace>(viewModel.uiState.value)
    }

    @Test
    fun `a taken room name is reported as a typed reason on the form`() = runTest {
        val space = Space(id = "6", name = "Flat")
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = listOf(space)),
            roomRepository = FakeRoomRepository(createResult = NetworkResult.Failure(NetworkError.Conflict)),
            queries = fakeQueries,
        )
        val effects = mutableListOf<SpaceUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffect.toList(effects) }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createRoom(space.id, "Kitchen")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf<SpaceUiEffect>(SpaceUiEffect.ValidationError(ErrorReason.ROOM_NAME_TAKEN, "new_room")),
            effects,
        )
    }

    @Test
    fun `retry reloads space after error`() = runTest {
        var shouldFail = true
        val space = Space(id = "4", name = "Loft")
        val repo = object : FakeSpaceRepository(spaces = listOf(space)) {
            override fun observeSpaces(): Flow<NetworkResult<List<Space>>> = if (shouldFail) {
                flowOf(
                    NetworkResult.Failure(NetworkError.ServerError),
                )
            } else {
                flowOf(NetworkResult.Success(spaces))
            }
        }
        val viewModel = SpaceViewModel(
            spaceRepository = repo,
            roomRepository = FakeRoomRepository(),
            queries = fakeQueries,
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
    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> = QueryResult.Value(0L)
    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> = mapper(FakeSqlCursor())
    override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.Value(FakeTransaction())
    override fun currentTransaction(): Transacter.Transaction? = null
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun notifyListeners(vararg queryKeys: String) = Unit
    override fun close() = Unit
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

private class FakeRoomRepository(
    private val createResult: NetworkResult<Room>? = null,
    private val deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit),
) : RoomRepository {
    override suspend fun createRoom(spaceId: String, name: String, icon: String?): NetworkResult<Room> =
        createResult ?: NetworkResult.Success(Room(id = "new_room", spaceId = spaceId, name = name, icon = icon))

    override suspend fun updateRoom(id: String, name: String, icon: String?): NetworkResult<Room> =
        NetworkResult.Success(Room(id = id, spaceId = "unknown", name = name, icon = icon))

    override suspend fun deleteRoom(id: String): NetworkResult<Unit> = deleteResult
}

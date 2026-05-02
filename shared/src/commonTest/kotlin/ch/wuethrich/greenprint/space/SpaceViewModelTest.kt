package ch.wuethrich.greenprint.space

import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.repository.AuthRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import ch.wuethrich.greenprint.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
            override suspend fun getSpaces(): NetworkResult<List<Space>> {
                if (shouldFail) return NetworkResult.Failure(NetworkError.ServerError)
                return super.getSpaces()
            }
        }
        val viewModel = SpaceViewModel(
            spaceRepository = repo,
            roomRepository = FakeRoomRepository(),
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

private open class FakeSpaceRepository(
    private val spaces: List<Space> = emptyList(),
    private val error: NetworkError? = null,
    private val createResult: Space? = null,
) : SpaceRepository {

    override suspend fun getSpaces(): NetworkResult<List<Space>> {
        if (error != null) return NetworkResult.Failure(error)
        return NetworkResult.Success(spaces)
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

private class FakeAuthRepository : AuthRepository {
    override val sessionToken: Flow<String?> = MutableStateFlow("mock_token")
    override suspend fun login() = Unit
    override suspend fun logout() = Unit
    override fun getAccessToken(): String? = "mock_token"
}

package ch.wuethrich.greenprint.space

import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.repository.AuthRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
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
            authRepository = FakeAuthRepository(),
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
            authRepository = FakeAuthRepository(),
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<SpaceUiState.HasSpace>(viewModel.uiState.value).also {
            assertEquals(space, it.space)
        }
    }

    @Test
    fun `initial state becomes Error when loading fails`() = runTest {
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(error = RuntimeException("Network error")),
            authRepository = FakeAuthRepository(),
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<SpaceUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `createSpace transitions Empty to HasSpace`() = runTest {
        val created = Space(id = "2", name = "Garden Flat")
        val viewModel = SpaceViewModel(
            spaceRepository = FakeSpaceRepository(spaces = emptyList(), createResult = created),
            authRepository = FakeAuthRepository(),
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
            authRepository = FakeAuthRepository(),
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
            authRepository = FakeAuthRepository(),
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
            override suspend fun getSpaces(): List<Space> {
                if (shouldFail) throw RuntimeException("temporary error")
                return super.getSpaces()
            }
        }
        val viewModel = SpaceViewModel(spaceRepository = repo, authRepository = FakeAuthRepository())
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
    private val error: Throwable? = null,
    private val createResult: Space? = null,
) : SpaceRepository {

    override suspend fun getSpaces(): List<Space> {
        if (error != null) throw error
        return spaces
    }

    override suspend fun getSpace(id: String): Space = spaces.first { it.id == id }

    override suspend fun createSpace(name: String): Space =
        createResult ?: Space(id = "new", name = name)

    override suspend fun updateSpace(id: String, name: String): Space =
        spaces.first { it.id == id }.copy(name = name)

    override suspend fun deleteSpace(id: String) = Unit
}

private class FakeAuthRepository : AuthRepository {
    override val sessionToken: Flow<String?> = MutableStateFlow("mock_token")
    override suspend fun login() = Unit
    override suspend fun logout() = Unit
    override fun getAccessToken(): String? = "mock_token"
}

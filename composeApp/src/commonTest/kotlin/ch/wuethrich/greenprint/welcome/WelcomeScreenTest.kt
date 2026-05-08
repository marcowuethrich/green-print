package ch.wuethrich.greenprint.welcome

import androidx.compose.ui.test.*
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import cafe.adriel.voyager.navigator.Navigator
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.model.*
import ch.wuethrich.greenprint.repository.*
import ch.wuethrich.greenprint.space.SpaceViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WelcomeScreenTest {

    private lateinit var fakeSpaceRepository: ControlledFakeSpaceRepository

    @BeforeTest
    fun setup() {
        fakeSpaceRepository = ControlledFakeSpaceRepository()
        startKoin {
            modules(module {
                single<GreenPrintDbQueries> { GreenPrintDbQueries(FakeSqlDriver()) }
                single<SpaceRepository> { fakeSpaceRepository }
                single<RoomRepository> { FakeRoomRepository() }
                single<PlantRepository> { FakePlantRepository() }
                viewModel { SpaceViewModel(get(), get(), get(), get()) }
            })
        }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun testWelcomeScreen_EmptyState_RendersCorrectly() = runComposeUiTest {
        fakeSpaceRepository.emit(NetworkResult.Success(emptyList()))
        
        setContent {
            Navigator(WelcomeScreen())
        }

        onNodeWithText("Welcome to GreenPrint").assertIsDisplayed()
        onNodeWithText("Start your plant journey by creating a space.").assertIsDisplayed()
        onNodeWithText("Create living space").assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreen_PopulatedState_RendersSpaceName() = runComposeUiTest {
        val space = Space(id = "1", name = "Urban Jungle", icon = "balcony")
        fakeSpaceRepository.emit(NetworkResult.Success(listOf(space)))

        setContent {
            Navigator(WelcomeScreen())
        }

        onNodeWithText("Urban Jungle").assertIsDisplayed()
        onNodeWithText("Tap to enter").assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreen_ErrorState_ShowsRetry() = runComposeUiTest {
        fakeSpaceRepository.emit(NetworkResult.Failure(NetworkError.Connectivity))

        setContent {
            Navigator(WelcomeScreen())
        }

        onNodeWithText("Network error", substring = true).assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreen_Modal_CanCreateSpace() = runComposeUiTest {
        fakeSpaceRepository.emit(NetworkResult.Success(emptyList()))

        setContent {
            Navigator(WelcomeScreen())
        }

        // Open Modal
        onNodeWithText("Create living space").performClick()
        
        // Verify Modal Title (Uses space_create_title)
        onNodeWithText("Your Living Space").assertIsDisplayed()

        // Type Name (Uses space_name_hint)
        onNodeWithText("Space name").performTextInput("Green Balcony")
        
        // Setup success result for creation
        fakeSpaceRepository.createResult = NetworkResult.Success(Space("2", "Green Balcony"))

        // Confirm (Uses common_save)
        onNodeWithText("Save").performClick()

        // Wait for state change and check if populated content appears
        onNodeWithText("Green Balcony").assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreen_Modal_ShowsValidationError() = runComposeUiTest {
        fakeSpaceRepository.emit(NetworkResult.Success(emptyList()))

        setContent {
            Navigator(WelcomeScreen())
        }

        // Open Modal
        onNodeWithText("Create living space").performClick()

        // Type Name
        onNodeWithText("Space name").performTextInput("Existing Space")
        
        // Setup conflict result
        fakeSpaceRepository.createResult = NetworkResult.Failure(NetworkError.Conflict)

        // Confirm
        onNodeWithText("Save").performClick()

        // Verify Validation Error Message (from SpaceViewModel.handleFormError conflict message)
        onNodeWithText("A space with this name already exists.").assertIsDisplayed()
        
        // Modal should still be open
        onNodeWithText("Your Living Space").assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreen_Modal_CanCancel() = runComposeUiTest {
        fakeSpaceRepository.emit(NetworkResult.Success(emptyList()))

        setContent {
            Navigator(WelcomeScreen())
        }

        // Open Modal
        onNodeWithText("Create living space").performClick()
        onNodeWithText("Your Living Space").assertIsDisplayed()

        // Cancel (Uses common_cancel)
        onNodeWithText("Cancel").performClick()

        // Verify Modal is gone
        onNodeWithText("Your Living Space").assertDoesNotExist()
        onNodeWithText("Welcome to GreenPrint").assertIsDisplayed()
    }
}

// --- Fakes ---

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

private class ControlledFakeSpaceRepository : SpaceRepository {
    private val flow = MutableSharedFlow<NetworkResult<List<Space>>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    var createResult: NetworkResult<Space>? = null

    fun emit(result: NetworkResult<List<Space>>) {
        flow.tryEmit(result)
    }

    override fun observeSpaces(): Flow<NetworkResult<List<Space>>> = flow
    override suspend fun getSpace(id: String): NetworkResult<Space> = NetworkResult.Success(Space("1", "Space"))
    
    override suspend fun createSpace(name: String, icon: String?): NetworkResult<Space> {
        val result = createResult ?: NetworkResult.Success(Space("new", name, icon))
        if (result is NetworkResult.Success) {
            emit(NetworkResult.Success(listOf(result.data)))
        }
        return result
    }

    override suspend fun updateSpace(id: String, name: String, icon: String?): NetworkResult<Space> = NetworkResult.Success(Space(id, name, icon))
    override suspend fun deleteSpace(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

private class FakeRoomRepository : RoomRepository {
    override suspend fun createRoom(spaceId: String, name: String, icon: String?): NetworkResult<Room> = NetworkResult.Success(Room("new", spaceId, name, icon))
    override suspend fun updateRoom(id: String, name: String, icon: String?): NetworkResult<Room> = NetworkResult.Success(Room(id, "unknown", name, icon))
    override suspend fun deleteRoom(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

private class FakePlantRepository : PlantRepository {
    override fun observePlants(roomId: String): Flow<NetworkResult<List<Plant>>> = flowOf(NetworkResult.Success(emptyList()))
    override suspend fun createPlant(roomId: String, request: CreatePlantRequest): NetworkResult<Plant> = NetworkResult.Failure(NetworkError.NotFound)
    override suspend fun updatePlant(id: String, request: UpdatePlantRequest): NetworkResult<Plant> = NetworkResult.Failure(NetworkError.NotFound)
    override suspend fun archivePlant(id: String, request: ArchivePlantRequest): NetworkResult<Plant> = NetworkResult.Failure(NetworkError.NotFound)
    override suspend fun deletePlant(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

private fun <T> flowOf(value: T): Flow<T> = kotlinx.coroutines.flow.flow { emit(value) }

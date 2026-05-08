package ch.wuethrich.greenprint.welcome

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.runComposeUiTest
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class WelcomeScreenTest {

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<GreenPrintDbQueries> { GreenPrintDbQueries(FakeSqlDriver()) }
                single<SpaceRepository> { FakeSpaceRepository() }
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testWelcomeScreenRendersEmptyStateWhenNoSpaces() = runComposeUiTest {
        setContent {
            org.koin.compose.KoinContext {
                // Need a Navigator since WelcomeScreen uses LocalNavigator.currentOrThrow
                Navigator(WelcomeScreen())
            }
        }

        // Wait for Loading to finish
        waitForIdle()

        try {
            // Assert that the title is displayed (app_name "GreenPrint" in top bar)
            // Check welcome_title "Welcome to GreenPrint"
            onNodeWithText("Welcome to GreenPrint", substring = true).assertIsDisplayed()
            
            // Assert the "Create Space" button is displayed
            onNodeWithText("Create living space", substring = true).assertIsDisplayed()
        } catch (e: AssertionError) {
            onRoot().printToLog("TEST_FAIL")
            throw e
        }
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

private class FakeSpaceRepository : SpaceRepository {
    override fun observeSpaces(): Flow<NetworkResult<List<Space>>> = flowOf(NetworkResult.Success(emptyList()))
    override suspend fun getSpace(id: String): NetworkResult<Space> = NetworkResult.Success(Space("1", "Space"))
    override suspend fun createSpace(name: String, icon: String?): NetworkResult<Space> = NetworkResult.Success(Space("new", name, icon))
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
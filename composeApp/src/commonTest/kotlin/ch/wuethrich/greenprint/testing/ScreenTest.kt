package ch.wuethrich.greenprint.testing

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for Compose UI screen tests. Each test gets:
 *  - fresh fake repositories ([spaceRepository], [roomRepository], [plantRepository]),
 *  - a Koin instance wired with [testAppModule] (plus any [extraModules]),
 *  - a [setScreenContent] helper that mounts the screen inside a Voyager [Navigator],
 *    optionally on top of a [MarkerScreen] for back-stack assertions.
 *
 * Override [extraModules] to inject additional bindings (e.g. screen-specific ViewModels).
 *
 * Usage:
 * ```
 * class MyScreenTest : ScreenTest() {
 *     @Test fun rendersTitle() = runComposeUiTest {
 *         spaceRepository.emitSpaces(listOf(Space("1", "Home")))
 *         setScreenContent(MyScreen())
 *         onNodeWithText("Home").assertIsDisplayed()
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
abstract class ScreenTest {

    val spaceRepository: FakeSpaceRepository = FakeSpaceRepository()
    val roomRepository: FakeRoomRepository = FakeRoomRepository()
    val plantRepository: FakePlantRepository = FakePlantRepository()
    val queries: GreenPrintDbQueries = GreenPrintDbQueries(FakeSqlDriver())

    /** Subclasses can add extra Koin modules (e.g. for screen-specific ViewModels). */
    open fun extraModules(): List<Module> = emptyList()

    @BeforeTest
    fun startTestKoin() {
        startKoin {
            modules(
                listOf(testAppModule(spaceRepository, roomRepository, plantRepository, queries)) +
                    extraModules(),
            )
        }
    }

    @AfterTest
    fun stopTestKoin() {
        stopKoin()
    }
}

/**
 * Mounts [screen] inside a Voyager [Navigator]. Pass [behind] to place a parent screen on
 * the back stack so you can assert that [screen] popped back to it (e.g. after delete).
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setScreenContent(screen: Screen, behind: Screen? = null) {
    setContent {
        if (behind != null) {
            Navigator(behind) { navigator ->
                CurrentScreen()
                LaunchedEffect(Unit) { navigator.push(screen) }
            }
        } else {
            Navigator(screen)
        }
    }
}

/**
 * Sentinel screen that displays a unique text marker. Used together with
 * [setScreenContent]'s `behind` parameter to detect back-stack pops in tests.
 */
class MarkerScreen(private val label: String = MARKER_TEXT) : Screen {
    @Composable
    override fun Content() {
        Text(label)
    }

    companion object {
        const val MARKER_TEXT: String = "test::backstack-parent"
    }
}

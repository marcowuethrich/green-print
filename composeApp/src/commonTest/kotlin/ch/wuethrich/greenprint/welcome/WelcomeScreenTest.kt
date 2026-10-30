package ch.wuethrich.greenprint.welcome

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.testing.ScreenTest
import ch.wuethrich.greenprint.testing.setScreenContent
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WelcomeScreenTest : ScreenTest() {

    @Test
    fun emptyState_rendersCreateCta() = runComposeUiTest {
        spaceRepository.emitSpaces(emptyList())
        setScreenContent(WelcomeScreen())

        onNodeWithText("Welcome to GreenPrint").assertIsDisplayed()
        onNodeWithText("Start your plant journey by creating a space.").assertIsDisplayed()
        onNodeWithText("Create living space").assertIsDisplayed()
    }

    @Test
    fun populatedState_rendersSpaceName() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(Space(id = "1", name = "Urban Jungle", icon = "balcony")))
        setScreenContent(WelcomeScreen())

        onNodeWithText("Urban Jungle").assertIsDisplayed()
        onNodeWithText("Tap to enter").assertIsDisplayed()
    }

    @Test
    fun errorState_showsConnectivityMessageAndRetry() = runComposeUiTest {
        spaceRepository.emitError(NetworkError.Connectivity)
        setScreenContent(WelcomeScreen())

        onNodeWithText("Network error", substring = true).assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun createModal_savesSpaceAndShowsItInPopulatedState() = runComposeUiTest {
        spaceRepository.emitSpaces(emptyList())
        setScreenContent(WelcomeScreen())

        onNodeWithText("Create living space").performClick()
        onNodeWithText("Your Living Space").assertIsDisplayed()

        onNodeWithText("Space name").performTextInput("Green Balcony")
        spaceRepository.createResult = NetworkResult.Success(Space("2", "Green Balcony"))
        onNodeWithText("Save").performClick()

        onNodeWithText("Green Balcony").assertIsDisplayed()
    }

    @Test
    fun createModal_showsConflictValidationAndStaysOpen() = runComposeUiTest {
        spaceRepository.emitSpaces(emptyList())
        setScreenContent(WelcomeScreen())

        onNodeWithText("Create living space").performClick()
        onNodeWithText("Space name").performTextInput("Existing Space")

        spaceRepository.createResult = NetworkResult.Failure(NetworkError.Conflict)
        onNodeWithText("Save").performClick()

        onNodeWithText("A space with this name already exists.").assertIsDisplayed()
        onNodeWithText("Your Living Space").assertIsDisplayed()
    }

    @Test
    fun createModal_dismissReturnsToEmptyState() = runComposeUiTest {
        spaceRepository.emitSpaces(emptyList())
        setScreenContent(WelcomeScreen())

        onNodeWithText("Create living space").performClick()
        onNodeWithText("Your Living Space").assertIsDisplayed()

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Welcome to GreenPrint").assertIsDisplayed()
    }
}

package ch.wuethrich.greenprint.space

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.testing.MarkerScreen
import ch.wuethrich.greenprint.testing.ScreenTest
import ch.wuethrich.greenprint.testing.setScreenContent
import kotlin.test.Test

/**
 * UI tests for [SpaceScreen]. Organized by responsibility:
 *  - state rendering (Loading / HasSpace / Error / no-rooms)
 *  - top bar + FAB
 *  - settings menu (rename / delete / leave)
 *  - room card actions (rename / delete)
 *  - modals (CreateRoom, RenameSpace, RenameRoom, DeleteSpace, DeleteRoom)
 *  - back-stack navigation (delete-space, empty-state auto-pop)
 *  - keyboard focus (ADR-013)
 */
@OptIn(ExperimentalTestApi::class)
class SpaceScreenTest : ScreenTest() {

    private val home = Space(id = "s1", name = "Home", icon = "house")
    private val livingRoom = Room(id = "r1", spaceId = "s1", name = "Living Room", icon = "sofa")
    private val kitchen = Room(id = "r2", spaceId = "s1", name = "Kitchen", icon = "kitchen")
    private val homeWithRooms = home.copy(rooms = listOf(livingRoom, kitchen))

    // region — state rendering

    @Test
    fun emptyRooms_showsAddFirstRoomCta() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("No Rooms Yet").assertIsDisplayed()
        onNodeWithText("Divide your space into rooms to organize your plants.").assertIsDisplayed()
        onNodeWithText("Add Your First Room").assertIsDisplayed()
    }

    @Test
    fun populatedRooms_showsRoomsSectionWithAllRooms() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onNodeWithText("Rooms").assertIsDisplayed()
        onNodeWithText("Living Room").assertIsDisplayed()
        onNodeWithText("Kitchen").assertIsDisplayed()
    }

    @Test
    fun errorState_showsConnectivityMessageAndRetry() = runComposeUiTest {
        spaceRepository.emitError(NetworkError.Connectivity)
        setScreenContent(SpaceScreen())

        onNodeWithText("Couldn't reach the server", substring = true).assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    // endregion

    // region — top bar & FAB

    @Test
    fun topBar_displaysSpaceName() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun fab_opensCreateRoomModal() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("Add Room").performClick()

        onNodeWithText("Add Room").assertIsDisplayed() // dialog title
        onNodeWithText("Room name").assertIsDisplayed() // input label
    }

    @Test
    fun emptyRooms_addFirstRoomButton_opensCreateRoomModal() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Add Your First Room").performClick()

        onNodeWithText("Room name").assertIsDisplayed()
    }

    // endregion

    // region — settings menu (top bar)

    @Test
    fun settingsMenu_renameOpensRenameSpaceDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Rename").performClick()

        onNodeWithText("Rename space").assertIsDisplayed()
    }

    @Test
    fun settingsMenu_deleteOpensDeleteSpaceDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Delete").performClick()

        onNodeWithText("Delete space?").assertIsDisplayed()
    }

    @Test
    fun settingsMenu_leavePopsBackStack() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen(), behind = MarkerScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Leave Space").performClick()

        waitUntil(timeoutMillis = 2000L) {
            onAllNodesWithText(
                MarkerScreen.MARKER_TEXT,
            ).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        onNodeWithText(MarkerScreen.MARKER_TEXT).assertIsDisplayed()
    }

    // endregion

    // region — room card menu

    @Test
    fun roomCard_renameOpensRenameRoomDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Rename").performClick()

        onNodeWithText("Rename Room").assertIsDisplayed()
    }

    @Test
    fun roomCard_deleteOpensDeleteRoomDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()

        onNodeWithText("Delete room?").assertIsDisplayed()
    }

    // endregion

    // region — CreateRoom modal

    @Test
    fun createRoom_savesAndShowsNewRoomInList() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Add Your First Room").performClick()
        onNodeWithText("Room name").performTextInput("Bedroom")

        roomRepository.createResult = NetworkResult.Success(
            Room(id = "r-new", spaceId = "s1", name = "Bedroom", icon = null),
        )

        onNodeWithText("Save").performClick()
        spaceRepository.emitSpaces(
            listOf(homeWithRooms.copy(rooms = listOf(livingRoom, Room("r-new", "s1", "Bedroom")))),
        )

        onNodeWithText("Bedroom").assertIsDisplayed()
    }

    @Test
    fun createRoom_conflictKeepsModalOpenWithError() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Add Your First Room").performClick()
        onNodeWithText("Room name").performTextInput("Living Room")

        roomRepository.createResult = NetworkResult.Failure(NetworkError.Conflict)
        onNodeWithText("Save").performClick()

        onNodeWithText("A room with this name already exists in this space.").assertIsDisplayed()
        onNodeWithText("Add Room").assertIsDisplayed()
    }

    @Test
    fun createRoom_imeDoneSubmitsForm() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Add Your First Room").performClick()
        val createdRoom = Room(id = "r-new", spaceId = "s1", name = "Office")
        roomRepository.createResult = NetworkResult.Success(createdRoom)

        onNode(hasSetTextAction()).performTextInput("Office")
        onNode(hasSetTextAction()).performImeAction()
        spaceRepository.emitSpaces(listOf(home.copy(rooms = listOf(createdRoom))))

        onNodeWithText("Office").assertIsDisplayed()
    }

    @Test
    fun createRoom_dismissClosesModal() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Add Your First Room").performClick()
        onNodeWithText("Cancel").performClick()

        onNodeWithText("No Rooms Yet").assertIsDisplayed()
    }

    // endregion

    // region — RenameSpace modal

    @Test
    fun renameSpace_prefillsCurrentName() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Rename").performClick()

        onAllNodesWithText("Home").assertCountAtLeast(2)
    }

    @Test
    fun renameSpace_savesAndUpdatesTitle() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Rename").performClick()

        onNode(hasSetTextAction()).performTextClearance()
        onNode(hasSetTextAction()).performTextInput("Apartment")
        spaceRepository.updateResult = NetworkResult.Success(home.copy(name = "Apartment"))

        onNodeWithText("Save").performClick()
        spaceRepository.emitSpaces(listOf(home.copy(name = "Apartment")))

        onNodeWithText("Apartment").assertIsDisplayed()
    }

    @Test
    fun renameSpace_conflictShowsError() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Rename").performClick()
        onNode(hasSetTextAction()).performTextClearance()
        onNode(hasSetTextAction()).performTextInput("Other")

        spaceRepository.updateResult = NetworkResult.Failure(NetworkError.Conflict)
        onNodeWithText("Save").performClick()

        onNodeWithText("A space with this name already exists.").assertIsDisplayed()
        onNodeWithText("Rename space").assertIsDisplayed()
    }

    // endregion

    // region — RenameRoom modal

    @Test
    fun renameRoom_prefillsCurrentNameAndSavesNewName() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Rename").performClick()

        onNodeWithText("Rename Room").assertIsDisplayed()

        onNode(hasSetTextAction()).performTextClearance()
        onNode(hasSetTextAction()).performTextInput("Lounge")
        roomRepository.updateResult = NetworkResult.Success(livingRoom.copy(name = "Lounge"))

        onNodeWithText("Save").performClick()
        spaceRepository.emitSpaces(
            listOf(homeWithRooms.copy(rooms = listOf(livingRoom.copy(name = "Lounge"), kitchen))),
        )

        onNodeWithText("Lounge").assertIsDisplayed()
    }

    // endregion

    // region — DeleteSpace dialog

    @Test
    fun deleteSpace_confirmDisabledUntilNameTyped() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Delete").performClick()

        onNodeWithText("Delete").assertIsNotEnabled()

        onNode(hasSetTextAction()).performTextInput("Home")

        onNode(hasSetTextAction()).performTextClearance()
        onNode(hasSetTextAction()).performTextInput("Wrong")
        onNodeWithText("Delete").assertIsNotEnabled()
    }

    @Test
    fun deleteSpace_confirmsAndPopsBackStack() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen(), behind = MarkerScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Delete").performClick()
        onNode(hasSetTextAction()).performTextInput("Home")
        onNodeWithText("Delete").performClick()

        waitUntil(timeoutMillis = 2000L) {
            onAllNodesWithText(
                MarkerScreen.MARKER_TEXT,
            ).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        onNodeWithText(MarkerScreen.MARKER_TEXT).assertIsDisplayed()
    }

    @Test
    fun deleteSpace_cancelClosesDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Cancel").performClick()

        onNodeWithText("No Rooms Yet").assertIsDisplayed()
    }

    // endregion

    // region — DeleteRoom dialog

    @Test
    fun deleteRoom_confirmRemovesRoomFromList() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()

        // Wait for the dialog to appear
        onNodeWithText("Delete room?").assertIsDisplayed()

        // Click delete in the dialog - use onLast() to pick the dialog button over the menu item
        onAllNodesWithText("Delete").onLast().performClick()

        // Simulate refresh
        spaceRepository.emitSpaces(listOf(homeWithRooms.copy(rooms = listOf(kitchen))))

        waitUntil(timeoutMillis = 2000L) {
            onAllNodesWithText("Living Room").fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty()
        }
        onNodeWithText("Kitchen").assertIsDisplayed()
    }

    @Test
    fun deleteRoom_cancelClosesDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Cancel").performClick()

        onNodeWithText("Living Room").assertIsDisplayed()
    }

    // endregion

    // region — back-stack effects

    @Test
    fun whenStateBecomesEmpty_popsBackToParent() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen(), behind = MarkerScreen())
        onNodeWithText("Home").assertIsDisplayed()

        spaceRepository.emitSpaces(emptyList())

        waitUntil(timeoutMillis = 2000L) {
            onAllNodesWithText(
                MarkerScreen.MARKER_TEXT,
            ).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        onNodeWithText(MarkerScreen.MARKER_TEXT).assertIsDisplayed()
    }

    // endregion

    // region — focus order (ADR-013)

    @Test
    fun emptyRooms_focusesAddFirstRoomButton() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        waitUntil(timeoutMillis = 2000L) {
            onAllNodes(isFocused() and hasText("Add Your First Room")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun createRoomModal_focusesNameField() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Add Your First Room").performClick()

        waitUntil(timeoutMillis = 2000L) {
            onAllNodes(isFocused() and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun deleteRoomDialog_focusesCancelButton() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()

        waitUntil(timeoutMillis = 2000L) {
            onAllNodes(isFocused() and hasText("Cancel")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun deleteSpaceDialog_focusesConfirmationField() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Delete").performClick()

        waitUntil(timeoutMillis = 2000L) {
            onAllNodes(isFocused() and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // endregion
}

private fun SemanticsNodeInteractionCollection.assertCountAtLeast(min: Int): SemanticsNodeInteractionCollection =
    apply {
        val count = fetchSemanticsNodes().size
        check(count >= min) { "Expected at least $min nodes, found $count." }
    }

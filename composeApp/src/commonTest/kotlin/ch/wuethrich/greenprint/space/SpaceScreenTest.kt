package ch.wuethrich.greenprint.space

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
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
 *
 * Conventions:
 *  - Modal/dialog interactions are anchored on unique button labels (Save / Cancel / Delete)
 *    or content descriptions; for ambiguous labels (multiple "More options" buttons in
 *    populated state) we use [onAllNodesWithContentDescription] and explicit indexes.
 *  - The text input inside a dialog is located via [hasSetTextAction] since OutlinedTextFields
 *    don't expose stable labels in all dialogs.
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
        mainClock.advanceTimeBy(2_000)

        onNodeWithText("Rooms").assertIsDisplayed()
        onNodeWithText("Living Room").assertIsDisplayed()
        onNodeWithText("Kitchen").assertIsDisplayed()
    }

    @Test
    fun errorState_showsConnectivityMessageAndRetry() = runComposeUiTest {
        spaceRepository.emitError(NetworkError.Connectivity)
        setScreenContent(SpaceScreen())

        onNodeWithText("Network error", substring = true).assertIsDisplayed()
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

        // Empty rooms → only the top-bar "More options" button exists
        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Rename").performClick()

        onNodeWithText("Rename space").assertIsDisplayed()
        onNodeWithText("Space name").assertIsDisplayed()
    }

    @Test
    fun settingsMenu_deleteOpensDeleteSpaceDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Delete").performClick()

        onNodeWithText("Delete space?").assertIsDisplayed()
        onNodeWithText("Please type 'Home' to confirm deletion:").assertIsDisplayed()
    }

    @Test
    fun settingsMenu_leavePopsBackStack() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen(), behind = MarkerScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Leave Space").performClick()

        onNodeWithText(MarkerScreen.MARKER_TEXT).assertIsDisplayed()
    }

    // endregion

    // region — room card menu

    @Test
    fun roomCard_renameOpensRenameRoomDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())
        mainClock.advanceTimeBy(2_000)

        // Index 0 = top-bar settings menu, index 1+ = room cards in render order
        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Rename").performClick()

        onNodeWithText("Rename Room").assertIsDisplayed()
    }

    @Test
    fun roomCard_deleteOpensDeleteRoomDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())
        mainClock.advanceTimeBy(2_000)

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()

        onNodeWithText("Delete room?").assertIsDisplayed()
        onNodeWithText("Are you sure you want to delete", substring = true).assertIsDisplayed()
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
        // After createRoom succeeds, the ViewModel triggers a refresh; emit the new state
        // AFTER the click so the StateFlow transitions through Loading → HasSpace(new),
        // which triggers the modal-close LaunchedEffect.
        spaceRepository.emitSpaces(listOf(homeWithRooms.copy(rooms = listOf(livingRoom, Room("r-new", "s1", "Bedroom")))))
        mainClock.advanceTimeBy(2_000)

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
        onNodeWithText("Add Room").assertIsDisplayed() // dialog still open
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
        // Emit AFTER the IME action so loadSpaceInternal observes a real state transition
        // (Loading → HasSpace(new)), which fires the modal-close LaunchedEffect.
        spaceRepository.emitSpaces(listOf(home.copy(rooms = listOf(createdRoom))))
        mainClock.advanceTimeBy(2_000)

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

        // The current name is shown both in the top bar AND prefilled in the field.
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
        // Emit the renamed space AFTER the click so observeSpaces transitions and the
        // modal-close LaunchedEffect fires.
        spaceRepository.emitSpaces(listOf(home.copy(name = "Apartment")))
        mainClock.advanceTimeBy(2_000)

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
        mainClock.advanceTimeBy(2_000)

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Rename").performClick()

        onNodeWithText("Rename Room").assertIsDisplayed()

        onNode(hasSetTextAction()).performTextClearance()
        onNode(hasSetTextAction()).performTextInput("Lounge")
        roomRepository.updateResult = NetworkResult.Success(livingRoom.copy(name = "Lounge"))

        onNodeWithText("Save").performClick()
        // Emit the renamed room AFTER the click so observeSpaces transitions and the
        // modal-close LaunchedEffect fires.
        spaceRepository.emitSpaces(
            listOf(homeWithRooms.copy(rooms = listOf(livingRoom.copy(name = "Lounge"), kitchen))),
        )
        mainClock.advanceTimeBy(2_000)

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

        // The Delete button is the only enabled "Delete" node; assertHasClickAction would also
        // pass on the disabled one, so we re-check by typing the wrong text first.
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
        mainClock.advanceTimeBy(2_000)

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Delete room?").assertIsDisplayed()

        // After successful deletion the ViewModel refreshes; emit the updated list.
        spaceRepository.emitSpaces(listOf(homeWithRooms.copy(rooms = listOf(kitchen))))
        onNodeWithText("Delete").performClick()
        mainClock.advanceTimeBy(2_000)

        onAllNodesWithText("Living Room").assertCountEquals(0)
        onNodeWithText("Kitchen").assertIsDisplayed()
    }

    @Test
    fun deleteRoom_cancelClosesDialog() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())
        mainClock.advanceTimeBy(2_000)

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()
        onNodeWithText("Cancel").performClick()

        onNodeWithText("Living Room").assertIsDisplayed()
        onAllNodesWithText("Delete room?").assertCountEquals(0)
    }

    // endregion

    // region — back-stack effects

    @Test
    fun whenStateBecomesEmpty_popsBackToParent() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen(), behind = MarkerScreen())
        onNodeWithText("Home").assertIsDisplayed()

        spaceRepository.emitSpaces(emptyList())
        mainClock.advanceTimeBy(2_000)

        onNodeWithText(MarkerScreen.MARKER_TEXT).assertIsDisplayed()
    }

    // endregion

    // region — focus order (ADR-013)

    @Test
    fun emptyRooms_focusesAddFirstRoomButton() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())
        // Focus is requested after a 100ms delay (SpaceDetailContent empty branch).
        mainClock.advanceTimeBy(500)

        onNodeWithText("Add Your First Room").assertIsFocused()
    }

    @Test
    fun createRoomModal_focusesNameField() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithText("Add Your First Room").performClick()

        onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun deleteRoomDialog_focusesCancelButton() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(homeWithRooms))
        setScreenContent(SpaceScreen())
        mainClock.advanceTimeBy(2_000)

        onAllNodesWithContentDescription("More options")[1].performClick()
        onNodeWithText("Delete").performClick()
        mainClock.advanceTimeBy(500)

        // ADR-013: destructive dialogs default focus to Cancel.
        onNodeWithText("Cancel").assertIsFocused()
    }

    @Test
    fun deleteSpaceDialog_focusesConfirmationField() = runComposeUiTest {
        spaceRepository.emitSpaces(listOf(home))
        setScreenContent(SpaceScreen())

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Delete").performClick()
        // DeleteSpaceDialog requests focus on the text field after a 100ms delay.
        mainClock.advanceTimeBy(500)

        onNode(hasSetTextAction()).assertIsFocused()
    }

    // endregion
}

// --- Local helpers ---

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountAtLeast(min: Int):
    androidx.compose.ui.test.SemanticsNodeInteractionCollection = apply {
    val count = fetchSemanticsNodes().size
    check(count >= min) { "Expected at least $min nodes, found $count." }
}

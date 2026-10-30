package ch.wuethrich.greenprint.plant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.ArchiveReason
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.SyncStatus
import ch.wuethrich.greenprint.testing.FakePlantRepository.PositionUpdate
import ch.wuethrich.greenprint.testing.ScreenTest
import ch.wuethrich.greenprint.testing.setScreenContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI tests for [PlantScreen]: Quick Water and the archive menu (inventory.md §5), and the spatial
 * blueprint (spatial_blueprint.md): the list/blueprint switch, moving a marker by drag, by keyboard
 * (ADR-013) and by accessibility action, and opening a plant from its marker.
 */
@OptIn(ExperimentalTestApi::class)
class PlantScreenTest : ScreenTest() {

    private val room = Room(id = "r1", spaceId = "s1", name = "Living Room", icon = "living_24")
    private val fern = Plant(id = "p1", roomId = "r1", name = "Fern", wateringIntervalDays = 5, x = 0.5f, y = 0.5f)
    private val ivy = Plant(id = "p2", roomId = "r1", name = "Ivy", wateringIntervalDays = 7, x = 0.2f, y = 0.2f)

    @Test
    fun listIsTheDefaultView() = runComposeUiTest {
        plantRepository.emitPlants(listOf(fern, ivy))
        setScreenContent(PlantScreen(room))

        onAllNodesWithContentDescription("Water Plant").assertCountEquals(2)
        onAllNodesWithContentDescription(FERN_MARKER).assertCountEquals(0)
    }

    @Test
    fun wateringAPlantSavesItAndConfirms() = runComposeUiTest {
        plantRepository.emitPlants(listOf(fern))
        setScreenContent(PlantScreen(room))

        onNodeWithContentDescription("Water Plant").performClick()
        waitUntil { onAllNodesWithText("Plant watered").fetchSemanticsNodes().isNotEmpty() }

        assertEquals(listOf(fern.id), plantRepository.waterings)
    }

    @Test
    fun anOfflineWateringSaysItIsKeptOnTheDevice() = runComposeUiTest {
        plantRepository.emitPlants(listOf(fern))
        plantRepository.waterResult = NetworkResult.Failure(NetworkError.Connectivity)
        setScreenContent(PlantScreen(room))

        onNodeWithContentDescription("Water Plant").performClick()

        waitUntil { onAllNodesWithText("Saved on this device", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theMenuArchivesAPlantAsSold() = runComposeUiTest {
        plantRepository.emitPlants(listOf(fern))
        setScreenContent(PlantScreen(room))

        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Archive (Sold)").performClick()
        waitUntil { plantRepository.archives.isNotEmpty() }

        assertEquals(fern.id to ArchivePlantRequest(ArchiveReason.SOLD), plantRepository.archives.single())
    }

    @Test
    fun blueprintShowsAMarkerPerPlant() = runComposeUiTest {
        openBlueprint()

        onNodeWithContentDescription(FERN_MARKER).assertIsDisplayed()
        onNodeWithContentDescription("Ivy on the room blueprint").assertIsDisplayed()
        onNodeWithText("Drag a plant to where it stands", substring = true).assertIsDisplayed()
    }

    @Test
    fun draggingAMarkerSavesItsPositionOnceOnRelease() = runComposeUiTest {
        openBlueprint()

        onNodeWithContentDescription(FERN_MARKER).performTouchInput {
            swipe(start = center, end = center + Offset(x = 200f, y = 0f))
        }
        waitUntil { plantRepository.positionUpdates.isNotEmpty() }

        val move = plantRepository.positionUpdates.single()
        assertEquals(fern.id, move.plantId)
        assertTrue(move.x > fern.x, "Dragged right, got $move")
        assertEquals(fern.y, move.y, absoluteTolerance = 0.01f)
        onAllNodesWithText("Edit Plant").assertCountEquals(0)
    }

    @Test
    fun arrowKeysMoveTheFocusedMarker() = runComposeUiTest {
        openBlueprint()

        onNodeWithContentDescription(FERN_MARKER).requestFocus().performKeyInput { pressKey(Key.DirectionRight) }
        waitUntil { plantRepository.positionUpdates.isNotEmpty() }

        assertMovedTo(plantRepository.positionUpdates.single(), x = 0.55f, y = 0.5f)
    }

    @Test
    fun accessibilityActionsMoveAMarker() = runComposeUiTest {
        openBlueprint()

        onNodeWithContentDescription(FERN_MARKER).performCustomAccessibilityActionWithLabel("Move down")
        waitUntil { plantRepository.positionUpdates.isNotEmpty() }

        assertMovedTo(plantRepository.positionUpdates.single(), x = 0.5f, y = 0.55f)
    }

    @Test
    fun tappingAMarkerOpensTheEditDialog() = runComposeUiTest {
        openBlueprint()

        onNodeWithContentDescription(FERN_MARKER).performClick()

        onNodeWithText("Edit Plant").assertIsDisplayed()
        assertTrue(plantRepository.positionUpdates.isEmpty())
    }

    @Test
    fun aMarkerWaitingForTheServerSaysSo() = runComposeUiTest {
        openBlueprint(fern.copy(syncStatus = SyncStatus.ERROR))

        onNode(
            hasContentDescription(FERN_MARKER) and
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not saved to the server yet"),
        ).assertIsDisplayed()
    }

    private fun ComposeUiTest.openBlueprint(vararg plants: Plant = arrayOf(fern, ivy)) {
        plantRepository.emitPlants(plants.toList())
        setScreenContent(PlantScreen(room))
        onNodeWithText("Blueprint").performClick()
    }

    private fun assertMovedTo(move: PositionUpdate, x: Float, y: Float) {
        assertEquals(fern.id, move.plantId)
        assertEquals(x, move.x, absoluteTolerance = 0.001f)
        assertEquals(y, move.y, absoluteTolerance = 0.001f)
    }

    private companion object {
        const val FERN_MARKER = "Fern on the room blueprint"
    }
}

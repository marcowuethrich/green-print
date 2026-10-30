package ch.wuethrich.greenprint.plant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.SyncStatus
import ch.wuethrich.greenprint.plant.BlueprintLayout
import ch.wuethrich.greenprint.plant.BlueprintPosition
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.plant_blueprint_marker
import greenprint.composeapp.generated.resources.plant_blueprint_move_down
import greenprint.composeapp.generated.resources.plant_blueprint_move_left
import greenprint.composeapp.generated.resources.plant_blueprint_move_right
import greenprint.composeapp.generated.resources.plant_blueprint_move_up
import greenprint.composeapp.generated.resources.plant_blueprint_not_synced
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Rooms are drawn wider than tall; the coordinates stay normalized either way (ADR-008). */
private const val BLUEPRINT_ASPECT_RATIO = 4f / 3f

/** Grid lines split the floor into this many cells per side, to help place plants by eye. */
private const val GRID_CELLS = 4

/** The four ways a marker moves without dragging: arrow keys and accessibility actions. */
private enum class Nudge(val dx: Float, val dy: Float) {
    LEFT(-BlueprintLayout.NUDGE_STEP, 0f),
    RIGHT(BlueprintLayout.NUDGE_STEP, 0f),
    UP(0f, -BlueprintLayout.NUDGE_STEP),
    DOWN(0f, BlueprintLayout.NUDGE_STEP),
}

/**
 * The room seen from above (spatial_blueprint.md §1): every plant is a marker at its normalized
 * (x, y). A marker is dragged to move it and the move is saved when it is let go; tapping it opens
 * the plant. With the keyboard, Tab reaches a marker and the arrow keys move it (ADR-013); screen
 * readers get the same moves as custom actions.
 */
@Composable
fun RoomBlueprint(
    plants: List<Plant>,
    onMove: (plant: Plant, x: Float, y: Float) -> Unit,
    onPlantClick: (Plant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val shape = MaterialTheme.shapes.large
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BLUEPRINT_ASPECT_RATIO)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(DesignSystem.CardBorder, gridColor, shape)
            .drawBehind { drawGrid(gridColor) },
    ) {
        val area = IntSize(constraints.maxWidth, constraints.maxHeight)
        plants.forEach { plant ->
            key(plant.id) {
                BlueprintMarker(
                    plant = plant,
                    area = area,
                    onMove = { x, y -> onMove(plant, x, y) },
                    onClick = { onPlantClick(plant) },
                )
            }
        }
    }
}

@Composable
private fun BlueprintMarker(
    plant: Plant,
    area: IntSize,
    onMove: (x: Float, y: Float) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Where the marker shows while it is dragged, and after a move until the plant comes back with
    // its new position; otherwise the marker would jump back for a frame.
    var pending by remember { mutableStateOf<BlueprintPosition?>(null) }
    LaunchedEffect(plant.x, plant.y) { pending = null }
    val position = pending ?: BlueprintPosition(plant.x, plant.y)
    val currentPosition by rememberUpdatedState(position)
    val currentOnMove by rememberUpdatedState(onMove)
    var focused by remember { mutableStateOf(false) }

    val moveTo = { target: BlueprintPosition ->
        val clamped = target.clamped()
        pending = clamped
        currentOnMove(clamped.x, clamped.y)
    }
    val nudge = { step: Nudge -> moveTo(BlueprintPosition(currentPosition.x + step.dx, currentPosition.y + step.dy)) }

    val isSynced = plant.syncStatus == SyncStatus.SYNCED
    val description = stringResource(Res.string.plant_blueprint_marker, plant.name)
    val notSynced = stringResource(Res.string.plant_blueprint_not_synced)
    val nudgeLabels = Nudge.entries.associateWith { it.label() }

    MarkerChip(
        name = plant.name,
        isSynced = isSynced,
        focused = focused,
        onClick = onClick,
        modifier = modifier
            .placeAt(position, area)
            .dragToMove(
                area = area,
                start = { currentPosition },
                onDrag = { pending = it },
                onDragEnd = moveTo,
                onDragCancel = { pending = null },
            ).onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                val step = event.nudge() ?: return@onKeyEvent false
                if (event.type == KeyEventType.KeyDown) nudge(step)
                true
            }.semantics {
                contentDescription = description
                if (!isSynced) stateDescription = notSynced
                customActions = Nudge.entries.map { step ->
                    CustomAccessibilityAction(nudgeLabels.getValue(step)) {
                        nudge(step)
                        true
                    }
                }
            },
    )
}

/** The marker itself: a pill with the plant's name; a dot while its position waits for the server. */
@Composable
private fun MarkerChip(
    name: String,
    isSynced: Boolean,
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.primaryContainer)
            .border(
                width = if (focused) DesignSystem.StrokeBold else DesignSystem.StrokeThin,
                color = if (focused) colors.primary else colors.outline,
                shape = CircleShape,
            ).clickable(onClick = onClick)
            .heightIn(min = DesignSystem.Space7)
            .padding(horizontal = DesignSystem.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSystem.Space2),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_leaf),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(DesignSystem.Space5),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = DesignSystem.Space8 + DesignSystem.Space6),
        )
        if (!isSynced) {
            Box(modifier = Modifier.size(DesignSystem.Space2).background(DesignSystem.Warning, CircleShape))
        }
    }
}

/**
 * Drags the marker across [area]. [onDrag] gets every position on the way, [onDragEnd] the one
 * where it was let go: only that one is saved.
 */
private fun Modifier.dragToMove(
    area: IntSize,
    start: () -> BlueprintPosition,
    onDrag: (BlueprintPosition) -> Unit,
    onDragEnd: (BlueprintPosition) -> Unit,
    onDragCancel: () -> Unit,
): Modifier = pointerInput(area) {
    var dragged = start()
    detectDragGestures(
        onDragStart = { dragged = start() },
        onDragEnd = { onDragEnd(dragged) },
        onDragCancel = onDragCancel,
    ) { change, drag ->
        change.consume()
        dragged = BlueprintPosition(dragged.x + drag.x / area.width, dragged.y + drag.y / area.height).clamped()
        onDrag(dragged)
    }
}

/** Centres the marker on [position], kept fully inside the room. */
private fun Modifier.placeAt(position: BlueprintPosition, area: IntSize): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        val left = (position.x * area.width - placeable.width / 2f).roundToInt()
        val top = (position.y * area.height - placeable.height / 2f).roundToInt()
        placeable.place(
            left.coerceIn(0, (area.width - placeable.width).coerceAtLeast(0)),
            top.coerceIn(0, (area.height - placeable.height).coerceAtLeast(0)),
        )
    }
}

private fun DrawScope.drawGrid(color: Color) {
    val stroke = DesignSystem.StrokeThin.toPx()
    for (line in 1 until GRID_CELLS) {
        val x = size.width * line / GRID_CELLS
        val y = size.height * line / GRID_CELLS
        drawLine(color, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = stroke)
        drawLine(color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = stroke)
    }
}

private fun KeyEvent.nudge(): Nudge? = when (key) {
    Key.DirectionLeft -> Nudge.LEFT
    Key.DirectionRight -> Nudge.RIGHT
    Key.DirectionUp -> Nudge.UP
    Key.DirectionDown -> Nudge.DOWN
    else -> null
}

@Composable
private fun Nudge.label(): String = stringResource(
    when (this) {
        Nudge.LEFT -> Res.string.plant_blueprint_move_left
        Nudge.RIGHT -> Res.string.plant_blueprint_move_right
        Nudge.UP -> Res.string.plant_blueprint_move_up
        Nudge.DOWN -> Res.string.plant_blueprint_move_down
    },
)

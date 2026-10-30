package ch.wuethrich.greenprint.plant

import kotlin.math.hypot

/** A spot on a room's blueprint, in normalized coordinates (0.0..1.0 on both axes, ADR-008). */
data class BlueprintPosition(val x: Float, val y: Float) {
    /** The same spot, kept inside the room. */
    fun clamped(): BlueprintPosition = BlueprintPosition(x.coerceIn(MIN, MAX), y.coerceIn(MIN, MAX))

    fun distanceTo(other: BlueprintPosition): Float = hypot(x - other.x, y - other.y)

    companion object {
        const val MIN = 0f
        const val MAX = 1f
    }
}

/** Placement rules for plants on the blueprint, shared by every client. */
object BlueprintLayout {
    /** How far one arrow-key press or accessibility action moves a plant. */
    const val NUDGE_STEP = 0.05f

    private const val GRID_CELLS = 5

    private val center = BlueprintPosition(0.5f, 0.5f)

    // The centres of a GRID_CELLS × GRID_CELLS grid, nearest the room's centre first, so a room's
    // first plant lands in the middle and ties go to the more central slot.
    private val slots: List<BlueprintPosition> =
        (0 until GRID_CELLS)
            .flatMap { row ->
                (0 until GRID_CELLS).map { column ->
                    BlueprintPosition((column + 0.5f) / GRID_CELLS, (row + 0.5f) / GRID_CELLS)
                }
            }.sortedBy { it.distanceTo(center) }

    /** Where a new plant goes: the grid slot farthest from every plant already in the room. */
    fun nextFreePosition(occupied: List<BlueprintPosition>): BlueprintPosition {
        if (occupied.isEmpty()) return slots.first()
        return slots.maxBy { slot -> occupied.minOf { slot.distanceTo(it) } }
    }
}

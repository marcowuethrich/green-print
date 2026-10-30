package ch.wuethrich.greenprint.plant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlueprintLayoutTest {

    @Test
    fun `the first plant of a room goes to its centre`() {
        assertEquals(BlueprintPosition(0.5f, 0.5f), BlueprintLayout.nextFreePosition(emptyList()))
    }

    @Test
    fun `a new plant keeps its distance from the plants already placed`() {
        val occupied = listOf(BlueprintPosition(0.5f, 0.5f))

        val next = BlueprintLayout.nextFreePosition(occupied)

        assertTrue(next.distanceTo(occupied.single()) > 0.5f, "Expected a far spot, got $next")
    }

    @Test
    fun `every new plant gets a spot of its own`() {
        val placed = mutableListOf<BlueprintPosition>()
        repeat(times = 10) { placed += BlueprintLayout.nextFreePosition(placed) }

        assertEquals(placed.size, placed.toSet().size)
        assertTrue(placed.all { it == it.clamped() }, "Every spot lies inside the room: $placed")
    }

    @Test
    fun `clamping keeps a position inside the room`() {
        assertEquals(BlueprintPosition(1f, 0f), BlueprintPosition(1.4f, -0.3f).clamped())
        assertEquals(BlueprintPosition(0.2f, 0.8f), BlueprintPosition(0.2f, 0.8f).clamped())
    }
}

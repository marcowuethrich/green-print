package ch.wuethrich.greenprint.plant

import ch.wuethrich.greenprint.model.Plant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

object PlantCareUtils {
    fun calculateHydration(plant: Plant): Float {
        val lastWatered = plant.lastWateredDate?.let { Instant.parse(it) } ?: return 0f
        val interval = plant.wateringIntervalDays.days
        val now = Clock.System.now()
        
        val elapsed = now - lastWatered
        val remaining = interval - elapsed
        
        return (remaining.inWholeMilliseconds.toFloat() / interval.inWholeMilliseconds.toFloat()).coerceIn(0f, 1f)
    }

    fun getBarometerColor(hydration: Float): Long {
        return when {
            hydration > 0.7f -> 0xFF2196F3 // Blue
            hydration > 0.3f -> 0xFFFFC107 // Yellow
            else -> 0xFFF44336 // Red
        }
    }
}
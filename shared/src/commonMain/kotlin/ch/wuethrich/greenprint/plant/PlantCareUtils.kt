package ch.wuethrich.greenprint.plant

import ch.wuethrich.greenprint.model.Plant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** How thirsty a plant is (ADR-007): the clients map each level to a theme colour. */
enum class HydrationLevel { HEALTHY, THIRSTY, DRY }

object PlantCareUtils {
    private const val HEALTHY_ABOVE = 0.7f
    private const val THIRSTY_ABOVE = 0.3f

    /** The share of the watering interval still left: 1 just watered, 0 due or never watered. */
    fun calculateHydration(plant: Plant, now: Instant = Clock.System.now()): Float {
        val lastWatered = plant.lastWateredDate?.let { Instant.parse(it) } ?: return 0f
        val interval = plant.wateringIntervalDays.days
        val remaining = interval - (now - lastWatered)
        return (remaining.inWholeMilliseconds.toFloat() / interval.inWholeMilliseconds.toFloat()).coerceIn(0f, 1f)
    }

    fun hydrationLevel(hydration: Float): HydrationLevel = when {
        hydration > HEALTHY_ABOVE -> HydrationLevel.HEALTHY
        hydration > THIRSTY_ABOVE -> HydrationLevel.THIRSTY
        else -> HydrationLevel.DRY
    }
}

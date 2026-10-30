package ch.wuethrich.greenprint.plant

/** How often a plant may be watered, in days (inventory.md §2); the server enforces the same range. */
object WateringInterval {
    const val MIN_DAYS = 1
    const val MAX_DAYS = 365

    /** What the form falls back to when its field holds no number. */
    const val DEFAULT_DAYS = 7

    val range: IntRange = MIN_DAYS..MAX_DAYS
}

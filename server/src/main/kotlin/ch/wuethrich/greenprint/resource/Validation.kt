package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.ApiError
import ch.wuethrich.greenprint.model.ArchiveReason
import ch.wuethrich.greenprint.plant.BlueprintPosition
import ch.wuethrich.greenprint.plant.WateringInterval
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

private const val NAME_MAX_LENGTH = 128

class ValidationException(val error: ApiError) : RuntimeException(error.code)

fun validateName(raw: String, field: String = "name"): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        throw ValidationException(ApiError(code = "name_blank", field = field))
    }
    if (trimmed.length > NAME_MAX_LENGTH) {
        throw ValidationException(ApiError(code = "name_too_long", field = field))
    }
    return trimmed
}

/** A blueprint coordinate is normalized (ADR-008): 0.0..1.0 with both edges allowed; NaN never is. */
fun validatePosition(value: Float, field: String): Float {
    if (value !in BlueprintPosition.MIN..BlueprintPosition.MAX) {
        throw ValidationException(ApiError(code = "position_out_of_range", field = field))
    }
    return value
}

/** A watering interval is a whole number of days in [WateringInterval.range] (inventory.md §2). */
fun validateInterval(days: Int, field: String = "wateringIntervalDays"): Int {
    if (days !in WateringInterval.range) {
        throw ValidationException(ApiError(code = "interval_out_of_range", field = field))
    }
    return days
}

/** An archive reason is one of [ArchiveReason.ALL] (ADR-009). */
fun validateArchiveReason(reason: String, field: String = "reason"): String {
    if (reason !in ArchiveReason.ALL) {
        throw ValidationException(ApiError(code = "archive_reason_invalid", field = field))
    }
    return reason
}

/** A watering's id is the UUID the client picked for it. */
fun validateWateringId(raw: String, field: String = "id"): UUID = try {
    UUID.fromString(raw)
} catch (_: IllegalArgumentException) {
    throw ValidationException(ApiError(code = "watering_id_invalid", field = field))
}

/**
 * A watering's time is an ISO-8601 instant that is not in the future; [clockSkew] allows for a
 * device clock that runs a little ahead.
 */
fun validateWateredAt(raw: String, now: Instant, field: String = "timestamp", clockSkew: Duration = MAX_CLOCK_SKEW): Instant {
    val wateredAt = try {
        Instant.parse(raw)
    } catch (_: DateTimeParseException) {
        throw ValidationException(ApiError(code = "timestamp_invalid", field = field))
    }
    if (wateredAt.isAfter(now.plus(clockSkew))) {
        throw ValidationException(ApiError(code = "timestamp_invalid", field = field))
    }
    return wateredAt
}

private val MAX_CLOCK_SKEW: Duration = Duration.ofMinutes(5)

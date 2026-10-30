package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.ApiError
import ch.wuethrich.greenprint.plant.BlueprintPosition

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

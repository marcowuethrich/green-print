package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.ApiError

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

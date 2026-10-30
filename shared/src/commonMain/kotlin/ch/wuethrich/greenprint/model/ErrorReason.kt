package ch.wuethrich.greenprint.model

/**
 * Why an action failed, as the screens name it. The ViewModels report a reason, and the UI maps each
 * one to a string resource (ADR-012), so `shared` holds no user-facing text.
 */
enum class ErrorReason {
    SESSION_EXPIRED,
    FORBIDDEN,
    NOT_FOUND,
    ALREADY_EXISTS,
    SPACE_NAME_TAKEN,
    ROOM_NAME_TAKEN,
    PLANT_NAME_TAKEN,
    SERVER,
    CONNECTIVITY,
    NAME_BLANK,
    NAME_TOO_LONG,
    INTERVAL_OUT_OF_RANGE,
    POSITION_OUT_OF_RANGE,
    INVALID_INPUT,
    UNKNOWN,
}

/** The reason to show for this error; [conflict] names what a `409` means for the action at hand. */
fun NetworkError.toErrorReason(conflict: ErrorReason = ErrorReason.ALREADY_EXISTS): ErrorReason = when (this) {
    NetworkError.Unauthorized -> ErrorReason.SESSION_EXPIRED
    NetworkError.Forbidden -> ErrorReason.FORBIDDEN
    NetworkError.NotFound -> ErrorReason.NOT_FOUND
    NetworkError.Conflict -> conflict
    NetworkError.ServerError -> ErrorReason.SERVER
    NetworkError.Connectivity -> ErrorReason.CONNECTIVITY
    is NetworkError.Validation -> validationReason(code)
    is NetworkError.Unknown -> ErrorReason.UNKNOWN
}

// The codes the server's Validation.kt answers with.
private fun validationReason(code: String): ErrorReason = when (code) {
    "name_blank" -> ErrorReason.NAME_BLANK
    "name_too_long" -> ErrorReason.NAME_TOO_LONG
    "interval_out_of_range" -> ErrorReason.INTERVAL_OUT_OF_RANGE
    "position_out_of_range" -> ErrorReason.POSITION_OUT_OF_RANGE
    else -> ErrorReason.INVALID_INPUT
}

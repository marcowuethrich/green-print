package ch.wuethrich.greenprint.ui

import ch.wuethrich.greenprint.model.ErrorReason
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.error_already_exists
import greenprint.composeapp.generated.resources.error_forbidden
import greenprint.composeapp.generated.resources.error_generic
import greenprint.composeapp.generated.resources.error_interval_out_of_range
import greenprint.composeapp.generated.resources.error_invalid_input
import greenprint.composeapp.generated.resources.error_name_blank
import greenprint.composeapp.generated.resources.error_name_too_long
import greenprint.composeapp.generated.resources.error_network
import greenprint.composeapp.generated.resources.error_not_found
import greenprint.composeapp.generated.resources.error_position_out_of_range
import greenprint.composeapp.generated.resources.error_server
import greenprint.composeapp.generated.resources.error_session_expired
import greenprint.composeapp.generated.resources.plant_name_taken
import greenprint.composeapp.generated.resources.room_name_taken
import greenprint.composeapp.generated.resources.space_name_taken
import org.jetbrains.compose.resources.StringResource

/** The text the UI shows for a failure the ViewModels report (ADR-012). */
val ErrorReason.text: StringResource
    get() = when (this) {
        ErrorReason.SESSION_EXPIRED -> Res.string.error_session_expired
        ErrorReason.FORBIDDEN -> Res.string.error_forbidden
        ErrorReason.NOT_FOUND -> Res.string.error_not_found
        ErrorReason.ALREADY_EXISTS -> Res.string.error_already_exists
        ErrorReason.SPACE_NAME_TAKEN -> Res.string.space_name_taken
        ErrorReason.ROOM_NAME_TAKEN -> Res.string.room_name_taken
        ErrorReason.PLANT_NAME_TAKEN -> Res.string.plant_name_taken
        ErrorReason.SERVER -> Res.string.error_server
        ErrorReason.CONNECTIVITY -> Res.string.error_network
        ErrorReason.NAME_BLANK -> Res.string.error_name_blank
        ErrorReason.NAME_TOO_LONG -> Res.string.error_name_too_long
        ErrorReason.INTERVAL_OUT_OF_RANGE -> Res.string.error_interval_out_of_range
        ErrorReason.POSITION_OUT_OF_RANGE -> Res.string.error_position_out_of_range
        ErrorReason.INVALID_INPUT -> Res.string.error_invalid_input
        ErrorReason.UNKNOWN -> Res.string.error_generic
    }

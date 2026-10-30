package ch.wuethrich.greenprint.space.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.CancelDeleteButtons
import ch.wuethrich.greenprint.ui.components.DeleteConfirmationDialog
import ch.wuethrich.greenprint.ui.components.DialogTitle
import ch.wuethrich.greenprint.ui.components.GenericFormModal
import ch.wuethrich.greenprint.ui.components.RequestInitialFocus
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_icon_picker_label
import greenprint.composeapp.generated.resources.common_ok
import greenprint.composeapp.generated.resources.room_add_dialog_title
import greenprint.composeapp.generated.resources.room_delete_dialog_message
import greenprint.composeapp.generated.resources.room_delete_dialog_title
import greenprint.composeapp.generated.resources.room_name_hint
import greenprint.composeapp.generated.resources.room_not_empty_message
import greenprint.composeapp.generated.resources.room_not_empty_title
import greenprint.composeapp.generated.resources.room_rename_dialog_title
import greenprint.composeapp.generated.resources.space_delete_dialog_confirm_hint
import greenprint.composeapp.generated.resources.space_delete_dialog_message
import greenprint.composeapp.generated.resources.space_delete_dialog_title
import greenprint.composeapp.generated.resources.space_name_hint
import greenprint.composeapp.generated.resources.space_rename_dialog_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateRoomModal(
    error: String?,
    onConfirm: (name: String, icon: String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    GenericFormModal(
        title = stringResource(Res.string.room_add_dialog_title),
        nameLabel = stringResource(Res.string.room_name_hint),
        availableIcons = IconRegistry.roomIcons,
        iconLabel = stringResource(Res.string.common_icon_picker_label),
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError,
    )
}

@Composable
fun RenameRoomDialog(
    room: Room,
    error: String?,
    onConfirm: (name: String, icon: String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    GenericFormModal(
        title = stringResource(Res.string.room_rename_dialog_title),
        nameLabel = stringResource(Res.string.room_name_hint),
        initialName = room.name,
        initialIcon = room.icon,
        availableIcons = IconRegistry.roomIcons,
        iconLabel = stringResource(Res.string.common_icon_picker_label),
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError,
    )
}

@Composable
fun DeleteRoomDialog(room: Room, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DeleteConfirmationDialog(
        title = stringResource(Res.string.room_delete_dialog_title),
        message = stringResource(Res.string.room_delete_dialog_message, room.name),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
fun RoomNotEmptyDialog(roomName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.room_not_empty_title)) },
        text = { Text(stringResource(Res.string.room_not_empty_message, roomName)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_ok))
            }
        },
    )
}

@Composable
fun RenameSpaceDialog(
    space: Space,
    error: String?,
    onConfirm: (name: String, icon: String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    GenericFormModal(
        title = stringResource(Res.string.space_rename_dialog_title),
        nameLabel = stringResource(Res.string.space_name_hint),
        initialName = space.name,
        initialIcon = space.icon,
        availableIcons = IconRegistry.spaceIcons,
        iconLabel = stringResource(Res.string.common_icon_picker_label),
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError,
    )
}

/** Deleting a space takes everything in it, so the user types its name to confirm. */
@Composable
fun DeleteSpaceDialog(spaceName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var confirmationText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    RequestInitialFocus(focusRequester)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { DialogTitle(stringResource(Res.string.space_delete_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.Space4)) {
                Text(stringResource(Res.string.space_delete_dialog_message))
                Text(
                    text = stringResource(Res.string.space_delete_dialog_confirm_hint, spaceName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    placeholder = { Text(spaceName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
        confirmButton = {
            CancelDeleteButtons(
                onCancel = onDismiss,
                onDelete = onConfirm,
                deleteEnabled = confirmationText == spaceName,
            )
        },
        dismissButton = null,
    )
}

package ch.wuethrich.greenprint.space.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.DeleteConfirmationDialog
import ch.wuethrich.greenprint.ui.components.GenericFormModal
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import ch.wuethrich.greenprint.ui.theme.LocalAnimationsEnabled
import greenprint.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CreateRoomModal(
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    GenericFormModal(
        title = stringResource(Res.string.room_add_dialog_title),
        nameLabel = stringResource(Res.string.room_name_hint),
        availableIcons = IconRegistry.roomIcons,
        iconLabel = stringResource(Res.string.room_icon_picker_label),
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError
    )
}

@Composable
fun RenameRoomDialog(
    currentName: String,
    currentIcon: String?,
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    GenericFormModal(
        title = stringResource(Res.string.room_rename_dialog_title),
        nameLabel = stringResource(Res.string.room_name_hint),
        initialName = currentName,
        initialIcon = currentIcon,
        availableIcons = IconRegistry.roomIcons,
        iconLabel = stringResource(Res.string.room_icon_picker_label),
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError
    )
}

@Composable
fun DeleteRoomDialog(
    room: Room,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DeleteConfirmationDialog(
        title = stringResource(Res.string.room_delete_dialog_title),
        message = "Are you sure you want to delete '${room.name}'?",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun RenameSpaceDialog(
    currentName: String,
    currentIcon: String?,
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    GenericFormModal(
        title = stringResource(Res.string.space_rename_dialog_title),
        nameLabel = stringResource(Res.string.space_name_hint),
        initialName = currentName,
        initialIcon = currentIcon,
        availableIcons = IconRegistry.spaceIcons,
        iconLabel = "Choose an icon (optional)",
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError
    )
}

@Composable
fun DeleteSpaceDialog(
    spaceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmationText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val animationsEnabled = LocalAnimationsEnabled.current

    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            delay(100.milliseconds)
        } else {
            withFrameNanos { }
        }
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(Res.string.space_delete_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(Res.string.space_delete_dialog_message))
                Text(
                    text = "Please type '$spaceName' to confirm deletion:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    placeholder = { Text(spaceName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystem.Space2)) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onConfirm,
                    enabled = confirmationText == spaceName,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(Res.string.common_delete), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null,
    )
}

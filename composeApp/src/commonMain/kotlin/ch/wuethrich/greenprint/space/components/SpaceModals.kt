package ch.wuethrich.greenprint.space.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.ui.IconPicker
import ch.wuethrich.greenprint.ui.IconRegistry
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_cancel
import greenprint.composeapp.generated.resources.common_delete
import greenprint.composeapp.generated.resources.common_save
import greenprint.composeapp.generated.resources.room_add_dialog_title
import greenprint.composeapp.generated.resources.room_delete_dialog_message
import greenprint.composeapp.generated.resources.room_delete_dialog_title
import greenprint.composeapp.generated.resources.room_icon_picker_label
import greenprint.composeapp.generated.resources.room_name_hint
import greenprint.composeapp.generated.resources.room_rename_dialog_title
import greenprint.composeapp.generated.resources.space_delete_dialog_message
import greenprint.composeapp.generated.resources.space_delete_dialog_title
import greenprint.composeapp.generated.resources.space_name_hint
import greenprint.composeapp.generated.resources.space_rename_dialog_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateRoomModal(
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                text = stringResource(Res.string.room_add_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it 
                        onClearError()
                    },
                    label = { Text(stringResource(Res.string.room_name_hint)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) {
                                onConfirm(name, selectedIcon)
                            }
                        }
                    )
                )
                
                IconPicker(
                    label = stringResource(Res.string.room_icon_picker_label),
                    availableIcons = IconRegistry.roomIcons,
                    selectedIcon = selectedIcon,
                    onIconSelected = { 
                        selectedIcon = if (it == selectedIcon) null else it 
                        onClearError()
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedIcon) },
                enabled = name.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(Res.string.common_save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.common_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
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
    var textFieldValue by remember(currentName) {
        mutableStateOf(
            TextFieldValue(
                text = currentName,
                selection = TextRange(0, currentName.length)
            )
        )
    }
    var selectedIcon by remember(currentIcon) { mutableStateOf(currentIcon) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(Res.string.room_rename_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { 
                        textFieldValue = it 
                        onClearError()
                    },
                    label = { Text(stringResource(Res.string.room_name_hint)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (textFieldValue.text.isNotBlank() && (textFieldValue.text != currentName || selectedIcon != currentIcon)) {
                                onConfirm(textFieldValue.text, selectedIcon)
                            }
                        }
                    )
                )
                
                IconPicker(
                    label = stringResource(Res.string.room_icon_picker_label),
                    availableIcons = IconRegistry.roomIcons,
                    selectedIcon = selectedIcon,
                    onIconSelected = { 
                        selectedIcon = if (it == selectedIcon) null else it 
                        onClearError()
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textFieldValue.text, selectedIcon) },
                enabled = textFieldValue.text.isNotBlank() && (textFieldValue.text != currentName || selectedIcon != currentIcon),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(Res.string.common_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
fun DeleteRoomDialog(
    roomName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(Res.string.room_delete_dialog_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(Res.string.room_delete_dialog_message, roomName)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(Res.string.common_delete), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocusRequester)
            ) {
                Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
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
    var textFieldValue by remember(currentName) {
        mutableStateOf(
            TextFieldValue(
                text = currentName,
                selection = TextRange(0, currentName.length)
            )
        )
    }
    var selectedIcon by remember(currentIcon) { mutableStateOf(currentIcon) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(Res.string.space_rename_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { 
                        textFieldValue = it 
                        onClearError()
                    },
                    label = { Text(stringResource(Res.string.space_name_hint)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (textFieldValue.text.isNotBlank() && (textFieldValue.text != currentName || selectedIcon != currentIcon)) {
                                onConfirm(textFieldValue.text, selectedIcon)
                            }
                        }
                    )
                )
                
                IconPicker(
                    label = "Choose an icon (optional)",
                    availableIcons = IconRegistry.spaceIcons,
                    selectedIcon = selectedIcon,
                    onIconSelected = { 
                        selectedIcon = if (it == selectedIcon) null else it 
                        onClearError()
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textFieldValue.text, selectedIcon) },
                enabled = textFieldValue.text.isNotBlank() && (textFieldValue.text != currentName || selectedIcon != currentIcon),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(Res.string.common_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
fun DeleteSpaceDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(Res.string.space_delete_dialog_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(Res.string.space_delete_dialog_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(Res.string.common_delete), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocusRequester)
            ) {
                Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

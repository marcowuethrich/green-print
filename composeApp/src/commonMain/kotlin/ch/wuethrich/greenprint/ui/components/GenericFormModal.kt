package ch.wuethrich.greenprint.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import ch.wuethrich.greenprint.ui.IconPicker
import ch.wuethrich.greenprint.ui.theme.DesignSystem

/** A name (and optional icon) form dialog: creating or renaming a space or a room. */
@Composable
fun GenericFormModal(
    title: String,
    nameLabel: String,
    onConfirm: (name: String, icon: String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    initialName: String = "",
    initialIcon: String? = null,
    availableIcons: List<String> = emptyList(),
    iconLabel: String? = null,
    error: String? = null,
) {
    var name by remember(initialName) { mutableStateOf(TextFieldValue(initialName, TextRange(0, initialName.length))) }
    var selectedIcon by remember(initialIcon) { mutableStateOf(initialIcon) }
    val focusRequester = remember { FocusRequester() }
    RequestInitialFocus(focusRequester)

    val isModified = name.text != initialName || selectedIcon != initialIcon
    val canSave = name.text.isNotBlank() && (initialName.isEmpty() || isModified)
    val save = { if (canSave) onConfirm(name.text, selectedIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { DialogTitle(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.Space4)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onClearError()
                    },
                    label = { Text(nameLabel) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DesignSystem.Space2)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                )
                if (availableIcons.isNotEmpty() && iconLabel != null) {
                    IconPicker(
                        label = iconLabel,
                        availableIcons = availableIcons,
                        selectedIcon = selectedIcon,
                        onIconSelect = {
                            selectedIcon = if (it == selectedIcon) null else it
                            onClearError()
                        },
                    )
                }
            }
        },
        confirmButton = { CancelSaveButtons(onCancel = onDismiss, onSave = save, saveEnabled = canSave) },
        dismissButton = null,
    )
}

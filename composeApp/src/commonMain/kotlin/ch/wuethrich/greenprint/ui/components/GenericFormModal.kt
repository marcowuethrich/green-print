package ch.wuethrich.greenprint.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.ui.IconPicker
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_cancel
import greenprint.composeapp.generated.resources.common_save
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GenericFormModal(
    title: String,
    nameLabel: String,
    initialName: String = "",
    initialIcon: String? = null,
    availableIcons: List<String> = emptyList(),
    iconLabel: String? = null,
    error: String? = null,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    var textFieldValue by remember(initialName) {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, initialName.length)
            )
        )
    }
    var selectedIcon by remember(initialIcon) { mutableStateOf(initialIcon) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        focusRequester.requestFocus()
    }

    val isModified = textFieldValue.text != initialName || selectedIcon != initialIcon
    val canSave = textFieldValue.text.isNotBlank() && (initialName.isEmpty() || isModified)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { 
                        textFieldValue = it 
                        onClearError()
                    },
                    label = { Text(nameLabel) },
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
                            if (canSave) {
                                onConfirm(textFieldValue.text, selectedIcon)
                            }
                        }
                    )
                )
                
                if (availableIcons.isNotEmpty() && iconLabel != null) {
                    IconPicker(
                        label = iconLabel,
                        availableIcons = availableIcons,
                        selectedIcon = selectedIcon,
                        onIconSelected = { 
                            selectedIcon = if (it == selectedIcon) null else it 
                            onClearError()
                        }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.common_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onConfirm(textFieldValue.text, selectedIcon) },
                    enabled = canSave,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(Res.string.common_save),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = null
    )
}
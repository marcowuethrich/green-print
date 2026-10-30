package ch.wuethrich.greenprint.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/** Asks before a permanent delete; focus starts on Cancel, the safe choice (ADR-013). */
@Composable
fun DeleteConfirmationDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val cancelFocusRequester = remember { FocusRequester() }
    RequestInitialFocus(cancelFocusRequester)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { DialogTitle(title) },
        text = { Text(message) },
        confirmButton = {
            CancelDeleteButtons(
                onCancel = onDismiss,
                onDelete = onConfirm,
                cancelModifier = Modifier.focusRequester(cancelFocusRequester),
            )
        },
        dismissButton = null,
    )
}

package ch.wuethrich.greenprint.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import ch.wuethrich.greenprint.ui.theme.LocalAnimationsEnabled
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_cancel
import greenprint.composeapp.generated.resources.common_delete
import greenprint.composeapp.generated.resources.common_save
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/**
 * Moves keyboard focus into a dialog once it is on screen (ADR-013): after its entrance
 * animation, or on the next frame when animations are off.
 */
@Composable
fun RequestInitialFocus(focusRequester: FocusRequester) {
    val animationsEnabled = LocalAnimationsEnabled.current
    LaunchedEffect(focusRequester) {
        if (animationsEnabled) {
            delay(DesignSystem.Motion.DURATION_INSTANT_MS.milliseconds)
        } else {
            withFrameNanos { }
        }
        focusRequester.requestFocus()
    }
}

@Composable
fun DialogTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

/** The confirm slot of every form dialog: Cancel, then the primary Save. */
@Composable
fun CancelSaveButtons(onCancel: () -> Unit, onSave: () -> Unit, saveEnabled: Boolean, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystem.Space2), modifier = modifier) {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onSave, enabled = saveEnabled, shape = MaterialTheme.shapes.medium) {
            Text(text = stringResource(Res.string.common_save), fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The confirm slot of every delete dialog: Cancel, then the destructive Delete. Cancel takes
 * [cancelModifier], so a dialog can put the initial focus on the safe choice.
 */
@Composable
fun CancelDeleteButtons(
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cancelModifier: Modifier = Modifier,
    deleteEnabled: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystem.Space2), modifier = modifier) {
        TextButton(onClick = onCancel, modifier = cancelModifier) {
            Text(text = stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onDelete,
            enabled = deleteEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text = stringResource(Res.string.common_delete), fontWeight = FontWeight.Bold)
        }
    }
}

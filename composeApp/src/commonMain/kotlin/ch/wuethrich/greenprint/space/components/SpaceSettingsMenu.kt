package ch.wuethrich.greenprint.space.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.wuethrich.greenprint.ui.components.DeleteMenuItem
import ch.wuethrich.greenprint.ui.components.MoreOptionsMenu
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_rename
import greenprint.composeapp.generated.resources.space_leave_action
import org.jetbrains.compose.resources.stringResource

@Composable
fun SpaceSettingsMenu(
    onLeave: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoreOptionsMenu(modifier = modifier) { onDismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.space_leave_action)) },
            onClick = {
                onDismiss()
                onLeave()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.common_rename)) },
            onClick = {
                onDismiss()
                onRenameClick()
            }
        )
        DeleteMenuItem(
            onDismiss = onDismiss,
            onClick = onDeleteClick
        )
    }
}

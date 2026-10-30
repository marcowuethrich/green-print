package ch.wuethrich.greenprint.space.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.wuethrich.greenprint.ui.components.DeleteMenuItem
import ch.wuethrich.greenprint.ui.components.MenuAction
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
    modifier: Modifier = Modifier,
) {
    MoreOptionsMenu(modifier = modifier) { onDismiss ->
        MenuAction(label = stringResource(Res.string.space_leave_action), onDismiss = onDismiss, onClick = onLeave)
        MenuAction(label = stringResource(Res.string.common_rename), onDismiss = onDismiss, onClick = onRenameClick)
        DeleteMenuItem(onDismiss = onDismiss, onClick = onDeleteClick)
    }
}

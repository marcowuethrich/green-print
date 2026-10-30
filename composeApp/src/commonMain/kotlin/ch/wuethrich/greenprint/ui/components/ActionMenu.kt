package ch.wuethrich.greenprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_delete
import greenprint.composeapp.generated.resources.common_more_options
import greenprint.composeapp.generated.resources.ic_more_vert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MoreOptionsMenu(modifier: Modifier = Modifier, content: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_vert),
                contentDescription = stringResource(Res.string.common_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            content { expanded = false }
        }
    }
}

/** A menu entry that closes the menu, then runs [onClick]. */
@Composable
fun MenuAction(label: String, onDismiss: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onDismiss()
            onClick()
        },
        modifier = modifier,
    )
}

@Composable
fun DeleteMenuItem(
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.common_delete),
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onDismiss()
            onClick()
        },
        modifier = modifier,
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error,
        ),
    )
}

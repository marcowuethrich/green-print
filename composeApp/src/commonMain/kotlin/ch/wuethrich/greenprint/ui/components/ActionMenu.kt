package ch.wuethrich.greenprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import greenprint.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MoreOptionsMenu(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_vert),
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            content { expanded = false }
        }
    }
}

@Composable
fun ColumnScope.DeleteMenuItem(
    label: String = stringResource(Res.string.common_delete),
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onDismiss()
            onClick()
        },
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error
        )
    )
}

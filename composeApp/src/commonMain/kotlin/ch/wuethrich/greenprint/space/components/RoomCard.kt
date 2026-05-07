package ch.wuethrich.greenprint.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_delete
import greenprint.composeapp.generated.resources.common_rename
import greenprint.composeapp.generated.resources.ic_more_vert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RoomCard(
    room: Room,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = DesignSystem.CardElevation,
            hoveredElevation = DesignSystem.CardElevationHovered,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconRegistry.getPainter(room.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_more_vert),
                        contentDescription = "Room Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_rename)) },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_delete)) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

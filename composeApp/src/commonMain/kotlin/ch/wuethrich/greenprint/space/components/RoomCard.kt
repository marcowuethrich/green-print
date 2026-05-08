package ch.wuethrich.greenprint.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.DeleteMenuItem
import ch.wuethrich.greenprint.ui.components.MoreOptionsMenu
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_rename
import org.jetbrains.compose.resources.stringResource

@Composable
fun RoomCard(
    room: Room,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
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
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconRegistry.getPainter(room.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(DesignSystem.Space4))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            MoreOptionsMenu { onDismiss ->
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
    }
}

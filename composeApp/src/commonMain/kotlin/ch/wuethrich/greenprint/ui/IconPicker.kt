package ch.wuethrich.greenprint.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.ui.theme.DesignSystem

@Composable
fun IconPicker(
    label: String,
    availableIcons: List<String>,
    selectedIcon: String?,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DesignSystem.Space2)
        )
        Spacer(modifier = Modifier.height(DesignSystem.Space2))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.Space4),
            contentPadding = PaddingValues(horizontal = DesignSystem.Space2)
        ) {
            itemsIndexed(availableIcons) { _, iconId ->
                val isSelected = iconId == selectedIcon
                
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(durationMillis = 200)
                )
                
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200)
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(durationMillis = 200)
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable { onIconSelected(iconId) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = IconRegistry.getPainter(iconId),
                        contentDescription = iconId,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

package ch.wuethrich.greenprint.plant.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.plant.HydrationLevel
import ch.wuethrich.greenprint.plant.PlantCareUtils
import ch.wuethrich.greenprint.ui.components.DeleteMenuItem
import ch.wuethrich.greenprint.ui.components.MenuAction
import ch.wuethrich.greenprint.ui.components.MoreOptionsMenu
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_edit
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.plant_archive_died
import greenprint.composeapp.generated.resources.plant_archive_gifted
import greenprint.composeapp.generated.resources.plant_delete
import greenprint.composeapp.generated.resources.plant_water_action
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The archive reasons the plant menu offers (inventory.md §5). */
private const val ARCHIVE_DIED = "DIED"
private const val ARCHIVE_GIFTED = "GIFTED"

@Composable
fun PlantItem(
    plant: Plant,
    onWaterClick: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: (reason: String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = DesignSystem.CardElevation,
            hoveredElevation = DesignSystem.CardElevationHovered,
        ),
    ) {
        Column(modifier = Modifier.padding(DesignSystem.Space4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.name, style = MaterialTheme.typography.titleMedium)
                    plant.species?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onWaterClick) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_leaf),
                        contentDescription = stringResource(Res.string.plant_water_action),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                PlantActionsMenu(
                    onEditClick = onEditClick,
                    onArchiveClick = onArchiveClick,
                    onDeleteClick = onDeleteClick,
                )
            }
            Spacer(modifier = Modifier.height(DesignSystem.Space3))
            HydrationBar(hydration = PlantCareUtils.calculateHydration(plant))
        }
    }
}

@Composable
private fun PlantActionsMenu(
    onEditClick: () -> Unit,
    onArchiveClick: (reason: String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoreOptionsMenu(modifier = modifier) { onDismiss ->
        MenuAction(label = stringResource(Res.string.common_edit), onDismiss = onDismiss, onClick = onEditClick)
        MenuAction(
            label = stringResource(Res.string.plant_archive_died),
            onDismiss = onDismiss,
            onClick = { onArchiveClick(ARCHIVE_DIED) },
        )
        MenuAction(
            label = stringResource(Res.string.plant_archive_gifted),
            onDismiss = onDismiss,
            onClick = { onArchiveClick(ARCHIVE_GIFTED) },
        )
        DeleteMenuItem(
            label = stringResource(Res.string.plant_delete),
            onDismiss = onDismiss,
            onClick = onDeleteClick,
        )
    }
}

/** How much of the watering interval is left, coloured by [HydrationLevel] (inventory.md). */
@Composable
private fun HydrationBar(hydration: Float, modifier: Modifier = Modifier) {
    val color = PlantCareUtils.hydrationLevel(hydration).color()
    LinearProgressIndicator(
        progress = { hydration },
        modifier = modifier.fillMaxWidth().height(DesignSystem.Space2),
        color = color,
        trackColor = color.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round,
    )
}

@Composable
private fun HydrationLevel.color(): Color = when (this) {
    HydrationLevel.HEALTHY -> MaterialTheme.colorScheme.primary
    HydrationLevel.THIRSTY -> DesignSystem.Warning
    HydrationLevel.DRY -> MaterialTheme.colorScheme.error
}

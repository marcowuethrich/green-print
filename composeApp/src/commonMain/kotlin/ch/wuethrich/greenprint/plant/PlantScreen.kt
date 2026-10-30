package ch.wuethrich.greenprint.plant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.plant.components.PlantFormModal
import ch.wuethrich.greenprint.plant.components.PlantItem
import ch.wuethrich.greenprint.plant.components.RoomBlueprint
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.AddFab
import ch.wuethrich.greenprint.ui.components.BackButton
import ch.wuethrich.greenprint.ui.components.DeleteConfirmationDialog
import ch.wuethrich.greenprint.ui.components.ErrorScreen
import ch.wuethrich.greenprint.ui.components.GreenPrintScaffold
import ch.wuethrich.greenprint.ui.components.LoadingScreen
import ch.wuethrich.greenprint.ui.motion.StaggerFadeUp
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.plant_add_action
import greenprint.composeapp.generated.resources.plant_blueprint_hint
import greenprint.composeapp.generated.resources.plant_delete_dialog_message
import greenprint.composeapp.generated.resources.plant_delete_dialog_title
import greenprint.composeapp.generated.resources.plant_no_plants
import greenprint.composeapp.generated.resources.plant_position_saved_offline
import greenprint.composeapp.generated.resources.plant_view_blueprint
import greenprint.composeapp.generated.resources.plant_view_list
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The dialog open on the plant screen. */
private sealed interface PlantDialog {
    data object Create : PlantDialog
    data class Edit(val plant: Plant) : PlantDialog
    data class Delete(val plant: Plant) : PlantDialog
}

/** What the user asked to do with one plant. */
private sealed interface PlantAction {
    data object Water : PlantAction
    data object Edit : PlantAction
    data class Archive(val reason: String) : PlantAction
    data object Delete : PlantAction
}

data class PlantScreen(val room: Room) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: PlantViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        var dialog by remember { mutableStateOf<PlantDialog?>(null) }

        LaunchedEffect(viewModel.uiEffect) {
            viewModel.uiEffect.collect { effect ->
                val message = when (effect) {
                    is PlantUiEffect.SuccessEffect -> effect.message
                    is PlantUiEffect.ValidationError -> effect.message
                    PlantUiEffect.PositionSavedOffline -> getString(Res.string.plant_position_saved_offline)
                }
                snackbarHostState.showSnackbar(message)
            }
        }
        LaunchedEffect(room.id) {
            viewModel.loadPlants(room.id)
        }

        GreenPrintScaffold(
            title = room.name,
            titleIcon = IconRegistry.getPainter(room.icon),
            navigationIcon = { BackButton(onClick = { navigator.pop() }) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                AddFab(
                    contentDescription = stringResource(Res.string.plant_add_action),
                    onClick = { dialog = PlantDialog.Create },
                )
            },
        ) { innerPadding ->
            PlantBody(
                uiState = uiState,
                onRetry = viewModel::retry,
                onPlantAction = { plant, action ->
                    when (action) {
                        PlantAction.Water -> viewModel.waterPlant(plant.id)
                        PlantAction.Edit -> dialog = PlantDialog.Edit(plant)
                        is PlantAction.Archive -> viewModel.archivePlant(plant.id, action.reason)
                        PlantAction.Delete -> dialog = PlantDialog.Delete(plant)
                    }
                },
                onMove = { plant, x, y -> viewModel.movePlant(plant.id, x, y) },
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            )
        }

        PlantDialogHost(
            dialog = dialog,
            onCreate = { name, species, interval -> viewModel.createPlant(room.id, name, species, interval) },
            onUpdate = { plant, name, species, interval -> viewModel.updatePlant(plant, name, species, interval) },
            onDelete = { viewModel.deletePlant(it.id) },
            onDismiss = { dialog = null },
        )
    }
}

/** How the room's plants are shown: as cards, or where they stand (spatial_blueprint.md). */
private enum class PlantView { LIST, BLUEPRINT }

@Composable
private fun PlantBody(
    uiState: PlantUiState,
    onRetry: () -> Unit,
    onPlantAction: (Plant, PlantAction) -> Unit,
    onMove: (plant: Plant, x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var view by rememberSaveable { mutableStateOf(PlantView.LIST) }
    Column(modifier = modifier) {
        if (uiState is PlantUiState.Success && uiState.plants.isNotEmpty()) {
            PlantViewSwitch(
                selected = view,
                onSelect = { view = it },
                modifier = Modifier.padding(horizontal = DesignSystem.Space4, vertical = DesignSystem.Space2),
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (uiState) {
                PlantUiState.Loading -> LoadingScreen()

                is PlantUiState.Error -> ErrorScreen(onRetry = onRetry, message = uiState.message)

                is PlantUiState.Success -> when (view) {
                    PlantView.LIST -> PlantList(plants = uiState.plants, onPlantAction = onPlantAction)

                    PlantView.BLUEPRINT -> BlueprintContent(
                        plants = uiState.plants,
                        onMove = onMove,
                        onPlantClick = { onPlantAction(it, PlantAction.Edit) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantViewSwitch(selected: PlantView, onSelect: (PlantView) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        PlantView.entries.forEachIndexed { index, view ->
            SegmentedButton(
                selected = view == selected,
                onClick = { onSelect(view) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = PlantView.entries.size),
                label = { Text(stringResource(view.label)) },
            )
        }
    }
}

private val PlantView.label: StringResource
    get() = when (this) {
        PlantView.LIST -> Res.string.plant_view_list
        PlantView.BLUEPRINT -> Res.string.plant_view_blueprint
    }

@Composable
private fun BlueprintContent(
    plants: List<Plant>,
    onMove: (plant: Plant, x: Float, y: Float) -> Unit,
    onPlantClick: (Plant) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSystem.Space3),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DesignSystem.Space4),
    ) {
        Text(
            text = stringResource(Res.string.plant_blueprint_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RoomBlueprint(plants = plants, onMove = onMove, onPlantClick = onPlantClick)
    }
}

@Composable
private fun PlantList(plants: List<Plant>, onPlantAction: (Plant, PlantAction) -> Unit, modifier: Modifier = Modifier) {
    if (plants.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(Res.string.plant_no_plants))
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(DesignSystem.Space4),
        verticalArrangement = Arrangement.spacedBy(DesignSystem.Space4),
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(plants, key = { _, plant -> plant.id }) { index, plant ->
            StaggerFadeUp(index = index) {
                PlantItem(
                    plant = plant,
                    onWaterClick = { onPlantAction(plant, PlantAction.Water) },
                    onEditClick = { onPlantAction(plant, PlantAction.Edit) },
                    onArchiveClick = { reason -> onPlantAction(plant, PlantAction.Archive(reason)) },
                    onDeleteClick = { onPlantAction(plant, PlantAction.Delete) },
                )
            }
        }
    }
}

/** Shows [dialog]; every confirm also closes it through [onDismiss]. */
@Composable
private fun PlantDialogHost(
    dialog: PlantDialog?,
    onCreate: (name: String, species: String?, intervalDays: Int) -> Unit,
    onUpdate: (plant: Plant, name: String, species: String?, intervalDays: Int) -> Unit,
    onDelete: (Plant) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        PlantDialog.Create -> PlantFormModal(
            onConfirm = { name, species, interval ->
                onCreate(name, species, interval)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is PlantDialog.Edit -> PlantFormModal(
            plant = dialog.plant,
            onConfirm = { name, species, interval ->
                onUpdate(dialog.plant, name, species, interval)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is PlantDialog.Delete -> DeleteConfirmationDialog(
            title = stringResource(Res.string.plant_delete_dialog_title),
            message = stringResource(Res.string.plant_delete_dialog_message, dialog.plant.name),
            onConfirm = {
                onDelete(dialog.plant)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

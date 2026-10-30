package ch.wuethrich.greenprint.plant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import greenprint.composeapp.generated.resources.plant_delete_dialog_message
import greenprint.composeapp.generated.resources.plant_delete_dialog_title
import greenprint.composeapp.generated.resources.plant_no_plants
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
                when (effect) {
                    is PlantUiEffect.SuccessEffect -> snackbarHostState.showSnackbar(effect.message)
                    is PlantUiEffect.ValidationError -> snackbarHostState.showSnackbar(effect.message)
                }
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
                onRetry = { viewModel.loadPlants(room.id) },
                onPlantAction = { plant, action ->
                    when (action) {
                        PlantAction.Water -> viewModel.waterPlant(plant.id)
                        PlantAction.Edit -> dialog = PlantDialog.Edit(plant)
                        is PlantAction.Archive -> viewModel.archivePlant(plant.id, action.reason)
                        PlantAction.Delete -> dialog = PlantDialog.Delete(plant)
                    }
                },
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            )
        }

        PlantDialogHost(
            dialog = dialog,
            onCreate = { name, species, interval -> viewModel.createPlant(room.id, name, species, interval) },
            onUpdate = { plant, name, species, interval -> viewModel.updatePlant(plant.id, name, species, interval) },
            onDelete = { viewModel.deletePlant(it.id) },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun PlantBody(
    uiState: PlantUiState,
    onRetry: () -> Unit,
    onPlantAction: (Plant, PlantAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (uiState) {
            PlantUiState.Loading -> LoadingScreen()
            is PlantUiState.Error -> ErrorScreen(onRetry = onRetry, message = uiState.message)
            is PlantUiState.Success -> PlantList(plants = uiState.plants, onPlantAction = onPlantAction)
        }
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

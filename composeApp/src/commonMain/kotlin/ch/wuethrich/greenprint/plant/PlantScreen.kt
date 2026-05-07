package ch.wuethrich.greenprint.plant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.plant.components.PlantFormModal
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.DeleteConfirmationDialog
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

data class PlantScreen(val room: Room) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: PlantViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        var showFormModal by remember { mutableStateOf(false) }
        var editingPlant by remember { mutableStateOf<ch.wuethrich.greenprint.model.Plant?>(null) }
        var plantToDelete by remember { mutableStateOf<ch.wuethrich.greenprint.model.Plant?>(null) }
        
        val snackbarHostState = remember { SnackbarHostState() }
        
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

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = IconRegistry.getPainter(room.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = room.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        editingPlant = null
                        showFormModal = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = "Add Plant")
                }
            },
            containerColor = Color.Transparent,
            modifier = Modifier.background(Brush.verticalGradient(DesignSystem.BackgroundGradient))
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (val state = uiState) {
                    is PlantUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is PlantUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    is PlantUiState.Success -> {
                        if (state.plants.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.plant_no_plants),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.plants) { plant ->
                                    PlantItem(
                                        plant = plant,
                                        onWaterClick = { viewModel.waterPlant(plant.id) },
                                        onEditClick = {
                                            editingPlant = plant
                                            showFormModal = true
                                        },
                                        onArchiveClick = { reason ->
                                            viewModel.archivePlant(plant.id, reason)
                                        },
                                        onDeleteClick = {
                                            plantToDelete = plant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFormModal) {
            PlantFormModal(
                plant = editingPlant,
                onConfirm = { name, species, interval ->
                    val plantToEdit = editingPlant
                    if (plantToEdit == null) {
                        viewModel.createPlant(room.id, name, species, interval)
                    } else {
                        viewModel.updatePlant(plantToEdit.id, name, species, interval)
                    }
                    showFormModal = false
                    editingPlant = null
                },
                onDismiss = { 
                    showFormModal = false
                    editingPlant = null
                }
            )
        }

        plantToDelete?.let { plant ->
            DeleteConfirmationDialog(
                title = stringResource(Res.string.plant_delete_dialog_title),
                message = "Are you sure you want to permanently delete '${plant.name}'?",
                onConfirm = {
                    viewModel.deletePlant(plant.id)
                    plantToDelete = null
                },
                onDismiss = { plantToDelete = null }
            )
        }
    }
}

@Composable
fun PlantItem(
    plant: ch.wuethrich.greenprint.model.Plant,
    onWaterClick: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val hydration = PlantCareUtils.calculateHydration(plant)
    val color = Color(PlantCareUtils.getBarometerColor(hydration))
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.name, style = MaterialTheme.typography.titleMedium)
                    plant.species?.let {
                        if (it.isNotBlank()) {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                IconButton(onClick = onWaterClick) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_leaf),
                        contentDescription = "Water Plant",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.plant_archive_died)) },
                            onClick = {
                                showMenu = false
                                onArchiveClick("DIED")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.plant_archive_gifted)) },
                            onClick = {
                                showMenu = false
                                onArchiveClick("GIFTED")
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = stringResource(Res.string.plant_delete),
                                    color = MaterialTheme.colorScheme.error
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { hydration },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

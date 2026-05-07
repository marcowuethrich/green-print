package ch.wuethrich.greenprint.plant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import ch.wuethrich.greenprint.plant.components.CreatePlantModal
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.ic_add
import greenprint.composeapp.generated.resources.ic_leaf
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

data class PlantScreen(val room: Room) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: PlantViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var showCreateModal by remember { mutableStateOf(false) }
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
                    onClick = { showCreateModal = true },
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
                            Text("No plants in this room yet.", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.plants) { plant ->
                                    PlantItem(plant, onWaterClick = { viewModel.waterPlant(plant.id) })
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateModal) {
            CreatePlantModal(
                onConfirm = { name, species, interval ->
                    viewModel.createPlant(room.id, name, species, interval)
                    showCreateModal = false
                },
                onDismiss = { showCreateModal = false }
            )
        }
    }
}

@Composable
fun PlantItem(
    plant: ch.wuethrich.greenprint.model.Plant,
    onWaterClick: () -> Unit
) {
    val hydration = PlantCareUtils.calculateHydration(plant)
    val color = Color(PlantCareUtils.getBarometerColor(hydration))

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
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onWaterClick) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_leaf),
                        contentDescription = "Water Plant",
                        tint = MaterialTheme.colorScheme.primary
                    )
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

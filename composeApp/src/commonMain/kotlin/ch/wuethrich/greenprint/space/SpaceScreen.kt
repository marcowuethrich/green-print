package ch.wuethrich.greenprint.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.plant.PlantScreen
import ch.wuethrich.greenprint.space.components.CreateRoomModal
import ch.wuethrich.greenprint.space.components.DeleteRoomDialog
import ch.wuethrich.greenprint.space.components.DeleteSpaceDialog
import ch.wuethrich.greenprint.space.components.RenameRoomDialog
import ch.wuethrich.greenprint.space.components.RenameSpaceDialog
import ch.wuethrich.greenprint.space.components.SpaceDetailContent
import ch.wuethrich.greenprint.space.components.SpaceEmptyContent
import ch.wuethrich.greenprint.space.components.SpaceErrorContent
import ch.wuethrich.greenprint.space.components.SpaceLoadingContent
import ch.wuethrich.greenprint.space.components.SpaceSettingsMenu
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

sealed interface SpaceModal {
    data object None : SpaceModal
    data object CreateRoom : SpaceModal
    data object RenameSpace : SpaceModal
    data class RenameRoom(val room: Room) : SpaceModal
    data object DeleteSpace : SpaceModal
    data class DeleteRoom(val room: Room) : SpaceModal
    data class RoomNotEmpty(val roomId: String, val roomName: String) : SpaceModal
}

class SpaceScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: SpaceViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var visible by remember { mutableStateOf(false) }
        val mainFocusRequester = remember { FocusRequester() }

        var activeModal by remember { mutableStateOf<SpaceModal>(SpaceModal.None) }
        var validationError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        // Handle one-time effects (like validation errors)
        LaunchedEffect(Unit) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is SpaceUiEffect.ValidationError -> {
                        validationError = effect.message
                        isSaving = false
                    }
                    is SpaceUiEffect.RoomHasPlants -> {
                        activeModal = SpaceModal.RoomNotEmpty(effect.roomId, effect.roomName)
                    }
                }
            }
        }

        // Auto-reset saving state and CLOSE MODAL on success
        LaunchedEffect(uiState) {
            if (isSaving && uiState is SpaceUiState.HasSpace && validationError == null) {
                isSaving = false
                activeModal = SpaceModal.None
            }
        }

        val title = when (val state = uiState) {
            is SpaceUiState.HasSpace -> state.space.name
            else -> ""
        }
        
        val iconString = when (val state = uiState) {
            is SpaceUiState.HasSpace -> state.space.icon
            else -> null
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState is SpaceUiState.HasSpace) {
                                Icon(
                                    painter = IconRegistry.getPainter(iconString),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            ) 
                        }
                    },
                    actions = {
                        if (uiState is SpaceUiState.HasSpace) {
                            SpaceSettingsMenu(
                                onLeave = { navigator.pop() },
                                onRenameClick = { 
                                    validationError = null
                                    activeModal = SpaceModal.RenameSpace 
                                },
                                onDeleteClick = { activeModal = SpaceModal.DeleteSpace }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                if (uiState is SpaceUiState.HasSpace) {
                    FloatingActionButton(
                        onClick = { 
                            validationError = null
                            isSaving = false
                            activeModal = SpaceModal.CreateRoom 
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = "Add Room")
                    }
                }
            },
            containerColor = Color.Transparent,
            modifier = Modifier.background(Brush.verticalGradient(DesignSystem.BackgroundGradient))
        ) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (val state = uiState) {
                            SpaceUiState.Loading -> SpaceLoadingContent()
                            SpaceUiState.Empty -> SpaceEmptyContent()
                            is SpaceUiState.HasSpace -> SpaceDetailContent(
                                state = state,
                                onRenameRoomClick = { 
                                    validationError = null
                                    activeModal = SpaceModal.RenameRoom(it) 
                                },
                                onDeleteRoomClick = { activeModal = SpaceModal.DeleteRoom(it) },
                                onRoomClick = { room ->
                                    navigator.push(PlantScreen(room))
                                },
                                onCreateRoomClick = {
                                    validationError = null
                                    activeModal = SpaceModal.CreateRoom
                                },
                                focusRequester = mainFocusRequester
                            )
                            is SpaceUiState.Error -> SpaceErrorContent(onRetry = viewModel::retry)
                        }
                    }
                }
            }
        }

        // Modal Handling
        when (val modal = activeModal) {
            SpaceModal.None -> {}
            SpaceModal.CreateRoom -> {
                if (uiState is SpaceUiState.HasSpace) {
                    CreateRoomModal(
                        error = validationError,
                        onConfirm = { name, icon ->
                            isSaving = true
                            viewModel.createRoom((uiState as SpaceUiState.HasSpace).space.id, name, icon)
                        },
                        onDismiss = { 
                            activeModal = SpaceModal.None
                            validationError = null
                        },
                        onClearError = { validationError = null }
                    )
                }
            }
            SpaceModal.RenameSpace -> {
                if (uiState is SpaceUiState.HasSpace) {
                    val space = (uiState as SpaceUiState.HasSpace).space
                    RenameSpaceDialog(
                        currentName = space.name,
                        currentIcon = space.icon,
                        error = validationError,
                        onConfirm = { name, icon ->
                            isSaving = true
                            viewModel.updateSpace(space.id, name, icon)
                        },
                        onDismiss = { 
                            activeModal = SpaceModal.None
                            validationError = null
                        },
                        onClearError = { validationError = null }
                    )
                }
            }
            is SpaceModal.RenameRoom -> {
                RenameRoomDialog(
                    currentName = modal.room.name,
                    currentIcon = modal.room.icon,
                    error = validationError,
                    onConfirm = { name, icon ->
                        isSaving = true
                        viewModel.updateRoom(modal.room.id, name, icon)
                    },
                    onDismiss = { 
                        activeModal = SpaceModal.None
                        validationError = null
                    },
                    onClearError = { validationError = null }
                )
            }
            SpaceModal.DeleteSpace -> {
                if (uiState is SpaceUiState.HasSpace) {
                    val space = (uiState as SpaceUiState.HasSpace).space
                    DeleteSpaceDialog(
                        spaceName = space.name,
                        onConfirm = {
                            viewModel.deleteSpace(space.id)
                            navigator.pop()
                        },
                        onDismiss = { activeModal = SpaceModal.None }
                    )
                }
            }
            is SpaceModal.DeleteRoom -> {
                DeleteRoomDialog(
                    room = modal.room,
                    onConfirm = {
                        viewModel.deleteRoom(modal.room.id)
                        activeModal = SpaceModal.None
                    },
                    onDismiss = { activeModal = SpaceModal.None }
                )
            }
            is SpaceModal.RoomNotEmpty -> {
                AlertDialog(
                    onDismissRequest = { activeModal = SpaceModal.None },
                    title = { Text("Room Not Empty") },
                    text = { Text("The room '${modal.roomName}' contains plants. Please move or archive them before deleting the room.") },
                    confirmButton = {
                        TextButton(onClick = { activeModal = SpaceModal.None }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

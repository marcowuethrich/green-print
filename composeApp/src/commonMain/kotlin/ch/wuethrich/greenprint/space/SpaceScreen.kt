package ch.wuethrich.greenprint.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.plant.PlantScreen
import ch.wuethrich.greenprint.space.components.*
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.ErrorScreen
import ch.wuethrich.greenprint.ui.components.GreenPrintScaffold
import ch.wuethrich.greenprint.ui.components.LoadingScreen
import ch.wuethrich.greenprint.ui.theme.LocalAnimationsEnabled
import greenprint.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
        val animationsEnabled = LocalAnimationsEnabled.current
        var visible by remember { mutableStateOf(!animationsEnabled) }
        val mainFocusRequester = remember { FocusRequester() }

        var activeModal by remember { mutableStateOf<SpaceModal>(SpaceModal.None) }
        var validationError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        if (animationsEnabled) {
            LaunchedEffect(Unit) {
                visible = true
            }
        }

        // Handle one-time effects (like validation errors)
        LaunchedEffect(viewModel.uiEffect) {
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
            if (uiState is SpaceUiState.Empty) {
                navigator.pop()
            }
        }

        val title = (uiState as? SpaceUiState.HasSpace)?.space?.name ?: ""
        val iconString = (uiState as? SpaceUiState.HasSpace)?.space?.icon

        GreenPrintScaffold(
            title = title,
            titleIcon = if (uiState is SpaceUiState.HasSpace) IconRegistry.getPainter(iconString) else null,
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val duration = if (animationsEnabled) 300 else 0
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(duration)) + 
                            slideInVertically(
                                initialOffsetY = { 20 },
                                animationSpec = tween(duration)
                            )
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (val state = uiState) {
                            SpaceUiState.Loading -> LoadingScreen()
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
                            is SpaceUiState.Error -> ErrorScreen(
                                message = state.message,
                                onRetry = viewModel::retry,
                            )
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
                    title = { Text(stringResource(Res.string.room_not_empty_title)) },
                    text = { Text(stringResource(Res.string.room_not_empty_message, modal.roomName)) },
                    confirmButton = {
                        TextButton(onClick = { activeModal = SpaceModal.None }) {
                            Text(stringResource(Res.string.common_ok))
                        }
                    }
                )
            }
        }
    }
}

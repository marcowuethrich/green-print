package ch.wuethrich.greenprint.space

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.plant.PlantScreen
import ch.wuethrich.greenprint.space.components.CreateRoomModal
import ch.wuethrich.greenprint.space.components.DeleteRoomDialog
import ch.wuethrich.greenprint.space.components.DeleteSpaceDialog
import ch.wuethrich.greenprint.space.components.RenameRoomDialog
import ch.wuethrich.greenprint.space.components.RenameSpaceDialog
import ch.wuethrich.greenprint.space.components.RoomNotEmptyDialog
import ch.wuethrich.greenprint.space.components.SpaceDetailContent
import ch.wuethrich.greenprint.space.components.SpaceEmptyContent
import ch.wuethrich.greenprint.space.components.SpaceSettingsMenu
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.AddFab
import ch.wuethrich.greenprint.ui.components.ErrorScreen
import ch.wuethrich.greenprint.ui.components.GreenPrintScaffold
import ch.wuethrich.greenprint.ui.components.LoadingScreen
import ch.wuethrich.greenprint.ui.motion.PageEntrance
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.room_add_action
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

/** The open dialog on the space screen and the state of its form. */
@Stable
private class SpaceModalState {
    var active: SpaceModal by mutableStateOf(SpaceModal.None)
        private set
    var validationError: String? by mutableStateOf(null)
    private var isSaving by mutableStateOf(false)

    fun open(modal: SpaceModal) {
        validationError = null
        isSaving = false
        active = modal
    }

    fun close() = open(SpaceModal.None)

    fun submitting() {
        isSaving = true
    }

    fun rejected(message: String) {
        validationError = message
        isSaving = false
    }

    fun onEffect(effect: SpaceUiEffect) = when (effect) {
        is SpaceUiEffect.ValidationError -> rejected(effect.message)
        is SpaceUiEffect.RoomHasPlants -> open(SpaceModal.RoomNotEmpty(effect.roomId, effect.roomName))
    }

    /** A save that came back without a validation error closes its dialog. */
    fun saved() {
        if (isSaving && validationError == null) close()
    }
}

class SpaceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: SpaceViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val modals = remember { SpaceModalState() }

        LaunchedEffect(viewModel.uiEffect) {
            viewModel.uiEffect.collect(modals::onEffect)
        }
        LaunchedEffect(uiState) {
            when (uiState) {
                is SpaceUiState.HasSpace -> modals.saved()
                SpaceUiState.Empty -> navigator.pop()
                else -> Unit
            }
        }

        val space = (uiState as? SpaceUiState.HasSpace)?.space
        GreenPrintScaffold(
            title = space?.name.orEmpty(),
            titleIcon = space?.let { IconRegistry.getPainter(it.icon) },
            actions = {
                if (space != null) {
                    SpaceSettingsMenu(
                        onLeave = { navigator.pop() },
                        onRenameClick = { modals.open(SpaceModal.RenameSpace) },
                        onDeleteClick = { modals.open(SpaceModal.DeleteSpace) },
                    )
                }
            },
            floatingActionButton = {
                if (space != null) {
                    AddFab(
                        contentDescription = stringResource(Res.string.room_add_action),
                        onClick = { modals.open(SpaceModal.CreateRoom) },
                    )
                }
            },
        ) { innerPadding ->
            SpaceBody(
                uiState = uiState,
                onRoomClick = { navigator.push(PlantScreen(it)) },
                onModalRequest = modals::open,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            )
        }

        if (space != null) {
            SpaceModalHost(
                space = space,
                modals = modals,
                onSubmit = { modal, name, icon -> viewModel.submit(modal, space.id, name, icon) },
                onDeleteConfirm = { modal ->
                    viewModel.confirmDelete(modal, space.id)
                    if (modal == SpaceModal.DeleteSpace) navigator.pop()
                },
            )
        }
    }
}

@Composable
private fun SpaceBody(
    uiState: SpaceUiState,
    onRoomClick: (Room) -> Unit,
    onModalRequest: (SpaceModal) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        PageEntrance {
            Column(
                modifier = Modifier.widthIn(max = DesignSystem.ContentMaxWidth).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (uiState) {
                    SpaceUiState.Loading -> LoadingScreen()

                    SpaceUiState.Empty -> SpaceEmptyContent()

                    is SpaceUiState.HasSpace -> SpaceDetailContent(
                        state = uiState,
                        onRenameRoomClick = { onModalRequest(SpaceModal.RenameRoom(it)) },
                        onDeleteRoomClick = { onModalRequest(SpaceModal.DeleteRoom(it)) },
                        onRoomClick = onRoomClick,
                        onCreateRoomClick = { onModalRequest(SpaceModal.CreateRoom) },
                    )

                    is SpaceUiState.Error -> ErrorScreen(onRetry = onRetry, message = uiState.message)
                }
            }
        }
    }
}

/**
 * Shows the open dialog. A submit marks the form as saving until the view model answers; a
 * confirmed delete closes its dialog.
 */
@Composable
private fun SpaceModalHost(
    space: Space,
    modals: SpaceModalState,
    onSubmit: (modal: SpaceModal, name: String, icon: String?) -> Unit,
    onDeleteConfirm: (modal: SpaceModal) -> Unit,
) {
    val modal = modals.active
    val error = modals.validationError
    val submit = { name: String, icon: String? ->
        modals.submitting()
        onSubmit(modal, name, icon)
    }
    val confirmDelete = {
        onDeleteConfirm(modal)
        modals.close()
    }
    val onClearError = { modals.validationError = null }
    when (modal) {
        SpaceModal.None -> Unit
        SpaceModal.CreateRoom -> CreateRoomModal(error, submit, modals::close, onClearError)
        SpaceModal.RenameSpace -> RenameSpaceDialog(space, error, submit, modals::close, onClearError)
        is SpaceModal.RenameRoom -> RenameRoomDialog(modal.room, error, submit, modals::close, onClearError)
        SpaceModal.DeleteSpace -> DeleteSpaceDialog(space.name, confirmDelete, modals::close)
        is SpaceModal.DeleteRoom -> DeleteRoomDialog(modal.room, confirmDelete, modals::close)
        is SpaceModal.RoomNotEmpty -> RoomNotEmptyDialog(modal.roomName, modals::close)
    }
}

private fun SpaceViewModel.submit(modal: SpaceModal, spaceId: String, name: String, icon: String?) {
    when (modal) {
        SpaceModal.CreateRoom -> createRoom(spaceId, name, icon)
        SpaceModal.RenameSpace -> updateSpace(spaceId, name, icon)
        is SpaceModal.RenameRoom -> updateRoom(modal.room.id, name, icon)
        else -> Unit
    }
}

private fun SpaceViewModel.confirmDelete(modal: SpaceModal, spaceId: String) {
    when (modal) {
        SpaceModal.DeleteSpace -> deleteSpace(spaceId)
        is SpaceModal.DeleteRoom -> deleteRoom(modal.room.id)
        else -> Unit
    }
}

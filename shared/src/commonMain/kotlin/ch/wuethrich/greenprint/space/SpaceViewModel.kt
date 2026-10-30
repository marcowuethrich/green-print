package ch.wuethrich.greenprint.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.async.coroutines.awaitAsList
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.model.ErrorReason
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.toErrorReason
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface SpaceUiState {
    data object Loading : SpaceUiState
    data object Empty : SpaceUiState
    data class HasSpace(val space: Space) : SpaceUiState
    data class Error(val reason: ErrorReason) : SpaceUiState
}

sealed interface SpaceUiEffect {
    data class ValidationError(val reason: ErrorReason, val targetId: String? = null) : SpaceUiEffect
    data class RoomHasPlants(val roomId: String, val roomName: String) : SpaceUiEffect
}

class SpaceViewModel(
    private val spaceRepository: SpaceRepository,
    private val roomRepository: RoomRepository,
    private val queries: GreenPrintDbQueries,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SpaceUiEffect>()
    val uiEffect: SharedFlow<SpaceUiEffect> = _uiEffect.asSharedFlow()

    private val refreshSignal = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            refreshSignal.collectLatest {
                loadSpaceInternal()
            }
        }
    }

    fun retry() {
        refreshSignal.value++
    }

    fun createSpace(name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = spaceRepository.createSpace(trimmed, icon)) {
                is NetworkResult.Success -> {
                    _uiState.value = SpaceUiState.HasSpace(result.data)
                }

                is NetworkResult.Failure -> handleFormError(
                    result.error,
                    ErrorReason.SPACE_NAME_TAKEN,
                    targetId = null,
                )
            }
        }
    }

    fun updateSpace(id: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = spaceRepository.updateSpace(id, trimmed, icon)) {
                is NetworkResult.Success -> _uiState.value = SpaceUiState.HasSpace(result.data)

                is NetworkResult.Failure -> handleFormError(
                    result.error,
                    ErrorReason.SPACE_NAME_TAKEN,
                    targetId = id,
                )
            }
        }
    }

    fun deleteSpace(id: String) {
        viewModelScope.launch {
            _uiState.value = SpaceUiState.Loading
            when (val result = spaceRepository.deleteSpace(id)) {
                is NetworkResult.Success -> _uiState.value = SpaceUiState.Empty
                is NetworkResult.Failure -> _uiState.value = SpaceUiState.Error(result.error.toErrorReason())
            }
        }
    }

    fun createRoom(spaceId: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = roomRepository.createRoom(spaceId, trimmed, icon)) {
                is NetworkResult.Success -> {
                    refreshSignal.value++
                }

                is NetworkResult.Failure -> handleFormError(
                    result.error,
                    ErrorReason.ROOM_NAME_TAKEN,
                    targetId = "new_room",
                )
            }
        }
    }

    fun updateRoom(id: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = roomRepository.updateRoom(id, trimmed, icon)) {
                is NetworkResult.Success -> refreshSignal.value++

                is NetworkResult.Failure -> handleFormError(
                    result.error,
                    ErrorReason.ROOM_NAME_TAKEN,
                    targetId = id,
                )
            }
        }
    }

    fun deleteRoom(id: String, force: Boolean = false) {
        viewModelScope.launch {
            if (!force && queries.selectPlantsByRoomId(id).awaitAsList().isNotEmpty()) {
                reportRoomHasPlants(id)
                return@launch
            }

            when (val result = roomRepository.deleteRoom(id)) {
                is NetworkResult.Success -> refreshSignal.value++

                // The server also counts plants this device has not cached, archived ones among them.
                is NetworkResult.Failure if result.error == NetworkError.Conflict -> reportRoomHasPlants(id)

                is NetworkResult.Failure -> _uiState.value = SpaceUiState.Error(result.error.toErrorReason())
            }
        }
    }

    private suspend fun reportRoomHasPlants(id: String) {
        val room = queries.selectAllRooms().awaitAsList().find { it.id == id }
        _uiEffect.emit(SpaceUiEffect.RoomHasPlants(id, room?.name.orEmpty()))
    }

    private suspend fun handleFormError(error: NetworkError, conflict: ErrorReason, targetId: String?) {
        val target = (error as? NetworkError.Validation)?.field ?: targetId
        _uiEffect.emit(SpaceUiEffect.ValidationError(error.toErrorReason(conflict), target))
    }

    private suspend fun loadSpaceInternal() {
        _uiState.value = SpaceUiState.Loading
        spaceRepository.observeSpaces().collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.value = result.data.firstOrNull()
                        ?.let { SpaceUiState.HasSpace(it) }
                        ?: SpaceUiState.Empty
                }

                is NetworkResult.Failure -> {
                    // Always show error if we don't have a space successfully loaded.
                    if (_uiState.value !is SpaceUiState.HasSpace) {
                        _uiState.value = SpaceUiState.Error(result.error.toErrorReason())
                    }
                }
            }
        }
    }
}

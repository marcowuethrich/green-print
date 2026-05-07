package ch.wuethrich.greenprint.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.repository.AuthRepository
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SpaceUiState {
    data object Loading : SpaceUiState
    data object Empty : SpaceUiState
    data class HasSpace(val space: Space) : SpaceUiState
    data class Error(val message: String) : SpaceUiState
}

sealed interface SpaceUiEffect {
    data class ValidationError(val message: String, val targetId: String? = null) : SpaceUiEffect
}

class SpaceViewModel(
    private val spaceRepository: SpaceRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SpaceUiEffect>()
    val uiEffect: SharedFlow<SpaceUiEffect> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            loadSpace()
        }
    }

    fun retry() {
        viewModelScope.launch { loadSpace() }
    }

    fun createSpace(name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = spaceRepository.createSpace(trimmed, icon)) {
                is NetworkResult.Success -> _uiState.value = SpaceUiState.HasSpace(result.data)
                is NetworkResult.Failure -> handleFormError(result.error, "A space with this name already exists.", targetId = null)
            }
        }
    }

    fun updateSpace(id: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = spaceRepository.updateSpace(id, trimmed, icon)) {
                is NetworkResult.Success -> _uiState.value = SpaceUiState.HasSpace(result.data)
                is NetworkResult.Failure -> handleFormError(result.error, "A space with this name already exists.", targetId = id)
            }
        }
    }

    fun deleteSpace(id: String) {
        viewModelScope.launch {
            _uiState.value = SpaceUiState.Loading
            when (val result = spaceRepository.deleteSpace(id)) {
                is NetworkResult.Success -> _uiState.value = SpaceUiState.Empty
                is NetworkResult.Failure -> _uiState.value = SpaceUiState.Error(result.error.toMessage())
            }
        }
    }

    fun createRoom(spaceId: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = roomRepository.createRoom(spaceId, trimmed, icon)) {
                is NetworkResult.Success -> loadSpace(silent = true)
                is NetworkResult.Failure -> handleFormError(result.error, "A room with this name already exists in this space.", targetId = "new_room")
            }
        }
    }

    fun updateRoom(id: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (val result = roomRepository.updateRoom(id, trimmed, icon)) {
                is NetworkResult.Success -> loadSpace(silent = true)
                is NetworkResult.Failure -> handleFormError(result.error, "A room with this name already exists in this space.", targetId = id)
            }
        }
    }

    fun deleteRoom(id: String) {
        viewModelScope.launch {
            when (val result = roomRepository.deleteRoom(id)) {
                is NetworkResult.Success -> loadSpace(silent = true)
                is NetworkResult.Failure -> _uiState.value = SpaceUiState.Error(result.error.toMessage())
            }
        }
    }

    private suspend fun handleFormError(error: NetworkError, conflictMessage: String, targetId: String?) {
        when (error) {
            is NetworkError.Conflict ->
                _uiEffect.emit(SpaceUiEffect.ValidationError(conflictMessage, targetId))
            is NetworkError.Validation ->
                _uiEffect.emit(SpaceUiEffect.ValidationError(error.toMessage(), error.field ?: targetId))
            else ->
                _uiEffect.emit(SpaceUiEffect.ValidationError(error.toMessage(), targetId))
        }
    }

    private suspend fun loadSpace(silent: Boolean = false) {
        if (!silent) _uiState.value = SpaceUiState.Loading
        spaceRepository.observeSpaces().collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.value = result.data.firstOrNull()
                        ?.let { SpaceUiState.HasSpace(it) }
                        ?: SpaceUiState.Empty
                }
                is NetworkResult.Failure -> {
                    if (!silent && _uiState.value is SpaceUiState.Loading) {
                        _uiState.value = SpaceUiState.Error(result.error.toMessage())
                    }
                    // For background updates, we might just log or show a toast/snackbar via effect
                }
            }
        }
    }

    private fun NetworkError.toMessage(): String = when (this) {
        NetworkError.Unauthorized -> "Please log in again."
        NetworkError.Forbidden -> "You don't have permission to perform this action."
        NetworkError.NotFound -> "The requested resource was not found."
        NetworkError.Conflict -> "This item already exists."
        NetworkError.ServerError -> "Server error: Please try again later."
        NetworkError.Connectivity -> "Network error: Please check your connection."
        is NetworkError.Validation -> validationMessage(code)
        is NetworkError.Unknown -> message ?: "An unexpected error occurred."
    }

    private fun validationMessage(code: String): String = when (code) {
        "name_blank" -> "Name cannot be empty."
        "name_too_long" -> "Name is too long (maximum 128 characters)."
        else -> "Please check the entered values."
    }
}

package ch.wuethrich.greenprint.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.repository.AuthRepository
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
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
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SpaceUiEffect>()
    val uiEffect: SharedFlow<SpaceUiEffect> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.login()
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
            try {
                val space = spaceRepository.createSpace(trimmed, icon)
                _uiState.value = SpaceUiState.HasSpace(space)
            } catch (e: Throwable) {
                handleFormError(e, "A space with this name already exists.", targetId = null)
            }
        }
    }

    fun updateSpace(id: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            try {
                val space = spaceRepository.updateSpace(id, trimmed, icon)
                _uiState.value = SpaceUiState.HasSpace(space)
            } catch (e: Throwable) {
                handleFormError(e, "A space with this name already exists.", targetId = id)
            }
        }
    }

    fun deleteSpace(id: String) {
        viewModelScope.launch {
            _uiState.value = SpaceUiState.Loading
            runCatching { spaceRepository.deleteSpace(id) }
                .onSuccess { _uiState.value = SpaceUiState.Empty }
                .onFailure { _uiState.value = SpaceUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun createRoom(spaceId: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            try {
                roomRepository.createRoom(spaceId, trimmed, icon)
                loadSpace(silent = true)
            } catch (e: Throwable) {
                handleFormError(e, "A room with this name already exists in this space.", targetId = "new_room")
            }
        }
    }

    fun updateRoom(id: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            try {
                roomRepository.updateRoom(id, trimmed, icon)
                loadSpace(silent = true)
            } catch (e: Throwable) {
                handleFormError(e, "A room with this name already exists in this space.", targetId = id)
            }
        }
    }

    fun deleteRoom(id: String) {
        viewModelScope.launch {
            runCatching { roomRepository.deleteRoom(id) }
                .onSuccess { loadSpace(silent = true) }
                .onFailure { _uiState.value = SpaceUiState.Error(it.message ?: "Unknown error") }
        }
    }

    private suspend fun handleFormError(e: Throwable, conflictMessage: String, targetId: String?) {
        val isConflict = when (e) {
            is ResponseException -> e.response.status == HttpStatusCode.Conflict
            else -> e.message?.contains("409") == true
        }

        if (isConflict) {
            _uiEffect.emit(SpaceUiEffect.ValidationError(conflictMessage, targetId))
        } else {
            val message = e.message ?: "An unexpected error occurred."
            val finalMessage = if (message.contains("Connect", ignoreCase = true) || message.contains("Timeout", ignoreCase = true)) {
                "Network error: Please check your connection."
            } else {
                message
            }
            _uiEffect.emit(SpaceUiEffect.ValidationError(finalMessage, targetId))
        }
    }

    private suspend fun loadSpace(silent: Boolean = false) {
        if (!silent) _uiState.value = SpaceUiState.Loading
        runCatching { spaceRepository.getSpaces() }
            .onSuccess { spaces ->
                _uiState.value = spaces.firstOrNull()
                    ?.let { SpaceUiState.HasSpace(it) }
                    ?: SpaceUiState.Empty
            }
            .onFailure { if (!silent) _uiState.value = SpaceUiState.Error(it.message ?: "Unknown error") }
    }
}

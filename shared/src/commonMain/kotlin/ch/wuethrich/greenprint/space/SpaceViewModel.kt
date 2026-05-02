package ch.wuethrich.greenprint.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.repository.AuthRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SpaceUiState {
    data object Loading : SpaceUiState
    data object Empty : SpaceUiState
    data class HasSpace(val space: Space) : SpaceUiState
    data class Error(val message: String) : SpaceUiState
}

class SpaceViewModel(
    private val spaceRepository: SpaceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

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
            _uiState.value = SpaceUiState.Loading
            runCatching { spaceRepository.createSpace(trimmed, icon) }
                .onSuccess { _uiState.value = SpaceUiState.HasSpace(it) }
                .onFailure { _uiState.value = SpaceUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun updateSpace(id: String, name: String, icon: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            runCatching { spaceRepository.updateSpace(id, trimmed, icon) }
                .onSuccess { _uiState.value = SpaceUiState.HasSpace(it) }
                .onFailure { _uiState.value = SpaceUiState.Error(it.message ?: "Unknown error") }
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

    private suspend fun loadSpace() {
        _uiState.value = SpaceUiState.Loading
        runCatching { spaceRepository.getSpaces() }
            .onSuccess { spaces ->
                _uiState.value = spaces.firstOrNull()
                    ?.let { SpaceUiState.HasSpace(it) }
                    ?: SpaceUiState.Empty
            }
            .onFailure { _uiState.value = SpaceUiState.Error(it.message ?: "Unknown error") }
    }
}

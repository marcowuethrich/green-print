package ch.wuethrich.greenprint.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.UpdatePlantRequest
import ch.wuethrich.greenprint.repository.PlantRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface PlantUiState {
    data object Loading : PlantUiState
    data class Success(val plants: List<Plant>) : PlantUiState
    data class Error(val message: String) : PlantUiState
}

sealed interface PlantUiEffect {
    data class ValidationError(val message: String) : PlantUiEffect
    data class SuccessEffect(val message: String) : PlantUiEffect
}

class PlantViewModel(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _roomId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<PlantUiState>(PlantUiState.Loading)
    val uiState: StateFlow<PlantUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlantUiEffect>()
    val uiEffect: SharedFlow<PlantUiEffect> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            _roomId.collectLatest { roomId ->
                if (roomId != null) {
                    plantRepository.observePlants(roomId).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                _uiState.value = PlantUiState.Success(result.data)
                            }
                            is NetworkResult.Failure -> {
                                // Only show error if we don't already have data (offline-first)
                                if (_uiState.value !is PlantUiState.Success) {
                                    _uiState.value = PlantUiState.Error(result.error.toMessage())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadPlants(roomId: String) {
        _roomId.value = roomId
    }

    fun createPlant(roomId: String, name: String, species: String?, intervalDays: Int) {
        viewModelScope.launch {
            val request = CreatePlantRequest(
                name = name,
                species = species,
                wateringIntervalDays = intervalDays
            )
            when (val result = plantRepository.createPlant(roomId, request)) {
                is NetworkResult.Success -> {
                    _uiEffect.emit(PlantUiEffect.SuccessEffect("Plant added successfully"))
                }
                is NetworkResult.Failure -> {
                    _uiEffect.emit(PlantUiEffect.ValidationError(result.error.toMessage()))
                }
            }
        }
    }

    fun updatePlant(id: String, name: String, species: String?, intervalDays: Int) {
        viewModelScope.launch {
            val request = UpdatePlantRequest(
                name = name,
                species = species,
                wateringIntervalDays = intervalDays
            )
            when (val result = plantRepository.updatePlant(id, request)) {
                is NetworkResult.Success -> {
                    _uiEffect.emit(PlantUiEffect.SuccessEffect("Plant updated"))
                }
                is NetworkResult.Failure -> {
                    _uiEffect.emit(PlantUiEffect.ValidationError(result.error.toMessage()))
                }
            }
        }
    }

    fun archivePlant(id: String, reason: String) {
        viewModelScope.launch {
            val request = ArchivePlantRequest(reason = reason)
            when (val result = plantRepository.archivePlant(id, request)) {
                is NetworkResult.Success -> {
                    _uiEffect.emit(PlantUiEffect.SuccessEffect("Plant archived"))
                }
                is NetworkResult.Failure -> {
                    _uiEffect.emit(PlantUiEffect.ValidationError(result.error.toMessage()))
                }
            }
        }
    }

    fun deletePlant(id: String) {
        viewModelScope.launch {
            when (val result = plantRepository.deletePlant(id)) {
                is NetworkResult.Success -> {
                    _uiEffect.emit(PlantUiEffect.SuccessEffect("Plant deleted"))
                }
                is NetworkResult.Failure -> {
                    _uiEffect.emit(PlantUiEffect.ValidationError(result.error.toMessage()))
                }
            }
        }
    }

    fun waterPlant(plantId: String) {
        viewModelScope.launch {
            _uiEffect.emit(PlantUiEffect.SuccessEffect("Plant watered!"))
        }
    }

    private fun NetworkError.toMessage(): String = when (this) {
        NetworkError.Unauthorized -> "Please log in again."
        NetworkError.Forbidden -> "You don't have permission."
        NetworkError.NotFound -> "Plant not found."
        NetworkError.Conflict -> "This plant already exists."
        NetworkError.ServerError -> "Server error."
        NetworkError.Connectivity -> "Check your connection."
        is NetworkError.Validation -> "Invalid input."
        is NetworkError.Unknown -> message ?: "Error occurred."
    }
}

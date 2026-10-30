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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

sealed interface PlantUiState {
    data object Loading : PlantUiState
    data class Success(val plants: List<Plant>) : PlantUiState
    data class Error(val message: String) : PlantUiState
}

sealed interface PlantUiEffect {
    data class ValidationError(val message: String) : PlantUiEffect
    data class SuccessEffect(val message: String) : PlantUiEffect

    /** A blueprint move is saved on this device only; the next refresh sends it to the server. */
    data object PositionSavedOffline : PlantUiEffect
}

class PlantViewModel(private val plantRepository: PlantRepository) : ViewModel() {

    private val selectedRoomId = MutableStateFlow<String?>(null)
    private val refreshSignal = MutableStateFlow(0)

    // The position sync in flight per plant; a newer move of the same plant cancels it.
    private val positionSyncs = mutableMapOf<String, Job>()

    private val _uiState = MutableStateFlow<PlantUiState>(PlantUiState.Loading)
    val uiState: StateFlow<PlantUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlantUiEffect>()
    val uiEffect: SharedFlow<PlantUiEffect> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(selectedRoomId.filterNotNull(), refreshSignal) { roomId, _ -> roomId }
                .collectLatest { roomId -> observePlants(roomId) }
        }
    }

    fun loadPlants(roomId: String) {
        selectedRoomId.value = roomId
    }

    /** Observes the room again: unsent moves go to the server and the list is fetched anew. */
    fun retry() {
        refreshSignal.value++
    }

    /** Adds a plant; it lands on the blueprint spot farthest from the plants already there. */
    fun createPlant(roomId: String, name: String, species: String?, intervalDays: Int) {
        val occupied = currentPlants().map { BlueprintPosition(it.x, it.y) }
        val position = BlueprintLayout.nextFreePosition(occupied)
        viewModelScope.launch {
            val request = CreatePlantRequest(
                name = name,
                species = species,
                wateringIntervalDays = intervalDays,
                x = position.x,
                y = position.y,
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

    /** Edits the form fields of [plant]; everything the form does not show is kept as it is. */
    fun updatePlant(plant: Plant, name: String, species: String?, intervalDays: Int) {
        viewModelScope.launch {
            val request = UpdatePlantRequest(
                name = name,
                species = species,
                potSize = plant.potSize,
                soilType = plant.soilType,
                wateringIntervalDays = intervalDays,
                photoUrl = plant.photoUrl,
                x = plant.x,
                y = plant.y,
            )
            when (val result = plantRepository.updatePlant(plant.id, request)) {
                is NetworkResult.Success -> {
                    _uiEffect.emit(PlantUiEffect.SuccessEffect("Plant updated"))
                }

                is NetworkResult.Failure -> {
                    _uiEffect.emit(PlantUiEffect.ValidationError(result.error.toMessage()))
                }
            }
        }
    }

    /**
     * Moves a plant on the blueprint, clamped into the room. The repository saves it on this device
     * before it calls the server, so an offline move is kept and only reported.
     */
    fun movePlant(id: String, x: Float, y: Float) {
        val position = BlueprintPosition(x, y).clamped()
        positionSyncs.remove(id)?.cancel()
        positionSyncs[id] = viewModelScope.launch {
            val result = plantRepository.updatePlantPosition(id, position.x, position.y)
            if (result is NetworkResult.Failure) {
                _uiEffect.emit(
                    if (result.error == NetworkError.Connectivity) {
                        PlantUiEffect.PositionSavedOffline
                    } else {
                        PlantUiEffect.ValidationError(result.error.toMessage())
                    },
                )
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

    // Quick Water is a UI stub until watering events are persisted (inventory.md §5).
    fun waterPlant(@Suppress("UnusedParameter") plantId: String) {
        viewModelScope.launch {
            _uiEffect.emit(PlantUiEffect.SuccessEffect("Plant watered!"))
        }
    }

    private suspend fun observePlants(roomId: String) {
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

    private fun currentPlants(): List<Plant> = (_uiState.value as? PlantUiState.Success)?.plants.orEmpty()
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

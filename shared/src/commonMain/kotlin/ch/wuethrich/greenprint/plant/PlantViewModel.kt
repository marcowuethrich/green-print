package ch.wuethrich.greenprint.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.ErrorReason
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.UpdatePlantRequest
import ch.wuethrich.greenprint.model.toErrorReason
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
    data class Error(val reason: ErrorReason) : PlantUiState
}

/** What a plant action changed, for the UI to confirm. */
enum class PlantChange { ADDED, UPDATED, WATERED, ARCHIVED, DELETED }

sealed interface PlantUiEffect {
    data class Saved(val change: PlantChange) : PlantUiEffect
    data class Failed(val reason: ErrorReason) : PlantUiEffect

    /** A blueprint move is saved on this device only; the next refresh sends it to the server. */
    data object PositionSavedOffline : PlantUiEffect

    /** A watering is saved on this device only; the next refresh sends it to the server. */
    data object WateringSavedOffline : PlantUiEffect
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
        val occupied = currentPlants.map { BlueprintPosition(it.x, it.y) }
        val position = BlueprintLayout.nextFreePosition(occupied)
        viewModelScope.launch {
            val request = CreatePlantRequest(
                name = name,
                species = species,
                wateringIntervalDays = intervalDays,
                x = position.x,
                y = position.y,
            )
            report(plantRepository.createPlant(roomId, request), PlantChange.ADDED)
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
            report(plantRepository.updatePlant(plant.id, request), PlantChange.UPDATED)
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
                        PlantUiEffect.Failed(result.error.toPlantReason())
                    },
                )
            }
        }
    }

    fun archivePlant(id: String, reason: String) {
        viewModelScope.launch {
            val request = ArchivePlantRequest(reason = reason)
            report(plantRepository.archivePlant(id, request), PlantChange.ARCHIVED)
        }
    }

    fun deletePlant(id: String) {
        viewModelScope.launch {
            report(plantRepository.deletePlant(id), PlantChange.DELETED)
        }
    }

    /**
     * Waters a plant now (Quick Water, inventory.md §5). The repository logs the watering on this
     * device before it calls the server, so the hydration bar fills at once and an offline watering is
     * kept and only reported.
     */
    fun waterPlant(plantId: String) {
        viewModelScope.launch {
            val result = plantRepository.waterPlant(plantId)
            if (result is NetworkResult.Failure && result.error == NetworkError.Connectivity) {
                _uiEffect.emit(PlantUiEffect.WateringSavedOffline)
            } else {
                report(result, PlantChange.WATERED)
            }
        }
    }

    private suspend fun report(result: NetworkResult<*>, change: PlantChange) {
        _uiEffect.emit(
            when (result) {
                is NetworkResult.Success -> PlantUiEffect.Saved(change)
                is NetworkResult.Failure -> PlantUiEffect.Failed(result.error.toPlantReason())
            },
        )
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
                        _uiState.value = PlantUiState.Error(result.error.toPlantReason())
                    }
                }
            }
        }
    }

    private val currentPlants: List<Plant>
        get() = (_uiState.value as? PlantUiState.Success)?.plants.orEmpty()
}

// A plant's name is unique in its room, so a 409 on a plant names that.
private fun NetworkError.toPlantReason(): ErrorReason = toErrorReason(conflict = ErrorReason.PLANT_NAME_TAKEN)

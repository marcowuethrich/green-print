package ch.wuethrich.greenprint.testing

import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.UpdatePlantRequest
import ch.wuethrich.greenprint.repository.PlantRepository
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Test double for [SpaceRepository] backed by a replayable [MutableSharedFlow] so tests can
 * push observed states before or after the screen is rendered. Mutator methods read from
 * `*Result` properties to allow per-test customization, defaulting to success.
 */
class FakeSpaceRepository : SpaceRepository {
    private val flow = replayingFlow<NetworkResult<List<Space>>>()

    var createResult: NetworkResult<Space>? = null
    var updateResult: NetworkResult<Space>? = null
    var deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit)

    fun emit(result: NetworkResult<List<Space>>) {
        flow.tryEmit(result)
    }

    fun emitSpaces(spaces: List<Space>) = emit(NetworkResult.Success(spaces))
    fun emitError(error: NetworkError) = emit(NetworkResult.Failure(error))

    override fun observeSpaces(): Flow<NetworkResult<List<Space>>> = flow

    override suspend fun getSpace(id: String): NetworkResult<Space> =
        NetworkResult.Success(Space(id, "Space"))

    override suspend fun createSpace(name: String, icon: String?): NetworkResult<Space> {
        val result = createResult ?: NetworkResult.Success(Space("new", name, icon))
        if (result is NetworkResult.Success) emitSpaces(listOf(result.data))
        return result
    }

    override suspend fun updateSpace(id: String, name: String, icon: String?): NetworkResult<Space> =
        updateResult ?: NetworkResult.Success(Space(id, name, icon))

    override suspend fun deleteSpace(id: String): NetworkResult<Unit> = deleteResult
}

class FakeRoomRepository : RoomRepository {
    var createResult: NetworkResult<Room>? = null
    var updateResult: NetworkResult<Room>? = null
    var deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit)

    override suspend fun createRoom(spaceId: String, name: String, icon: String?): NetworkResult<Room> =
        createResult ?: NetworkResult.Success(Room("new", spaceId, name, icon))

    override suspend fun updateRoom(id: String, name: String, icon: String?): NetworkResult<Room> =
        updateResult ?: NetworkResult.Success(Room(id, "unknown", name, icon))

    override suspend fun deleteRoom(id: String): NetworkResult<Unit> = deleteResult
}

class FakePlantRepository : PlantRepository {
    private val flow = replayingFlow<NetworkResult<List<Plant>>>().also {
        it.tryEmit(NetworkResult.Success(emptyList()))
    }

    var createResult: NetworkResult<Plant>? = null
    var updateResult: NetworkResult<Plant>? = null
    var archiveResult: NetworkResult<Plant>? = null
    var deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit)

    fun emit(result: NetworkResult<List<Plant>>) {
        flow.tryEmit(result)
    }

    fun emitPlants(plants: List<Plant>) = emit(NetworkResult.Success(plants))
    fun emitError(error: NetworkError) = emit(NetworkResult.Failure(error))

    override fun observePlants(roomId: String): Flow<NetworkResult<List<Plant>>> = flow

    override suspend fun createPlant(roomId: String, request: CreatePlantRequest): NetworkResult<Plant> =
        createResult ?: NetworkResult.Failure(NetworkError.NotFound)

    override suspend fun updatePlant(id: String, request: UpdatePlantRequest): NetworkResult<Plant> =
        updateResult ?: NetworkResult.Failure(NetworkError.NotFound)

    override suspend fun archivePlant(id: String, request: ArchivePlantRequest): NetworkResult<Plant> =
        archiveResult ?: NetworkResult.Failure(NetworkError.NotFound)

    override suspend fun deletePlant(id: String): NetworkResult<Unit> = deleteResult
}

private fun <T> replayingFlow(): MutableSharedFlow<T> =
    MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

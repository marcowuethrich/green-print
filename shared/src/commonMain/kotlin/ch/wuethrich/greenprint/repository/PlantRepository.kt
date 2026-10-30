package ch.wuethrich.greenprint.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.db.PlantEntity
import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.RecordWateringRequest
import ch.wuethrich.greenprint.model.SyncStatus
import ch.wuethrich.greenprint.model.UpdatePlantPositionRequest
import ch.wuethrich.greenprint.model.UpdatePlantRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface PlantRepository {
    fun observePlants(roomId: String): Flow<NetworkResult<List<Plant>>>
    suspend fun createPlant(roomId: String, request: CreatePlantRequest): NetworkResult<Plant>
    suspend fun updatePlant(id: String, request: UpdatePlantRequest): NetworkResult<Plant>

    /**
     * Moves a plant on its room's blueprint. Write-through: the local row takes the new position,
     * marked [SyncStatus.PENDING], before the server is called, so the move survives being offline
     * and restarting the app. A failed call leaves it [SyncStatus.ERROR]; the next [observePlants]
     * of its room sends it again.
     */
    suspend fun updatePlantPosition(id: String, x: Float, y: Float): NetworkResult<Plant>

    /**
     * Waters a plant now. Write-through like a move: the watering is logged on the device
     * ([SyncStatus.PENDING]) and the plant's `lastWateredDate` moves before the server is called. A
     * failed call leaves the watering [SyncStatus.ERROR]; the next [observePlants] of its room sends it
     * again, under the same id, so the server stores it once.
     */
    suspend fun waterPlant(id: String): NetworkResult<Plant>
    suspend fun archivePlant(id: String, request: ArchivePlantRequest): NetworkResult<Plant>
    suspend fun deletePlant(id: String): NetworkResult<Unit>
}

class PlantRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val queries: GreenPrintDbQueries,
    private val dbDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) : PlantRepository {

    override fun observePlants(roomId: String): Flow<NetworkResult<List<Plant>>> = flow {
        // 1. Emit from DB reactively
        val dbFlow = queries
            .selectPlantsByRoomId(roomId)
            .asFlow()
            .mapToList(dbDispatcher)
            .map { entities ->
                val activePlants = entities.filter { it.archiveDate == null }.map { it.toDomain() }
                NetworkResult.Success(activePlants)
            }

        // 2. Send local waterings and moves the server has not confirmed yet, then refresh from the network
        coroutineScope {
            launch {
                syncPendingChanges(roomId)
                val result = httpClient.safeRequest<List<Plant>> {
                    url("$baseUrl/rooms/$roomId/plants")
                    method = HttpMethod.Get
                }
                if (result is NetworkResult.Success) {
                    queries.transaction {
                        // A row still waiting for the server keeps its local state (last write wins).
                        val unsyncedIds = queries
                            .selectLocallyChangedPlantIdsByRoomId(roomId)
                            .awaitAsList()
                            .toSet()
                        queries.deleteSyncedPlantsByRoomId(roomId)
                        result.data.filterNot { it.id in unsyncedIds }.forEach { queries.cachePlant(it) }
                    }
                }
            }
            emitAll(dbFlow)
        }
    }

    override suspend fun createPlant(roomId: String, request: CreatePlantRequest): NetworkResult<Plant> {
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/rooms/$roomId/plants")
            method = HttpMethod.Post
            setBody(request)
        }

        if (result is NetworkResult.Success) {
            queries.cachePlant(result.data)
        }

        return result
    }

    override suspend fun updatePlant(id: String, request: UpdatePlantRequest): NetworkResult<Plant> {
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/plants/$id")
            method = HttpMethod.Put
            setBody(request)
        }

        if (result is NetworkResult.Success) {
            queries.cachePlant(result.data)
        }

        return result
    }

    override suspend fun updatePlantPosition(id: String, x: Float, y: Float): NetworkResult<Plant> {
        val movedAt = clock.now().toEpochMilliseconds()
        queries.updatePlantPosition(
            x = x.toDouble(),
            y = y.toDouble(),
            syncStatus = SyncStatus.PENDING,
            updatedAt = movedAt,
            id = id,
        )
        return pushPosition(id, x, y, movedAt)
    }

    override suspend fun waterPlant(id: String): NetworkResult<Plant> {
        val now = clock.now()
        val wateringId = Uuid.random().toString()
        val wateredAt = now.toString()
        queries.transaction {
            queries.insertWateringEvent(
                id = wateringId,
                plantId = id,
                timestamp = wateredAt,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now.toEpochMilliseconds(),
            )
            queries.updatePlantLastWatered(lastWateredDate = wateredAt, id = id)
        }
        return pushWatering(wateringId, id, wateredAt)
    }

    override suspend fun archivePlant(id: String, request: ArchivePlantRequest): NetworkResult<Plant> {
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/plants/$id/archive")
            method = HttpMethod.Post
            setBody(request)
        }

        if (result is NetworkResult.Success) {
            queries.cachePlant(result.data)
        }

        return result
    }

    override suspend fun deletePlant(id: String): NetworkResult<Unit> {
        val result = httpClient.safeRequest<Unit> {
            url("$baseUrl/plants/$id")
            method = HttpMethod.Delete
        }

        if (result is NetworkResult.Success) {
            queries.forgetPlant(id)
        }

        return result
    }

    /** Sends what this device changed in the room and the server has not confirmed: waterings, then moves. */
    private suspend fun syncPendingChanges(roomId: String) {
        queries.selectUnsyncedWateringEventsByRoomId(roomId).awaitAsList().forEach { watering ->
            val result = pushWatering(watering.id, watering.plantId, watering.timestamp)
            // The plant is gone on the server: there is nothing left to water.
            if (result is NetworkResult.Failure && result.error == NetworkError.NotFound) {
                queries.forgetPlant(watering.plantId)
            }
        }
        queries.selectUnsyncedPlantsByRoomId(roomId).awaitAsList().forEach { plant ->
            val result = pushPosition(plant.id, plant.x.toFloat(), plant.y.toFloat(), plant.updatedAt)
            // The plant is gone on the server: there is nothing left to move.
            if (result is NetworkResult.Failure && result.error == NetworkError.NotFound) {
                queries.forgetPlant(plant.id)
            }
        }
    }

    /** Sends one watering and settles it; the plant takes the server's date unless a newer one waits. */
    private suspend fun pushWatering(wateringId: String, plantId: String, wateredAt: String): NetworkResult<Plant> {
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/plants/$plantId/waterings")
            method = HttpMethod.Post
            setBody(RecordWateringRequest(id = wateringId, timestamp = wateredAt))
        }
        queries.transaction {
            if (result is NetworkResult.Success) {
                queries.updateWateringEventSyncStatus(syncStatus = SyncStatus.SYNCED, id = wateringId)
                queries.updatePlantLastWatered(lastWateredDate = queries.lastWatered(result.data), id = plantId)
            } else {
                queries.updateWateringEventSyncStatus(syncStatus = SyncStatus.ERROR, id = wateringId)
            }
        }
        return result
    }

    /** Sends the move stamped [movedAt] and settles its row, unless a newer move replaced it meanwhile. */
    private suspend fun pushPosition(id: String, x: Float, y: Float, movedAt: Long): NetworkResult<Plant> {
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/plants/$id/position")
            method = HttpMethod.Put
            setBody(UpdatePlantPositionRequest(x, y))
        }
        val status = if (result is NetworkResult.Success) SyncStatus.SYNCED else SyncStatus.ERROR
        queries.updatePlantSyncStatusIfUnchanged(syncStatus = status, id = id, updatedAt = movedAt)
        return result
    }
}

private suspend fun GreenPrintDbQueries.cachePlant(plant: Plant) {
    insertPlant(
        id = plant.id,
        roomId = plant.roomId,
        name = plant.name,
        species = plant.species,
        potSize = plant.potSize,
        soilType = plant.soilType,
        wateringIntervalDays = plant.wateringIntervalDays.toLong(),
        lastWateredDate = lastWatered(plant),
        photoUrl = plant.photoUrl,
        archiveDate = plant.archiveDate,
        archiveReason = plant.archiveReason,
        x = plant.x.toDouble(),
        y = plant.y.toDouble(),
        syncStatus = plant.syncStatus,
        updatedAt = plant.updatedAt,
    )
}

/** The later of the server's last watering of [plant] and any watering still waiting on this device. */
private suspend fun GreenPrintDbQueries.lastWatered(plant: Plant): String? {
    val pending = selectUnsyncedWateringEventsByPlantId(plant.id).awaitAsList().map { it.timestamp }
    return (pending + listOfNotNull(plant.lastWateredDate)).maxByOrNull { Instant.parse(it) }
}

/** Drops a plant and its waterings from the cache. */
private suspend fun GreenPrintDbQueries.forgetPlant(id: String) {
    transaction {
        deleteWateringEventsByPlantId(id)
        deletePlant(id)
    }
}

private fun PlantEntity.toDomain(): Plant = Plant(
    id = id,
    roomId = roomId,
    name = name,
    species = species,
    potSize = potSize,
    soilType = soilType,
    wateringIntervalDays = wateringIntervalDays.toInt(),
    lastWateredDate = lastWateredDate,
    photoUrl = photoUrl,
    archiveDate = archiveDate,
    archiveReason = archiveReason,
    x = x.toFloat(),
    y = y.toFloat(),
    syncStatus = syncStatus,
    updatedAt = updatedAt,
)

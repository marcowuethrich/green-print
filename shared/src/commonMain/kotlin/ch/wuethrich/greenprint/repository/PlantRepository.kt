package ch.wuethrich.greenprint.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
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
     * and restarting the app. A call that got no answer, a server fault or an expired session leaves
     * it [SyncStatus.ERROR], and the next [observePlants] of its room sends it again; a move the server
     * turns down ([isRejection]) is given up, and the next refresh shows the server's position.
     */
    suspend fun updatePlantPosition(id: String, x: Float, y: Float): NetworkResult<Plant>

    /**
     * Waters a plant now. Write-through like a move: the watering is logged on the device
     * ([SyncStatus.PENDING]) and the plant's `lastWateredDate` moves before the server is called. A
     * call that got no answer, a server fault or an expired session leaves the watering
     * [SyncStatus.ERROR]; the next [observePlants] of its room sends it again, under the same id, so
     * the server stores it once. A watering the server turns down ([isRejection]) is dropped, and the
     * next refresh shows the server's date.
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

    private val writeLog = PlantWriteLog()
    private val plantLocks = PlantLocks()

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
                val refreshStart = writeLog.mark()
                val result = httpClient.safeRequest<List<Plant>> {
                    url("$baseUrl/rooms/$roomId/plants")
                    method = HttpMethod.Get
                }
                if (result is NetworkResult.Success) {
                    queries.transaction {
                        // A row still waiting for the server keeps its local state (last write wins), and
                        // so does a row written while the request was on the way: the answer may predate it.
                        val keep = queries.selectLocallyChangedPlantIdsByRoomId(roomId).awaitAsList().toSet() +
                            writeLog.writtenSince(refreshStart)
                        queries.deletePlantsByRoomIdExcept(roomId = roomId, keep = keep)
                        result.data.filterNot { it.id in keep }.forEach { queries.cachePlant(it) }
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
            queries.cachePlant(result.data, writeLog)
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
            queries.cachePlant(result.data, writeLog)
        }

        return result
    }

    override suspend fun updatePlantPosition(id: String, x: Float, y: Float): NetworkResult<Plant> {
        val movedAt = clock.now().toEpochMilliseconds()
        writeLog.wrote(id)
        queries.updatePlantPosition(
            x = x.toDouble(),
            y = y.toDouble(),
            syncStatus = SyncStatus.PENDING,
            updatedAt = movedAt,
            id = id,
        )
        return pushPosition(id, movedAt)
    }

    override suspend fun waterPlant(id: String): NetworkResult<Plant> {
        val now = clock.now()
        val wateringId = Uuid.random().toString()
        val wateredAt = now.toString()
        writeLog.wrote(id)
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
        return pushWatering(wateringId, id)
    }

    override suspend fun archivePlant(id: String, request: ArchivePlantRequest): NetworkResult<Plant> {
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/plants/$id/archive")
            method = HttpMethod.Post
            setBody(request)
        }

        if (result is NetworkResult.Success) {
            queries.cachePlant(result.data, writeLog)
        }

        return result
    }

    override suspend fun deletePlant(id: String): NetworkResult<Unit> {
        val result = httpClient.safeRequest<Unit> {
            url("$baseUrl/plants/$id")
            method = HttpMethod.Delete
        }

        if (result is NetworkResult.Success) {
            queries.forgetPlant(id, writeLog)
        }

        return result
    }

    /**
     * Sends what this device changed in the room and the server has not confirmed: waterings, then
     * moves. A write still on its way waits for its answer first ([plantLocks]) and is not sent again.
     */
    private suspend fun syncPendingChanges(roomId: String) {
        queries.selectUnsyncedWateringEventsByRoomId(roomId).awaitAsList().forEach { watering ->
            pushWatering(watering.id, watering.plantId)
        }
        queries.selectUnsyncedPlantsByRoomId(roomId).awaitAsList().forEach { plant ->
            pushPosition(plant.id, plant.updatedAt)
        }
    }

    /**
     * Sends one watering, unless it was sent meanwhile, and settles it: confirmed, the plant takes the
     * server's date unless a newer one waits; turned down, it is dropped.
     */
    private suspend fun pushWatering(wateringId: String, plantId: String): NetworkResult<Plant> =
        plantLocks.withLock(plantId) {
            val watering = queries.selectWateringEventById(wateringId).awaitAsOneOrNull()
            if (watering == null || watering.syncStatus == SyncStatus.SYNCED) {
                return@withLock queries.localPlant(plantId)
            }
            val result = httpClient.safeRequest<Plant> {
                url("$baseUrl/plants/$plantId/waterings")
                method = HttpMethod.Post
                setBody(RecordWateringRequest(id = wateringId, timestamp = watering.timestamp))
            }
            writeLog.wrote(plantId)
            when {
                result is NetworkResult.Success -> queries.transaction {
                    queries.updateWateringEventSyncStatus(syncStatus = SyncStatus.SYNCED, id = wateringId)
                    queries.updatePlantLastWatered(lastWateredDate = queries.lastWatered(result.data), id = plantId)
                }

                result.isGone() -> queries.forgetPlant(plantId)

                result.isRejected() -> queries.deleteWateringEvent(wateringId)

                else -> queries.updateWateringEventSyncStatus(syncStatus = SyncStatus.ERROR, id = wateringId)
            }
            result
        }

    /**
     * Sends the plant's position if it is still the move stamped [movedAt] and unsent; a newer move
     * sends itself. The answer settles the row unless a newer move replaced it meanwhile.
     */
    private suspend fun pushPosition(id: String, movedAt: Long): NetworkResult<Plant> = plantLocks.withLock(id) {
        val plant = queries.selectPlantById(id).awaitAsOneOrNull()
        if (plant == null || plant.updatedAt != movedAt || plant.syncStatus == SyncStatus.SYNCED) {
            return@withLock queries.localPlant(id)
        }
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/plants/$id/position")
            method = HttpMethod.Put
            setBody(UpdatePlantPositionRequest(plant.x.toFloat(), plant.y.toFloat()))
        }
        writeLog.wrote(id)
        if (result.isGone()) {
            queries.forgetPlant(id)
        } else {
            // A move turned down is given up: the row counts as synced, so a refresh shows the server's.
            val settled = result is NetworkResult.Success || result.isRejected()
            val status = if (settled) SyncStatus.SYNCED else SyncStatus.ERROR
            queries.updatePlantSyncStatusIfUnchanged(syncStatus = status, id = id, updatedAt = movedAt)
        }
        result
    }
}

// The plant is gone on the server: there is nothing left to water or move.
private fun NetworkResult<*>.isGone(): Boolean = this is NetworkResult.Failure && error == NetworkError.NotFound

private fun NetworkResult<*>.isRejected(): Boolean = this is NetworkResult.Failure && error.isRejection

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

/** Caches [plant] as written by this device now, so a refresh already on its way keeps it. */
private suspend fun GreenPrintDbQueries.cachePlant(plant: Plant, writeLog: PlantWriteLog) {
    writeLog.wrote(plant.id)
    cachePlant(plant)
}

/** Forgets a plant as written by this device now, so a refresh already on its way does not bring it back. */
private suspend fun GreenPrintDbQueries.forgetPlant(id: String, writeLog: PlantWriteLog) {
    writeLog.wrote(id)
    forgetPlant(id)
}

/** The plant as this device has it, for a write that a newer one or an earlier send took care of. */
private suspend fun GreenPrintDbQueries.localPlant(id: String): NetworkResult<Plant> =
    selectPlantById(id).awaitAsOneOrNull()?.let { NetworkResult.Success(it.toDomain()) }
        ?: NetworkResult.Failure(NetworkError.NotFound)

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

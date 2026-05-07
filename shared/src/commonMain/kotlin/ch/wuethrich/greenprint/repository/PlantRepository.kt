package ch.wuethrich.greenprint.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Plant
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

interface PlantRepository {
    fun observePlants(roomId: String): Flow<NetworkResult<List<Plant>>>
    suspend fun createPlant(roomId: String, request: CreatePlantRequest): NetworkResult<Plant>
    suspend fun deletePlant(id: String): NetworkResult<Unit>
}

class PlantRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val queries: GreenPrintDbQueries
) : BaseRepository(), PlantRepository {

    override fun observePlants(roomId: String): Flow<NetworkResult<List<Plant>>> = flow {
        // 1. Emit from DB reactively
        val dbFlow = queries.selectPlantsByRoomId(roomId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> NetworkResult.Success(entities.map { it.toDomain() }) }

        // 2. Trigger network fetch in background
        coroutineScope {
            launch {
                val result = httpClient.safeRequest<List<Plant>> {
                    url("$baseUrl/rooms/$roomId/plants")
                    method = HttpMethod.Get
                }
                if (result is NetworkResult.Success) {
                    queries.transaction {
                        // Delete plants for this room that are NOT pending sync (simple version)
                        queries.selectPlantsByRoomId(roomId).executeAsList().forEach {
                            queries.deletePlant(it.id)
                        }
                        result.data.forEach { insertPlantIntoDb(it) }
                    }
                }
            }
            emitAll(dbFlow)
        }
    }

    private fun fetchPlantsFromCache(roomId: String): List<Plant> {
        return queries.selectPlantsByRoomId(roomId).executeAsList().map { it.toDomain() }
    }

    override suspend fun createPlant(roomId: String, request: CreatePlantRequest): NetworkResult<Plant> {
        val result = httpClient.safeRequest<Plant> {
            url("$baseUrl/rooms/$roomId/plants")
            method = HttpMethod.Post
            setBody(request)
        }

        if (result is NetworkResult.Success) {
            insertPlantIntoDb(result.data)
        }

        return result
    }

    override suspend fun deletePlant(id: String): NetworkResult<Unit> {
        val result = httpClient.safeRequest<Unit> {
            url("$baseUrl/plants/$id")
            method = HttpMethod.Delete
        }

        if (result is NetworkResult.Success) {
            queries.deletePlant(id)
        }

        return result
    }

    private fun insertPlantIntoDb(plant: Plant) {
        queries.insertPlant(
            id = plant.id,
            roomId = plant.roomId,
            name = plant.name,
            species = plant.species,
            potSize = plant.potSize,
            soilType = plant.soilType,
            wateringIntervalDays = plant.wateringIntervalDays.toLong(),
            lastWateredDate = plant.lastWateredDate,
            photoUrl = plant.photoUrl,
            archiveDate = plant.archiveDate,
            archiveReason = plant.archiveReason,
            x = plant.x.toDouble(),
            y = plant.y.toDouble(),
            syncStatus = plant.syncStatus,
            updatedAt = plant.updatedAt
        )
    }

    private fun ch.wuethrich.greenprint.db.PlantEntity.toDomain(): Plant {
        return Plant(
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
            updatedAt = updatedAt
        )
    }
}

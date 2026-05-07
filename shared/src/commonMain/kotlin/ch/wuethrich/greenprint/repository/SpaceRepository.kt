package ch.wuethrich.greenprint.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.UpdateSpaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface SpaceRepository {
    fun observeSpaces(): Flow<NetworkResult<List<Space>>>
    suspend fun getSpace(id: String): NetworkResult<Space>
    suspend fun createSpace(name: String, icon: String? = null): NetworkResult<Space>
    suspend fun updateSpace(id: String, name: String, icon: String? = null): NetworkResult<Space>
    suspend fun deleteSpace(id: String): NetworkResult<Unit>
}

class SpaceRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val queries: GreenPrintDbQueries
) : BaseRepository(), SpaceRepository {

    override fun observeSpaces(): Flow<NetworkResult<List<Space>>> = flow {
        // 1. Emit from DB reactively
        val dbFlow = queries.selectAllSpaces()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                val spaces = entities.map { entity ->
                    val rooms = queries.selectRoomsBySpaceId(entity.id).executeAsList().map { roomEntity ->
                        Room(roomEntity.id, roomEntity.spaceId, roomEntity.name, roomEntity.icon, roomEntity.syncStatus, roomEntity.updatedAt)
                    }
                    Space(entity.id, entity.name, entity.icon, rooms, entity.syncStatus, entity.updatedAt)
                }
                NetworkResult.Success(spaces)
            }

        // 2. Trigger network fetch in background
        coroutineScope {
            launch {
                val result = httpClient.safeRequest<List<Space>> {
                    url("$baseUrl/spaces")
                    method = HttpMethod.Get
                }
                if (result is NetworkResult.Success) {
                    queries.transaction {
                        queries.deleteAllSpaces()
                        result.data.forEach { space ->
                            queries.insertSpace(space.id, space.name, space.icon, space.syncStatus, space.updatedAt)
                            space.rooms.forEach { room ->
                                queries.insertRoom(room.id, space.id, room.name, room.icon, room.syncStatus, room.updatedAt)
                            }
                        }
                    }
                }
            }
            emitAll(dbFlow)
        }
    }

    private fun fetchSpacesFromCache(): List<Space> {
        return queries.selectAllSpaces().executeAsList().map { entity ->
            val rooms = queries.selectRoomsBySpaceId(entity.id).executeAsList().map { roomEntity ->
                Room(roomEntity.id, roomEntity.spaceId, roomEntity.name, roomEntity.icon, roomEntity.syncStatus, roomEntity.updatedAt)
            }
            Space(entity.id, entity.name, entity.icon, rooms, entity.syncStatus, entity.updatedAt)
        }
    }

    override suspend fun getSpace(id: String): NetworkResult<Space> {
        return httpClient.safeRequest<Space> {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Get
        }.also { result ->
            if (result is NetworkResult.Success) {
                val space = result.data
                queries.insertSpace(space.id, space.name, space.icon, space.syncStatus, space.updatedAt)
                space.rooms.forEach { room ->
                    queries.insertRoom(room.id, space.id, room.name, room.icon, room.syncStatus, room.updatedAt)
                }
            }
        }
    }

    override suspend fun createSpace(name: String, icon: String?): NetworkResult<Space> {
        return httpClient.safeRequest<Space> {
            url("$baseUrl/spaces")
            method = HttpMethod.Post
            setBody(CreateSpaceRequest(name, icon))
        }.also { result ->
            if (result is NetworkResult.Success) {
                val space = result.data
                queries.insertSpace(space.id, space.name, space.icon, space.syncStatus, space.updatedAt)
            }
        }
    }

    override suspend fun updateSpace(id: String, name: String, icon: String?): NetworkResult<Space> {
        return httpClient.safeRequest<Space> {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Put
            setBody(UpdateSpaceRequest(name, icon))
        }.also { result ->
            if (result is NetworkResult.Success) {
                val space = result.data
                queries.insertSpace(space.id, space.name, space.icon, space.syncStatus, space.updatedAt)
            }
        }
    }

    override suspend fun deleteSpace(id: String): NetworkResult<Unit> {
        return httpClient.safeRequest<Unit> {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Delete
        }.also { result ->
            if (result is NetworkResult.Success) {
                queries.deleteSpace(id)
            }
        }
    }
}

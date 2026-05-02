package ch.wuethrich.greenprint.repository

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

interface SpaceRepository {
    suspend fun getSpaces(): NetworkResult<List<Space>>
    suspend fun getSpace(id: String): NetworkResult<Space>
    suspend fun createSpace(name: String, icon: String? = null): NetworkResult<Space>
    suspend fun updateSpace(id: String, name: String, icon: String? = null): NetworkResult<Space>
    suspend fun deleteSpace(id: String): NetworkResult<Unit>
}

class SpaceRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val queries: GreenPrintDbQueries
) : SpaceRepository {
    override suspend fun getSpaces(): NetworkResult<List<Space>> {
        val result = httpClient.safeRequest<List<Space>> {
            url("$baseUrl/spaces")
            method = HttpMethod.Get
        }
        
        if (result is NetworkResult.Success) {
            queries.transaction {
                queries.deleteAllSpaces()
                result.data.forEach { space ->
                    queries.insertSpace(space.id, space.name, space.icon)
                    space.rooms.forEach { room ->
                        queries.insertRoom(room.id, space.id, room.name, room.icon)
                    }
                }
            }
        }
        
        // If network fails, return cached data as success (Offline-first approach)
        val cachedSpaces = queries.selectAllSpaces().executeAsList().map { entity ->
            val rooms = queries.selectRoomsBySpaceId(entity.id).executeAsList().map { roomEntity ->
                Room(roomEntity.id, roomEntity.spaceId, roomEntity.name, roomEntity.icon)
            }
            Space(entity.id, entity.name, entity.icon, rooms)
        }
        
        return if (result is NetworkResult.Failure && cachedSpaces.isNotEmpty()) {
            NetworkResult.Success(cachedSpaces)
        } else {
            result
        }
    }

    override suspend fun getSpace(id: String): NetworkResult<Space> {
        val result = httpClient.safeRequest<Space> {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Get
        }
        
        if (result is NetworkResult.Success) {
            val space = result.data
            queries.insertSpace(space.id, space.name, space.icon)
            space.rooms.forEach { room ->
                queries.insertRoom(room.id, space.id, room.name, room.icon)
            }
        }
        
        return result
    }

    override suspend fun createSpace(name: String, icon: String?): NetworkResult<Space> {
        val result = httpClient.safeRequest<Space> {
            url("$baseUrl/spaces")
            method = HttpMethod.Post
            setBody(CreateSpaceRequest(name, icon))
        }
        
        if (result is NetworkResult.Success) {
            val space = result.data
            queries.insertSpace(space.id, space.name, space.icon)
        }
        
        return result
    }

    override suspend fun updateSpace(id: String, name: String, icon: String?): NetworkResult<Space> {
        val result = httpClient.safeRequest<Space> {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Put
            setBody(UpdateSpaceRequest(name, icon))
        }
        
        if (result is NetworkResult.Success) {
            val space = result.data
            queries.insertSpace(space.id, space.name, space.icon)
        }
        
        return result
    }

    override suspend fun deleteSpace(id: String): NetworkResult<Unit> {
        val result = httpClient.safeRequest<Unit> {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Delete
        }
        
        if (result is NetworkResult.Success) {
            queries.deleteSpace(id)
        }
        
        return result
    }
}

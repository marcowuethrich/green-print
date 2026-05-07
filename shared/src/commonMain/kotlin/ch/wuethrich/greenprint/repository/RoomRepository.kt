package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.NetworkResult
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod

interface RoomRepository {
    suspend fun createRoom(spaceId: String, name: String, icon: String? = null): NetworkResult<Room>
    suspend fun updateRoom(id: String, name: String, icon: String? = null): NetworkResult<Room>
    suspend fun deleteRoom(id: String): NetworkResult<Unit>
}

class RoomRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val queries: GreenPrintDbQueries
) : RoomRepository {
    override suspend fun createRoom(spaceId: String, name: String, icon: String?): NetworkResult<Room> {
        val result = httpClient.safeRequest<Room> {
            url("$baseUrl/spaces/$spaceId/rooms")
            method = HttpMethod.Post
            setBody(CreateRoomRequest(name, icon))
        }
        
        if (result is NetworkResult.Success) {
            val room = result.data
            queries.insertRoom(room.id, room.spaceId, room.name, room.icon, room.syncStatus, room.updatedAt)
        }
        
        return result
    }

    override suspend fun updateRoom(id: String, name: String, icon: String?): NetworkResult<Room> {
        val result = httpClient.safeRequest<Room> {
            url("$baseUrl/rooms/$id")
            method = HttpMethod.Put
            setBody(UpdateRoomRequest(name, icon))
        }
        
        if (result is NetworkResult.Success) {
            val room = result.data
            queries.insertRoom(room.id, room.spaceId, room.name, room.icon, room.syncStatus, room.updatedAt)
        }
        
        return result
    }

    override suspend fun deleteRoom(id: String): NetworkResult<Unit> {
        val result = httpClient.safeRequest<Unit> {
            url("$baseUrl/rooms/$id")
            method = HttpMethod.Delete
        }
        
        if (result is NetworkResult.Success) {
            queries.deleteRoom(id)
        }
        
        return result
    }
}

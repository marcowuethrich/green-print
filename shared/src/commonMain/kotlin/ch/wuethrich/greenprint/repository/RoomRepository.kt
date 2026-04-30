package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

interface RoomRepository {
    suspend fun createRoom(spaceId: Long, name: String): Room
    suspend fun updateRoom(id: Long, name: String): Room
    suspend fun deleteRoom(id: Long)
}

class RoomRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : RoomRepository {
    override suspend fun createRoom(spaceId: Long, name: String): Room {
        return httpClient.post("$baseUrl/spaces/$spaceId/rooms") {
            setBody(CreateRoomRequest(name))
        }.body()
    }

    override suspend fun updateRoom(id: Long, name: String): Room {
        return httpClient.put("$baseUrl/rooms/$id") {
            setBody(UpdateRoomRequest(name))
        }.body()
    }

    override suspend fun deleteRoom(id: Long) {
        httpClient.delete("$baseUrl/rooms/$id")
    }
}

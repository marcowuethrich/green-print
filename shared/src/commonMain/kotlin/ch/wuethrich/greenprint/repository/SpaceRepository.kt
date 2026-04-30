package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.UpdateSpaceRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

interface SpaceRepository {
    suspend fun getSpaces(): List<Space>
    suspend fun getSpace(id: Long): Space
    suspend fun createSpace(name: String): Space
    suspend fun updateSpace(id: Long, name: String): Space
    suspend fun deleteSpace(id: Long)
}

class SpaceRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : SpaceRepository {
    override suspend fun getSpaces(): List<Space> {
        return httpClient.get("$baseUrl/spaces").body()
    }

    override suspend fun getSpace(id: Long): Space {
        return httpClient.get("$baseUrl/spaces/$id").body()
    }

    override suspend fun createSpace(name: String): Space {
        return httpClient.post("$baseUrl/spaces") {
            setBody(CreateSpaceRequest(name))
        }.body()
    }

    override suspend fun updateSpace(id: Long, name: String): Space {
        return httpClient.put("$baseUrl/spaces/$id") {
            setBody(UpdateSpaceRequest(name))
        }.body()
    }

    override suspend fun deleteSpace(id: Long) {
        httpClient.delete("$baseUrl/spaces/$id")
    }
}

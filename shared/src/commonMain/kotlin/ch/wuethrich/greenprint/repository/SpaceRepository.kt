package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.NetworkResult
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
    private val baseUrl: String
) : SpaceRepository {
    override suspend fun getSpaces(): NetworkResult<List<Space>> {
        return httpClient.safeRequest {
            url("$baseUrl/spaces")
            method = HttpMethod.Get
        }
    }

    override suspend fun getSpace(id: String): NetworkResult<Space> {
        return httpClient.safeRequest {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Get
        }
    }

    override suspend fun createSpace(name: String, icon: String?): NetworkResult<Space> {
        return httpClient.safeRequest {
            url("$baseUrl/spaces")
            method = HttpMethod.Post
            setBody(CreateSpaceRequest(name, icon))
        }
    }

    override suspend fun updateSpace(id: String, name: String, icon: String?): NetworkResult<Space> {
        return httpClient.safeRequest {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Put
            setBody(UpdateSpaceRequest(name, icon))
        }
    }

    override suspend fun deleteSpace(id: String): NetworkResult<Unit> {
        return httpClient.safeRequest {
            url("$baseUrl/spaces/$id")
            method = HttpMethod.Delete
        }
    }
}

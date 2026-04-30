package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.GreetingResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface GreetingRepository {
    suspend fun getGreeting(): GreetingResponse
}

class GreetingRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : GreetingRepository {
    override suspend fun getGreeting(): GreetingResponse {
        return httpClient.get("$baseUrl/hello").body()
    }
}

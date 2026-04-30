package ch.wuethrich.greenprint.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingRepositoryTest {

    @Test
    fun `getGreeting returns correct message`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"message": "Hello Test!"}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val repository = GreetingRepositoryImpl(httpClient, "http://localhost:8080")

        val result = repository.getGreeting()

        assertEquals("Hello Test!", result.message)
    }
}

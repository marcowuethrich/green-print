package ch.wuethrich.greenprint.di

import ch.wuethrich.greenprint.BASE_URL
import ch.wuethrich.greenprint.repository.AuthRepository
import ch.wuethrich.greenprint.repository.NoOpAuthRepository
import ch.wuethrich.greenprint.repository.PlantRepository
import ch.wuethrich.greenprint.repository.PlantRepositoryImpl
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.RoomRepositoryImpl
import ch.wuethrich.greenprint.repository.SpaceRepository
import ch.wuethrich.greenprint.repository.SpaceRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single<AuthRepository> { NoOpAuthRepository() }

    single {
        val authRepository = get<AuthRepository>()
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                level = LogLevel.NONE
            }
            install(DefaultRequest) {
                contentType(ContentType.Application.Json)
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        authRepository.getAccessToken()?.let {
                            BearerTokens(it, "")
                        }
                    }
                    sendWithoutRequest { request ->
                        // Send the token for all requests to our own backend
                        request.url.host == "localhost" || request.url.host == "127.0.0.1"
                    }
                }
            }
        }
    }
    single<SpaceRepository> { SpaceRepositoryImpl(get(), BASE_URL, get()) }
    single<RoomRepository> { RoomRepositoryImpl(get(), BASE_URL, get()) }
    single<PlantRepository> { PlantRepositoryImpl(get(), BASE_URL, get()) }
}

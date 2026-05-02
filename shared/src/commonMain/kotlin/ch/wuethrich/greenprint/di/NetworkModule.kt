package ch.wuethrich.greenprint.di

import ch.wuethrich.greenprint.BASE_URL
import ch.wuethrich.greenprint.repository.AuthRepository
import ch.wuethrich.greenprint.repository.NoOpAuthRepository
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
                level = LogLevel.HEADERS
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
                    refreshTokens {
                        // In a real app, we would call a refresh endpoint here.
                        // For NoOp, we just ensure we have a token.
                        authRepository.login()
                        authRepository.getAccessToken()?.let {
                            BearerTokens(it, "")
                        }
                    }
                }
            }
        }
    }
    single<SpaceRepository> { SpaceRepositoryImpl(get(), BASE_URL) }
    single<RoomRepository> { RoomRepositoryImpl(get(), BASE_URL) }
}

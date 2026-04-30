package ch.wuethrich.greenprint.di

import ch.wuethrich.greenprint.BASE_URL
import ch.wuethrich.greenprint.repository.GreetingRepository
import ch.wuethrich.greenprint.repository.GreetingRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
            // Auth (Bearer JWT) installed once OIDC flow is complete — see ADR-001
        }
    }
    single<GreetingRepository> { GreetingRepositoryImpl(get(), BASE_URL) }
}

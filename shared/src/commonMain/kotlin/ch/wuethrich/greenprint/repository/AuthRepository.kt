package ch.wuethrich.greenprint.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthRepository {
    val sessionToken: Flow<String?>
    suspend fun login()
    suspend fun logout()
    fun getAccessToken(): String?
}

/**
 * A "No-Op" implementation for the portfolio phase.
 * Returns a mock token to satisfy interceptors without a real OIDC flow.
 */
class NoOpAuthRepository : AuthRepository {
    private val _sessionToken = MutableStateFlow<String?>(null)
    override val sessionToken: StateFlow<String?> = _sessionToken.asStateFlow()

    override suspend fun login() {
        // Simulating a successful login
        _sessionToken.value = "mock_token_for_portfolio"
    }

    override suspend fun logout() {
        _sessionToken.value = null
    }

    override fun getAccessToken(): String? = _sessionToken.value
}

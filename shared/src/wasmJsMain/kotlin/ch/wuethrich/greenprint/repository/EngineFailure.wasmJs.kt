package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.NetworkError

/**
 * Ktor's fetch engine reports a fetch that never got an answer (offline, the server down, CORS) as a
 * `kotlin.Error`, not an `IOException`: in the browser that is how being offline looks.
 */
actual fun Throwable.toEngineFailure(): NetworkError? = when (this) {
    is Exception -> NetworkError.Unknown(message)
    is Error -> NetworkError.Connectivity
    else -> null
}

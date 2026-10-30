package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.NetworkError

/**
 * The [NetworkError] for a failure the HTTP engine threw that is neither an HTTP error nor an
 * `IOException`, or null when it is no engine failure and must be thrown on.
 */
expect fun Throwable.toEngineFailure(): NetworkError?

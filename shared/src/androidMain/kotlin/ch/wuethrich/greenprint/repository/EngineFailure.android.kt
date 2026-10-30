package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.NetworkError

actual fun Throwable.toEngineFailure(): NetworkError? = (this as? Exception)?.let { NetworkError.Unknown(it.message) }

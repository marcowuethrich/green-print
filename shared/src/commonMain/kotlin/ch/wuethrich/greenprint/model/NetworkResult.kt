package ch.wuethrich.greenprint.model

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}

sealed interface NetworkError {
    data object Unauthorized : NetworkError
    data object Forbidden : NetworkError
    data object NotFound : NetworkError
    data object Conflict : NetworkError
    data object ServerError : NetworkError
    data object Connectivity : NetworkError
    data class Validation(val code: String, val field: String? = null) : NetworkError
    data class Unknown(val message: String?) : NetworkError
}

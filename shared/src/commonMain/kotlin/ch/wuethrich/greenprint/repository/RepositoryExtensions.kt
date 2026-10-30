package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.ApiError
import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.IOException

suspend inline fun <reified T> HttpClient.safeRequest(block: HttpRequestBuilder.() -> Unit): NetworkResult<T> = try {
    val response = request(block)
    if (response.status == HttpStatusCode.NoContent) {
        // A 204 has no body to decode; the callers that expect one ask for NetworkResult<Unit>.
        @Suppress("UNCHECKED_CAST")
        NetworkResult.Success(Unit as T)
    } else {
        NetworkResult.Success(response.body())
    }
} catch (e: ResponseException) {
    NetworkResult.Failure(e.toNetworkError())
} catch (_: IOException) {
    NetworkResult.Failure(NetworkError.Connectivity)
} catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
    // The network boundary: any other engine failure (a browser fetch error, a malformed body) becomes a
    // result, and a cancelled caller stays cancelled instead of seeing a failure. What is no engine
    // failure (an OutOfMemoryError on the JVM) is thrown on.
    currentCoroutineContext().ensureActive()
    NetworkResult.Failure(e.toEngineFailure() ?: throw e)
}

suspend fun ResponseException.toNetworkError(): NetworkError = when (response.status) {
    HttpStatusCode.Unauthorized -> NetworkError.Unauthorized

    HttpStatusCode.Forbidden -> NetworkError.Forbidden

    HttpStatusCode.NotFound -> NetworkError.NotFound

    HttpStatusCode.Conflict -> NetworkError.Conflict

    // A 400 always means the request is wrong, even without the server's error body.
    HttpStatusCode.BadRequest -> parseValidation()
        ?: NetworkError.Validation(code = "bad_request")

    else -> if (response.status.value >= SERVER_ERRORS) {
        NetworkError.ServerError
    } else {
        NetworkError.Unknown("Status: ${response.status}")
    }
}

private const val SERVER_ERRORS = 500

private suspend fun ResponseException.parseValidation(): NetworkError.Validation? = try {
    val apiError: ApiError = response.body()
    NetworkError.Validation(apiError.code, apiError.field)
} catch (_: Exception) {
    null
}

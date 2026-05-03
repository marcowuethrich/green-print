package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.io.IOException

suspend inline fun <reified T> HttpClient.safeRequest(
    block: HttpRequestBuilder.() -> Unit
): NetworkResult<T> = try {
    val response = request(block)
    NetworkResult.Success(response.body())
} catch (e: ResponseException) {
    NetworkResult.Failure(e.toNetworkError())
} catch (e: IOException) {
    NetworkResult.Failure(NetworkError.Connectivity)
} catch (e: Exception) {
    NetworkResult.Failure(NetworkError.Unknown(e.message))
}

fun ResponseException.toNetworkError(): NetworkError = when (response.status) {
    HttpStatusCode.Unauthorized -> NetworkError.Unauthorized
    HttpStatusCode.Forbidden -> NetworkError.Forbidden
    HttpStatusCode.NotFound -> NetworkError.NotFound
    HttpStatusCode.Conflict -> NetworkError.Conflict
    HttpStatusCode.InternalServerError -> NetworkError.ServerError
    else -> NetworkError.Unknown("Status: ${response.status}")
}

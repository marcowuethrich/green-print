package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException

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

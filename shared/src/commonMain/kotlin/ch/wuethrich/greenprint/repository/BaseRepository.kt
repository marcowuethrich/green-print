package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.NetworkError
import ch.wuethrich.greenprint.model.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseRepository {
    /**
     * Helper to perform an offline-first data fetch.
     * It first emits the [cached] data, then tries to [fetch] from network.
     * If [fetch] is successful, it calls [save] to update the cache and emits the new data.
     */
    protected fun <T> offlineFirst(
        cached: suspend () -> T,
        fetch: suspend () -> NetworkResult<T>,
        save: suspend (T) -> Unit
    ): Flow<NetworkResult<T>> = flow {
        // 1. Emit current cache
        emit(NetworkResult.Success(cached()))

        // 2. Fetch from network
        try {
            val result = fetch()
            if (result is NetworkResult.Success) {
                save(result.data)
                // 3. Emit updated data from cache to ensure consistency with DB state
                emit(NetworkResult.Success(cached()))
            } else if (result is NetworkResult.Failure) {
                // Optionally emit failure if cache was empty or we want to show error
                // For a professional offline-first, we might just keep the cached data
                emit(result)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(NetworkResult.Failure(NetworkError.Unknown(e.message ?: e.toString())))
        }
    }
}

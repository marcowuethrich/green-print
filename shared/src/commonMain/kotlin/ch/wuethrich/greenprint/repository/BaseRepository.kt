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
        val initialCache = cached()
        emit(NetworkResult.Success(initialCache))

        // 2. Fetch from network
        try {
            val result = fetch()
            if (result is NetworkResult.Success) {
                save(result.data)
                
                // 3. Try to emit updated data from cache
                val updatedCache = cached()
                
                // Fallback: If cache is empty but network gave us data, the DB might be non-functional (Wasm mock)
                // We check if T is a List and handle accordingly.
                val isCacheEmpty = (updatedCache as? Collection<*>)?.isEmpty() ?: (updatedCache == null)
                val isNetworkNotEmpty = (result.data as? Collection<*>)?.isNotEmpty() ?: (result.data != null)
                
                if (isCacheEmpty && isNetworkNotEmpty) {
                    emit(result)
                } else {
                    emit(NetworkResult.Success(updatedCache))
                }
            } else if (result is NetworkResult.Failure) {
                // If network fails and cache was empty, we emit the failure
                if ((initialCache as? Collection<*>)?.isEmpty() != false) {
                    emit(result)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(NetworkResult.Failure(NetworkError.Unknown(e.message ?: e.toString())))
        }
    }
}

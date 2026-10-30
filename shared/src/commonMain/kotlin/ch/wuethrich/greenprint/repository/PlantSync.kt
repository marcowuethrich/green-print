package ch.wuethrich.greenprint.repository

import ch.wuethrich.greenprint.model.NetworkError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The plants this device wrote to its cache, in the order of the writes. A refresh takes a [mark]
 * before it asks the server and keeps every plant [writtenSince] then: the server's answer may be
 * older than a write it confirmed while the refresh was on the way.
 */
internal class PlantWriteLog {
    private val mutex = Mutex()
    private var writes = 0L
    private val lastWrite = mutableMapOf<String, Long>()

    suspend fun mark(): Long = mutex.withLock { writes }

    suspend fun wrote(plantId: String) {
        mutex.withLock { lastWrite[plantId] = ++writes }
    }

    suspend fun writtenSince(mark: Long): Set<String> = mutex.withLock {
        lastWrite.filterValues { it > mark }.keys.toSet()
    }
}

/** One lock per plant, so the plant's writes reach the server one at a time and in order. */
internal class PlantLocks {
    private val mutex = Mutex()
    private val locks = mutableMapOf<String, Mutex>()

    suspend fun <T> withLock(plantId: String, action: suspend () -> T): T =
        mutex.withLock { locks.getOrPut(plantId) { Mutex() } }.withLock { action() }
}

/**
 * Whether the server turned a write down for good, so sending it again cannot help: it answered the
 * write as invalid, conflicting or forbidden. No answer, a server fault, an expired session or an
 * error the app cannot place leave the write to be sent again.
 */
internal val NetworkError.isRejection: Boolean
    get() = this is NetworkError.Validation || this == NetworkError.Conflict || this == NetworkError.Forbidden

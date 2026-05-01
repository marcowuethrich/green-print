package ch.wuethrich.greenprint.util

import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> withTransaction(block: suspend () -> T): T {
    return Panache.withTransaction {
        CoroutineScope(Dispatchers.Unconfined).async {
            block()
        }.asUni()
    }.awaitSuspending()
}
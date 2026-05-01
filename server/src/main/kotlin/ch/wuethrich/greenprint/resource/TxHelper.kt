package ch.wuethrich.greenprint.resource

import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> withTx(block: suspend () -> T): T {
    return Panache.withTransaction {
        CoroutineScope(Dispatchers.Unconfined).async {
            block()
        }.asUni()
    }.awaitSuspending()
}

package ch.wuethrich.greenprint.resource

import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> withTx(block: suspend () -> T): T = coroutineScope {
    Panache.withTransaction {
        async(Dispatchers.Unconfined) { block() }.asUni()
    }.awaitSuspending()
}

/**
 * Performs a direct JPQL delete bypassing the Hibernate session cache,
 * avoiding un-delete conflicts when the entity was previously loaded in the same session.
 */
suspend fun PanacheEntityBase.deleteDirectly(id: UUID?): Boolean {
    val entityName = this::class.simpleName
    val deletedCount = Panache.getSession().chain { session ->
        session.createMutationQuery("DELETE FROM $entityName WHERE id = :id")
            .setParameter("id", id)
            .executeUpdate()
    }.awaitSuspending()
    return deletedCount > 0
}

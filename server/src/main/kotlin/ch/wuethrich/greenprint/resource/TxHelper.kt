package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> withTx(block: suspend () -> T): T {
    return Panache.withTransaction {
        CoroutineScope(Dispatchers.Unconfined).async {
            block()
        }.asUni()
    }.awaitSuspending()
}

fun requireOwner(entityUser: UserEntity, currentUser: UserEntity) {
    if (entityUser.id != currentUser.id) throw WebApplicationException(Response.Status.FORBIDDEN)
}

/**
 * Universal extension to perform a direct JPQL delete on ANY PanacheEntityBase instance,
 * bypassing the Hibernate session cache to avoid un-delete conflicts.
 */
suspend fun PanacheEntityBase.deleteDirectly(id: UUID?): Boolean {
    val entityName = this::class.simpleName

    val deletedCount = Panache.getSession().chain { session ->
        // Changed to createMutationQuery for Hibernate 6+ compatibility
        session.createMutationQuery("DELETE FROM $entityName WHERE id = :id")
            .setParameter("id", id)
            .executeUpdate()
    }.awaitSuspending()

    return deletedCount > 0
}
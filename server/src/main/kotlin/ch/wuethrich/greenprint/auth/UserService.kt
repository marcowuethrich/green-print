package ch.wuethrich.greenprint.auth

import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class UserService {

    @Inject
    lateinit var identity: SecurityIdentity

    val externalId: String
        get() = identity.principal.name

    suspend fun currentUser(): UserEntity {
        val id = externalId
        return UserEntity.find("externalId", id).firstResult().awaitSuspending()
            ?: UserEntity().apply { externalId = id }
                .also { it.persist<UserEntity>().awaitSuspending() }
    }
}
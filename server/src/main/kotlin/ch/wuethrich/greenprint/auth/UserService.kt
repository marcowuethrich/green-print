package ch.wuethrich.greenprint.auth

import ch.wuethrich.greenprint.dev.DevDataSeeder
import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject

@ApplicationScoped
class UserService {

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var devDataSeeder: Instance<DevDataSeeder>

    val externalId: String
        get() = identity.principal.name

    suspend fun currentUser(): UserEntity {
        val id = externalId
        Panache.getSession().chain { session ->
            session.createNativeQuery<Void>(
                "INSERT INTO users (id, external_id) VALUES (gen_random_uuid(), :externalId) " +
                    "ON CONFLICT (external_id) DO NOTHING",
            ).setParameter("externalId", id).executeUpdate()
        }.awaitSuspending()

        val user = UserEntity.find("externalId", id).firstResult().awaitSuspending()
            ?: error("user upsert returned no row for externalId=$id")

        // Trigger dynamic seeding in dev mode if seeder is present
        if (!devDataSeeder.isUnsatisfied) {
            devDataSeeder.get().seedDataForUser(user)
        }

        return user
    }
}

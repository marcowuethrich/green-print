package ch.wuethrich.greenprint.dev

import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

private const val SEED_SPACE =
    "INSERT INTO spaces (id, user_id, name, icon) VALUES (:id, :userId, 'My Urban Jungle', 'home')"

private const val SEED_ROOMS =
    "INSERT INTO rooms (id, space_id, name, icon) VALUES " +
        "(:lrId, :spaceId, 'Living Room', 'chair'), " +
        "(:brId, :spaceId, 'Bedroom', 'bed'), " +
        "(:baId, :spaceId, 'Balcony', 'wb_sunny')"

private const val SEED_PLANTS =
    "INSERT INTO plants (id, room_id, name, species, pot_size, soil_type, watering_interval_days, x, y) VALUES " +
        "(gen_random_uuid(), :lrId, 'Big Monstera', 'Monstera Deliciosa', 'Large', 'Peat Mix', 7, 0.25, 0.75), " +
        "(gen_random_uuid(), :lrId, 'Marble Queen', 'Pothos', 'Small', 'Universal', 10, 0.85, 0.15), " +
        "(gen_random_uuid(), :brId, 'Tall Snake', 'Sansevieria', 'Medium', 'Cactus Soil', 21, 0.5, 0.5)"

@IfBuildProfile("dev")
@ApplicationScoped
class DevDataSeeder {

    suspend fun seedDataForUser(user: UserEntity) {
        val userId = user.id ?: return

        Panache.withTransaction {
            Panache.getSession().chain { session ->
                // Check if user already has data to avoid duplicate seeding
                session.createNativeQuery<Long>("SELECT COUNT(*) FROM spaces WHERE user_id = :userId")
                    .setParameter("userId", userId)
                    .singleResult
                    .chain { count ->
                        if (count as Long > 0) {
                            return@chain session.flush() // Already seeded
                        }

                        // Seed a Space
                        val spaceId = UUID.randomUUID()
                        session.createNativeQuery<Void>(SEED_SPACE)
                            .setParameter("id", spaceId)
                            .setParameter("userId", userId)
                            .executeUpdate()
                            .chain { _ ->
                                // Seed Rooms
                                val lrId = UUID.randomUUID()
                                val brId = UUID.randomUUID()
                                val baId = UUID.randomUUID()

                                session.createNativeQuery<Void>(SEED_ROOMS)
                                    .setParameter("lrId", lrId)
                                    .setParameter("brId", brId)
                                    .setParameter("baId", baId)
                                    .setParameter("spaceId", spaceId)
                                    .executeUpdate()
                                    .chain { _ ->
                                        // Seed Plants
                                        session.createNativeQuery<Void>(SEED_PLANTS)
                                            .setParameter("lrId", lrId)
                                            .setParameter("brId", brId)
                                            .executeUpdate()
                                    }
                            }
                    }
            }
        }.awaitSuspending()
    }
}

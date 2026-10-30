package ch.wuethrich.greenprint.dev

import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

// Icons are ids from the client's IconRegistry; the balcony has none and shows the default.
private const val SEED_SPACE =
    "INSERT INTO spaces (id, user_id, name, icon) VALUES (:id, :userId, 'My Urban Jungle', 'house_24')"

private const val SEED_ROOMS =
    "INSERT INTO rooms (id, space_id, name, icon) VALUES " +
        "(:lrId, :spaceId, 'Living Room', 'living_24'), " +
        "(:brId, :spaceId, 'Bedroom', 'bedroom_24'), " +
        "(:baId, :spaceId, 'Balcony', NULL)"

// Each room's plants sit apart on its blueprint (x, y in 0.0..1.0), so every marker can be grabbed. The
// last waterings are spread so the list shows every hydration level: healthy (more than 70% of the
// interval left), thirsty (30-70%) and dry (under 30%, or overdue).
private const val SEED_PLANTS =
    "INSERT INTO plants (id, room_id, name, species, pot_size, soil_type, watering_interval_days, " +
        "last_watered_date, x, y, updated_at) VALUES " +
        "(gen_random_uuid(), :lrId, 'Big Monstera', 'Monstera Deliciosa', 'Large', 'Peat Mix', 7, " +
        "now() - interval '1 day', 0.2, 0.75, now()), " +
        "(gen_random_uuid(), :lrId, 'Marble Queen', 'Pothos', 'Small', 'Universal', 10, " +
        "now() - interval '5 days', 0.8, 0.2, now()), " +
        "(gen_random_uuid(), :lrId, 'Fiddle Leaf', 'Ficus Lyrata', 'Large', 'Universal', 9, " +
        "now() - interval '11 days', 0.75, 0.7, now()), " +
        "(gen_random_uuid(), :brId, 'Tall Snake', 'Sansevieria', 'Medium', 'Cactus Soil', 21, " +
        "now() - interval '3 days', 0.3, 0.3, now()), " +
        "(gen_random_uuid(), :brId, 'Peace Lily', 'Spathiphyllum', 'Medium', 'Peat Mix', 5, " +
        "now() - interval '4 days', 0.7, 0.65, now()), " +
        "(gen_random_uuid(), :baId, 'Rosemary', 'Salvia Rosmarinus', 'Small', 'Herb Mix', 4, " +
        "now() - interval '2 days', 0.5, 0.5, now())"

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
                                            .setParameter("baId", baId)
                                            .executeUpdate()
                                    }
                            }
                    }
            }
        }.awaitSuspending()
    }
}

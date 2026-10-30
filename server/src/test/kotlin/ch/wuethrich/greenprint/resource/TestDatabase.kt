package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.PlantEntity
import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.UserEntity
import ch.wuethrich.greenprint.entity.WateringEventEntity
import org.hibernate.reactive.mutiny.Mutiny

/**
 * Empties every table, children before parents, so a test starts from a clean database whatever ran
 * before it. Hibernate's test DDL creates the foreign keys without ON DELETE CASCADE.
 */
fun Mutiny.SessionFactory.clearDatabase() {
    withTransaction { _ ->
        WateringEventEntity
            .deleteAll()
            .flatMap { PlantEntity.deleteAll() }
            .flatMap { RoomEntity.deleteAll() }
            .flatMap { SpaceEntity.deleteAll() }
            .flatMap { UserEntity.deleteAll() }
    }.await().indefinitely()
}

package ch.wuethrich.greenprint.entity

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** One watering of a plant (ADR-007). The id comes from the client, so a resent watering is stored once. */
@Entity
@Table(name = "watering_events")
class WateringEventEntity : PanacheEntityBase {
    @Id
    lateinit var id: UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    lateinit var plant: PlantEntity

    @Column(name = "watered_at")
    lateinit var wateredAt: Instant

    companion object : PanacheCompanionBase<WateringEventEntity, UUID>
}

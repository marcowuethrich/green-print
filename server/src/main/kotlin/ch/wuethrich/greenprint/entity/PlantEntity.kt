package ch.wuethrich.greenprint.entity

import ch.wuethrich.greenprint.model.Plant
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "plants", uniqueConstraints = [UniqueConstraint(columnNames = ["room_id", "name"])])
class PlantEntity : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
    lateinit var room: RoomEntity

    lateinit var name: String

    var species: String? = null

    @Column(name = "pot_size")
    var potSize: String? = null

    @Column(name = "soil_type")
    var soilType: String? = null

    @Column(name = "watering_interval_days")
    var wateringIntervalDays: Int = 0

    @Column(name = "last_watered_date")
    var lastWateredDate: Instant? = null

    @Column(name = "photo_url")
    var photoUrl: String? = null

    @Column(name = "archive_date")
    var archiveDate: Instant? = null

    @Column(name = "archive_reason")
    var archiveReason: String? = null

    var x: Float = 0f
    var y: Float = 0f

    companion object : PanacheCompanionBase<PlantEntity, UUID>
}

fun PlantEntity.toDto() = Plant(
    id = id!!.toString(),
    roomId = room.id!!.toString(),
    name = name,
    species = species,
    potSize = potSize,
    soilType = soilType,
    wateringIntervalDays = wateringIntervalDays,
    lastWateredDate = lastWateredDate?.toString(),
    photoUrl = photoUrl,
    archiveDate = archiveDate?.toString(),
    archiveReason = archiveReason,
    x = x,
    y = y,
    syncStatus = "S_SYNCED",
    updatedAt = 0L,
)

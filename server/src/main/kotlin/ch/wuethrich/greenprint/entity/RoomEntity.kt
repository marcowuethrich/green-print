package ch.wuethrich.greenprint.entity

import ch.wuethrich.greenprint.model.Room
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(name = "rooms", uniqueConstraints = [UniqueConstraint(columnNames = ["space_id", "name"])])
class RoomEntity : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    lateinit var name: String

    var icon: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "space_id")
    lateinit var space: SpaceEntity

    companion object : PanacheCompanionBase<RoomEntity, UUID>
}

fun RoomEntity.toDto() = Room(
    id = id!!.toString(),
    spaceId = space.id!!.toString(),
    name = name,
    icon = icon,
)

package ch.wuethrich.greenprint.entity

import ch.wuethrich.greenprint.model.Space
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(name = "spaces", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "name"])])
class SpaceEntity : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    lateinit var user: UserEntity

    lateinit var name: String

    var icon: String? = null

    @OneToMany(mappedBy = "space", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var rooms: MutableList<RoomEntity> = mutableListOf()

    companion object : PanacheCompanionBase<SpaceEntity, UUID>
}

fun SpaceEntity.toDto() = Space(
    id = id!!.toString(),
    name = name,
    icon = icon,
    rooms = rooms.map { it.toDto() }
)

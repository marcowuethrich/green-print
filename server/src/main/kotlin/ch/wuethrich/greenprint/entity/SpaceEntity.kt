package ch.wuethrich.greenprint.entity

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
import java.util.UUID

@Entity
@Table(name = "spaces")
class SpaceEntity : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    lateinit var user: UserEntity

    lateinit var name: String

    @OneToMany(mappedBy = "space", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var rooms: MutableList<RoomEntity> = mutableListOf()

    companion object : PanacheCompanionBase<SpaceEntity, UUID>
}

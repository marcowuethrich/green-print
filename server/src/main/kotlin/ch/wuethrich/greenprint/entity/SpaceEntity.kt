package ch.wuethrich.greenprint.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "spaces")
class SpaceEntity : PanacheEntity() {
    lateinit var userId: String
    lateinit var name: String

    @OneToMany(mappedBy = "space", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var rooms: MutableList<RoomEntity> = mutableListOf()

    companion object : PanacheCompanion<SpaceEntity>
}

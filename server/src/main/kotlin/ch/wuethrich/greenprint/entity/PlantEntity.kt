package ch.wuethrich.greenprint.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "plants")
class PlantEntity : PanacheEntity() {
    lateinit var name: String
    var species: String? = null
    var notes: String? = null

    companion object : PanacheCompanion<PlantEntity>
}

package ch.wuethrich.greenprint.entity

import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
class PlantEntityTest {

    @Test
    @TestTransaction
    fun `persist and find plant`() {
        val plant = PlantEntity().apply {
            name = "Monstera"
            species = "Monstera deliciosa"
            notes = "Water weekly"
        }
        plant.persist()

        val found = PlantEntity.findById(plant.id!!)
        assertNotNull(found)
        assertEquals("Monstera", found!!.name)
        assertEquals("Monstera deliciosa", found.species)
        assertEquals("Water weekly", found.notes)
    }

    @Test
    @TestTransaction
    fun `list all plants`() {
        val before = PlantEntity.count()
        PlantEntity().apply { name = "Ficus" }.persist()
        PlantEntity().apply { name = "Pothos" }.persist()

        assertEquals(before + 2, PlantEntity.count())
    }

    @Test
    @TestTransaction
    fun `update plant`() {
        val plant = PlantEntity().apply { name = "Cactus" }
        plant.persist()

        plant.notes = "Very low water needs"

        val found = PlantEntity.findById(plant.id!!)
        assertEquals("Very low water needs", found!!.notes)
    }

    @Test
    @TestTransaction
    fun `delete plant`() {
        val plant = PlantEntity().apply { name = "Temporary" }
        plant.persist()
        val id = plant.id!!

        plant.delete()

        assertNull(PlantEntity.findById(id))
    }

    @Test
    @TestTransaction
    fun `optional fields are nullable`() {
        val plant = PlantEntity().apply { name = "Unknown" }
        plant.persist()

        val found = PlantEntity.findById(plant.id!!)!!
        assertNull(found.species)
        assertNull(found.notes)
    }
}

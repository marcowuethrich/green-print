package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.UpdatePlantRequest
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@QuarkusTest
@ResourceLock("database")
class RoomPlantResourceTest {

    @Inject
    lateinit var sf: Mutiny.SessionFactory

    private val snakePlant = CreatePlantRequest(
        name = "Snake Plant",
        species = "Sansevieria trifasciata",
        potSize = "Medium",
        soilType = "Cactus Mix",
        wateringIntervalDays = 14,
    )

    @BeforeEach
    fun setup() {
        sf.clearDatabase()
    }

    @Test
    @TestSecurity(user = "test-user", roles = ["user"])
    fun `test create and get plants`() {
        val roomId = createRoom()

        given()
            .contentType(ContentType.JSON)
            .body(snakePlant)
            .`when`().post("/rooms/$roomId/plants")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", `is`("Snake Plant"))
            .body("species", `is`("Sansevieria trifasciata"))
            .body("potSize", `is`("Medium"))
            .body("soilType", `is`("Cactus Mix"))
            .body("wateringIntervalDays", `is`(14))

        given()
            .`when`().get("/rooms/$roomId/plants")
            .then()
            .statusCode(200)
            .body("size()", `is`(1))
            .body("[0].name", `is`("Snake Plant"))
    }

    @Test
    @TestSecurity(user = "test-user", roles = ["user"])
    fun `test update and archive plant`() {
        val roomId = createRoom()
        val plantId = createPlant(roomId)

        val updateRequest = UpdatePlantRequest(
            name = "Updated Snake Plant",
            species = "Dracaena trifasciata",
            potSize = "Large",
            soilType = "All-purpose Mix",
            wateringIntervalDays = 20,
            photoUrl = "https://example.com/photo.jpg",
            x = 0.1f,
            y = 0.2f,
        )
        given()
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .`when`().put("/plants/$plantId")
            .then()
            .statusCode(200)
            .body("id", `is`(plantId))
            .body("name", `is`("Updated Snake Plant"))
            .body("species", `is`("Dracaena trifasciata"))
            .body("potSize", `is`("Large"))
            .body("soilType", `is`("All-purpose Mix"))
            .body("wateringIntervalDays", `is`(20))
            .body("photoUrl", `is`("https://example.com/photo.jpg"))
            .body("x", `is`(0.1f))
            .body("y", `is`(0.2f))

        given()
            .contentType(ContentType.JSON)
            .body(ArchivePlantRequest(reason = "DIED"))
            .`when`().post("/plants/$plantId/archive")
            .then()
            .statusCode(200)
            .body("archiveReason", `is`("DIED"))
            .body("archiveDate", notNullValue())

        // An archived plant leaves the active list but can still be deleted for good.
        given()
            .`when`().get("/rooms/$roomId/plants")
            .then()
            .statusCode(200)
            .body("size()", `is`(0))
        given()
            .`when`().delete("/plants/$plantId")
            .then()
            .statusCode(204)
    }

    /** Creates a space with one room and returns the room's id. */
    private fun createRoom(): String {
        val space = given()
            .contentType(ContentType.JSON)
            .body(CreateSpaceRequest(name = "Plant Space"))
            .`when`().post("/spaces")
            .then()
            .statusCode(200)
            .extract().`as`(Space::class.java)

        return given()
            .contentType(ContentType.JSON)
            .body(CreateRoomRequest(name = "Living Room"))
            .`when`().post("/spaces/${space.id}/rooms")
            .then()
            .statusCode(201)
            .extract().path("id")
    }

    private fun createPlant(roomId: String): String = given()
        .contentType(ContentType.JSON)
        .body(snakePlant)
        .`when`().post("/rooms/$roomId/plants")
        .then()
        .statusCode(201)
        .extract().`as`(Plant::class.java)
        .id
}

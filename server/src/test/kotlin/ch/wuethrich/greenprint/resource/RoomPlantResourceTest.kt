package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.*
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

    @BeforeEach
    fun setup() {
        sf.clearDatabase()
    }

    @Test
    @TestSecurity(user = "test-user", roles = ["user"])
    fun `test create and get plants`() {
        // 1. Create a space
        val space = given()
            .contentType(ContentType.JSON)
            .body(CreateSpaceRequest(name = "Plant Space"))
            .`when`().post("/spaces")
            .then()
            .statusCode(200)
            .extract().`as`(Space::class.java)

        val spaceId = space.id

        // 2. Create a room
        val roomResponse = given()
            .contentType(ContentType.JSON)
            .body(CreateRoomRequest(name = "Living Room"))
            .`when`().post("/spaces/$spaceId/rooms")
            .then()
            .statusCode(201)
            .extract().response()
        
        val roomId = roomResponse.path<String>("id")

        // 3. Create a plant
        val createRequest = CreatePlantRequest(
            name = "Snake Plant",
            species = "Sansevieria trifasciata",
            potSize = "Medium",
            soilType = "Cactus Mix",
            wateringIntervalDays = 14
        )

        given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .`when`().post("/rooms/$roomId/plants")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", `is`("Snake Plant"))
            .body("species", `is`("Sansevieria trifasciata"))
            .body("potSize", `is`("Medium"))
            .body("soilType", `is`("Cactus Mix"))
            .body("wateringIntervalDays", `is`(14))

        // 4. Get plants for the room
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
        // 1. Create a space
        val space = given()
            .contentType(ContentType.JSON)
            .body(CreateSpaceRequest(name = "Test Space"))
            .`when`().post("/spaces")
            .then()
            .statusCode(200)
            .extract().`as`(Space::class.java)

        val spaceId = space.id

        // 2. Create a room
        val roomResponse = given()
            .contentType(ContentType.JSON)
            .body(CreateRoomRequest(name = "Living Room"))
            .`when`().post("/spaces/$spaceId/rooms")
            .then()
            .statusCode(201)
            .extract().response()
        
        val roomId = roomResponse.path<String>("id")

        // 3. Create a plant
        val createRequest = CreatePlantRequest(
            name = "Snake Plant",
            species = "Sansevieria trifasciata",
            potSize = "Medium",
            soilType = "Cactus Mix",
            wateringIntervalDays = 14
        )

        val plant = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .`when`().post("/rooms/$roomId/plants")
            .then()
            .statusCode(201)
            .extract().`as`(Plant::class.java)

        val plantId = plant.id

        // 4. Update the plant
        val updateRequest = UpdatePlantRequest(
            name = "Updated Snake Plant",
            species = "Dracaena trifasciata",
            potSize = "Large",
            soilType = "All-purpose Mix",
            wateringIntervalDays = 20,
            photoUrl = "https://example.com/photo.jpg",
            x = 10f,
            y = 20f
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
            .body("x", `is`(10.0f))
            .body("y", `is`(20.0f))

        // 5. Archive the plant
        val archiveRequest = ArchivePlantRequest(reason = "DIED")
        given()
            .contentType(ContentType.JSON)
            .body(archiveRequest)
            .`when`().post("/plants/$plantId/archive")
            .then()
            .statusCode(200)
            .body("archiveReason", `is`("DIED"))
            .body("archiveDate", notNullValue())

        // 6. Verify it's gone from active list
        given()
            .`when`().get("/rooms/$roomId/plants")
            .then()
            .statusCode(200)
            .body("size()", `is`(0))

        // 7. Delete the plant (permanent)
        given()
            .`when`().delete("/plants/$plantId")
            .then()
            .statusCode(204)
    }
}

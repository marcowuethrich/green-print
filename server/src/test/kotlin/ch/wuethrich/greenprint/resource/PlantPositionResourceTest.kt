package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.UpdatePlantPositionRequest
import ch.wuethrich.greenprint.model.UpdatePlantRequest
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.equalTo
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

/** `PUT /plants/{id}/position` and the position checks on create and update (spatial_blueprint.md). */
@QuarkusTest
@ResourceLock("database")
class PlantPositionResourceTest {

    @Inject
    lateinit var sf: Mutiny.SessionFactory

    @BeforeEach
    fun setup() {
        sf.clearDatabase()
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `moving a plant saves its position`() {
        val roomId = createRoom()
        val plantId = createPlant(roomId)

        movePlant(plantId, x = 0.25f, y = 0.75f)
            .then().statusCode(200)
            .body("id", equalTo(plantId))
            .body("x", equalTo(0.25f))
            .body("y", equalTo(0.75f))

        given()
            .`when`().get("/rooms/$roomId/plants")
            .then().statusCode(200)
            .body("[0].x", equalTo(0.25f))
            .body("[0].y", equalTo(0.75f))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `the room's edges are valid positions`() {
        val plantId = createPlant(createRoom())

        movePlant(plantId, x = 0f, y = 1f).then().statusCode(200)
        movePlant(plantId, x = 1f, y = 0f).then().statusCode(200)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `x outside the room returns 400`() {
        val plantId = createPlant(createRoom())

        movePlant(plantId, x = 1.5f, y = 0.5f)
            .then().statusCode(400)
            .body("code", equalTo("position_out_of_range"))
            .body("field", equalTo("x"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `y outside the room returns 400`() {
        val plantId = createPlant(createRoom())

        movePlant(plantId, x = 0.5f, y = -0.1f)
            .then().statusCode(400)
            .body("code", equalTo("position_out_of_range"))
            .body("field", equalTo("y"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `an invalid move leaves the position unchanged`() {
        val roomId = createRoom()
        val plantId = createPlant(roomId)
        movePlant(plantId, x = 0.4f, y = 0.6f).then().statusCode(200)

        movePlant(plantId, x = 2f, y = 0.6f).then().statusCode(400)

        given()
            .`when`().get("/rooms/$roomId/plants")
            .then().statusCode(200)
            .body("[0].x", equalTo(0.4f))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `moving an unknown plant returns 404`() {
        movePlant("00000000-0000-0000-0000-000000000000", x = 0.5f, y = 0.5f)
            .then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating a plant outside the room returns 400`() {
        val roomId = createRoom()

        given()
            .contentType(ContentType.JSON)
            .body(CreatePlantRequest(name = "Fern", wateringIntervalDays = 5, x = 0.5f, y = 1.01f))
            .`when`().post("/rooms/$roomId/plants")
            .then().statusCode(400)
            .body("code", equalTo("position_out_of_range"))
            .body("field", equalTo("y"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `updating a plant outside the room returns 400`() {
        val plantId = createPlant(createRoom())

        given()
            .contentType(ContentType.JSON)
            .body(UpdatePlantRequest(name = "Fern", wateringIntervalDays = 5, x = -0.5f, y = 0.5f))
            .`when`().put("/plants/$plantId")
            .then().statusCode(400)
            .body("code", equalTo("position_out_of_range"))
            .body("field", equalTo("x"))
    }

    private fun movePlant(plantId: String, x: Float, y: Float) = given()
        .contentType(ContentType.JSON)
        .body(UpdatePlantPositionRequest(x = x, y = y))
        .`when`().put("/plants/$plantId/position")

    /** Creates a space with one room and returns the room's id. */
    private fun createRoom(): String {
        val space = given()
            .contentType(ContentType.JSON)
            .body(CreateSpaceRequest(name = "Home"))
            .`when`().post("/spaces")
            .then().statusCode(200)
            .extract().`as`(Space::class.java)

        return given()
            .contentType(ContentType.JSON)
            .body(CreateRoomRequest(name = "Living Room"))
            .`when`().post("/spaces/${space.id}/rooms")
            .then().statusCode(201)
            .extract().path("id")
    }

    private fun createPlant(roomId: String): String = given()
        .contentType(ContentType.JSON)
        .body(CreatePlantRequest(name = "Fern", wateringIntervalDays = 5, x = 0.5f, y = 0.5f))
        .`when`().post("/rooms/$roomId/plants")
        .then().statusCode(201)
        .extract().`as`(Plant::class.java)
        .id
}

package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.Space
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class RoomPlantResourceTest {

    @Test
    @TestSecurity(user = "test-user", roles = ["user"])
    fun `test create and get plants`() {
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
}

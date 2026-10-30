package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.model.RecordWateringRequest
import ch.wuethrich.greenprint.model.Space
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.equalTo
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** `POST /plants/{id}/waterings` (inventory.md §5). */
@QuarkusTest
@ResourceLock("database")
class PlantWateringResourceTest {

    @Inject
    lateinit var sf: Mutiny.SessionFactory

    @BeforeEach
    fun setup() {
        sf.clearDatabase()
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `a watering sets the plant's last watered date`() {
        val roomId = createRoom()
        val plantId = createPlant(roomId)
        val wateredAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        val watered = water(plantId, wateredAt = wateredAt)
            .then().statusCode(200)
            .extract().`as`(Plant::class.java)

        assertEquals(wateredAt, Instant.parse(watered.lastWateredDate))
        given()
            .`when`().get("/rooms/$roomId/plants")
            .then().statusCode(200)
            .body("[0].lastWateredDate", equalTo(watered.lastWateredDate))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `a resent watering is recorded once`() {
        val plantId = createPlant(createRoom())
        val wateringId = UUID.randomUUID().toString()
        val wateredAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        water(plantId, wateringId, wateredAt).then().statusCode(200)
        water(plantId, wateringId, wateredAt).then().statusCode(200)

        assertEquals(1L, wateringCount())
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `an older watering is logged but keeps the newer date`() {
        val plantId = createPlant(createRoom())
        val latest = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        water(plantId, wateredAt = latest).then().statusCode(200)

        val watered = water(plantId, wateredAt = latest.minus(2, ChronoUnit.DAYS))
            .then().statusCode(200)
            .extract().`as`(Plant::class.java)

        assertEquals(latest, Instant.parse(watered.lastWateredDate))
        assertEquals(2L, wateringCount())
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `a watering moves the plant's updatedAt`() {
        val roomId = createRoom()
        val created = given()
            .contentType(ContentType.JSON)
            .body(CreatePlantRequest(name = "Fern", wateringIntervalDays = 5))
            .`when`().post("/rooms/$roomId/plants")
            .then().statusCode(201)
            .extract().`as`(Plant::class.java)

        val watered = water(created.id).then().statusCode(200).extract().`as`(Plant::class.java)

        assertTrue(created.updatedAt > 0L)
        assertTrue(watered.updatedAt >= created.updatedAt)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `a watering in the future returns 400`() {
        val plantId = createPlant(createRoom())

        water(plantId, wateredAt = Instant.now().plus(1, ChronoUnit.HOURS))
            .then().statusCode(400)
            .body("code", equalTo("timestamp_invalid"))
            .body("field", equalTo("timestamp"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `a malformed timestamp returns 400`() {
        val plantId = createPlant(createRoom())

        given()
            .contentType(ContentType.JSON)
            .body(RecordWateringRequest(id = UUID.randomUUID().toString(), timestamp = "yesterday"))
            .`when`().post("/plants/$plantId/waterings")
            .then().statusCode(400)
            .body("code", equalTo("timestamp_invalid"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `a watering id that is not a UUID returns 400`() {
        val plantId = createPlant(createRoom())

        water(plantId, wateringId = "first")
            .then().statusCode(400)
            .body("code", equalTo("watering_id_invalid"))
            .body("field", equalTo("id"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `watering an unknown plant returns 404`() {
        water("00000000-0000-0000-0000-000000000000").then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `a watered plant can still be deleted`() {
        val plantId = createPlant(createRoom())
        water(plantId).then().statusCode(200)

        given().`when`().delete("/plants/$plantId").then().statusCode(204)

        assertEquals(0L, wateringCount())
    }

    private fun water(
        plantId: String,
        wateringId: String = UUID.randomUUID().toString(),
        wateredAt: Instant = Instant.now(),
    ) = given()
        .contentType(ContentType.JSON)
        .body(RecordWateringRequest(id = wateringId, timestamp = wateredAt.toString()))
        .`when`().post("/plants/$plantId/waterings")

    private fun wateringCount(): Long = sf
        .withSession { session ->
            session.createSelectionQuery(
                "SELECT count(*) FROM WateringEventEntity",
                Long::class.javaObjectType,
            ).singleResult
        }.await()
        .indefinitely()

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
        .body(CreatePlantRequest(name = "Fern", wateringIntervalDays = 5))
        .`when`().post("/rooms/$roomId/plants")
        .then().statusCode(201)
        .extract().`as`(Plant::class.java)
        .id
}

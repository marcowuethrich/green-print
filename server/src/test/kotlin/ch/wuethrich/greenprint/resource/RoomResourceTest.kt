package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import jakarta.inject.Inject
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.parallel.ResourceLock

@QuarkusTest
@ResourceLock("database")
class RoomResourceTest {

    @Inject
    lateinit var sf: Mutiny.SessionFactory

    @BeforeEach
    fun setup() {
        sf.withTransaction { _ ->
            RoomEntity.deleteAll()
                .flatMap { SpaceEntity.deleteAll() }
                .flatMap { UserEntity.deleteAll() }
        }.await().indefinitely()
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `test room lifecycle`() {
        val spaceId = createSpace("My Apartment")

        val roomId = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Living Room"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(201)
            .body("name", equalTo("Living Room"))
            .body("spaceId", equalTo(spaceId))
            .extract().path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Balcony"}""")
            .`when`().put("/rooms/$roomId")
            .then().statusCode(200)
            .body("name", equalTo("Balcony"))

        given()
            .`when`().delete("/rooms/$roomId")
            .then().statusCode(204)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `duplicate room name in same space returns 409`() {
        val spaceId = createSpace("My Apartment")

        createRoom(spaceId, "Kitchen")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Kitchen"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(409)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `same room name in different spaces is allowed`() {
        val spaceId1 = createSpace("Apartment")
        val spaceId2 = createSpace("Office")

        createRoom(spaceId1, "Kitchen")
        createRoom(spaceId2, "Kitchen")
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating room without icon sets default`() {
        val spaceId = createSpace("Space")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "No Icon Room"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(201)
            .body("icon", equalTo("default_room"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating room with icon stores it`() {
        val spaceId = createSpace("Space")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Custom Icon Room", "icon": "sofa_24"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(201)
            .body("icon", equalTo("sofa_24"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `updating room without icon sets default`() {
        val spaceId = createSpace("Space")
        val roomId = createRoom(spaceId, "Initial Room", icon = "sofa_24")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Updated Room"}""")
            .`when`().put("/rooms/$roomId")
            .then().statusCode(200)
            .body("icon", equalTo("default_room"))
    }

    private fun createSpace(name: String): String {
        return given()
            .contentType(ContentType.JSON)
            .body("""{"name": "$name"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .extract().path("id")
    }

    private fun createRoom(spaceId: String, name: String, icon: String? = null): String {
        val body = if (icon != null) {
            """{"name": "$name", "icon": "$icon"}"""
        } else {
            """{"name": "$name"}"""
        }
        return given()
            .contentType(ContentType.JSON)
            .body(body)
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(201)
            .extract().path("id")
    }
}

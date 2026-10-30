package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@QuarkusTest
@ResourceLock("database")
class RoomResourceTest {

    @Inject
    lateinit var sf: Mutiny.SessionFactory

    @BeforeEach
    fun setup() {
        sf.clearDatabase()
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

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating room with empty name returns 400`() {
        val spaceId = createSpace("Space")
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": ""}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(400)
            .body("code", equalTo("name_blank"))
            .body("field", equalTo("name"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating room with name longer than 128 chars returns 400`() {
        val spaceId = createSpace("Space")
        val tooLong = "a".repeat(129)
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "$tooLong"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(400)
            .body("code", equalTo("name_too_long"))
            .body("field", equalTo("name"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating room trims whitespace from name`() {
        val spaceId = createSpace("Space")
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "  Trimmed  "}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(201)
            .body("name", equalTo("Trimmed"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating room in another user's space returns 404`() {
        val bobsSpaceId = givenSpaceInDb(forUser = "bob", named = "Bobs Apartment")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Hijacked"}""")
            .`when`().post("/spaces/$bobsSpaceId/rooms")
            .then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `updating another user's room returns 404`() {
        val bobsRoomId = givenRoomInDb(forUser = "bob", spaceNamed = "Bobs Apartment", roomNamed = "Bobs Living Room")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Hijacked"}""")
            .`when`().put("/rooms/$bobsRoomId")
            .then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `deleting another user's room returns 404`() {
        val bobsRoomId = givenRoomInDb(forUser = "bob", spaceNamed = "Bobs Apartment", roomNamed = "Bobs Living Room")

        given()
            .`when`().delete("/rooms/$bobsRoomId")
            .then().statusCode(404)
    }

    // Every test seeds bob's data, but the call names the user and the names so the test reads on its own.
    @Suppress("SameParameterValue")
    private fun givenSpaceInDb(forUser: String, named: String): String {
        var spaceId = ""
        sf.withTransaction { _ ->
            val user = UserEntity().apply { externalId = forUser }
            user.persist<UserEntity>().flatMap { persistedUser ->
                val space = SpaceEntity().apply {
                    name = named
                    this.user = persistedUser
                }
                space.persist<SpaceEntity>().invoke { _ -> spaceId = space.id!!.toString() }
            }
        }.await().indefinitely()
        return spaceId
    }

    // Every test seeds bob's data, but the call names the user and the names so the test reads on its own.
    @Suppress("SameParameterValue")
    private fun givenRoomInDb(forUser: String, spaceNamed: String, roomNamed: String): String {
        var roomId = ""
        sf.withTransaction { _ ->
            val user = UserEntity().apply { externalId = forUser }
            user.persist<UserEntity>().flatMap { persistedUser ->
                val space = SpaceEntity().apply {
                    name = spaceNamed
                    this.user = persistedUser
                }
                space.persist<SpaceEntity>().flatMap { persistedSpace ->
                    val room = RoomEntity().apply {
                        name = roomNamed
                        this.space = persistedSpace
                    }
                    room.persist<RoomEntity>().invoke { _ -> roomId = room.id!!.toString() }
                }
            }
        }.await().indefinitely()
        return roomId
    }

    private fun createSpace(name: String): String = given()
        .contentType(ContentType.JSON)
        .body("""{"name": "$name"}""")
        .`when`().post("/spaces")
        .then().statusCode(200)
        .extract().path("id")

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

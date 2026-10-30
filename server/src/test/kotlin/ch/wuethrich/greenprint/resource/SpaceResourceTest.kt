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
import org.hamcrest.Matchers.hasSize
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@QuarkusTest
@ResourceLock("database")
class SpaceResourceTest {

    @Inject
    lateinit var sf: Mutiny.SessionFactory

    @BeforeEach
    fun setup() {
        sf.clearDatabase()
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `duplicate space name for same user returns 409`() {
        createSpace("My Apartment")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "My Apartment"}""")
            .`when`().post("/spaces")
            .then().statusCode(409)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `space name uniqueness is scoped to user - alice side`() {
        givenSpaceInDb(forUser = "bob", named = "Unique-Flat")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Unique-Flat"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
    }

    @Test
    @TestSecurity(user = "bob", roles = ["user"])
    fun `space name uniqueness is scoped to user - bob side`() {
        givenSpaceInDb(forUser = "alice", named = "Unique-Flat")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Unique-Flat"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `test space lifecycle`() {
        val spaceId = createSpace("My Apartment")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Our Villa"}""")
            .`when`().put("/spaces/$spaceId")
            .then().statusCode(200)
            .body("name", equalTo("Our Villa"))

        given()
            .`when`().get("/spaces")
            .then().statusCode(200)
            .body("$", hasSize<Any>(1))
            .body("[0].name", equalTo("Our Villa"))

        given()
            .`when`().delete("/spaces/$spaceId")
            .then().statusCode(204)

        given()
            .`when`().get("/spaces")
            .then().statusCode(200)
            .body("$", hasSize<Any>(0))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating space without icon sets default`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "No Icon Space"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .body("name", equalTo("No Icon Space"))
            .body("icon", equalTo("default_space"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating space with icon stores it`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Custom Icon Space", "icon": "house_24"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .body("name", equalTo("Custom Icon Space"))
            .body("icon", equalTo("house_24"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `updating space without icon sets default`() {
        val spaceId = createSpace("Initial Space", icon = "house_24")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Updated Space"}""")
            .`when`().put("/spaces/$spaceId")
            .then().statusCode(200)
            .body("icon", equalTo("default_space"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating space with empty name returns 400`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": ""}""")
            .`when`().post("/spaces")
            .then().statusCode(400)
            .body("code", equalTo("name_blank"))
            .body("field", equalTo("name"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating space with whitespace-only name returns 400`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "   "}""")
            .`when`().post("/spaces")
            .then().statusCode(400)
            .body("code", equalTo("name_blank"))
            .body("field", equalTo("name"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating space with name longer than 128 chars returns 400`() {
        val tooLong = "a".repeat(129)
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "$tooLong"}""")
            .`when`().post("/spaces")
            .then().statusCode(400)
            .body("code", equalTo("name_too_long"))
            .body("field", equalTo("name"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `creating space trims whitespace from name`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "  Trimmed  "}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .body("name", equalTo("Trimmed"))
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `getting another user's space returns 404`() {
        val bobsSpaceId = givenSpaceInDb(forUser = "bob", named = "Bobs Apartment")

        given()
            .`when`().get("/spaces/$bobsSpaceId")
            .then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `updating another user's space returns 404`() {
        val bobsSpaceId = givenSpaceInDb(forUser = "bob", named = "Bobs Apartment")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Hijacked"}""")
            .`when`().put("/spaces/$bobsSpaceId")
            .then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `deleting another user's space returns 404`() {
        val bobsSpaceId = givenSpaceInDb(forUser = "bob", named = "Bobs Apartment")

        given()
            .`when`().delete("/spaces/$bobsSpaceId")
            .then().statusCode(404)
    }

    private fun createSpace(name: String, icon: String? = null): String {
        val body = if (icon != null) {
            """{"name": "$name", "icon": "$icon"}"""
        } else {
            """{"name": "$name"}"""
        }
        return given()
            .contentType(ContentType.JSON)
            .body(body)
            .`when`().post("/spaces")
            .then().statusCode(200)
            .extract().path("id")
    }

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
}

package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.UserEntity
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import jakarta.inject.Inject
import org.hibernate.reactive.mutiny.Mutiny
import org.junit.jupiter.api.parallel.ResourceLock

@QuarkusTest
@ResourceLock("database")
class SpaceResourceTest {

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
    private fun givenSpaceInDb(forUser: String, named: String) {
        sf.withTransaction { _ ->
            val user = UserEntity().apply { externalId = forUser }
            user.persist<UserEntity>().flatMap { persistedUser ->
                val space = SpaceEntity().apply {
                    name = named
                    this.user = persistedUser
                }
                space.persist<SpaceEntity>()
            }
        }.await().indefinitely()
    }
}

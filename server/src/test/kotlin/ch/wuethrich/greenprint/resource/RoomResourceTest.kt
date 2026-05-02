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
        val spaceId = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "My Apartment"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .extract().path<String>("id")

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
        val spaceId = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "My Apartment"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .extract().path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Kitchen"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Kitchen"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(409)
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `same room name in different spaces is allowed`() {
        val spaceId1 = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Apartment"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .extract().path<String>("id")

        val spaceId2 = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Office"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .extract().path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Kitchen"}""")
            .`when`().post("/spaces/$spaceId1/rooms")
            .then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Kitchen"}""")
            .`when`().post("/spaces/$spaceId2/rooms")
            .then().statusCode(201)
    }
}

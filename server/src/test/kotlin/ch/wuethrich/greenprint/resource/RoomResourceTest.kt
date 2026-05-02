package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
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
            RoomEntity.deleteAll().flatMap { SpaceEntity.deleteAll() }
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
            .extract().path<Int>("id")

        val roomId = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "Living Room"}""")
            .`when`().post("/spaces/$spaceId/rooms")
            .then().statusCode(201)
            .body("name", equalTo("Living Room"))
            .body("spaceId", equalTo(spaceId))
            .extract().path<Int>("id")

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
}

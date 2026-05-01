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

@QuarkusTest
class RoomResourceTest {

    @BeforeEach
    @RunOnVertxContext
    fun setup(asserter: UniAsserter) {
        asserter.execute {
            Panache.withTransaction {
                RoomEntity.deleteAll().flatMap { SpaceEntity.deleteAll() }
            }
        }
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
            .`when`().put("/spaces/$spaceId/rooms/$roomId")
            .then().statusCode(200)
            .body("name", equalTo("Balcony"))

        given()
            .`when`().delete("/spaces/$spaceId/rooms/$roomId")
            .then().statusCode(204)
    }
}

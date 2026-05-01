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
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class SpaceResourceTest {

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
    fun `test space lifecycle`() {
        val spaceId = given()
            .contentType(ContentType.JSON)
            .body("""{"name": "My Apartment"}""")
            .`when`().post("/spaces")
            .then().statusCode(200)
            .body("name", equalTo("My Apartment"))
            .extract().path<Int>("id")

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
}

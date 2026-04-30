package ch.wuethrich.greenprint.resource

import io.quarkus.security.UnauthorizedException
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@QuarkusTest
class GreetingResourceTest {

    @Inject
    lateinit var greetingResource: GreetingResource

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `authenticated request returns greeting`() = runTest {
        val response = greetingResource.hello()
        assertEquals("Hello from Quarkus and Kotlin Coroutines!", response.message)
    }

    @Test
    fun `unauthenticated request is rejected`() = runTest {
        assertThrows<UnauthorizedException> {
            greetingResource.hello()
        }
    }
}
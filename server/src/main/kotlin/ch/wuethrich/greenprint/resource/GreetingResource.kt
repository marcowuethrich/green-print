package ch.wuethrich.greenprint.resource

import io.quarkus.security.Authenticated
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
@Authenticated
open class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    open suspend fun hello(): String {
        return "Hello from Quarkus and Kotlin Coroutines!"
    }
}

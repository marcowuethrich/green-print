package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.model.GreetingResponse
import io.quarkus.security.Authenticated
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
@Authenticated
open class GreetingResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    open suspend fun hello(): GreetingResponse {
        return GreetingResponse("Hello from Quarkus and Kotlin Coroutines!")
    }
}

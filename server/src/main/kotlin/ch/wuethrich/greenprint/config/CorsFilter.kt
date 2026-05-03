package ch.wuethrich.greenprint.config

import io.quarkus.arc.profile.IfBuildProfile
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import java.io.IOException

@Provider
@IfBuildProfile("dev")
class CorsFilter : ContainerResponseFilter {
    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        val headers = responseContext.headers
        // Use putSingle to ensure we don't have duplicate headers if multiple filters run
        headers.putSingle("Access-Control-Allow-Origin", "http://localhost:8081")
        headers.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
        headers.putSingle("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, x-requested-with")
        headers.putSingle("Access-Control-Allow-Credentials", "true")
    }
}

package ch.wuethrich.greenprint.config

import io.quarkus.arc.profile.IfBuildProfile
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities

@Provider
@PreMatching
@IfBuildProfile("dev")
@Priority(Priorities.AUTHENTICATION - 1)
class CorsRequestFilter : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        if (requestContext.method == "OPTIONS") {
            requestContext.abortWith(
                Response.ok()
                    .header("Access-Control-Allow-Origin", "http://localhost:8081")
                    .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                    .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, x-requested-with")
                    .header("Access-Control-Allow-Credentials", "true")
                    .build()
            )
        }
    }
}

package ch.wuethrich.greenprint.auth

import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.security.identity.IdentityProviderManager
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.vertx.http.runtime.security.ChallengeData
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import java.util.Optional

@IfBuildProfile("dev")
@ApplicationScoped
@Priority(1)
class DevBearerAuthMechanism : HttpAuthenticationMechanism {

    override fun authenticate(
        context: RoutingContext,
        identityProviderManager: IdentityProviderManager,
    ): Uni<SecurityIdentity> {
        val authHeader: String? = context.request().getHeader("Authorization")
        
        val token = authHeader
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring(7)
            ?: return Uni.createFrom().optional(Optional.empty())

        return Uni.createFrom().item(
            QuarkusSecurityIdentity.builder()
                .setPrincipal { token }
                .setAnonymous(false)
                .addRole("user")
                .build()
        )
    }

    override fun getCredentialTransport(context: RoutingContext): Uni<HttpCredentialTransport> =
        Uni.createFrom().item(
            HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "Bearer")
        )

    override fun getChallenge(context: RoutingContext): Uni<ChallengeData> =
        Uni.createFrom().item(ChallengeData(401, "WWW-Authenticate", "Bearer"))
}

package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.UpdateSpaceRequest
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SpaceResource {

    @Inject
    lateinit var identity: SecurityIdentity

    private fun getUserId(): String = identity.principal?.name ?: "mock_user"

    @GET
    @Path("/spaces")
    @WithSession
    fun getSpaces(): Uni<List<Space>> =
        SpaceEntity.find("userId", getUserId()).list()
            .map { list -> list.map { it.toDto() } }

    @GET
    @Path("/spaces/{id}")
    @WithSession
    fun getSpace(@PathParam("id") id: Long): Uni<Response> =
        SpaceEntity.findById(id).map { entity ->
            when {
                entity == null -> Response.status(Response.Status.NOT_FOUND).build()
                entity.userId != getUserId() -> Response.status(Response.Status.FORBIDDEN).build()
                else -> Response.ok(entity.toDto()).build()
            }
        }

    @POST
    @Path("/spaces")
    @WithTransaction
    fun createSpace(request: CreateSpaceRequest): Uni<Space> {
        val entity = SpaceEntity().apply {
            name = request.name
            userId = getUserId()
        }
        return entity.persist<SpaceEntity>().map { it.toDto() }
    }

    @PUT
    @Path("/spaces/{id}")
    @WithTransaction
    fun updateSpace(@PathParam("id") id: Long, request: UpdateSpaceRequest): Uni<Response> =
        SpaceEntity.findById(id).chain { entity ->
            when {
                entity == null -> Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build())
                entity.userId != getUserId() -> Uni.createFrom().item(Response.status(Response.Status.FORBIDDEN).build())
                else -> {
                    entity.name = request.name
                    Uni.createFrom().item(Response.ok(entity.toDto()).build())
                }
            }
        }

    @DELETE
    @Path("/spaces/{id}")
    @WithTransaction
    fun deleteSpace(@PathParam("id") id: Long): Uni<Response> =
        SpaceEntity.findById(id).chain { entity ->
            when {
                entity == null -> Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build())
                entity.userId != getUserId() -> Uni.createFrom().item(Response.status(Response.Status.FORBIDDEN).build())
                else -> entity.delete().replaceWith(Response.noContent().build())
            }
        }

    private fun SpaceEntity.toDto() = Space(
        id = id!!,
        name = name,
        rooms = rooms.map { Room(it.id!!, id!!, it.name) }
    )
}

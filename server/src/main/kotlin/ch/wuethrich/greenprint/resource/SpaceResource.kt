package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.auth.UserService
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.UpdateSpaceRequest
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.coroutines.awaitSuspending
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
import java.util.UUID

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SpaceResource {

    @Inject
    lateinit var userService: UserService

    @GET
    @Path("/spaces")
    suspend fun getSpaces(): List<Space> = withTx {
        val user = userService.currentUser()
        SpaceEntity.find("user", user).list().awaitSuspending().map { it.toDto() }
    }

    @GET
    @Path("/spaces/{id}")
    suspend fun getSpace(@PathParam("id") id: UUID): Response = withTx {
        val entity = SpaceEntity.findById(id).awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        requireOwner(entity.user, userService.currentUser())
        Response.ok(entity.toDto()).build()
    }

    @POST
    @Path("/spaces")
    suspend fun createSpace(request: CreateSpaceRequest): Space = withTx {
        val user = userService.currentUser()
        val entity = SpaceEntity().apply {
            name = request.name
            this.user = user
        }
        entity.persist<SpaceEntity>().awaitSuspending().toDto()
    }

    @PUT
    @Path("/spaces/{id}")
    suspend fun updateSpace(@PathParam("id") id: UUID, request: UpdateSpaceRequest): Response = withTx {
        val entity = SpaceEntity.findById(id).awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        requireOwner(entity.user, userService.currentUser())
        entity.name = request.name
        Response.ok(entity.toDto()).build()
    }

    @DELETE
    @Path("/spaces/{id}")
    suspend fun deleteSpace(@PathParam("id") id: UUID): Response = withTx {
        val entity = SpaceEntity.findById(id).awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        requireOwner(entity.user, userService.currentUser())
        entity.delete().awaitSuspending()
        Response.noContent().build()
    }

    private fun SpaceEntity.toDto() = Space(
        id = id!!.toString(),
        name = name,
        rooms = rooms.map { Room(it.id!!.toString(), id!!.toString(), it.name) }
    )
}

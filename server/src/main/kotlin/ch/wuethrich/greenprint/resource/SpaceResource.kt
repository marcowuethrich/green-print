package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.auth.UserService
import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.toDto
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.model.UpdateSpaceRequest
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

@Path("/spaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SpaceResource {

    @Inject
    lateinit var userService: UserService

    @GET
    suspend fun getSpaces(): List<Space> = withTx {
        val user = userService.currentUser()
        SpaceEntity.find("user", user).list().awaitSuspending().map { it.toDto() }
    }

    @GET
    @Path("/{id}")
    suspend fun getSpace(@PathParam("id") id: UUID): Response = withTx {
        val user = userService.currentUser()
        val entity = SpaceEntity.find("id = ?1 and user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        Response.ok(entity.toDto()).build()
    }

    @POST
    suspend fun createSpace(request: CreateSpaceRequest): Space = withTx {
        val name = validateName(request.name)
        val user = userService.currentUser()
        val entity = SpaceEntity().apply {
            this.name = name
            this.icon = request.icon ?: "default_space"
            this.user = user
        }
        entity.persist<SpaceEntity>().awaitSuspending().toDto()
    }

    @PUT
    @Path("/{id}")
    suspend fun updateSpace(@PathParam("id") id: UUID, request: UpdateSpaceRequest): Response = withTx {
        val name = validateName(request.name)
        val user = userService.currentUser()
        val entity = SpaceEntity.find("id = ?1 and user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        entity.name = name
        entity.icon = request.icon ?: "default_space"
        Response.ok(entity.toDto()).build()
    }

    @DELETE
    @Path("/{id}")
    suspend fun deleteSpace(@PathParam("id") id: UUID): Response = withTx {
        val user = userService.currentUser()
        val entity = SpaceEntity.find("id = ?1 and user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        entity.delete().awaitSuspending()
        Response.noContent().build()
    }

    @POST
    @Path("/{spaceId}/rooms")
    suspend fun createRoom(@PathParam("spaceId") spaceId: UUID, request: CreateRoomRequest): Response = withTx {
        val name = validateName(request.name)
        val user = userService.currentUser()
        val space = SpaceEntity.find("id = ?1 and user = ?2", spaceId, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        val room = RoomEntity().apply {
            this.name = name
            this.icon = request.icon ?: "default_room"
            this.space = space
        }
        room.persist<RoomEntity>().awaitSuspending()
        Response.status(Response.Status.CREATED).entity(room.toDto()).build()
    }
}

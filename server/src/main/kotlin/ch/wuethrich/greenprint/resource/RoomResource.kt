package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.auth.UserService
import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.toDto
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
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
class RoomResource {

    @Inject
    lateinit var userService: UserService

    @POST
    @Path("/spaces/{spaceId}/rooms")
    suspend fun createRoom(
        @PathParam("spaceId") spaceId: UUID,
        request: CreateRoomRequest
    ): Response = withTx {
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

    @PUT
    @Path("/rooms/{id}")
    suspend fun updateRoom(
        @PathParam("id") id: UUID,
        request: UpdateRoomRequest
    ): Response = withTx {
        val name = validateName(request.name)
        val user = userService.currentUser()
        val room = RoomEntity.find("id = ?1 and space.user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        room.name = name
        room.icon = request.icon ?: "default_room"
        Response.ok(room.toDto()).build()
    }

    @DELETE
    @Path("/rooms/{id}")
    suspend fun deleteRoom(
        @PathParam("id") id: UUID
    ): Response = withTx {
        val user = userService.currentUser()
        val room = RoomEntity.find("id = ?1 and space.user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        room.deleteDirectly(room.id)
        Response.noContent().build()
    }
}

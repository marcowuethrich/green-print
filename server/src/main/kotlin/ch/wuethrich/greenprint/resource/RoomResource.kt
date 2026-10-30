package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.auth.UserService
import ch.wuethrich.greenprint.entity.PlantEntity
import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.toDto
import ch.wuethrich.greenprint.model.ApiError
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class RoomResource {

    @Inject
    lateinit var userService: UserService

    @PUT
    @Path("/{id}")
    suspend fun updateRoom(@PathParam("id") id: UUID, request: UpdateRoomRequest): Response = withTx {
        val name = validateName(request.name)
        val user = userService.currentUser()
        val room = RoomEntity.find("id = ?1 and space.user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        room.name = name
        room.icon = request.icon ?: "default_room"
        Response.ok(room.toDto()).build()
    }

    /**
     * Deletes an empty room. A room that still holds plants, archived ones included (they are its
     * history, ADR-009), answers 409 `room_not_empty`, as the app refuses it (inventory.md §4).
     */
    @DELETE
    @Path("/{id}")
    suspend fun deleteRoom(@PathParam("id") id: UUID): Response = withTx {
        val user = userService.currentUser()
        val room = RoomEntity.find("id = ?1 and space.user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        if (PlantEntity.count("room", room).awaitSuspending() > 0) {
            return@withTx Response
                .status(Response.Status.CONFLICT)
                .entity(ApiError(code = "room_not_empty"))
                .build()
        }
        room.deleteDirectly(room.id)
        Response.noContent().build()
    }
}

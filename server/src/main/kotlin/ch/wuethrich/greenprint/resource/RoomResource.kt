package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
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

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class RoomResource {

    @Inject
    lateinit var identity: SecurityIdentity

    private fun getUserId(): String = identity.principal?.name ?: "mock_user"

    @POST
    @Path("/spaces/{spaceId}/rooms")
    suspend fun createRoom(
        @PathParam("spaceId") spaceId: Long,
        request: CreateRoomRequest
    ): Response = withTx {
        val space = SpaceEntity.findById(spaceId).awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        if (space.userId != getUserId()) {
            return@withTx Response.status(Response.Status.FORBIDDEN).build()
        }

        val room = RoomEntity().apply {
            name = request.name
            this.space = space
        }
        room.persist<RoomEntity>().awaitSuspending()
        Response.status(Response.Status.CREATED).entity(room.toDto()).build()
    }

    @PUT
    @Path("/rooms/{id}")
    suspend fun updateRoom(
        @PathParam("id") id: Long,
        request: UpdateRoomRequest
    ): Response = withTx {
        val room = RoomEntity.findById(id).awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        if (room.space.userId != getUserId()) {
            return@withTx Response.status(Response.Status.FORBIDDEN).build()
        }

        room.name = request.name
        Response.ok(room.toDto()).build()
    }

    @DELETE
    @Path("/rooms/{id}")
    suspend fun deleteRoom(
        @PathParam("id") id: Long
    ): Response = withTx {
        val room = RoomEntity.findById(id).awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        if (room.space.userId != getUserId()) {
            return@withTx Response.status(Response.Status.FORBIDDEN).build()
        }

        room.deleteDirectly()
        Response.noContent().build()
    }

    private fun RoomEntity.toDto() = Room(
        id = id!!,
        spaceId = space.id!!,
        name = name
    )
}

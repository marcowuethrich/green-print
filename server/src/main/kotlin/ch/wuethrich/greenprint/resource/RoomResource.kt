package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.quarkus.security.Authenticated
import jakarta.transaction.Transactional
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

    @POST
    @Path("/spaces/{spaceId}/rooms")
    @Transactional
    fun createRoom(@PathParam("spaceId") spaceId: Long, request: CreateRoomRequest): Response {
        val space = SpaceEntity.findById(spaceId) ?: return Response.status(Response.Status.NOT_FOUND).build()
        val room = RoomEntity().apply {
            name = request.name
            this.space = space
        }
        room.persist()
        return Response.status(Response.Status.CREATED).entity(room.toDto()).build()
    }

    @PUT
    @Path("/rooms/{id}")
    @Transactional
    fun updateRoom(@PathParam("id") id: Long, request: UpdateRoomRequest): Response {
        val room = RoomEntity.findById(id) ?: return Response.status(Response.Status.NOT_FOUND).build()
        room.name = request.name
        return Response.ok(room.toDto()).build()
    }

    @DELETE
    @Path("/rooms/{id}")
    @Transactional
    fun deleteRoom(@PathParam("id") id: Long): Response {
        val room = RoomEntity.findById(id) ?: return Response.status(Response.Status.NOT_FOUND).build()
        room.delete()
        return Response.noContent().build()
    }

    private fun RoomEntity.toDto() = Room(
        id = id!!,
        spaceId = space.id!!,
        name = name
    )
}

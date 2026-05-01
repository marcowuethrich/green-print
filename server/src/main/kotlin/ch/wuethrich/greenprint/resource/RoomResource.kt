package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
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
    @WithTransaction
    fun createRoom(
        @PathParam("spaceId") spaceId: Long,
        request: CreateRoomRequest
    ): Uni<Response> = SpaceEntity.findById(spaceId).chain { space ->
        when {
            space == null -> Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build())
            space.userId != getUserId() -> Uni.createFrom().item(Response.status(Response.Status.FORBIDDEN).build())
            else -> {
                val room = RoomEntity().apply {
                    name = request.name
                    this.space = space
                }
                room.persist<RoomEntity>().map {
                    Response.status(Response.Status.CREATED).entity(it.toDto()).build()
                }
            }
        }
    }

    @PUT
    @Path("/spaces/{spaceId}/rooms/{id}")
    @WithTransaction
    fun updateRoom(
        @PathParam("spaceId") spaceId: Long,
        @PathParam("id") id: Long,
        request: UpdateRoomRequest
    ): Uni<Response> = RoomEntity.findById(id).chain { room ->
        when {
            room == null -> Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build())
            room.space.userId != getUserId() -> Uni.createFrom()
                .item(Response.status(Response.Status.FORBIDDEN).build())

            else -> {
                room.name = request.name
                Uni.createFrom().item(Response.ok(room.toDto()).build())
            }
        }
    }

    @DELETE
    @Path("/spaces/{spaceId}/rooms/{id}")
    @WithTransaction
    fun deleteRoom(
        @PathParam("spaceId") spaceId: Long,
        @PathParam("id") id: Long
    ): Uni<Response> = RoomEntity.findById(id).chain { room ->
        when {
            room == null -> Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build())
            room.space.userId != getUserId() -> Uni.createFrom()
                .item(Response.status(Response.Status.FORBIDDEN).build())

            else -> {
                RoomEntity.delete("id = ?1", id).replaceWith(Response.noContent().build())
            }
        }
    }

    private fun RoomEntity.toDto() = Room(
        id = id!!,
        spaceId = space.id!!,
        name = name
    )
}

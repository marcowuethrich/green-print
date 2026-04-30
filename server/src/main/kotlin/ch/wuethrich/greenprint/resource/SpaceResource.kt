package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.UpdateSpaceRequest
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.model.Space
import io.quarkus.security.Authenticated
import jakarta.transaction.Transactional
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

@Path("/spaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SpaceResource {

    @GET
    fun getSpaces(): List<Space> {
        return SpaceEntity.listAll().map { it.toDto() }
    }

    @POST
    @Transactional
    fun createSpace(request: CreateSpaceRequest): Space {
        val entity = SpaceEntity().apply {
            name = request.name
        }
        entity.persist()
        return entity.toDto()
    }

    @PUT
    @Path("/{id}")
    @Transactional
    fun updateSpace(@PathParam("id") id: Long, request: UpdateSpaceRequest): Response {
        val entity = SpaceEntity.findById(id) ?: return Response.status(Response.Status.NOT_FOUND).build()
        entity.name = request.name
        return Response.ok(entity.toDto()).build()
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    fun deleteSpace(@PathParam("id") id: Long): Response {
        val entity = SpaceEntity.findById(id) ?: return Response.status(Response.Status.NOT_FOUND).build()
        entity.delete()
        return Response.noContent().build()
    }

    private fun SpaceEntity.toDto() = Space(
        id = id!!,
        name = name,
        rooms = rooms.map { Room(it.id!!, it.space.id!!, it.name) }
    )
}

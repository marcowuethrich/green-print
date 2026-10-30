package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.auth.UserService
import ch.wuethrich.greenprint.entity.PlantEntity
import ch.wuethrich.greenprint.entity.toDto
import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.UpdatePlantRequest
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
import java.time.Instant
import java.util.UUID

@Path("/plants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class PlantResource {

    @Inject
    lateinit var userService: UserService

    @PUT
    @Path("/{id}")
    suspend fun updatePlant(@PathParam("id") id: UUID, request: UpdatePlantRequest): Response = withTx {
        val name = validateName(request.name)
        val user = userService.currentUser()
        val plant = PlantEntity.find("id = ?1 and room.space.user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        plant.apply {
            this.name = name
            this.species = request.species
            this.potSize = request.potSize
            this.soilType = request.soilType
            this.wateringIntervalDays = request.wateringIntervalDays
            this.photoUrl = request.photoUrl
            this.x = request.x
            this.y = request.y
        }

        Response.ok(plant.toDto()).build()
    }

    @POST
    @Path("/{id}/archive")
    suspend fun archivePlant(@PathParam("id") id: UUID, request: ArchivePlantRequest): Response = withTx {
        val user = userService.currentUser()
        val plant = PlantEntity.find("id = ?1 and room.space.user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        plant.archiveDate = Instant.now()
        plant.archiveReason = request.reason

        Response.ok(plant.toDto()).build()
    }

    @DELETE
    @Path("/{id}")
    suspend fun deletePlant(@PathParam("id") id: UUID): Response = withTx {
        val user = userService.currentUser()
        val plant = PlantEntity.find("id = ?1 and room.space.user = ?2", id, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()
        plant.deleteDirectly(plant.id)
        Response.noContent().build()
    }
}

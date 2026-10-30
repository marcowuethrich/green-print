package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.auth.UserService
import ch.wuethrich.greenprint.entity.PlantEntity
import ch.wuethrich.greenprint.entity.WateringEventEntity
import ch.wuethrich.greenprint.entity.toDto
import ch.wuethrich.greenprint.model.ApiError
import ch.wuethrich.greenprint.model.ArchivePlantRequest
import ch.wuethrich.greenprint.model.RecordWateringRequest
import ch.wuethrich.greenprint.model.UpdatePlantPositionRequest
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
        val interval = validateInterval(request.wateringIntervalDays)
        val x = validatePosition(request.x, "x")
        val y = validatePosition(request.y, "y")
        val plant = findOwnedPlant(id) ?: return@withTx notFound()

        plant.apply {
            this.name = name
            this.species = request.species
            this.potSize = request.potSize
            this.soilType = request.soilType
            this.wateringIntervalDays = interval
            this.photoUrl = request.photoUrl
            this.x = x
            this.y = y
        }

        Response.ok(plant.toDto()).build()
    }

    /** Moves a plant on its room's blueprint (spatial_blueprint.md §2). */
    @PUT
    @Path("/{id}/position")
    suspend fun updatePosition(@PathParam("id") id: UUID, request: UpdatePlantPositionRequest): Response = withTx {
        val x = validatePosition(request.x, "x")
        val y = validatePosition(request.y, "y")
        val plant = findOwnedPlant(id) ?: return@withTx notFound()

        plant.x = x
        plant.y = y

        Response.ok(plant.toDto()).build()
    }

    /**
     * Records a watering (inventory.md §5) and returns the plant with its new `lastWateredDate`. The
     * client picks the watering's id, so a resent watering is stored once; a watering older than the
     * plant's last one is logged but leaves `lastWateredDate` as it is.
     */
    @POST
    @Path("/{id}/waterings")
    suspend fun recordWatering(@PathParam("id") id: UUID, request: RecordWateringRequest): Response = withTx {
        val wateringId = validateWateringId(request.id)
        val wateredAt = validateWateredAt(request.timestamp, now = Instant.now())
        val plant = findOwnedPlant(id) ?: return@withTx notFound()

        val existing = WateringEventEntity.findById(wateringId).awaitSuspending()
        when {
            existing == null -> {
                WateringEventEntity()
                    .apply {
                        this.id = wateringId
                        this.plant = plant
                        this.wateredAt = wateredAt
                    }.persist<WateringEventEntity>()
                    .awaitSuspending()
                if (plant.lastWateredDate?.isAfter(wateredAt) != true) plant.lastWateredDate = wateredAt
            }

            // The same id for another plant is not a resend but a clash.
            existing.plant.id != plant.id ->
                return@withTx Response
                    .status(Response.Status.CONFLICT)
                    .entity(ApiError(code = "watering_id_taken", field = "id"))
                    .build()
        }

        Response.ok(plant.toDto()).build()
    }

    @POST
    @Path("/{id}/archive")
    suspend fun archivePlant(@PathParam("id") id: UUID, request: ArchivePlantRequest): Response = withTx {
        val reason = validateArchiveReason(request.reason)
        val plant = findOwnedPlant(id) ?: return@withTx notFound()

        plant.archiveDate = Instant.now()
        plant.archiveReason = reason

        Response.ok(plant.toDto()).build()
    }

    @DELETE
    @Path("/{id}")
    suspend fun deletePlant(@PathParam("id") id: UUID): Response = withTx {
        val plant = findOwnedPlant(id) ?: return@withTx notFound()
        // Hibernate's dev and test DDL has no ON DELETE CASCADE, so the log goes first.
        WateringEventEntity.delete("plant.id = ?1", plant.id!!).awaitSuspending()
        plant.deleteDirectly(plant.id)
        Response.noContent().build()
    }

    /** The plant, if it lives in one of the current user's spaces. */
    private suspend fun findOwnedPlant(id: UUID): PlantEntity? {
        val user = userService.currentUser()
        return PlantEntity
            .find("id = ?1 and room.space.user = ?2", id, user)
            .firstResult()
            .awaitSuspending()
    }

    private fun notFound(): Response = Response.status(Response.Status.NOT_FOUND).build()
}

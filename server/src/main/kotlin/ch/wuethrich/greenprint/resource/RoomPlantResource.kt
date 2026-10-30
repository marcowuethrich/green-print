package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.auth.UserService
import ch.wuethrich.greenprint.entity.PlantEntity
import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.entity.toDto
import ch.wuethrich.greenprint.model.CreatePlantRequest
import ch.wuethrich.greenprint.model.Plant
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Instant
import java.util.UUID

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class RoomPlantResource {

    @Inject
    lateinit var userService: UserService

    @GET
    @Path("/{roomId}/plants")
    suspend fun getPlants(@PathParam("roomId") roomId: UUID): List<Plant> = withTx {
        val user = userService.currentUser()
        val room = RoomEntity.find("id = ?1 and space.user = ?2", roomId, user)
            .firstResult().awaitSuspending()
            ?: return@withTx emptyList<Plant>()
        PlantEntity.find("room = ?1 and archiveDate is null", room).list().awaitSuspending().map { it.toDto() }
    }

    @POST
    @Path("/{roomId}/plants")
    suspend fun createPlant(@PathParam("roomId") roomId: UUID, request: CreatePlantRequest): Response = withTx {
        val name = validateName(request.name)
        val interval = validateInterval(request.wateringIntervalDays)
        val x = validatePosition(request.x, "x")
        val y = validatePosition(request.y, "y")
        val user = userService.currentUser()
        val room = RoomEntity.find("id = ?1 and space.user = ?2", roomId, user)
            .firstResult().awaitSuspending()
            ?: return@withTx Response.status(Response.Status.NOT_FOUND).build()

        val plant = PlantEntity().apply {
            this.name = name
            this.species = request.species
            this.potSize = request.potSize
            this.soilType = request.soilType
            this.wateringIntervalDays = interval
            // A plant counts as watered when it is added, so it does not start out dry (inventory.md).
            this.lastWateredDate = Instant.now()
            this.room = room
            this.x = x
            this.y = y
        }
        plant.persist<PlantEntity>().awaitSuspending()
        Response.status(Response.Status.CREATED).entity(plant.toDto()).build()
    }
}

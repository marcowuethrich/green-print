package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.model.CreateRoomRequest
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.UpdateRoomRequest
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class RoomResourceTest {

    @Inject
    lateinit var spaceResource: SpaceResource

    @Inject
    lateinit var roomResource: RoomResource

    @BeforeEach
    @Transactional
    fun setup() {
        RoomEntity.deleteAll()
        SpaceEntity.deleteAll()
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `test room lifecycle`() {
        // 1. Create Space first
        val space = spaceResource.createSpace(CreateSpaceRequest("My Apartment"))

        // 2. Create Room
        val roomResponse = roomResource.createRoom(space.id, CreateRoomRequest("Living Room"))
        assertEquals(201, roomResponse.status)
        val createdRoom = roomResponse.entity as ch.wuethrich.greenprint.model.Room
        assertEquals("Living Room", createdRoom.name)
        assertEquals(space.id, createdRoom.spaceId)

        // 3. Update Room
        val updateResponse = roomResource.updateRoom(createdRoom.id, UpdateRoomRequest("Balcony"))
        assertEquals(200, updateResponse.status)
        val updatedRoom = updateResponse.entity as ch.wuethrich.greenprint.model.Room
        assertEquals("Balcony", updatedRoom.name)

        // 4. Delete Room
        val deleteRoomResponse = roomResource.deleteRoom(createdRoom.id)
        assertEquals(204, deleteRoomResponse.status)
    }
}

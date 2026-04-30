package ch.wuethrich.greenprint.resource

import ch.wuethrich.greenprint.entity.SpaceEntity
import ch.wuethrich.greenprint.entity.RoomEntity
import ch.wuethrich.greenprint.model.CreateSpaceRequest
import ch.wuethrich.greenprint.model.UpdateSpaceRequest
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class SpaceResourceTest {

    @Inject
    lateinit var spaceResource: SpaceResource

    @BeforeEach
    @Transactional
    fun setup() {
        RoomEntity.deleteAll()
        SpaceEntity.deleteAll()
    }

    @Test
    @TestSecurity(user = "alice", roles = ["user"])
    fun `test space lifecycle`() {
        // 1. Create Space
        val space = spaceResource.createSpace(CreateSpaceRequest("My Apartment"))
        assertNotNull(space.id)
        assertEquals("My Apartment", space.name)

        // 2. Update Space
        val updateResponse = spaceResource.updateSpace(space.id, UpdateSpaceRequest("Our Villa"))
        assertEquals(200, updateResponse.status)
        val updatedSpace = updateResponse.entity as ch.wuethrich.greenprint.model.Space
        assertEquals("Our Villa", updatedSpace.name)

        // 3. Get Spaces
        val spaces = spaceResource.getSpaces()
        assertEquals(1, spaces.size)
        assertEquals("Our Villa", spaces[0].name)

        // 4. Delete Space
        val deleteSpaceResponse = spaceResource.deleteSpace(space.id)
        assertEquals(204, deleteSpaceResponse.status)

        // Verify emptiness
        val spacesAfterDelete = spaceResource.getSpaces()
        assertTrue(spacesAfterDelete.isEmpty())
    }
}

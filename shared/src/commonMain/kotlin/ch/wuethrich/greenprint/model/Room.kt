package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String,
    val spaceId: String,
    val name: String,
    val icon: String? = null,
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = 0L,
)

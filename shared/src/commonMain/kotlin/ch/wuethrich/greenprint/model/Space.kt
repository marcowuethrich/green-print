package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class Space(
    val id: String,
    val name: String,
    val icon: String? = null,
    val rooms: List<Room> = emptyList(),
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = 0L,
)

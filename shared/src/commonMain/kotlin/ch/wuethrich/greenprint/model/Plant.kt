package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class Plant(
    val id: String,
    val roomId: String,
    val name: String,
    val species: String? = null,
    val potSize: String? = null,
    val soilType: String? = null,
    val wateringIntervalDays: Int,
    val lastWateredDate: String? = null,
    val photoUrl: String? = null,
    val archiveDate: String? = null,
    val archiveReason: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = 0L,
)

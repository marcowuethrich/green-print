package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class WateringEvent(
    val id: String,
    val plantId: String,
    val timestamp: String,
    val syncStatus: String = "S_SYNCED",
    val updatedAt: Long = 0L
)

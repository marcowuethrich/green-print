package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePlantRequest(
    val name: String,
    val species: String? = null,
    val potSize: String? = null,
    val soilType: String? = null,
    val wateringIntervalDays: Int,
    val photoUrl: String? = null,
    val x: Float = 0f,
    val y: Float = 0f
)

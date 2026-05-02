package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomRequest(
    val name: String,
    val icon: String? = null
)

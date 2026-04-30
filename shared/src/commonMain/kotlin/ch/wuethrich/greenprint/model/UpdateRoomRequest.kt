package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateRoomRequest(
    val name: String
)

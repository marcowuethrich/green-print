package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: Long,
    val spaceId: Long,
    val name: String
)

package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String,
    val spaceId: String,
    val name: String,
    val icon: String? = null
)

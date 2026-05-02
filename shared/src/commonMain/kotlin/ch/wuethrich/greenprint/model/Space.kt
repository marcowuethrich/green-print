package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class Space(
    val id: String,
    val name: String,
    val rooms: List<Room> = emptyList()
)

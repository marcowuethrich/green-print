package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class Space(
    val id: Long,
    val name: String,
    val rooms: List<Room> = emptyList()
)

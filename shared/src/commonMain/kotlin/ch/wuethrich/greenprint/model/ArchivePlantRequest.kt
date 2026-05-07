package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class ArchivePlantRequest(
    val reason: String
)

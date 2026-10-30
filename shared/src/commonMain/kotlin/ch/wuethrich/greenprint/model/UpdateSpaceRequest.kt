package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSpaceRequest(val name: String, val icon: String? = null)

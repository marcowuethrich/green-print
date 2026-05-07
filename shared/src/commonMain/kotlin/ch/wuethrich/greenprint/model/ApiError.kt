package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: String,
    val field: String? = null
)

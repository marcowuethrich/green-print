package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

@Serializable
data class GreetingResponse(
    val message: String
)

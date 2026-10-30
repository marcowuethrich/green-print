package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

/** Moves a plant on its room's blueprint: normalized coordinates in 0.0..1.0 (ADR-008). */
@Serializable
data class UpdatePlantPositionRequest(val x: Float, val y: Float)

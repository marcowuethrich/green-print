package ch.wuethrich.greenprint.model

import kotlinx.serialization.Serializable

/**
 * One watering of a plant, as the client recorded it: [id] is a UUID the client picks, so sending the
 * same watering again (a retry after a lost answer) records it once; [timestamp] is an ISO-8601 instant.
 */
@Serializable
data class RecordWateringRequest(val id: String, val timestamp: String)

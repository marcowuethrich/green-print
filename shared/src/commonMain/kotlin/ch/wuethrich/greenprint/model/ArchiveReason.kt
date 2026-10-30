package ch.wuethrich.greenprint.model

/** Why a plant left the collection (ADR-009); the server accepts these values only. */
object ArchiveReason {
    const val DIED = "DIED"
    const val GIFTED = "GIFTED"
    const val SOLD = "SOLD"

    val ALL: Set<String> = setOf(DIED, GIFTED, SOLD)
}

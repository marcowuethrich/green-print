package ch.wuethrich.greenprint.model

/** Sync state of a locally cached entity (docs/features/inventory.md §6.2). */
object SyncStatus {
    /** The local row matches the server. */
    const val SYNCED = "S_SYNCED"

    /** Written locally, not yet confirmed by the server. */
    const val PENDING = "S_PENDING"

    /** The last attempt to reach the server failed; retried on the next refresh. */
    const val ERROR = "S_ERROR"
}

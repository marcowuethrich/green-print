package ch.wuethrich.greenprint.db

import app.cash.sqldelight.db.SqlDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        // Note: SQLDelight on Wasm requires a WebWorkerDriver which depends on browser APIs.
        // Unresolved references to org.w3c.dom are currently blocking this implementation.
        TODO("Wasm driver not yet implemented")
    }
}

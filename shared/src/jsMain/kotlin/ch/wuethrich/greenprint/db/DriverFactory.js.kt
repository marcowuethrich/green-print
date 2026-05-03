package ch.wuethrich.greenprint.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        // Note: The worker script must be provided in the distribution
        val worker = Worker("sqldelight-worker.js")
        return WebWorkerDriver(worker)
    }
}

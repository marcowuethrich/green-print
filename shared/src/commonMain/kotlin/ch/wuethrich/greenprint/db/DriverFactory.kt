package ch.wuethrich.greenprint.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Opens the platform's SQLite driver. The schema is generated asynchronously for the web worker
 * driver (ADR-017); the native, Android and JVM drivers run it synchronously.
 */
expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): GreenPrintDb {
    val driver = driverFactory.createDriver()
    return GreenPrintDb(driver)
}

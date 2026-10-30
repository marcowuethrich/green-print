package ch.wuethrich.greenprint.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(GreenPrintDb.Schema.synchronous(), "greenprint.db")
}

package ch.wuethrich.greenprint.db

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): GreenPrintDb {
    val driver = driverFactory.createDriver()
    return GreenPrintDb(driver)
}

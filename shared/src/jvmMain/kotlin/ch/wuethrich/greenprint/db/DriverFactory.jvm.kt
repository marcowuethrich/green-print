package ch.wuethrich.greenprint.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class DriverFactory {
    /**
     * The desktop cache is a file in the user's home, like the phones' app databases, so changes
     * made offline survive a restart. The driver creates the schema on first start.
     */
    actual fun createDriver(): SqlDriver {
        val directory = File(System.getProperty("user.home"), ".greenprint").apply { mkdirs() }
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${File(directory, "greenprint.db").absolutePath}",
            properties = Properties(),
            schema = GreenPrintDb.Schema,
        )
    }
}

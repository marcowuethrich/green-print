package ch.wuethrich.greenprint.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * The browser's cache (ADR-017): SQLite in a web worker that saves the database to IndexedDB. Its
 * schema can only be created asynchronously, so the web entry point opens the cache with
 * [openWebCache] before Koin starts and hands this factory to it.
 */
actual class DriverFactory internal constructor(private val driver: SqlDriver) {
    actual fun createDriver(): SqlDriver = driver
}

/**
 * Talks to the cache in [worker] (the app bundles it: `composeApp/src/webMain/resources`) and brings
 * the saved database up to the current schema.
 */
suspend fun openWebCache(worker: Worker): DriverFactory {
    val driver = WebWorkerDriver(worker)
    driver.migrateToCurrentSchema()
    return DriverFactory(driver)
}

/** SQLite's `user_version` holds the schema version, as the native and Android drivers keep it. */
private suspend fun SqlDriver.migrateToCurrentSchema() {
    val schema = GreenPrintDb.Schema
    val version = executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
        parameters = 0,
    ).await()
    when {
        version == 0L -> schema.create(this).await()
        version < schema.version -> schema.migrate(this, version, schema.version).await()
        else -> return
    }
    execute(identifier = null, sql = "PRAGMA user_version = ${schema.version}", parameters = 0).await()
}

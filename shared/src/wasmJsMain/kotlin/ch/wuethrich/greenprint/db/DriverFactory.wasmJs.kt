package ch.wuethrich.greenprint.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement

/**
 * The web build has no local cache yet: this driver persists nothing and every read is empty. The
 * screens read from the cache only, so the wasm app shows no spaces, rooms or plants even when the
 * server has them. A real cache needs SQLDelight's sql.js web worker driver and an async-generated
 * schema.
 */
actual class DriverFactory {
    actual fun createDriver(): SqlDriver = NoOpSqlDriver
}

private object NoOpSqlDriver : SqlDriver {
    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> = QueryResult.Value(0L)

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> = mapper(EmptyCursor)

    override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.Value(NoOpTransaction())

    override fun currentTransaction(): Transacter.Transaction? = null

    override fun close() = Unit

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun notifyListeners(vararg queryKeys: String) = Unit
}

private object EmptyCursor : SqlCursor {
    override fun next(): QueryResult<Boolean> = QueryResult.Value(false)

    override fun getString(index: Int): String? = null

    override fun getLong(index: Int): Long? = null

    override fun getBytes(index: Int): ByteArray? = null

    override fun getDouble(index: Int): Double? = null

    override fun getBoolean(index: Int): Boolean? = null
}

private class NoOpTransaction : Transacter.Transaction() {
    override val enclosingTransaction: Transacter.Transaction? = null

    override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.Unit
}

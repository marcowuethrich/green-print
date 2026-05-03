package ch.wuethrich.greenprint.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlPreparedStatement

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return object : SqlDriver {
            override fun execute(
                identifier: Int?,
                sql: String,
                parameters: Int,
                binders: (SqlPreparedStatement.() -> Unit)?
            ): QueryResult<Long> = QueryResult.Value(0L)

            override fun <R> executeQuery(
                identifier: Int?,
                sql: String,
                mapper: (SqlCursor) -> QueryResult<R>,
                parameters: Int,
                binders: (SqlPreparedStatement.() -> Unit)?
            ): QueryResult<R> {
                return mapper(object : SqlCursor {
                    override fun next(): QueryResult<Boolean> = QueryResult.Value(false)
                    override fun getString(index: Int): String? = null
                    override fun getLong(index: Int): Long? = null
                    override fun getBytes(index: Int): ByteArray? = null
                    override fun getDouble(index: Int): Double? = null
                    override fun getBoolean(index: Int): Boolean? = null
                })
            }

            override fun newTransaction(): QueryResult<app.cash.sqldelight.Transacter.Transaction> {
                return QueryResult.Value(object : app.cash.sqldelight.Transacter.Transaction() {
                    override val enclosingTransaction: app.cash.sqldelight.Transacter.Transaction? = null
                    override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.Unit
                })
            }

            override fun currentTransaction(): app.cash.sqldelight.Transacter.Transaction? = null
            
            override fun close() {}
            
            override fun addListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {}
            override fun removeListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {}
            override fun notifyListeners(vararg queryKeys: String) {}
        }
    }
}

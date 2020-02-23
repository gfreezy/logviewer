package io.allsunday.logviewer.repositories

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.allsunday.logviewer.pojos.*
import io.allsunday.logviewer.tables.LogTable
import io.allsunday.logviewer.tables.SpanTable
import io.allsunday.logviewer.tables.TraceQueryCondition
import io.allsunday.logviewer.tables.TraceTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class SpanRepository {
    fun addOrUpdateSpan(span: Span) = transaction {
        if (span.parentId == null) {
            TraceTable.insertOrUpdate(span)
        }
        SpanTable.insertOrUpdate(span)
    }

    fun addOrUpdateSpans(spans: List<Span>) = transaction {
        for (span in spans) {
            addOrUpdateSpan(span)
        }
    }

    fun addLog(log: Log) = transaction {
        LogTable.insert(log)
    }

    fun addLogs(logs: List<Log>) = transaction {
        for (it in logs) {
            LogTable.insert(it)
        }
    }

    fun pagedTraces(
        traceQueryCondition: TraceQueryCondition? = null,
        cursor: Cursor<Long>? = null,
        size: Int = 20
    ): Page<Trace, Long> = transaction {
        TraceTable.pagedTraces(traceQueryCondition, cursor, size)
    }

    fun pagedTraceSpans(traceId: Long, cursor: Cursor<Long>? = null, size: Int = 20): Page<Span, Long> =
        transaction {
            SpanTable.pagedTraceSpans(traceId, cursor, size)
        }

    fun pagedTraceLogs(traceId: Long, cursor: Cursor<Long>? = null, size: Int = 20): Page<Log, Long> =
        transaction {
            LogTable.pagedTraceLogs(traceId, cursor, size)
        }

    fun getTrace(traceId: Long): TraceDto = transaction {
        val pageSpans = pagedTraceSpans(traceId, size = 1000)
        val pageLogs = pagedTraceLogs(traceId, size = 1000)
        TraceDto.build(traceId, pageSpans.content, pageLogs.content)
    }

    fun listTraces(traceQueryCondition: TraceQueryCondition? = null, size: Int = 20): List<TraceDto> = transaction {
        val pageTraces = pagedTraces(traceQueryCondition, size = size)
        pageTraces.content.map { getTrace(it.id) }
    }

    companion object {
        fun initDatabase(url: String, user: String, password: String) {
            val config = HikariConfig()
            config.jdbcUrl = url
            config.username = user
            config.password = password
            config.isAutoCommit = false
            config.addDataSourceProperty("cachePrepStmts", "true")
            config.addDataSourceProperty("prepStmtCacheSize", "250")
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

            val ds = HikariDataSource(config)

            Database.connect(ds)

            transaction {
                SchemaUtils.create(TraceTable, SpanTable, LogTable)
            }
        }
    }
}

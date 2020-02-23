package io.allsunday.logviewer.repositories

import io.allsunday.logviewer.pojos.*
import io.allsunday.logviewer.tables.LogTable
import io.allsunday.logviewer.tables.SpanTable
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

    fun addLog(log: Log) = transaction {
        LogTable.insert(log)
    }

    fun pagedTraces(cursor: Cursor<Long>? = null, size: Int = 20): Page<Trace, Long> = transaction {
        TraceTable.pagedTraces(cursor, size)
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

    fun listTraces(size: Int): List<TraceDto> = transaction {
        val pageTraces = pagedTraces(size = size)
        pageTraces.content.map { getTrace(it.id) }
    }

    companion object {
        fun initDatabase(url: String, user: String, password: String) {
            Database.connect(
                url, driver = "com.mysql.cj.jdbc.Driver",
                user = user, password = password
            )

            transaction {
                SchemaUtils.create(TraceTable, SpanTable, LogTable)
            }
        }
    }
}

package io.allsunday.logviewer.tables

import io.allsunday.logviewer.libs.exposed.jsonb
import io.allsunday.logviewer.pojos.Cursor
import io.allsunday.logviewer.pojos.Log
import io.allsunday.logviewer.pojos.Page
import org.jetbrains.exposed.sql.*

object LogTable : Table() {
    private val cId = long("id")
    private val cSpanId = long("span_id").index()
    private val cTraceId = long("trace_id")
    private val cTimestamp = long("timestamp")
    private val cfields = jsonb<Map<String, String>>("fields")

    override val primaryKey = PrimaryKey(cId)

    fun insert(log: Log) {
        LogTable.insert {
            it[cId] = log.id
            it[cSpanId] = log.spanId
            it[cTraceId] = log.traceId
            it[cTimestamp] = log.timestamp
            it[cfields] = log.fields
        }
    }

    fun pagedTraceLogs(traceId: Long, cursor: Cursor<Long>? = null, size: Int = 20): Page<Log, Long> {
        var query = LogTable.select { cTraceId.eq(traceId) }.orderBy(cId, SortOrder.ASC).limit(size)
        query = cursor?.next?.let { query.andWhere { cId.less(it) } } ?: query
        val l = query.map {
            Log(
                it[cId],
                it[cTraceId],
                it[cTimestamp],
                it[cSpanId],
                it[cfields]
            )
        }
        val nextCursor = if (l.size <= size) {
            null
        } else {
            Cursor(l.last().id)
        }
        return Page(l, nextCursor)
    }
}

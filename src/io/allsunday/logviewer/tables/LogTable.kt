package io.allsunday.logviewer.tables

import io.allsunday.logviewer.libs.exposed.jsonb
import io.allsunday.logviewer.pojos.Cursor
import io.allsunday.logviewer.pojos.Log
import io.allsunday.logviewer.pojos.Page
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select

object LogTable : Table() {
    private val cId = long("id")
    private val cSpanId = long("span_id")
    private val cTraceId = long("trace_id").index()
    private val cTimestamp = long("timestamp")
    private val cfields = jsonb<Map<String, String>>("fields")

    override val primaryKey = PrimaryKey(cId)

    fun insert(log: Log) {
        LogTable.insertIgnore {
            it[cId] = log.id
            it[cSpanId] = log.spanId
            it[cTraceId] = log.traceId
            it[cTimestamp] = log.timestamp
            it[cfields] = log.fields
        }
    }

    fun pagedTraceLogs(traceId: Long, cursor: Cursor<Long>? = null, size: Int = 20): Page<Log, Long> {
        var query = LogTable.select { cTraceId.eq(traceId) }.limit(size)
        query = cursor?.next?.let { query.andWhere { cId.less(it) } } ?: query
        val l = query.map {
            Log(
                id = it[cId],
                timestamp = it[cTimestamp],
                traceId = it[cTraceId],
                spanId = it[cSpanId],
                fields = it[cfields]
            )
        }
        val nextCursor = if (l.size < size) {
            null
        } else {
            Cursor(l.last().id)
        }
        return Page(l, nextCursor)
    }
}

package io.allsunday.logviewer.tables

import io.allsunday.logviewer.libs.exposed.jsonb
import io.allsunday.logviewer.pojos.Cursor
import io.allsunday.logviewer.pojos.Page
import io.allsunday.logviewer.pojos.Span
import org.jetbrains.exposed.sql.*

object SpanTable : Table() {
    private val cId = long("id")
    private val cName = varchar("name", length = 50)
    private val cParentId = long("parent_id").nullable()
    private val cTraceId = long("trace_id").index()
    private val cTimestamp = long("timestamp")
    private val cDuration = long("duration")
    private val cTags = jsonb<Map<String, String>>("tags")
    private val cFinished = bool("finished")

    override val primaryKey = PrimaryKey(cId)

    fun insertOrUpdate(span: Span) {
        SpanTable.insertIgnore {
            it[cId] = span.id
            it[cName] = span.name
            it[cParentId] = span.parentId
            it[cTraceId] = span.traceId
            it[cTimestamp] = span.timestamp
            it[cDuration] = span.duration
            it[cTags] = span.tags
            it[cFinished] = span.finished
        }

        SpanTable.update({
            (cId eq span.id) and (cDuration greaterEq span.duration)
        }) {
            it[cDuration] = span.duration
            it[cFinished] = span.finished
        }
    }

    fun pagedTraceSpans(traceId: Long, cursor: Cursor<Long>? = null, size: Int = 20): Page<Span, Long> {
        var query = SpanTable.select { cTraceId.eq(traceId) }.limit(size)
        query = cursor?.next?.let { query.andWhere { cId.less(it) } } ?: query
        val l = query.map {
            Span(
                id = it[cId],
                parentId = it[cParentId],
                traceId = it[cTraceId],
                name = it[cName],
                timestamp = it[cTimestamp],
                duration = it[cDuration],
                tags = it[cTags],
                finished = it[cFinished]
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

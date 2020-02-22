package io.allsunday.logviewer.tables

import io.allsunday.logviewer.libs.exposed.jsonb
import io.allsunday.logviewer.pojos.Cursor
import io.allsunday.logviewer.pojos.Page
import io.allsunday.logviewer.pojos.Span
import org.jetbrains.exposed.sql.*
import java.sql.SQLException

object SpanTable : Table() {
    private val cId = long("id")
    private val cName = varchar("name", length = 50)
    private val cParentId = long("parent_id").nullable()
    private val cTraceId = long("trace_id").index()
    private val cTimestamp = long("timestamp")
    private val cDuration = long("duration")
    private val cTags = jsonb<Map<String, String>>("tags")
    private val cFinished = bool("finished")
    private val cVersion = integer("version")

    override val primaryKey = PrimaryKey(cId)

    fun insertOrUpdate(span: Span) {
        val row = SpanTable.select {
            cId.eq(span.id)
        }.firstOrNull()

        if (row != null) {
            if (span.timestamp >= row[cTimestamp]) {
                val ret = SpanTable.update({
                    (cId eq row[cId]) and (cVersion eq row[cVersion])
                }) {
                    it[cDuration] = span.duration
                    it[cFinished] = span.finished
                    it[cVersion] =
                        Expression.build { cVersion.plus(1) }
                }
                if (ret == 0) {
                    throw SQLException("Update SpanTable failed, retry. id: ${row[cId]}, version: ${row[cVersion]}")
                }
            }
        } else {
            SpanTable.insert {
                it[cId] = span.id
                it[cName] = span.name
                it[cParentId] = span.parentId
                it[cTraceId] = span.traceId
                it[cTimestamp] = span.timestamp
                it[cDuration] = span.duration
                it[cTags] = span.tags
                it[cFinished] = span.finished
                it[cVersion] = 1
            }
        }
    }

    fun pagedTraceSpans(traceId: Long, cursor: Cursor<Long>? = null, size: Int = 20): Page<Span, Long> {
        var query = SpanTable.select { cTraceId.eq(traceId) }.orderBy(cId, SortOrder.ASC).limit(size)
        query = cursor?.next?.let { query.andWhere { cId.less(it) } } ?: query
        val l = query.map {
            Span(
                it[cId],
                it[cParentId],
                it[cTraceId],
                it[cName],
                it[cTimestamp],
                it[cDuration],
                it[cTags],
                it[cFinished]
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

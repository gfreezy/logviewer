package io.allsunday.logviewer.tables

import io.allsunday.logviewer.pojos.Cursor
import io.allsunday.logviewer.pojos.Page
import io.allsunday.logviewer.pojos.Span
import io.allsunday.logviewer.pojos.Trace
import org.jetbrains.exposed.sql.*
import java.sql.SQLException

object TraceTable : Table() {
    private val cId = long("id")
    private val cName = varchar("name", length = 50)
    private val cTimestamp = long("timestamp")
    private val cDuration = long("duration")
    private val cFinished = bool("finished")
    private val cVersion = integer("version")

    override val primaryKey = PrimaryKey(cId)

    @Throws(SQLException::class)
    fun insertOrUpdate(span: Span) {
        assert(span.parentId == null)

        val row = TraceTable.select {
            cId.eq(span.traceId)
        }.firstOrNull()

        if (row != null) {
            if (span.timestamp >= row[cTimestamp]) {
                val ret = TraceTable.update({
                    (cId eq row[cId]) and (cVersion eq row[cVersion])
                }) {
                    it[cDuration] = span.duration
                    it[cFinished] = span.finished
                    it[cVersion] =
                        Expression.build { cVersion.plus(1) }
                }
                if (ret == 0) {
                    throw SQLException("Update TraceTable failed, retry. id: ${row[cId]}, version: ${row[cVersion]}")
                }
            }
        } else {
            TraceTable.insert {
                it[cName] = span.name
                it[cTimestamp] = span.timestamp
                it[cId] = span.traceId
                it[cDuration] = span.duration
                it[cFinished] = span.finished
                it[cVersion] = 1
            }
        }
    }

    fun pagedTraces(cursor: Cursor<Long>? = null, size: Int = 20): Page<Trace, Long> {
        var query = TraceTable.selectAll().orderBy(cId, SortOrder.DESC).limit(size)
        query = cursor?.next?.let { query.andWhere { cId.less(it) } } ?: query
        val l = query.map { Trace(it[cId], it[cName], it[cTimestamp], it[cDuration], it[cFinished]) }
        val nextCursor = if (l.size <= size) {
            null
        } else {
            Cursor(l.last().id)
        }
        return Page(l, nextCursor)
    }
}
package io.allsunday.logviewer.tables

import io.allsunday.logviewer.pojos.Cursor
import io.allsunday.logviewer.pojos.Page
import io.allsunday.logviewer.pojos.Span
import io.allsunday.logviewer.pojos.Trace
import org.jetbrains.exposed.sql.*
import java.sql.SQLException

data class TraceQueryCondition(
    val minDuration: Long?,
    val maxDuration: Long?,
    val beginTs: Long?,
    val endTs: Long?,
    val finished: Boolean?
)

object TraceTable : Table() {
    private val cId = long("id")
    private val cName = varchar("name", length = 50)
    private val cTimestamp = long("timestamp").index()
    private val cDuration = long("duration").index()
    private val cFinished = bool("finished").index()

    override val primaryKey = PrimaryKey(cId)

    @Throws(SQLException::class)
    fun insertOrUpdate(span: Span) {
        assert(span.parentId == null)

        TraceTable.insertIgnore {
            it[cName] = span.name
            it[cTimestamp] = span.timestamp
            it[cId] = span.traceId
            it[cDuration] = span.duration
            it[cFinished] = span.finished
        }

        TraceTable.update({
            (cId eq span.traceId) and (cDuration greaterEq span.duration)
        }) {
            it[cDuration] = span.duration
            it[cFinished] = span.finished
        }
    }

    fun pagedTraces(
        condition: TraceQueryCondition? = null,
        cursor: Cursor<Long>? = null,
        size: Int = 20
    ): Page<Trace, Long> {
        var query = TraceTable.selectAll().orderBy(cId, SortOrder.DESC).limit(size)
        if (condition != null) {
            condition.finished?.let { query.andWhere { cFinished eq it } }
            condition.minDuration?.let { query.andWhere { cDuration greaterEq it } }
            condition.maxDuration?.let { query.andWhere { cDuration less it } }
            condition.beginTs?.let { query.andWhere { cTimestamp greaterEq it } }
            condition.endTs?.let { query.andWhere { cTimestamp less it } }
        }
        query = cursor?.next?.let { query.andWhere { cId.less(it) } } ?: query
        val l = query.map {
            Trace(
                id = it[cId],
                name = it[cName],
                timestamp = it[cTimestamp],
                duration = it[cDuration],
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

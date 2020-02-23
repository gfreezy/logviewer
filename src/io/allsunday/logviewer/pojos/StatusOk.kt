package io.allsunday.logviewer.pojos

const val PROCESS_ID = "tracing"
const val SERVICE_NAME = "trace"

data class PagedDto<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int = 0,
    val errors: List<String>? = null
)

data class FieldDto(
    val key: String,
    val value: String,
    val type: String = "string"
)

data class ProcessDto(
    val serviceName: String,
    val tags: List<FieldDto>
)

data class LogDto(
    val timestamp: Long,
    val fields: List<FieldDto>
) {
    companion object {
        fun build(log: Log): LogDto {
            val fields = log.fields.map { FieldDto(it.key, it.value) }
            return LogDto(
                timestamp = log.timestamp,
                fields = fields
            )
        }
    }
}

data class ReferenceDto(
    val traceID: String,
    val spanID: String,
    val refType: String = "CHILD_OF"
)

data class SpanDto(
    val traceID: String,
    val spanID: String,
    val operationName: String,
    val references: List<ReferenceDto>,
    val startTime: Long,
    val duration: Long,
    val tags: List<FieldDto>,
    val logs: List<LogDto>,
    val processID: String = PROCESS_ID,
    val flags: Int = 1
) {
    companion object {
        fun build(span: Span, logs: List<Log>): SpanDto {
            return SpanDto(
                traceID = span.traceId.toString(),
                spanID = span.id.toString(),
                operationName = span.name,
                references = span.parentId?.let {
                    listOf(
                        ReferenceDto(
                            span.traceId.toString(),
                            it.toString()
                        )
                    )
                }.orEmpty(),
                startTime = span.timestamp,
                duration = span.duration,
                tags = span.tags.map { FieldDto(it.key, it.value) },
                logs = logs.map { LogDto.build(it) }
            )
        }
    }
}

data class TraceDto(
    val traceID: String,
    val spans: List<SpanDto>,
    val processes: Map<String, ProcessDto>,
    val warnings: List<String>? = null
) {
    companion object {
        fun build(traceId: Long, spans: List<Span>, logs: List<Log>): TraceDto {
            return TraceDto(
                traceID = traceId.toString(),
                spans = spans.map { SpanDto.build(span = it, logs = logs.filter { log -> log.spanId == it.id }) },
                processes = mapOf(PROCESS_ID to ProcessDto(SERVICE_NAME, emptyList()))
            )
        }
    }
}

fun pagedTraces(data: List<TraceDto>) = PagedDto(data, 0, 0)
fun pagedTraces(data: TraceDto) = PagedDto(listOf(data), 0, 0)

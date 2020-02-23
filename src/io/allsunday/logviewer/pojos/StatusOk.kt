package io.allsunday.logviewer.pojos

import kotlinx.serialization.Serializable

const val PROCESS_ID = "tracing"
const val OPERATION_NAME = "trace"
const val SERVICE_NAME = "trace"

@Serializable
enum class Status {
    OK,
    ERROR
}

@Serializable
data class Ok<T>(
    val content: T
) {
    val status: Status = Status.OK

    companion object {
        val empty = Ok(null)
    }
}

@Serializable
data class Error<T>(
    val content: T
) {
    val status: Status = Status.ERROR
}


@Serializable
data class PagedDto<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int = 0,
    val errors: List<String>? = null
)

@Serializable
data class FieldDto(
    val key: String,
    val value: String,
    val type: String = "string"
)

@Serializable
data class ProcessDto(
    val serviceName: String,
    val tags: List<FieldDto>
)

@Serializable
data class LogDto(
    val timestamp: Long,
    val fields: List<FieldDto>
) {
    companion object {
        fun build(log: Log): LogDto {
            val fields = log.fields.map { FieldDto(it.key, it.value) }
            return LogDto(log.timestamp, fields)
        }
    }
}

@Serializable
data class ReferenceDto(
    val traceID: String,
    val spanID: String,
    val refType: String = "CHILD_OF"
)

@Serializable
data class SpanDto(
    val traceID: String,
    val spanID: String,
    val operationName: String,
    val references: List<ReferenceDto>,
    val startTime: Long,
    val duration: Int,
    val tags: List<FieldDto>,
    val logs: List<LogDto>,
    val processID: String = PROCESS_ID,
    val flags: Int = 1
) {
    companion object {
        fun build(span: Span, logs: List<Log>): SpanDto {
            return SpanDto(
                span.traceId.toString(),
                span.id.toString(),
                span.name,
                span.parentId?.let { listOf(ReferenceDto(span.traceId.toString(), it.toString())) }.orEmpty(),
                span.timestamp,
                span.duration.toInt(),
                span.tags.map { FieldDto(it.key, it.value) },
                logs.map { LogDto.build(it) }
            )
        }
    }
}

@Serializable
data class TraceDto(
    val traceID: String,
    val spans: List<SpanDto>,
    val processes: Map<String, ProcessDto>,
    val warnings: List<String>? = null
) {
    companion object {
        fun build(traceId: Long, spans: List<Span>, logs: List<Log>): TraceDto {
            return TraceDto(
                traceId.toString(),
                spans.map { SpanDto.build(it, logs.filter { log -> log.spanId == it.id }) },
                mapOf(PROCESS_ID to ProcessDto(SERVICE_NAME, emptyList()))
            )
        }
    }
}

fun pagedTraces(data: List<TraceDto>) = PagedDto(data, 0, 0)
fun pagedTraces(data: TraceDto) = PagedDto(listOf(data), 0, 0)

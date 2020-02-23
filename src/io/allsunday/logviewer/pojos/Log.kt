package io.allsunday.logviewer.pojos

data class Log(
    val id: Long,
    val timestamp: Long,
    val traceId: Long,
    val spanId: Long,
    val fields: Map<String, String>
)

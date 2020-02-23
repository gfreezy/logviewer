package io.allsunday.logviewer.pojos

data class Span(
    val id: Long,
    val parentId: Long?,
    val traceId: Long,
    val name: String,
    val timestamp: Long,
    val duration: Long,
    val tags: Map<String, String>,
    val finished: Boolean
)

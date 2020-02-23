package io.allsunday.logviewer.pojos

import kotlinx.serialization.Serializable

@Serializable
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

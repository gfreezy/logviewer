package io.allsunday.logviewer.pojos

import kotlinx.serialization.Serializable

@Serializable
data class Trace(
    val id: Long,
    val name: String,
    val timestamp: Long,
    val duration: Long,
    val finished: Boolean
)

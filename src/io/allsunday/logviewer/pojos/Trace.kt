package io.allsunday.logviewer.pojos

data class Trace(
    val id: Long,
    val name: String,
    val timestamp: Long,
    val duration: Long,
    val finished: Boolean
)

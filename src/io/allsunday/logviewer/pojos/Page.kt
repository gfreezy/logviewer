package io.allsunday.logviewer.pojos

import kotlinx.serialization.Serializable

@Serializable
data class Page<O, T>(
    val content: List<O>,
    val cursor: Cursor<T>?
)

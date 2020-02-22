package io.allsunday.logviewer.pojos

data class Page<O, T>(
    val content: List<O>,
    val cursor: Cursor<T>?
)

package io.allsunday.logviewer.pojos

import kotlinx.serialization.Serializable

@Serializable
data class Cursor<T>(val next: T)

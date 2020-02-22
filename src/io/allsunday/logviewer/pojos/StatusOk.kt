package io.allsunday.logviewer.pojos

enum class Status {
    OK,
    ERROR
}

data class Ok<T>(
    val content: T
) {
    val status: Status = Status.OK

    companion object {
        val empty = Ok(null)
    }
}

data class Error<T>(
    val content: T
) {
    val status: Status = Status.ERROR
}

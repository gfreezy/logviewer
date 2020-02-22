package io.allsunday.logviewer.libs.exposed

fun <T> retryUntilReturns(expected: T, maxAttempts: Int = 3, f: () -> T): T {
    assert(maxAttempts > 0)

    var ret: T? = null
    for (attempt in 1..maxAttempts) {
        ret = f()
        if (ret == expected) {
            break
        }
    }
    return ret!!
}

fun <T : Throwable> retryOnException(exception: Class<T>, maxAttempts: Int = 3, f: () -> T): T {
    assert(maxAttempts > 0)

    var throwable: Throwable? = null
    for (attempt in 1..maxAttempts) {
        try {
            return f()
        } catch (e: Throwable) {
            if (exception.isInstance(e)) {
                throwable = e
                continue
            } else {
                throwable = e
                break
            }
        }
    }
    throw throwable!!
}

package com.anksystems.lib.ext

import java.util.zip.CRC32

inline fun <A, B, R> takeIfNotNull(a: A?, b: B?, code: (A, B) -> R): R? {
    return if (a != null && b != null) {
        code(a, b)
    } else null
}

fun CRC32.calculateOnce(byteArray: ByteArray): Long {
    reset()
    update(byteArray)
    val result = value
    reset()

    return result
}

inline fun <T> tryOrNull(log: Boolean = false, f: () -> T): T? {
    return try {
        f()
    } catch (e: Exception) {
        println(e)
        null
    }
}

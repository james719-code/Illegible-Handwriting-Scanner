package com.enon.writingai.core.utils

object IdGenerator {
    private var nextValue: Int = 1

    fun next(prefix: String): String {
        val value = nextValue++
        return "$prefix-$value"
    }
}

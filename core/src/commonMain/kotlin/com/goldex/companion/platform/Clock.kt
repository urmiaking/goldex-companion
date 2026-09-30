package com.goldex.companion.platform

fun interface Clock {
    fun nowMillis(): Long
}

expect object SystemClock : Clock {
    override fun nowMillis(): Long
}

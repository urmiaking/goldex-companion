package com.goldex.companion.platform

actual object SystemClock : Clock {
    actual override fun nowMillis(): Long = System.currentTimeMillis()
}

package com.goldex.companion.platform

/** Formatting only. Financial arithmetic and rounding policies remain in domain code. */
expect object DecimalFormatting {
    fun groupedInteger(value: Long): String
    fun weight(value: Double): String
    fun fixed(value: Double, decimals: Int): String
}

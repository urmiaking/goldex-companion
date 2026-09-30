package com.goldex.companion.platform

data class LocalDateParts(
    val year: Int,
    val month: Int,
    val day: Int,
    /** Matches the existing Calendar convention: Sunday=1, Saturday=7. */
    val dayOfWeek: Int,
    val startOfDayMillis: Long
)

/** Device timezone/locale boundary; Solar Hijri conversions remain shared domain policies. */
expect object LocalCalendar {
    fun parts(timestamp: Long): LocalDateParts
    fun toEpochMillis(
        year: Int, month: Int, day: Int,
        hour: Int = 0, minute: Int = 0, second: Int = 0
    ): Long
    fun formatDateTime(pattern: String, timestamp: Long, useUsLocale: Boolean = false): String
}

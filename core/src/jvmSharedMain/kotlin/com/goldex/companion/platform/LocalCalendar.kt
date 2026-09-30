package com.goldex.companion.platform

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual object LocalCalendar {
    actual fun parts(timestamp: Long): LocalDateParts {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return LocalDateParts(year, month, day, dayOfWeek, calendar.timeInMillis)
    }

    actual fun toEpochMillis(
        year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int
    ): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    actual fun formatDateTime(pattern: String, timestamp: Long, useUsLocale: Boolean): String =
        SimpleDateFormat(pattern, if (useUsLocale) Locale.US else Locale.getDefault())
            .format(Date(timestamp))
}

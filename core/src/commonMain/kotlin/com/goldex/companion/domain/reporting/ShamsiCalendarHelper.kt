package com.goldex.companion.domain.reporting

import com.goldex.companion.platform.LocalCalendar

object ShamsiCalendarHelper {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val WEEKDAY_SHORT_NAMES = listOf(
        "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    fun getMonthNameFa(month: Int): String {
        return PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "فروردین" }
    }

    /**
     * Determines whether a given Solar Hijri year is a leap year (کبیسه).
     */
    fun isShamsiLeapYear(jy: Int): Boolean {
        val matches = intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)
        val mod = (jy + 38) % 33
        return matches.contains(mod)
    }

    /**
     * Returns the number of days in a Shamsi month (1..12).
     */
    fun getDaysInShamsiMonth(jy: Int, jm: Int): Int {
        return when {
            jm in 1..6 -> 31
            jm in 7..11 -> 30
            jm == 12 -> if (isShamsiLeapYear(jy)) 30 else 29
            else -> 30
        }
    }

    /**
     * Converts Gregorian date to Solar Hijri (jy, jm, jd).
     */
    fun gregorianToShamsi(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) (gy + 1) else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += ((days - 1) / 365)
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
        val jd = 1 + (if (days < 186) (days % 31) else ((days - 186) % 30))
        return Triple(jy, jm, jd)
    }

    /**
     * Converts Solar Hijri date to Gregorian (gy, gm, gd).
     */
    fun shamsiToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy2 = jy - 979
        val jm2 = jm - 1
        val jd2 = jd - 1

        var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33 + 3) / 4)
        for (i in 0 until jm2) {
            jDayNo += if (i < 6) 31 else 30
        }
        jDayNo += jd2

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gdm = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12 && gDayNo >= gdm[gm + 1]) {
            gDayNo -= gdm[gm + 1]
            gm++
        }
        val gd = gDayNo + 1
        return Triple(gy, gm + 1, gd)
    }

    /**
     * Converts a Shamsi date to Unix epoch millisecond timestamp.
     */
    fun shamsiToMillis(jy: Int, jm: Int, jd: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Long {
        val (gy, gm, gd) = shamsiToGregorian(jy, jm, jd)
        return LocalCalendar.toEpochMillis(gy, gm, gd, hour, minute, second)
    }

    /**
     * Converts Unix epoch millisecond timestamp to Shamsi (jy, jm, jd).
     */
    fun millisToShamsi(millis: Long): Triple<Int, Int, Int> {
        val date = LocalCalendar.parts(millis)
        return gregorianToShamsi(date.year, date.month, date.day)
    }

    /**
     * Returns the weekday index (0..6) of the 1st day of the given Shamsi month.
     * Saturday = 0, Sunday = 1, ... Friday = 6.
     */
    fun getFirstDayWeekdayIndex(jy: Int, jm: Int): Int {
        val (gy, gm, gd) = shamsiToGregorian(jy, jm, 1)
        return LocalCalendar.parts(LocalCalendar.toEpochMillis(gy, gm, gd)).dayOfWeek % 7
    }

    /**
     * Formats date as YYYY/MM/DD
     */
    fun formatShamsi(jy: Int, jm: Int, jd: Int): String {
        val mm = jm.toString().padStart(2, '0')
        val dd = jd.toString().padStart(2, '0')
        return "$jy/$mm/$dd"
    }

    /**
     * Parses "YYYY/MM/DD" into (jy, jm, jd)
     */
    fun parseShamsi(dateStr: String): Triple<Int, Int, Int>? {
        val parts = dateStr.replace("-", "/").replace(".", "/").split("/")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return Triple(y, m, d)
    }
}

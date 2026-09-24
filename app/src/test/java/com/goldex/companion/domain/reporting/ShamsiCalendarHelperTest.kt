package com.goldex.companion.domain.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShamsiCalendarHelperTest {

    @Test
    fun testKnownGregorianToShamsiDates() {
        // Nowrouz 1403: 2024-03-20
        val shamsiNowrouz1403 = ShamsiCalendarHelper.gregorianToShamsi(2024, 3, 20)
        assertEquals(Triple(1403, 1, 1), shamsiNowrouz1403)

        // 2024-09-21 corresponds to 1403-06-31
        val shamsiShahrivarEnd = ShamsiCalendarHelper.gregorianToShamsi(2024, 9, 21)
        assertEquals(Triple(1403, 6, 31), shamsiShahrivarEnd)

        // 2024-09-22 corresponds to 1403-07-01
        val shamsiMehrStart = ShamsiCalendarHelper.gregorianToShamsi(2024, 9, 22)
        assertEquals(Triple(1403, 7, 1), shamsiMehrStart)
    }

    @Test
    fun testShamsiToGregorianRoundtrip() {
        val testDates = listOf(
            Triple(1403, 1, 1),
            Triple(1403, 6, 31),
            Triple(1403, 7, 1),
            Triple(1403, 12, 29),
            Triple(1402, 12, 29),
            Triple(1399, 12, 30) // 1399 is leap
        )

        for (shamsi in testDates) {
            val (gYear, gMonth, gDay) = ShamsiCalendarHelper.shamsiToGregorian(shamsi.first, shamsi.second, shamsi.third)
            val convertedBack = ShamsiCalendarHelper.gregorianToShamsi(gYear, gMonth, gDay)
            assertEquals("Roundtrip failed for $shamsi", shamsi, convertedBack)
        }
    }

    @Test
    fun testDaysInMonth() {
        // Months 1..6 have 31 days
        for (m in 1..6) {
            assertEquals(31, ShamsiCalendarHelper.getDaysInShamsiMonth(1403, m))
        }

        // Months 7..11 have 30 days
        for (m in 7..11) {
            assertEquals(30, ShamsiCalendarHelper.getDaysInShamsiMonth(1403, m))
        }

        // Month 12: 1403 is leap (Esfand has 30 days), 1402 is not leap (29 days)
        assertTrue(ShamsiCalendarHelper.isShamsiLeapYear(1403))
        assertEquals(30, ShamsiCalendarHelper.getDaysInShamsiMonth(1403, 12))

        assertFalse(ShamsiCalendarHelper.isShamsiLeapYear(1402))
        assertEquals(29, ShamsiCalendarHelper.getDaysInShamsiMonth(1402, 12))
    }

    @Test
    fun testParseAndFormatShamsi() {
        val parsed = ShamsiCalendarHelper.parseShamsi("1403/07/04")
        assertNotNull(parsed)
        assertEquals(Triple(1403, 7, 4), parsed)

        val formatted = ShamsiCalendarHelper.formatShamsi(1403, 7, 4)
        assertEquals("1403/07/04", formatted)

        assertNull(ShamsiCalendarHelper.parseShamsi("invalid"))
    }

    @Test
    fun testShamsiToMillisAndBack() {
        val shamsi = Triple(1403, 5, 15)
        val millis = ShamsiCalendarHelper.shamsiToMillis(shamsi.first, shamsi.second, shamsi.third)
        assertTrue(millis > 0)

        val back = ShamsiCalendarHelper.millisToShamsi(millis)
        assertEquals(shamsi, back)
    }
}

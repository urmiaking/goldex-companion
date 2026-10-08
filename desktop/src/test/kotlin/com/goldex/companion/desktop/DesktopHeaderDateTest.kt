package com.goldex.companion.desktop

import com.goldex.companion.desktop.ui.workspaceDate
import java.time.Instant
import org.junit.Test
import kotlin.test.assertEquals

class DesktopHeaderDateTest {
    @Test fun persianDateUsesTodayInTehranAndRollsOverAtLocalMidnight() {
        fun date(utc: String) = workspaceDate(Instant.parse(utc).toEpochMilli())
        assertEquals("چهارشنبه ۱۵ مهر ۱۴۰۵", date("2026-10-07T12:00:00Z"))
        assertEquals("چهارشنبه ۱۵ مهر ۱۴۰۵", date("2026-10-07T20:29:59Z"))
        assertEquals("پنج‌شنبه ۱۶ مهر ۱۴۰۵", date("2026-10-07T20:30:00Z"))
        assertEquals("شنبه ۱ فروردین ۱۴۰۵", date("2026-03-21T00:00:00Z"))
    }
}

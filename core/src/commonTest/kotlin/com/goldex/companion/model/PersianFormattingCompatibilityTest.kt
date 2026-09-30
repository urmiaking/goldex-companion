package com.goldex.companion.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersianFormattingCompatibilityTest {
    @Test fun moneyWeightSignsAndPercentKeepTheirPublishedRepresentation() {
        assertEquals("۱,۲۳۴,۵۶۷", PersianNumberFormatter.formatPrice(1234567L))
        assertEquals("۱,۲۳۴-", PersianNumberFormatter.formatPrice(-1234.9))
        assertEquals("۰.۰۰۱", PersianNumberFormatter.formatWeight(0.001))
        assertEquals("۱,۲۳۴.۵۶۷-", PersianNumberFormatter.formatWeight(-1234.567))
        assertEquals("۱.۲۳۵+", PersianNumberFormatter.formatSignedWeight(1.235))
        assertEquals("۰.۰۰۰", PersianNumberFormatter.formatSignedWeight(0.0))
        assertEquals("۹", PersianNumberFormatter.formatPercent(9.0))
        assertEquals("۷.۲۵", PersianNumberFormatter.formatPercent(7.25))
        assertEquals("۱.۲۳-", PersianNumberFormatter.formatDouble(-1.23))
        assertEquals("۱,۰۰۰- (۱.۵٪-)", PersianNumberFormatter.formatDelta(-1000, -1.5))
    }

    @Test fun existingPersianDecimalAndGroupingInputsRemainCompatible() {
        assertEquals(1234.567, PersianNumberFormatter.parseToCleanDouble("۱،۲۳۴٫۵۶۷"))
        assertEquals(1.25, PersianNumberFormatter.parseToCleanDouble("۱/۲۵"))
        assertEquals(1234567L, PersianNumberFormatter.parseToCleanLong("۱,۲۳۴,۵۶۷"))
        assertNull(PersianNumberFormatter.parseToCleanLong("۱۲٫۵"))
        assertNull(PersianNumberFormatter.parseToCleanDouble("invalid"))
    }
}

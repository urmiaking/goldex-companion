package com.goldex.companion.ui.reporting

import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.domain.reporting.ReportingUiState
import com.goldex.companion.domain.reporting.VatReportLedger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportingShareTextTest {
    @Test
    fun vatSummaryUsesRecordedTaxAndDoesNotClaimOfficialExport() {
        val text = reportingShareText(
            ReportingBreakdownType.VAT_REPORT,
            ReportingUiState(vatReport = VatReportLedger(
                taxableBaseTomans = 300_000L,
                totalVatCollectedTomans = 27_000L,
                taxExemptRawGoldTomans = 2_000_000L
            ))
        )

        assertTrue(text.contains("۳۰۰,۰۰۰"))
        assertTrue(text.contains("۲۷,۰۰۰"))
        assertTrue(text.contains("۲,۰۰۰,۰۰۰"))
        assertFalse(text.contains("رسمی"))
    }
}

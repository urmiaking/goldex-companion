package com.goldex.companion.domain.reporting

import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.WageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportingUseCasesTest {

    @Test
    fun testPeriodTimeRange_Today_ProducesValidBoundaries() {
        val now = 1727175600000L // arbitrary timestamp
        val (start, end) = ReportingUseCases.calculatePeriodTimeRange(ReportingPeriod.TODAY, currentTimeMs = now)

        assertTrue("End must be greater than start", end > start)
        assertEquals("Range should span 24 hours minus 1 ms", 86_399_999L, end - start)
        assertTrue("Now should fall inside the today range", now in start..end)
    }

    @Test
    fun testPeriodTimeRange_ThisMonth_ProducesValidBoundaries() {
        val now = System.currentTimeMillis()
        val (start, end) = ReportingUseCases.calculatePeriodTimeRange(ReportingPeriod.THIS_MONTH, currentTimeMs = now)

        assertTrue(end >= start)
        assertTrue(now in start..end)
    }

    @Test
    fun testPeriodTimeRange_ThisYear_ProducesValidBoundaries() {
        val now = System.currentTimeMillis()
        val (start, end) = ReportingUseCases.calculatePeriodTimeRange(ReportingPeriod.THIS_YEAR, currentTimeMs = now)

        assertTrue(end >= start)
        assertTrue(now in start..end)
    }

    @Test
    fun testPeriodTimeRange_Custom_RespectsProvidedTimestamps() {
        val customStart = 1000L
        val customEnd = 5000L
        val (start, end) = ReportingUseCases.calculatePeriodTimeRange(
            ReportingPeriod.CUSTOM,
            customStartMs = customStart,
            customEndMs = customEnd
        )

        assertEquals(1000L, start)
        assertEquals(5000L, end)
    }

    @Test
    fun testFinancialVaultKpi_CalculatesAccurately() {
        val now = System.currentTimeMillis()
        val periodRange = Pair(now - 100_000L, now + 100_000L)

        val item1 = CraftedGoldItem(
            grossWeight = 10.0,
            netWeight = 10.0,
            spotPrice = 20_000_000L,
            wageInput = 10.0,
            wageAmount = 20_000_000.0,
            profitPercent = 7.0,
            profitAmount = 15_400_000.0,
            taxPercent = 9.0,
            taxAmount = 3_186_000.0,
            rawGoldValue = 200_000_000.0,
            totalPayable = 238_586_000.0,
            equivalent18kWeight = 10.0
        )

        val invoice = BarterInvoice(
            createdAt = now,
            salesItems = listOf(item1),
            cashPosAmount = 238_586_000L
        )

        val customer = Customer(
            name = "مشتری تستی",
            cashDebtTomans = 50_000_000L,
            goldDebtGrams = 2.5
        )

        val inventoryItem = InventoryItem(
            code = "R-100",
            title = "انگشتر البرنادو",
            category = InventoryCategory.RINGS,
            grossWeightGrams = 5.0,
            karat = Karat.K18,
            quantity = 2,
            location = "سینی شماره ۱ ویترین اصلی"
        )

        val kpi = ReportingUseCases.calculateFinancialVaultKpi(
            invoices = listOf(invoice),
            customers = listOf(customer),
            inventoryItems = listOf(inventoryItem),
            periodRange = periodRange
        )

        // Gross profit = wage (20,000,000) + profit (15,400,000) = 35,400,000
        assertEquals(35_400_000L, kpi.grossProfitTomans)
        // Inventory weight = 5.0 * 2 = 10.0 grams
        assertEquals(10.0, kpi.inventoryGoldWeight18k, 0.001)
        // Customer receivables = 50,000,000
        assertEquals(50_000_000L, kpi.customerReceivablesTomans)
        assertEquals(1, kpi.debtorCustomersCount)
    }

    @Test
    fun testQuickDailySummary_CalculatesTodayTrades() {
        val now = System.currentTimeMillis()
        val todayRange = Pair(now - 50_000L, now + 50_000L)

        val salesItem = CraftedGoldItem(
            grossWeight = 4.0,
            netWeight = 4.0,
            spotPrice = 20_000_000L,
            wageInput = 5.0,
            wageAmount = 4_000_000.0,
            profitPercent = 7.0,
            profitAmount = 5_880_000.0,
            taxPercent = 9.0,
            taxAmount = 889_200.0,
            rawGoldValue = 80_000_000.0,
            totalPayable = 90_769_200.0,
            equivalent18kWeight = 4.0
        )

        val scrapItem = ScrapGoldItem(
            grossWeight = 3.0,
            netWeight = 3.0,
            spotPrice = 20_000_000L,
            effectiveGramPrice = 19_600_000L,
            totalPayable = 58_800_000.0,
            equivalent18kWeight = 2.94
        )

        val invoice = BarterInvoice(
            createdAt = now,
            salesItems = listOf(salesItem),
            receivedItems = listOf(scrapItem)
        )

        val summary = ReportingUseCases.calculateQuickDailySummary(
            invoices = listOf(invoice),
            todayRange = todayRange
        )

        assertEquals(1, summary.salesCount)
        assertEquals(4.0, summary.salesWeight18k, 0.001)
        assertEquals(90_769_200L, summary.salesAmountTomans)

        assertEquals(1, summary.purchaseCount)
        assertEquals(2.94, summary.purchaseWeight18k, 0.001)
        assertEquals(58_800_000L, summary.purchaseAmountTomans)
    }

    @Test
    fun testVatReport_FollowsIranianGoldTaxLaw() {
        val now = System.currentTimeMillis()
        val periodRange = Pair(now - 10_000L, now + 10_000L)

        val item = CraftedGoldItem(
            grossWeight = 10.0,
            netWeight = 10.0,
            spotPrice = 20_000_000L,
            wageInput = 10.0,
            wageAmount = 20_000_000.0,
            profitPercent = 7.0,
            profitAmount = 15_400_000.0,
            taxPercent = 9.0,
            taxAmount = 3_186_000.0,
            rawGoldValue = 200_000_000.0,
            totalPayable = 238_586_000.0,
            equivalent18kWeight = 10.0
        )

        val invoice = BarterInvoice(
            createdAt = now,
            salesItems = listOf(item)
        )

        val vatReport = ReportingUseCases.calculateVatReport(
            invoices = listOf(invoice),
            periodRange = periodRange,
            defaultVatPercent = 9.0
        )

        // Taxable Base = Wage (20M) + Profit (15.4M) = 35.4M
        assertEquals(35_400_000L, vatReport.taxableBaseTomans)
        // VAT = 9% of 35.4M = 3,186,000
        assertEquals(3_186_000L, vatReport.totalVatCollectedTomans)
        // Raw Gold Exempt Base = 200,000,000
        assertEquals(200_000_000L, vatReport.taxExemptRawGoldTomans)
    }

    @Test
    fun testEmptyData_ReturnsZeroMetricsWithoutCrashing() {
        val range = Pair(100L, 200L)
        val kpi = ReportingUseCases.calculateFinancialVaultKpi(emptyList(), emptyList(), emptyList(), range)
        assertEquals(0L, kpi.grossProfitTomans)
        assertEquals(0.0, kpi.inventoryGoldWeight18k, 0.001)
        assertEquals(0L, kpi.turnoverTomans)
        assertEquals(0L, kpi.customerReceivablesTomans)

        val quick = ReportingUseCases.calculateQuickDailySummary(emptyList(), range)
        assertEquals(0, quick.salesCount)
        assertEquals(0L, quick.salesAmountTomans)

        val sp = ReportingUseCases.calculateSalesPerformance(emptyList(), range)
        assertEquals(0L, sp.grossSalesTomans)

        val gi = ReportingUseCases.calculateGoldInventory(emptyList())
        assertEquals(0.0, gi.totalWeight18k, 0.001)

        val dc = ReportingUseCases.calculateDebtorsCreditors(emptyList())
        assertEquals(0L, dc.totalReceivablesTomans)

        val vat = ReportingUseCases.calculateVatReport(emptyList(), range)
        assertEquals(0L, vat.taxableBaseTomans)
    }
}

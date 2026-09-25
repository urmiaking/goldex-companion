package com.goldex.companion.domain.reporting

import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportingDetailsUseCaseTest {
    private val sale = CraftedGoldItem(
        title = "انگشتر ۱۸ عیار",
        grossWeight = 2.0,
        netWeight = 2.0,
        spotPrice = 10_000_000L,
        wageInput = 10.0,
        wageAmount = 2_000_000.0,
        profitPercent = 7.0,
        profitAmount = 1_540_000.0,
        taxPercent = 9.0,
        taxAmount = 318_600.0,
        rawGoldValue = 20_000_000.0,
        totalPayable = 23_858_600.0,
        equivalent18kWeight = 2.0
    )

    @Test
    fun periodDetailsUseOnlyRecordedSalesWithinRange() {
        val details = ReportingDetailsUseCase.calculate(
            invoices = listOf(
                BarterInvoice(createdAt = 125L, salesItems = listOf(sale)),
                BarterInvoice(createdAt = 250L, salesItems = listOf(sale))
            ),
            customers = emptyList(), inventoryItems = emptyList(), adjustments = emptyList(),
            periodRange = 100L to 199L
        )

        assertEquals(1, details.salesCategories.size)
        assertEquals(23_858_600L, details.salesCategories.single().salesTomans)
        assertEquals(2_000_000L, details.salesCategories.single().wageTomans)
        assertEquals(1_540_000L, details.salesCategories.single().profitTomans)
        assertEquals(318_600L, details.salesCategories.single().vatTomans)
        assertEquals(3_540_000L, details.profitBucketsTomans.sum())
        assertEquals("انگشتر ۱۸ عیار", details.topSalesItems.single().title)
    }

    @Test
    fun inventoryAndBalancesReflectCurrentRecords() {
        val details = ReportingDetailsUseCase.calculate(
            invoices = emptyList(),
            customers = listOf(Customer(id = "customer-1", name = "آزمایش", cashDebtTomans = -5_000L, goldDebtGrams = 1.25)),
            inventoryItems = listOf(InventoryItem(code = "A", title = "انگشتر", grossWeightGrams = 2.5,
                quantity = 2, location = "ویترین")),
            adjustments = listOf(StockAdjustment(itemId = "item-1", itemTitle = "انگشتر",
                type = StockAdjustmentType.CHARGE, weightGrams = 5.0, timestamp = 100L)),
            periodRange = 100L to 200L
        )

        assertEquals(-5_000L, details.customerBalances.single().cashDebtTomans)
        assertEquals(1.25, details.customerBalances.single().goldDebtGrams, 0.001)
        assertEquals(5.0, details.inventoryLocations.single().weight18k, 0.001)
        assertEquals(2, details.inventoryLocations.single().piecesCount)
        assertEquals("انگشتر", details.inventoryMovements.single().title)
    }
}

package com.goldex.companion.domain.customers

import com.goldex.companion.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomerStatementBalancesTest {
    @Test fun twoThousandRowsReconstructOpeningAndLatestBalancesRegardlessOfInputOrder() {
        val rows = (1..2000).map { LedgerTransaction(id = "tx-$it", customerId = "customer", timestamp = it.toLong(),
            type = LedgerEntryType.CASH_RIAL, direction = LedgerDirection.PAY, amountTomans = 10L) }
        val result = customerStatementBalances(Customer(id = "customer", name = "مشتری", goldDebtGrams = 2.5, cashDebtTomans = 20_100), rows.shuffled())
        assertEquals(2000, result.size)
        assertEquals(2.5 to 20_100L, result["tx-2000"])
        assertEquals(2.5 to 110L, result["tx-1"])
        for (i in 1..2000) assertEquals(100L + i * 10, result["tx-$i"]?.second)
    }

    @Test fun settlementUsesRecordedEffectsAndTimestampTieBreakIsPreserved() {
        val receipt = LedgerTransaction(id = "receipt", customerId = "c", timestamp = 1, dateTime = "2",
            settlement = LedgerSettlement(LedgerEntryType.GOLD_WEIGHT, 10L, "دلخواه", "", false, -1.0, 30L))
        val older = LedgerTransaction(id = "older", customerId = "c", timestamp = 1, dateTime = "1",
            direction = LedgerDirection.PAY, equivalent750WeightGrams = 2.0)
        val result = customerStatementBalances(Customer(id = "c", name = "مشتری", goldDebtGrams = 3.0, cashDebtTomans = 50L), listOf(older, receipt))
        assertEquals(3.0 to 50L, result["receipt"])
        assertEquals(4.0 to 20L, result["older"])
    }
}

package com.goldex.companion.domain.reporting

import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.MarketHistoryConverter
import java.util.Calendar

object ReportingUseCases {

    /**
     * Calculates the [startMs, endMs] millisecond boundaries for the given [ReportingPeriod].
     */
    fun calculatePeriodTimeRange(
        period: ReportingPeriod,
        customStartMs: Long? = null,
        customEndMs: Long? = null,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
        }

        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Saturday=7, Sunday=1, ... Friday=6

        // Midnight today (00:00:00.000)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfTodayMs = cal.timeInMillis

        // End of today (23:59:59.999)
        val endOfTodayMs = startOfTodayMs + 86_400_000L - 1L

        return when (period) {
            ReportingPeriod.TODAY -> Pair(startOfTodayMs, endOfTodayMs)

            ReportingPeriod.THIS_WEEK -> {
                // In Iranian business calendar, Saturday is day 0 of the week
                val daysSinceSaturday = dayOfWeek % 7
                val startOfWeekMs = startOfTodayMs - (daysSinceSaturday * 86_400_000L)
                Pair(startOfWeekMs, endOfTodayMs)
            }

            ReportingPeriod.THIS_MONTH -> {
                val (_, _, jd) = MarketHistoryConverter.gregorianToShamsi(gy, gm, gd)
                val daysSinceMonthStart = (jd - 1).coerceAtLeast(0)
                val startOfMonthMs = startOfTodayMs - (daysSinceMonthStart * 86_400_000L)
                Pair(startOfMonthMs, endOfTodayMs)
            }

            ReportingPeriod.THIS_QUARTER -> {
                val (_, jm, jd) = MarketHistoryConverter.gregorianToShamsi(gy, gm, gd)
                val monthsSinceQuarterStart = (jm - 1) % 3
                val daysPerPriorMonth = if (jm <= 6) 31 else 30
                val daysSinceQuarterStart = monthsSinceQuarterStart * daysPerPriorMonth + jd - 1
                Pair(startOfTodayMs - daysSinceQuarterStart * 86_400_000L, endOfTodayMs)
            }

            ReportingPeriod.THIS_YEAR -> {
                val (_, jm, jd) = MarketHistoryConverter.gregorianToShamsi(gy, gm, gd)
                val daysPassedInYear = if (jm <= 6) {
                    (jm - 1) * 31 + (jd - 1)
                } else {
                    (6 * 31) + (jm - 7) * 30 + (jd - 1)
                }
                val startOfYearMs = startOfTodayMs - (daysPassedInYear.coerceAtLeast(0) * 86_400_000L)
                Pair(startOfYearMs, endOfTodayMs)
            }

            ReportingPeriod.CUSTOM -> {
                val start = customStartMs ?: startOfTodayMs
                val end = customEndMs ?: endOfTodayMs
                Pair(start.coerceAtMost(end), end.coerceAtLeast(start))
            }
        }
    }

    /**
     * Aggregates the 4 Hero Card KPIs based on data within the selected time window.
     */
    fun calculateFinancialVaultKpi(
        invoices: List<BarterInvoice>,
        customers: List<Customer>,
        inventoryItems: List<InventoryItem>,
        periodRange: Pair<Long, Long>
    ): FinancialVaultKpi {
        val (startMs, endMs) = periodRange
        val periodInvoices = invoices.filter { it.createdAt in startMs..endMs }

        // 1. Gross Profit: Wage earned + profit margin across sales items in period
        var grossProfit = 0L
        for (invoice in periodInvoices) {
            for (item in invoice.salesItems) {
                if (item is CraftedGoldItem) {
                    grossProfit += (item.wageAmount + item.profitAmount).toLong()
                }
            }
        }

        // 2. Total Gold Weight in Vault + Showcase (current snapshot)
        val inventoryWeight18k = inventoryItems.sumOf { it.weightIn18kGrams * it.quantity }

        // 3. Financial Turnover: Sum of sales + received amounts + payments in period
        var turnover = 0L
        for (invoice in periodInvoices) {
            turnover += invoice.salesItems.sumOf { it.totalPayable }.toLong()
            turnover += invoice.receivedItems.sumOf { it.totalPayable }.toLong()
            turnover += invoice.totalPaymentsAmount
        }

        // 4. Customer Receivables: Outstanding cash balance from customers and invoices
        var receivables = 0L
        var debtorCount = 0
        for (customer in customers) {
            if (customer.cashDebtTomans > 0L) {
                receivables += customer.cashDebtTomans
                debtorCount++
            }
        }
        val unsettledInvoicesCount = invoices.count { !it.isFullySettled && it.remainingBalanceTomans > 0L }

        return FinancialVaultKpi(
            grossProfitTomans = grossProfit,
            inventoryGoldWeight18k = inventoryWeight18k,
            turnoverTomans = turnover,
            customerReceivablesTomans = receivables,
            unsettledInvoicesCount = unsettledInvoicesCount,
            debtorCustomersCount = debtorCount
        )
    }

    /**
     * Calculates Today's Quick Realtime Summary.
     */
    fun calculateQuickDailySummary(
        invoices: List<BarterInvoice>,
        todayRange: Pair<Long, Long>
    ): QuickDailySummary {
        val (startOfToday, endOfToday) = todayRange
        val todayInvoices = invoices.filter { it.createdAt in startOfToday..endOfToday }

        var salesCount = 0
        var salesWeight18k = 0.0
        var salesAmountTomans = 0L

        var purchaseCount = 0
        var purchaseWeight18k = 0.0
        var purchaseAmountTomans = 0L

        for (invoice in todayInvoices) {
            salesCount += invoice.salesItems.size
            salesWeight18k += invoice.salesItems.sumOf { it.equivalent18kWeight }
            salesAmountTomans += invoice.salesItems.sumOf { it.totalPayable }.toLong()

            purchaseCount += invoice.receivedItems.size
            purchaseWeight18k += invoice.receivedItems.sumOf { it.equivalent18kWeight }
            purchaseAmountTomans += invoice.receivedItems.sumOf { it.totalPayable }.toLong()
        }

        return QuickDailySummary(
            salesCount = salesCount,
            salesWeight18k = salesWeight18k,
            salesAmountTomans = salesAmountTomans,
            purchaseCount = purchaseCount,
            purchaseWeight18k = purchaseWeight18k,
            purchaseAmountTomans = purchaseAmountTomans
        )
    }

    /**
     * Aggregates Module 1: Sales Performance & Gross Profit Ledger
     */
    fun calculateSalesPerformance(
        invoices: List<BarterInvoice>,
        periodRange: Pair<Long, Long>
    ): SalesPerformanceLedger {
        val (startMs, endMs) = periodRange
        val periodInvoices = invoices.filter { it.createdAt in startMs..endMs }

        var grossSales = 0L
        var rawGoldValue = 0L
        var totalWage = 0L
        var totalProfit = 0L
        var totalTax = 0L
        var totalWeight18k = 0.0
        var itemsCount = 0

        for (invoice in periodInvoices) {
            for (item in invoice.salesItems) {
                itemsCount++
                totalWeight18k += item.equivalent18kWeight
                grossSales += item.totalPayable.toLong()

                if (item is CraftedGoldItem) {
                    rawGoldValue += item.rawGoldValue.toLong()
                    totalWage += item.wageAmount.toLong()
                    totalProfit += item.profitAmount.toLong()
                    totalTax += item.taxAmount.toLong()
                } else {
                    rawGoldValue += item.totalPayable.toLong()
                }
            }
        }

        return SalesPerformanceLedger(
            grossSalesTomans = grossSales,
            rawGoldValueTomans = rawGoldValue,
            totalWageTomans = totalWage,
            totalProfitTomans = totalProfit,
            totalTaxTomans = totalTax,
            totalWeight18k = totalWeight18k,
            itemsCount = itemsCount
        )
    }

    /**
     * Aggregates Module 2: Gold Inventory & Vault Ledger
     */
    fun calculateGoldInventory(
        items: List<InventoryItem>
    ): GoldInventoryLedger {
        var totalWeight18k = 0.0
        var showcaseWeight = 0.0
        var vaultWeight = 0.0
        var totalPieces = 0

        for (item in items) {
            val weight = item.weightIn18kGrams * item.quantity
            totalWeight18k += weight
            totalPieces += item.quantity

            val isShowcase = item.location.contains("ویترین") || item.location.contains("سینی")
            if (isShowcase) {
                showcaseWeight += weight
            } else {
                vaultWeight += weight
            }
        }

        val activeTrays = items.map { it.location }.filter { it.contains("سینی") || it.contains("ویترین") }.distinct().size
        val activeSafes = items.map { it.location }.filter { it.contains("گاوصندوق") || it.contains("انبار") }.distinct().size

        return GoldInventoryLedger(
            totalWeight18k = totalWeight18k,
            showcaseWeight18k = showcaseWeight,
            vaultWeight18k = vaultWeight,
            piecesCount = totalPieces,
            activeTraysCount = activeTrays,
            activeSafesCount = activeSafes
        )
    }

    /**
     * Aggregates Module 3: Customer Debtors and Creditors Ledger
     */
    fun calculateDebtorsCreditors(
        customers: List<Customer>
    ): DebtorsCreditorsLedger {
        var totalReceivables = 0L
        var debtorCount = 0
        var totalPayables = 0L
        var creditorCount = 0
        var totalGoldDebt = 0.0

        for (customer in customers) {
            if (customer.cashDebtTomans > 0L) {
                totalReceivables += customer.cashDebtTomans
                debtorCount++
            } else if (customer.cashDebtTomans < 0L) {
                totalPayables += -customer.cashDebtTomans
                creditorCount++
            }
            totalGoldDebt += customer.goldDebtGrams
        }

        return DebtorsCreditorsLedger(
            totalReceivablesTomans = totalReceivables,
            debtorCount = debtorCount,
            totalPayablesTomans = totalPayables,
            creditorCount = creditorCount,
            goldDebtGrams = totalGoldDebt
        )
    }

    /**
     * Aggregates Module 4: VAT & Moadian Tax System
     */
    fun calculateVatReport(
        invoices: List<BarterInvoice>,
        periodRange: Pair<Long, Long>,
        defaultVatPercent: Double = 9.0
    ): VatReportLedger {
        val (startMs, endMs) = periodRange
        val periodInvoices = invoices.filter { it.createdAt in startMs..endMs }

        var taxableBase = 0L
        var totalVat = 0L
        var rawGoldExempt = 0L

        for (invoice in periodInvoices) {
            for (item in invoice.salesItems) {
                if (item is CraftedGoldItem) {
                    val wageProfit = (item.wageAmount + item.profitAmount).toLong()
                    taxableBase += wageProfit
                    totalVat += item.taxAmount.toLong()
                    rawGoldExempt += item.rawGoldValue.toLong()
                }
            }
        }

        return VatReportLedger(
            taxableBaseTomans = taxableBase,
            totalVatCollectedTomans = totalVat,
            taxExemptRawGoldTomans = rawGoldExempt,
            vatPercent = defaultVatPercent
        )
    }
}

package com.goldex.companion.domain.reporting

import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType

/** Projects recorded activity for the four full report screens; it never invents sample rows. */
object ReportingDetailsUseCase {
    fun calculate(
        invoices: List<BarterInvoice>,
        customers: List<Customer>,
        inventoryItems: List<InventoryItem>,
        adjustments: List<StockAdjustment>,
        periodRange: Pair<Long, Long>
    ): ReportingDetails {
        val (start, end) = periodRange
        val periodInvoices = invoices.filter { it.createdAt in start..end }
        val sales = periodInvoices.flatMap { it.salesItems }
        val categories = sales.groupBy { it.category }.map { (category, items) ->
            val crafted = items.filterIsInstance<CraftedGoldItem>()
            SalesCategoryReportRow(
                title = category.titleFa,
                salesTomans = items.sumOf { it.totalPayable.toLong() },
                wageTomans = crafted.sumOf { it.wageAmount.toLong() },
                profitTomans = crafted.sumOf { it.profitAmount.toLong() },
                vatTomans = crafted.sumOf { it.taxAmount.toLong() }
            )
        }.sortedByDescending { it.salesTomans }

        val topItems = sales.filterIsInstance<CraftedGoldItem>()
            .map {
                SalesItemReportRow(
                    title = it.title,
                    weight18k = it.equivalent18kWeight,
                    earningsTomans = it.wageAmount.toLong() + it.profitAmount.toLong()
                )
            }
            .sortedByDescending { it.earningsTomans }
            .take(5)

        val buckets = LongArray(4)
        val previousBuckets = LongArray(4)
        val duration = (end - start).coerceAtLeast(1L)
        periodInvoices.forEach { invoice ->
            val bucket = (((invoice.createdAt - start).toDouble() / duration) * 4)
                .toInt().coerceIn(0, 3)
            buckets[bucket] += invoice.salesItems.filterIsInstance<CraftedGoldItem>()
                .sumOf { it.wageAmount.toLong() + it.profitAmount.toLong() }
        }
        val previousStart = start - duration - 1L
        invoices.filter { it.createdAt in previousStart until start }.forEach { invoice ->
            val bucket = (((invoice.createdAt - previousStart).toDouble() / duration) * 4)
                .toInt().coerceIn(0, 3)
            previousBuckets[bucket] += invoice.salesItems.filterIsInstance<CraftedGoldItem>()
                .sumOf { it.wageAmount.toLong() + it.profitAmount.toLong() }
        }

        val balances = customers.filter { it.cashDebtTomans != 0L || it.goldDebtGrams != 0.0 }
            .map {
                CustomerBalanceReportRow(
                    id = it.id,
                    name = it.name,
                    role = it.role,
                    cashDebtTomans = it.cashDebtTomans,
                    goldDebtGrams = it.goldDebtGrams
                )
            }
            .sortedByDescending { kotlin.math.abs(it.cashDebtTomans) }

        val locations = inventoryItems.groupBy { it.location.ifBlank { "محل ثبت‌نشده" } }
            .map { (location, items) ->
                InventoryLocationReportRow(
                    location = location,
                    weight18k = items.sumOf { it.weightIn18kGrams * it.quantity },
                    piecesCount = items.sumOf { it.quantity }
                )
            }
            .sortedByDescending { it.weight18k }

        val periodAdjustments = adjustments.filter { it.timestamp in start..end }
        val movements = periodAdjustments.sortedByDescending { it.timestamp }.take(8).map {
            InventoryMovementReportRow(
                title = it.itemTitle,
                typeLabel = it.type.labelFa,
                weightGrams = it.weightGrams,
                timestamp = it.timestamp
            )
        }

        return ReportingDetails(
            salesCategories = categories,
            topSalesItems = topItems,
            profitBucketsTomans = buckets.toList(),
            previousProfitBucketsTomans = previousBuckets.toList(),
            customerBalances = balances,
            inventoryLocations = locations,
            inventoryMovements = movements,
            periodInboundGrams = periodAdjustments.filter { it.type == StockAdjustmentType.CHARGE }.sumOf { it.weightGrams },
            periodOutboundGrams = periodAdjustments.filter { it.type == StockAdjustmentType.DEDUCT }.sumOf { it.weightGrams }
        )
    }
}

package com.goldex.companion.domain.inventory

import com.goldex.companion.model.*

data class InventoryPriceBreakdown(val raw: Long, val wage: Long, val profit: Long, val tax: Long) {
    val total: Long get() = raw + wage + profit + tax
}

/** Preserves InventoryItem's established operation order and whole-toman truncation. */
object InventoryPricing {
    fun breakdown(item: InventoryItem, spot18: Long): InventoryPriceBreakdown? {
        if (spot18 !in 1..1_000_000_000_000L) return null
        val rawValue = item.netGoldWeightGrams * spot18 * (item.customKaratValue.toDouble() / 750.0)
        if (!rawValue.isFinite() || rawValue > 1_000_000_000_000_000_000.0) return null
        val raw = rawValue.toLong()
        val wage = (if (item.wageType == WageType.PERCENTAGE) raw * (item.wageValue / 100.0) else item.netGoldWeightGrams * item.wageValue).toLong()
        val profit = if (item.category == InventoryCategory.COINS) 0L else ((raw + wage) * (item.profitPercent / 100.0)).toLong()
        val tax = ((wage + profit) * (item.taxPercent / 100.0)).toLong()
        return InventoryPriceBreakdown(raw, wage, profit, tax)
    }
}

object InventoryStockPolicy {
    fun adjustedQuantity(item: InventoryItem, type: StockAdjustmentType, count: Int): Int {
        require(count > 0) { "تعداد ورود یا خروج باید مثبت باشد" }
        val result = item.quantity.toLong() + if (type == StockAdjustmentType.CHARGE) count.toLong() else -count.toLong()
        require(result in 0..1_000_000) { "موجودی کافی نیست یا تعداد از سقف مجاز بیشتر است" }
        return result.toInt()
    }
}

package com.goldex.companion.model

import java.util.UUID

enum class InventoryCategory(val titleFa: String) {
    ALL("همه موجودی"),
    RINGS("انگشتر و حلقه"),
    BANGLES("دستبند و النگو"),
    NECKLACES("گردنبند و مدال"),
    SETS("سرویس و نیم‌ست"),
    COINS("مسکوکات و سکه"),
    MISC("متفرقه")
}

enum class StockAdjustmentType(val labelFa: String) {
    CHARGE("شارژ موجودی (ورود)"),
    DEDUCT("کسر موجودی (خروج)")
}

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val title: String,
    val category: InventoryCategory = InventoryCategory.RINGS,
    val location: String = "سینی شماره ۱ ویترین اصلی",
    val grossWeightGrams: Double = 0.0,
    val stoneWeightGrams: Double = 0.0,
    val karat: Karat = Karat.K18,
    val customKaratValue: Int = 750,
    val workshop: String = "کارگاه زرین تهران",
    val wagePercent: Double = 0.0,
    val profitPercent: Double = 7.0,
    val taxPercent: Double = 9.0,
    val rfidTag: String = "",
    val quantity: Int = 1,
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val netGoldWeightGrams: Double
        get() = (grossWeightGrams - stoneWeightGrams).coerceAtLeast(0.0)

    /**
     * Calculates the estimated gallery retail price using official gold guild rules:
     * RawGoldValue = NetWeight * Spot18k * Karat / 750
     * WageAmount = RawGoldValue * WagePercent / 100
     * ProfitAmount = (RawGoldValue + WageAmount) * ProfitPercent / 100
     * TaxAmount = (WageAmount + ProfitAmount) * TaxPercent / 100
     * TotalPayable = RawGoldValue + WageAmount + ProfitAmount + TaxAmount
     */
    fun calculateEstimatedValue(spotPrice18k: Long): Long {
        if (spotPrice18k <= 0L || netGoldWeightGrams <= 0.0) return 0L
        val karatRatio = karat.purityRatio / Karat.K18.purityRatio
        val rawGold = (netGoldWeightGrams * spotPrice18k.toDouble() * karatRatio).toLong()
        val wage = (rawGold.toDouble() * (wagePercent / 100.0)).toLong()
        val profit = ((rawGold + wage).toDouble() * (profitPercent / 100.0)).toLong()
        val tax = ((wage + profit).toDouble() * (taxPercent / 100.0)).toLong()
        return rawGold + wage + profit + tax
    }

    /**
     * Equivalent gold weight converted to 18 Karat standard (750 purity).
     */
    val weightIn18kGrams: Double
        get() = netGoldWeightGrams * (karat.purityRatio / Karat.K18.purityRatio)
}

data class StockAdjustment(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemTitle: String,
    val type: StockAdjustmentType = StockAdjustmentType.CHARGE,
    val quantityChange: Int = 1,
    val weightGrams: Double = 0.0,
    val reason: String = "دریافت از کارگاه ساخت",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

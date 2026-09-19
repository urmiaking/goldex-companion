package com.goldex.companion.model

import java.util.UUID

enum class InventoryCategory(val titleFa: String) {
    ALL("همه موجودی"),
    RINGS("انگشتر و حلقه"),
    BANGLES("دستبند و النگو"),
    NECKLACES("گردنبند و زنجیر"),
    SETS("سرویس و نیم‌ست"),
    JEWELRY("جواهرات و نگین"),
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
    val customKaratValue: Int = when (karat) {
        Karat.K21 -> 875
        Karat.K24 -> 1000
        else -> 750
    },
    val workshop: String = "کارگاه زرین تهران",
    val wageType: WageType = WageType.PERCENTAGE,
    val wagePercent: Double = 0.0,
    val wageValue: Double = wagePercent,
    val profitPercent: Double = when (category) {
        InventoryCategory.JEWELRY -> 20.0
        InventoryCategory.COINS -> 0.0
        else -> 7.0
    },
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
     * WageAmount = RawGoldValue * WagePercent / 100 or NetWeight * WagePerGram
     * ProfitAmount = (RawGoldValue + WageAmount) * ProfitPercent / 100 (0% for coins)
     * TaxAmount = (WageAmount + ProfitAmount) * TaxPercent / 100
     * TotalPayable = RawGoldValue + WageAmount + ProfitAmount + TaxAmount
     */
    fun calculateEstimatedValue(spotPrice18k: Long, customVatRate: Double? = null): Long {
        if (spotPrice18k <= 0L || netGoldWeightGrams <= 0.0) return 0L
        val karatRatio = customKaratValue.toDouble() / 750.0
        val rawGold = (netGoldWeightGrams * spotPrice18k.toDouble() * karatRatio).toLong()
        val wage = when (wageType) {
            WageType.PERCENTAGE -> (rawGold.toDouble() * (wageValue / 100.0)).toLong()
            WageType.TOMAN_PER_GRAM -> (netGoldWeightGrams * wageValue).toLong()
        }
        val profit = if (category == InventoryCategory.COINS) 0L else ((rawGold + wage).toDouble() * (profitPercent / 100.0)).toLong()
        val effectiveTaxPercent = customVatRate ?: taxPercent
        val tax = ((wage + profit).toDouble() * (effectiveTaxPercent / 100.0)).toLong()
        return rawGold + wage + profit + tax
    }

    /**
     * Equivalent gold weight converted to 18 Karat standard (750 purity).
     */
    val weightIn18kGrams: Double
        get() = netGoldWeightGrams * (customKaratValue.toDouble() / 750.0)
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

package com.goldex.companion.domain.onboarding

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.model.*

/** Wizard inputs. Cash fields are retained for UI compatibility; existing behavior does not seed them. */
data class OpeningInventoryInput(
    val vitrinWeight: String = "",
    val vitrinOjrat: String = "",
    val meltWeight: String = "",
    val meltAyar: String = "750",
    val coinTamam: Int = 0,
    val coinNim: Int = 0,
    val coinRob: Int = 0,
    val coinQadim: Int = 0,
    val coinGerami: Int = 0,
    val cashTankhah: String = "",
    val bankBalances: String = ""
)

data class OpeningInventoryPlan(
    val inventory: List<InventoryItem>,
    val portfolio: List<PortfolioItem>
)

/** Preserves the existing opening weights, purity, fees and portfolio valuation inputs. */
object OpeningInventoryPolicy {
    fun build(settings: AppSettings, input: OpeningInventoryInput): OpeningInventoryPlan {
        val inventory = mutableListOf<InventoryItem>()
        val portfolio = mutableListOf<PortfolioItem>()
        val vitrinWeight = PersianNumberFormatter.parseToCleanDouble(input.vitrinWeight) ?: 0.0
        val vitrinOjrat = PersianNumberFormatter.parseToCleanDouble(input.vitrinOjrat) ?: 0.0
        if (vitrinWeight > 0.0) {
            val galleryTitle = settings.galleryName.ifBlank { "گالری" }
            val title = "مصنوعات ویترین ($galleryTitle)"
            inventory += InventoryItem(
                code = "VITRIN-01", title = title, category = InventoryCategory.SETS,
                location = "سینی شماره ۱ ویترین اصلی", grossWeightGrams = vitrinWeight,
                karat = Karat.K18, customKaratValue = 750, workshop = galleryTitle,
                wageType = WageType.PERCENTAGE, wagePercent = vitrinOjrat, wageValue = vitrinOjrat,
                profitPercent = settings.defaultProfitPercent.toDoubleOrNull() ?: 7.0,
                taxPercent = settings.defaultTaxPercent.toDoubleOrNull() ?: 9.0, quantity = 1
            )
            portfolio += PortfolioItem(
                title = title, category = PortfolioCategory.GOLD, weightGrams = vitrinWeight,
                karat = Karat.K18, purchasePriceTotal = 0L, purchaseDate = "موجودی اول دوره"
            )
        }
        val meltWeight = PersianNumberFormatter.parseToCleanDouble(input.meltWeight) ?: 0.0
        val meltAyar = PersianNumberFormatter.parseToCleanLong(input.meltAyar)?.toInt() ?: 750
        if (meltWeight > 0.0) {
            val karat = if (meltAyar >= 900) Karat.K21 else Karat.K18
            val title = "طلای آبشده گاوصندوق"
            inventory += InventoryItem(
                code = "MELT-01", title = title, category = InventoryCategory.MISC,
                location = "گاوصندوق اصلی", grossWeightGrams = meltWeight,
                karat = karat, customKaratValue = meltAyar, workshop = "ری‌گیری و ذوب",
                wageType = WageType.PERCENTAGE, wagePercent = 0.0,
                profitPercent = 0.0, taxPercent = 0.0, quantity = 1
            )
            portfolio += PortfolioItem(
                title = title, category = PortfolioCategory.GOLD, weightGrams = meltWeight,
                karat = karat, purchasePriceTotal = 0L, purchaseDate = "موجودی اول دوره"
            )
        }
        fun coin(count: Int, code: String, title: String, weight: Double, type: CoinType) {
            if (count <= 0) return
            inventory += InventoryItem(
                code = code, title = title, category = InventoryCategory.COINS,
                location = "گاوصندوق اصلی", grossWeightGrams = weight,
                karat = Karat.K21, customKaratValue = 900, quantity = count,
                profitPercent = 0.0, taxPercent = 0.0
            )
            portfolio += PortfolioItem(
                title = title, category = PortfolioCategory.COIN, quantity = count,
                coinType = type, purchaseDate = "موجودی اول دوره"
            )
        }
        coin(input.coinTamam, "COIN-EMAMI", "تمام بهار آزادی (طرح جدید)", 8.133, CoinType.EMAMI)
        coin(input.coinNim, "COIN-NIM", "نیم سکه بهار آزادی", 4.066, CoinType.HALF)
        coin(input.coinRob, "COIN-ROB", "ربع سکه بهار آزادی", 2.033, CoinType.QUARTER)
        coin(input.coinQadim, "COIN-QADIM", "تمام بهار آزادی (طرح قدیم)", 8.133, CoinType.BAHAR)
        coin(input.coinGerami, "COIN-GERAMI", "سکه یک گرمی بانکی", 1.01, CoinType.GERAMI)
        return OpeningInventoryPlan(inventory.toList(), portfolio.toList())
    }
}

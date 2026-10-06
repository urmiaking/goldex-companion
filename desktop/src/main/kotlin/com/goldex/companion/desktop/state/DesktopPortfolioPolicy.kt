package com.goldex.companion.desktop.state

import com.goldex.companion.data.*
import com.goldex.companion.domain.portfolio.PortfolioSummary
import com.goldex.companion.domain.portfolio.PortfolioValuation
import com.goldex.companion.model.*
import java.math.BigDecimal

data class PortfolioDraft(
    val id: String? = null, val title: String = "", val category: PortfolioCategory = PortfolioCategory.GOLD,
    val weight: String = "", val karat: Karat = Karat.K18, val quantity: String = "1", val coin: CoinType = CoinType.EMAMI,
    val cost: String = "", val date: String = "", val errors: Map<String, String> = emptyMap()
)

data class AssetRow(val item: PortfolioItem, val value: Long?, val profit: Long?)
data class PortfolioProjection(val rows: List<AssetRow>, val summary: PortfolioSummary?, val goldWeight18: Double, val coinCount: Long) {
    val knownPurchaseBasis get() = rows.isNotEmpty() && rows.all { it.item.purchasePriceTotal > 0 }
}

object DesktopPortfolioPolicy {
    fun normalized(text: String) = PersianNumberFormatter.toEnglishDigits(text.trim()).replace("٬", "").replace("،", "").replace(",", "").replace("٫", ".")
    fun validate(draft: PortfolioDraft): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (draft.title.trim().isEmpty() || draft.title.length > 120) errors["title"] = "نام دارایی را با حداکثر ۱۲۰ حرف وارد کنید"
        val costText = normalized(draft.cost)
        if (!costText.matches(Regex("[0-9]+")) || costText.toLongOrNull()?.let { it in 0..1_000_000_000_000_000L } != true) errors["cost"] = "مبلغ خرید را به تومان وارد کنید؛ برای نامشخص، صفر بگذارید"
        if (draft.category == PortfolioCategory.GOLD) {
            val weight = normalized(draft.weight)
            if (!weight.matches(Regex("[0-9]+(\\.[0-9]{1,3})?")) || weight.toDoubleOrNull()?.let { it > 0 && it <= 1_000_000 } != true) errors["weight"] = "وزن مثبت با حداکثر سه رقم اعشار وارد کنید"
        } else if (normalized(draft.quantity).toIntOrNull()?.let { it in 1..1_000_000 } != true) errors["quantity"] = "تعداد صحیح بین ۱ و ۱۰۰۰۰۰۰ وارد کنید"
        if (draft.date.length > 32) errors["date"] = "تاریخ را کوتاه وارد کنید"
        return errors
    }

    fun toItem(draft: PortfolioDraft): PortfolioItem {
        require(validate(draft).isEmpty())
        val base = PortfolioItem(title = draft.title.trim(), category = draft.category,
            weightGrams = if (draft.category == PortfolioCategory.GOLD) normalized(draft.weight).toDouble() else 0.0,
            karat = draft.karat, quantity = if (draft.category == PortfolioCategory.COIN) normalized(draft.quantity).toInt() else 1,
            coinType = if (draft.category == PortfolioCategory.COIN) draft.coin else null,
            purchasePriceTotal = normalized(draft.cost).toLong(), purchaseDate = draft.date.trim())
        return draft.id?.let { base.copy(id = it) } ?: base
    }

    fun draft(item: PortfolioItem) = PortfolioDraft(item.id, item.title, item.category, item.weightGrams.toString(), item.karat,
        item.quantity.toString(), item.coinType ?: CoinType.EMAMI, item.purchasePriceTotal.toString(), item.purchaseDate)

    fun project(items: List<PortfolioItem>, rates: MarketRates?): PortfolioProjection {
        val rows = items.map { item ->
            val quote = rates?.let { when (item.category) {
                PortfolioCategory.GOLD -> it.gold18
                PortfolioCategory.COIN -> when (item.coinType) { CoinType.BAHAR -> it.coinBahar; CoinType.HALF -> it.coinHalf; CoinType.QUARTER -> it.coinQuarter; CoinType.GERAMI -> it.coinGerami; else -> it.coinEmami }
            } } ?: 0
            // Guard the existing model's Long boundary before invoking its established valuation.
            val multiplier = if (item.category == PortfolioCategory.GOLD) item.weightGrams * item.karat.purityRatio / Karat.K18.purityRatio else item.quantity.toDouble()
            val safe = quote > 0 && BigDecimal.valueOf(quote).multiply(BigDecimal.valueOf(multiplier)) < BigDecimal.valueOf(Long.MAX_VALUE)
            val value = if (safe && rates != null) item.calculateCurrentValue(rates) else null
            AssetRow(item, value, value?.takeIf { item.purchasePriceTotal > 0 }?.minus(item.purchasePriceTotal))
        }
        val summary = if (rows.all { it.value != null } && rates != null && runCatching {
                rows.fold(0L) { sum, row -> Math.addExact(sum, row.value!!) }
                items.fold(0L) { sum, item -> Math.addExact(sum, item.purchasePriceTotal) }
            }.isSuccess) PortfolioValuation.summarize(items, rates) else null
        return PortfolioProjection(rows, summary, items.filter { it.category == PortfolioCategory.GOLD }.sumOf { it.weightGrams * it.karat.purityRatio / Karat.K18.purityRatio },
            items.filter { it.category == PortfolioCategory.COIN }.sumOf { it.quantity.toLong() })
    }

    fun observedTime(timestamp: Long): String {
        val time = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.of("Asia/Tehran"))
        val (year, month, day) = MarketHistoryConverter.gregorianToShamsi(time.year, time.monthValue, time.dayOfMonth)
        val date = "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')} ${time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))}"
        return PersianNumberFormatter.toPersianDigits(date)
    }
}

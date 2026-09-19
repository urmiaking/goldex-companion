package com.goldex.companion.data

import com.goldex.companion.model.MarketRateItemType

enum class PriceSource(val labelFa: String) {
    ISIGNAL("آی‌سیگنال (isignal.ir)"),
    TALA_IR("طلا.آی‌آر (tala.ir)"),
    TGJU("شبکه طلا و ارز (tgju.org)")
}

data class MarketRates(
    val gold18: Long = 23_360_000L,       // هر گرم طلای ۱۸ عیار (تومان)
    val gold24: Long = 31_148_000L,       // هر گرم طلای ۲۴ عیار (تومان)
    val goldMelt: Long = 101_500_000L,    // مظنه مثقال طلای آبشده ۱۷ عیار (تومان)
    val coinEmami: Long = 234_000_000L,   // سکه تمام طرح جدید (امامی)
    val coinBahar: Long = 230_000_000L,   // سکه تمام بهار آزادی
    val coinHalf: Long = 120_000_000L,    // نیم سکه بهار آزادی
    val coinQuarter: Long = 66_000_000L,  // ربع سکه بهار آزادی
    val coinGerami: Long = 35_000_000L,   // سکه گرمی
    val usd: Long = 221_500L,             // دلار آزاد (تومان)
    val ons: Double = 4435.0,             // انس جهانی
    val lastUpdated: String = "--:--:--",
    val source: PriceSource = PriceSource.ISIGNAL,
    val isLive: Boolean = true
)

data class MarketRateItemSummary(
    val type: MarketRateItemType,
    val currentPrice: Long = 0L,
    val dayLow: Long = 0L,
    val dayHigh: Long = 0L,
    val openPrice: Long = 0L,
    val changeAmount: Long = 0L,
    val changePercent: Double = 0.0,
    val isPositive: Boolean = true,
    val lastUpdated: Long = 0L
)

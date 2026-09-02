package com.goldex.companion.data

enum class PriceSource(val labelFa: String) {
    ISIGNAL("آی‌سیگنال (isignal.ir)"),
    TALA_IR("طلا.آی‌آر (tala.ir)")
}

data class MarketRates(
    val gold18: Long = 22_835_100L,       // هر گرم طلای ۱۸ عیار (تومان)
    val gold24: Long = 30_446_500L,       // هر گرم طلای ۲۴ عیار (تومان)
    val goldMelt: Long = 98_975_000L,     // مظنه مثقال طلای آبشده ۱۷ عیار (تومان)
    val coinEmami: Long = 228_000_000L,   // سکه تمام طرح جدید (امامی)
    val coinBahar: Long = 224_000_000L,   // سکه تمام بهار آزادی
    val coinHalf: Long = 115_500_000L,    // نیم سکه بهار آزادی
    val coinQuarter: Long = 62_500_000L,  // ربع سکه بهار آزادی
    val coinGerami: Long = 32_000_000L,   // سکه گرمی
    val usd: Long = 85_000L,              // دلار آزاد
    val ons: Double = 2500.0,             // انس جهانی
    val lastUpdated: String = "--:--:--",
    val source: PriceSource = PriceSource.ISIGNAL,
    val isLive: Boolean = true
)

package com.goldex.companion.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MarketRates(
    val gold18: Long = 3_720_000L,        // هر گرم طلای ۱۸ عیار
    val gold24: Long = 4_960_000L,        // هر گرم طلای ۲۴ عیار
    val goldMelt: Long = 16_115_000L,     // مظنه مثقال طلای آبشده ۱۷ عیار
    val coinEmami: Long = 42_850_000L,    // سکه تمام طرح جدید (امامی)
    val coinBahar: Long = 39_800_000L,    // سکه تمام بهار آزادی
    val coinHalf: Long = 24_100_000L,     // نیم سکه بهار آزادی
    val coinQuarter: Long = 14_550_000L,  // ربع سکه بهار آزادی
    val coinGerami: Long = 7_100_000L,    // سکه گرمی
    val usd: Long = 62_500L,              // دلار آزاد
    val ons: Double = 2500.0,             // انس طلای جهانی
    val lastUpdated: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val isLive: Boolean = true
)

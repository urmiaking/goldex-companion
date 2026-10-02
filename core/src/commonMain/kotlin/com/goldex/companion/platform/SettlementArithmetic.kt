package com.goldex.companion.platform

/** Decimal arithmetic: whole-toman valuations use HALF_UP; conversions retain sub-milligram remainders. */
expect object SettlementArithmetic {
    fun goldToTomans(weightGrams: Double, rate: Long): Long
    fun tomansToGold(amount: Long, rate: Long): Double
}

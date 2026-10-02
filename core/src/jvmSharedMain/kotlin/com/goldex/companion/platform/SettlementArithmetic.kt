package com.goldex.companion.platform

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

actual object SettlementArithmetic {
    actual fun goldToTomans(weightGrams: Double, rate: Long): Long {
        require(weightGrams.isFinite() && weightGrams >= 0 && rate > 0)
        return BigDecimal.valueOf(weightGrams).multiply(BigDecimal.valueOf(rate))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    actual fun tomansToGold(amount: Long, rate: Long): Double {
        require(amount >= 0 && rate > 0)
        return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(rate), MathContext.DECIMAL128).toDouble()
    }
}

package com.goldex.companion.domain.calculator

import com.goldex.companion.platform.SettlementArithmetic
import kotlin.math.roundToInt

/** Fineness is expressed in thousandths; 1..24 inputs use the calculator's karat convention. */
object KaratConversionPolicy {
    fun fineness(value: Int): Double? = when (value) {
        in 1..24 -> (value * 1000.0 / 24.0).roundToInt().toDouble()
        in 25..1000 -> value.toDouble()
        else -> null
    }

    fun convert(weight: Double, from: Int, to: Int): KaratConversionResult? {
        if (!weight.isFinite() || weight <= 0.0) return null
        val source = fineness(from) ?: return null
        val target = fineness(to) ?: return null
        val result = KaratConversionResult(
            targetWeight = weight * source / target,
            gold750Weight = weight * source / 750.0,
            pureGoldWeight = weight * source / 1000.0
        )
        return result.takeIf { it.targetWeight.isFinite() && it.gold750Weight.isFinite() }
    }

    fun assay(weight: Double, agreed: Int, actual: Int): AssaySettlementResult? {
        if (!weight.isFinite() || weight <= 0.0) return null
        val condition = fineness(agreed) ?: return null
        val measured = fineness(actual) ?: return null
        return AssaySettlementResult(
            sourceWeight = weight,
            adjustedWeight = weight * measured / condition,
            finenessDifference = measured - condition,
            gold750Difference = weight * (measured - condition) / 750.0,
            gold750Weight = weight * measured / 750.0
        ).takeIf { it.adjustedWeight.isFinite() && it.gold750Difference.isFinite() && it.gold750Weight.isFinite() }
    }
}

data class KaratConversionResult(val targetWeight: Double, val gold750Weight: Double, val pureGoldWeight: Double)

/** Positive difference is credit to the gold's owner; negative is their debit. */
data class AssaySettlementResult(
    val sourceWeight: Double,
    val adjustedWeight: Double,
    val finenessDifference: Double,
    val gold750Difference: Double,
    val gold750Weight: Double
) {
    /** Whole tomans, HALF_UP. An unavailable rate/overflow has no cash valuation. */
    fun valueAt(rate750: Long): Long? {
        if (rate750 <= 0L) return null
        return runCatching {
            SettlementArithmetic.assayDifferenceToTomans(sourceWeight, finenessDifference.toInt(), rate750)
        }.getOrNull()
    }
}

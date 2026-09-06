package com.goldex.companion.domain.calculator

import com.goldex.companion.model.CoinBubbleResult
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.DetailedJewelryResult
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.model.WageType
import kotlin.math.round

object GoldCalculationUseCases {
    const val MESGHAL_TO_GRAM_18K_RATIO: Double = 4.33185

    fun toSpotPrice18k(spotInput: Long, basis: PriceBasisTab): Long {
        if (spotInput <= 0L) return 0L
        return when (basis) {
            PriceBasisTab.K18 -> spotInput
            PriceBasisTab.K24 -> round(spotInput * (18.0 / 24.0)).toLong()
            PriceBasisTab.MESGHAL -> round(spotInput / MESGHAL_TO_GRAM_18K_RATIO).toLong()
        }
    }

    fun fromSpotPrice18k(spot18k: Long, targetBasis: PriceBasisTab): Long {
        if (spot18k <= 0L) return 0L
        return when (targetBasis) {
            PriceBasisTab.K18 -> spot18k
            PriceBasisTab.K24 -> round(spot18k * (24.0 / 18.0)).toLong()
            PriceBasisTab.MESGHAL -> round(spot18k * MESGHAL_TO_GRAM_18K_RATIO).toLong()
        }
    }
    fun calculateJewelry(
        grossWeight: Double,
        stoneWeight: Double,
        karat: Karat,
        spotPrice18k: Long,
        wageType: WageType,
        wageInput: Double,
        profitPercent: Double,
        taxPercent: Double
    ): DetailedJewelryResult? {
        val netWeight = (grossWeight - stoneWeight).coerceAtLeast(0.0)
        if (netWeight <= 0.0 || spotPrice18k <= 0L) return null

        val pureGramSpot = spotPrice18k.toDouble() / Karat.K18.purityRatio
        val rawGoldValue = netWeight * karat.purityRatio * pureGramSpot
        val wageAmount = when (wageType) {
            WageType.PERCENTAGE -> rawGoldValue * (wageInput / 100.0)
            WageType.TOMAN_PER_GRAM -> netWeight * wageInput
        }
        val profitAmount = (rawGoldValue + wageAmount) * (profitPercent / 100.0)
        val taxAmount = (wageAmount + profitAmount) * (taxPercent / 100.0)
        val totalPayable = rawGoldValue + wageAmount + profitAmount + taxAmount

        return DetailedJewelryResult(
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            netWeight = netWeight,
            rawGoldValue = rawGoldValue,
            wageAmount = wageAmount,
            profitAmount = profitAmount,
            taxAmount = taxAmount,
            totalPayable = totalPayable,
            effectiveGramPrice = totalPayable / netWeight
        )
    }

    fun calculateMelt(mesghalPrice: Double, weight: Double): MeltCalculation {
        val gram18kPrice = (mesghalPrice / MESGHAL_TO_GRAM_18K_RATIO).toLong()
        return MeltCalculation(
            gram18kPrice = gram18kPrice,
            totalValue = gram18kPrice * weight
        )
    }

    fun calculateCoinBubble(
        coin: CoinType,
        marketPrice: Double,
        usd: Long,
        ounce: Double
    ): CoinBubbleResult {
        val gram24Price = (ounce * usd.toDouble()) / 31.1035
        val intrinsicValue = (coin.pureWeightGrams * gram24Price) + coin.mintFee
        val bubbleAmount = marketPrice - intrinsicValue
        val bubblePercent = if (intrinsicValue > 0.0) {
            (bubbleAmount / intrinsicValue) * 100.0
        } else {
            0.0
        }

        return CoinBubbleResult(
            coinType = coin,
            marketPrice = marketPrice,
            intrinsicValue = intrinsicValue,
            bubbleAmount = bubbleAmount,
            bubblePercent = bubblePercent
        )
    }

    fun calculateKaratConversion(weight: Double, from: Karat, to: Karat): Double {
        return if (to.purityRatio > 0.0) {
            weight * (from.purityRatio / to.purityRatio)
        } else {
            0.0
        }
    }
}

data class MeltCalculation(
    val gram18kPrice: Long,
    val totalValue: Double
)

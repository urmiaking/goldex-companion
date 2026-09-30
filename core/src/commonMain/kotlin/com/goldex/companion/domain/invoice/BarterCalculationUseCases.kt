package com.goldex.companion.domain.invoice

import com.goldex.companion.model.BankCoinItem
import com.goldex.companion.model.BarterBalance
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.MeltGoldItem
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.WageType
import kotlin.math.roundToLong

object BarterCalculationUseCases {

    fun calculateCraftedItem(
        title: String,
        karat: Karat = Karat.K18,
        customKaratValue: Int = 750,
        grossWeight: Double,
        stoneWeight: Double,
        spotPrice18k: Long,
        wageType: WageType,
        wageInput: Double,
        profitPercent: Double,
        taxPercent: Double
    ): CraftedGoldItem {
        val netWeight = (grossWeight - stoneWeight).coerceAtLeast(0.0)
        val actualKarat = if (customKaratValue > 0) customKaratValue else (karat.karatNumber * 1000 / 24)
        val karatRatio = actualKarat.toDouble() / 750.0
        val rawGoldValue = netWeight * spotPrice18k * karatRatio
        val wageAmount = when (wageType) {
            WageType.PERCENTAGE -> rawGoldValue * (wageInput / 100.0)
            WageType.TOMAN_PER_GRAM -> netWeight * wageInput
        }
        val profitAmount = (rawGoldValue + wageAmount) * (profitPercent / 100.0)
        val taxAmount = (wageAmount + profitAmount) * (taxPercent / 100.0)
        val totalPayable = rawGoldValue + wageAmount + profitAmount + taxAmount
        val eq18k = netWeight * karatRatio

        return CraftedGoldItem(
            title = title.ifBlank { "دستبند و زیورآلات ساخته ${actualKarat} عیار" },
            karat = karat,
            customKaratValue = actualKarat,
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            netWeight = netWeight,
            spotPrice = spotPrice18k,
            wageType = wageType,
            wageInput = wageInput,
            wageAmount = wageAmount,
            profitPercent = profitPercent,
            profitAmount = profitAmount,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            rawGoldValue = rawGoldValue,
            totalPayable = totalPayable,
            equivalent18kWeight = eq18k
        )
    }

    fun calculateScrapItem(
        title: String,
        baseKarat: Int,
        karatDeficit: Int,
        grossWeight: Double,
        stoneWeight: Double,
        spotPrice18k: Long,
        deductionPerGram: Long = 15000L,
        exchangeCommissionPercent: Double = 0.0
    ): ScrapGoldItem {
        val netWeight = (grossWeight - stoneWeight).coerceAtLeast(0.0)
        val payableKarat = (baseKarat - karatDeficit).coerceIn(0, 1000)
        val eq18k = netWeight * (payableKarat.toDouble() / 750.0)
        val baseGramPrice = spotPrice18k * (payableKarat.toDouble() / 750.0)
        val afterDeduction = (baseGramPrice - deductionPerGram).coerceAtLeast(0.0)
        val effectiveGramPrice = (afterDeduction * (1.0 - (exchangeCommissionPercent / 100.0))).roundToLong()
        val totalPayable = netWeight * effectiveGramPrice

        return ScrapGoldItem(
            title = title.ifBlank { "طلای متفرقه و کهنه مستعمل" },
            baseKarat = baseKarat,
            karatDeficit = karatDeficit,
            payableKarat = payableKarat,
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            netWeight = netWeight,
            spotPrice = spotPrice18k,
            deductionPerGram = deductionPerGram,
            exchangeCommissionPercent = exchangeCommissionPercent,
            effectiveGramPrice = effectiveGramPrice,
            totalPayable = totalPayable,
            equivalent18kWeight = eq18k
        )
    }

    fun calculateMeltItem(
        title: String,
        weight: Double,
        labKarat: Int,
        angNumber: String,
        labName: String,
        spotPrice18k: Long
    ): MeltGoldItem {
        val eq18k = weight * (labKarat.toDouble() / 750.0)
        val totalPayable = eq18k * spotPrice18k
        val generatedTitle = if (angNumber.isNotBlank()) {
            "طلای آبشده سنتی (انگ $angNumber)"
        } else {
            "طلای آبشده سنتی"
        }
        return MeltGoldItem(
            title = title.ifBlank { generatedTitle },
            weight = weight,
            labKarat = labKarat,
            angNumber = angNumber,
            labName = labName,
            spotPrice = spotPrice18k,
            totalPayable = totalPayable,
            equivalent18kWeight = eq18k
        )
    }

    fun calculateCoinItem(
        coinType: CoinType,
        count: Int,
        hasHologram: Boolean,
        unitPrice: Long
    ): BankCoinItem {
        val totalPayable = count.toDouble() * unitPrice
        // Coin Karat Standard: 900 purity to 750 (18k) conversion
        val eq18k = count * coinType.totalWeightGrams * (coinType.purity / 0.750)
        return BankCoinItem(
            title = coinType.titleFa,
            coinType = coinType,
            count = count,
            hasHologram = hasHologram,
            unitPrice = unitPrice,
            totalPayable = totalPayable,
            equivalent18kWeight = eq18k
        )
    }

    fun calculateBalance(
        salesItems: List<BarterItem>,
        receivedItems: List<BarterItem>
    ): BarterBalance {
        val totalSalesAmount = salesItems.sumOf { it.totalPayable }
        val totalSales18kWeight = salesItems.sumOf { it.equivalent18kWeight }
        val totalSalesCoinCount = salesItems.filterIsInstance<BankCoinItem>().sumOf { it.count }

        val totalReceivedAmount = receivedItems.sumOf { it.totalPayable }
        val totalReceived18kWeight = receivedItems.sumOf { it.equivalent18kWeight }

        val netPayableAmount = totalSalesAmount - totalReceivedAmount
        val net18kWeightDelta = totalSales18kWeight - totalReceived18kWeight

        return BarterBalance(
            totalSalesAmount = totalSalesAmount,
            totalSales18kWeight = totalSales18kWeight,
            totalSalesCoinCount = totalSalesCoinCount,
            totalReceivedAmount = totalReceivedAmount,
            totalReceived18kWeight = totalReceived18kWeight,
            netPayableAmount = netPayableAmount,
            net18kWeightDelta = net18kWeightDelta,
            isCustomerDebtor = netPayableAmount > 0,
            isSettled = kotlin.math.abs(netPayableAmount) < 1.0
        )
    }
}

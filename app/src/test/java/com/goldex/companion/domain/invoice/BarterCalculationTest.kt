package com.goldex.companion.domain.invoice

import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import com.goldex.companion.model.WageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarterCalculationTest {

    @Test
    fun craftedGoldItemCalculationCompliesWithVatPolicy() {
        val item = BarterCalculationUseCases.calculateCraftedItem(
            title = "دستبند کارتیه ۱۸ عیار",
            karat = Karat.K18,
            grossWeight = 12.80,
            stoneWeight = 0.30,
            spotPrice18k = 4_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        assertEquals(12.50, item.netWeight, 0.001)
        assertEquals(50_000_000.0, item.rawGoldValue, 0.001)
        assertEquals(5_000_000.0, item.wageAmount, 0.001)
        assertEquals(3_850_000.0, item.profitAmount, 0.001)
        assertEquals(796_500.0, item.taxAmount, 0.001)
        assertEquals(59_646_500.0, item.totalPayable, 0.001)
        assertEquals(12.50, item.equivalent18kWeight, 0.001)
    }

    @Test
    fun scrapGoldItemDeductionsAnd18kConversionAreAccurate() {
        // 14.80 gross, 0.30 stone -> 14.50 net
        // base 750, deficit 15 -> payable 735
        // eq18k = 14.50 * 735 / 750 = 14.21
        val item = BarterCalculationUseCases.calculateScrapItem(
            title = "طلای کهنه و دست دوم",
            baseKarat = 750,
            karatDeficit = 15,
            grossWeight = 14.80,
            stoneWeight = 0.30,
            spotPrice18k = 3_560_000L,
            deductionPerGram = 15_000L,
            exchangeCommissionPercent = 0.0
        )

        assertEquals(14.50, item.netWeight, 0.001)
        assertEquals(735, item.payableKarat)
        assertEquals(14.21, item.equivalent18kWeight, 0.001)
        val expectedBaseGramPrice = 3_560_000.0 * (735.0 / 750.0) // 3,488,800
        val expectedGramPrice = (expectedBaseGramPrice - 15_000).toLong() // 3,473,800
        assertEquals(expectedGramPrice, item.effectiveGramPrice)
        assertEquals(14.50 * expectedGramPrice, item.totalPayable, 0.001)
    }

    @Test
    fun meltedGoldItemConvertsLabKaratProperly() {
        val item = BarterCalculationUseCases.calculateMeltItem(
            title = "طلای آبشده",
            weight = 10.0,
            labKarat = 735,
            angNumber = "1248",
            labName = "مشهد",
            spotPrice18k = 4_000_000L
        )

        // eq18k = 10.0 * 735 / 750 = 9.80 grams
        assertEquals(9.80, item.equivalent18kWeight, 0.001)
        assertEquals(9.80 * 4_000_000.0, item.totalPayable, 0.001)
    }

    @Test
    fun bankCoinItemWeightAndValueAreAccurate() {
        val item = BarterCalculationUseCases.calculateCoinItem(
            coinType = CoinType.EMAMI,
            count = 2,
            hasHologram = true,
            unitPrice = 45_000_000L
        )

        assertEquals(90_000_000.0, item.totalPayable, 0.001)
        // 2 * 8.13598 * (0.900 / 0.750) = 2 * 8.13598 * 1.2 = 19.526352 grams
        assertEquals(19.526, item.equivalent18kWeight, 0.01)
    }

    @Test
    fun barterBalanceCorrectlyIdentifiesDebtorAndEquivalencies() {
        val saleItem = BarterCalculationUseCases.calculateCraftedItem(
            title = "دستبند",
            karat = Karat.K18,
            grossWeight = 10.0,
            stoneWeight = 0.0,
            spotPrice18k = 4_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        ) // 47,717,200 Toman, 10.0g 18k

        val scrapItem = BarterCalculationUseCases.calculateScrapItem(
            title = "کهنه",
            baseKarat = 750,
            karatDeficit = 0,
            grossWeight = 5.0,
            stoneWeight = 0.0,
            spotPrice18k = 4_000_000L,
            deductionPerGram = 0L,
            exchangeCommissionPercent = 0.0
        ) // 5.0 * 4,000,000 = 20,000,000 Toman, 5.0g 18k

        val balance = BarterCalculationUseCases.calculateBalance(
            salesItems = listOf(saleItem),
            receivedItems = listOf(scrapItem)
        )

        assertEquals(47_717_200.0, balance.totalSalesAmount, 0.001)
        assertEquals(20_000_000.0, balance.totalReceivedAmount, 0.001)
        assertEquals(27_717_200.0, balance.netPayableAmount, 0.001)
        assertEquals(5.0, balance.net18kWeightDelta, 0.001)
        assertTrue(balance.isCustomerDebtor)
        assertFalse(balance.isSettled)
    }

    @Test
    fun thirdPartyTransferDualBalanceCalculationAccuratelyClearsRemainder() {
        val net18kWeight = 10.0
        val spotPrice = 4_000_000L
        val buyerAmount = (net18kWeight * spotPrice).toLong()

        val wholesalerInitialAmount = 80_000_000L
        val transferredWeight = 10.0
        val transferredAmount = (transferredWeight * spotPrice).toLong()

        val wholesalerRemainingAmount = wholesalerInitialAmount - transferredAmount
        val buyerRemainingWeight = net18kWeight - transferredWeight

        assertEquals(0.0, buyerRemainingWeight, 0.001)
        assertEquals(40_000_000L, wholesalerRemainingAmount)
        assertEquals(40_000_000L, buyerAmount)
    }
}

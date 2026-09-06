package com.goldex.companion.ui.calculator

import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.DetailedJewelryResult
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.model.WageType

data class JewelryUiState(
    val itemTitleInput: String = "قطعه طلا ۱",
    val priceBasisTab: PriceBasisTab = PriceBasisTab.K18,
    val isManualSpotDialogVisible: Boolean = false,
    val isStoneWeightDialogVisible: Boolean = false,
    val grossWeightInput: String = "10",
    val stoneWeightInput: String = "0",
    val selectedKarat: Karat = Karat.K18,
    val spotPriceInput: String = "23360000",
    val wageType: WageType = WageType.PERCENTAGE,
    val wageInput: String = "12",
    val profitPercentInput: String = "7",
    val taxPercentInput: String = "9",
    val jewelryResult: DetailedJewelryResult? = null,
    val priceInWords: String = "",
    val rates: MarketRates = MarketRates()
)

interface JewelryActions {
    fun setManualSpotDialogVisible(visible: Boolean)
    fun setPriceBasisTab(tab: PriceBasisTab)
    fun onGrossWeightChanged(newWeight: String)
    fun onWageTypeChanged(type: WageType)
    fun decrementWage()
    fun incrementWage()
    fun onWageChanged(newWage: String)
    fun applyPresetProfit(preset: Double)
    fun setStoneWeightDialogVisible(visible: Boolean)
    fun addItemToInvoice()
    fun selectTab(tab: AppTab)
    fun resetJewelry()
    fun applyPresetSpotPrice(price: Long)
    fun onSpotPriceChanged(newPrice: String)
    fun onStoneWeightChanged(newStone: String)
    fun onProfitPercentChanged(newProfit: String)
    fun onTaxPercentChanged(newTax: String)
    fun onKaratSelected(karat: Karat)
    fun applyPresetTax(preset: Double)
}

package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class PortfolioCategory(val labelFa: String) {
    GOLD("قطعه یا شمش طلا"),
    COIN("سکه بانکی")
}

data class PortfolioItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: PortfolioCategory,
    val weightGrams: Double = 0.0,
    val karat: Karat = Karat.K18,
    val quantity: Int = 1,
    val coinType: CoinType? = null,
    val purchasePriceTotal: Long = 0L,
    val purchaseDate: String = ""
) {
    fun calculateCurrentValue(rates: MarketRates): Long {
        return when (category) {
            PortfolioCategory.GOLD -> {
                val ratio18k = karat.purityRatio / Karat.K18.purityRatio
                (weightGrams * ratio18k * rates.gold18).toLong()
            }
            PortfolioCategory.COIN -> {
                val coinRate = when (coinType) {
                    CoinType.EMAMI -> rates.coinEmami
                    CoinType.BAHAR -> rates.coinBahar
                    CoinType.HALF -> rates.coinHalf
                    CoinType.QUARTER -> rates.coinQuarter
                    CoinType.GERAMI -> rates.coinGerami
                    null -> rates.coinEmami
                }
                coinRate * quantity
            }
        }
    }

    fun calculateProfit(rates: MarketRates): Long {
        return calculateCurrentValue(rates) - purchasePriceTotal
    }

    fun calculateProfitPercent(rates: MarketRates): Double {
        if (purchasePriceTotal <= 0L) return 0.0
        val diff = calculateCurrentValue(rates) - purchasePriceTotal
        return (diff.toDouble() / purchasePriceTotal.toDouble()) * 100.0
    }
}

class PortfolioRepository(context: Context) : PortfolioStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("goldex_portfolio_prefs", Context.MODE_PRIVATE)

    override fun getItems(): List<PortfolioItem> {
        val jsonString = prefs.getString("items_json", null)
        if (jsonString.isNullOrBlank()) {
            return emptyList()
        }

        return PersistenceJsonCodecs.decodePortfolioItems(jsonString)
    }

    override fun addItem(item: PortfolioItem) {
        val current = getItems().toMutableList()
        current.add(0, item)
        saveItems(current)
    }

    override fun deleteItem(id: String) {
        val current = getItems().filter { it.id != id }
        saveItems(current)
    }

    private fun saveItems(items: List<PortfolioItem>) {
        prefs.edit().putString("items_json", PersistenceJsonCodecs.encodePortfolioItems(items)).apply()
    }
}

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

class PortfolioRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("goldex_portfolio_prefs", Context.MODE_PRIVATE)

    fun getItems(): List<PortfolioItem> {
        val jsonString = prefs.getString("items_json", null)
        if (jsonString.isNullOrBlank()) {
            val defaults = listOf(
                PortfolioItem(
                    id = "default_gold_1",
                    title = "دستبند زنجیری کارتیه",
                    category = PortfolioCategory.GOLD,
                    weightGrams = 8.5,
                    karat = Karat.K18,
                    purchasePriceTotal = 175_000_000L,
                    purchaseDate = "۱۴۰۳/۰۵/۱۰"
                ),
                PortfolioItem(
                    id = "default_coin_1",
                    title = "سکه تمام طرح جدید (امامی)",
                    category = PortfolioCategory.COIN,
                    quantity = 2,
                    coinType = CoinType.EMAMI,
                    purchasePriceTotal = 430_000_000L,
                    purchaseDate = "۱۴۰۳/۰۳/۱۵"
                )
            )
            saveItems(defaults)
            return defaults
        }

        val list = mutableListOf<PortfolioItem>()
        try {
            val arr = JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PortfolioItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        category = PortfolioCategory.valueOf(obj.getString("category")),
                        weightGrams = obj.optDouble("weightGrams", 0.0),
                        karat = Karat.valueOf(obj.optString("karat", "K18")),
                        quantity = obj.optInt("quantity", 1),
                        coinType = if (obj.has("coinType")) CoinType.valueOf(obj.getString("coinType")) else null,
                        purchasePriceTotal = obj.optLong("purchasePriceTotal", 0L),
                        purchaseDate = obj.optString("purchaseDate", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addItem(item: PortfolioItem) {
        val current = getItems().toMutableList()
        current.add(0, item)
        saveItems(current)
    }

    fun deleteItem(id: String) {
        val current = getItems().filter { it.id != id }
        saveItems(current)
    }

    private fun saveItems(items: List<PortfolioItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("category", item.category.name)
                put("weightGrams", item.weightGrams)
                put("karat", item.karat.name)
                put("quantity", item.quantity)
                item.coinType?.let { put("coinType", it.name) }
                put("purchasePriceTotal", item.purchasePriceTotal)
                put("purchaseDate", item.purchaseDate)
            }
            arr.put(obj)
        }
        prefs.edit().putString("items_json", arr.toString()).apply()
    }
}

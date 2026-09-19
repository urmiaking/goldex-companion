package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import com.goldex.companion.model.WageType

class InventoryRepository(context: Context) : InventoryStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("goldex_inventory_prefs", Context.MODE_PRIVATE)

    override fun getItems(): List<InventoryItem> {
        val json = prefs.getString("inventory_items_json", null)
        if (json.isNullOrBlank()) {
            val defaults = getInitialDefaultItems()
            saveItems(defaults)
            return defaults
        }
        return PersistenceJsonCodecs.decodeInventoryItems(json)
    }

    override fun addItem(item: InventoryItem) {
        val current = getItems().toMutableList()
        current.add(0, item)
        saveItems(current)
    }

    override fun updateItem(item: InventoryItem) {
        val current = getItems().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        saveItems(current)
    }

    override fun deleteItem(id: String) {
        val current = getItems().filter { it.id != id }
        saveItems(current)
    }

    override fun adjustStock(adjustment: StockAdjustment) {
        val items = getItems().toMutableList()
        val index = items.indexOfFirst { it.id == adjustment.itemId }
        if (index != -1) {
            val existing = items[index]
            val newQty = when (adjustment.type) {
                StockAdjustmentType.CHARGE -> existing.quantity + adjustment.quantityChange
                StockAdjustmentType.DEDUCT -> (existing.quantity - adjustment.quantityChange).coerceAtLeast(0)
            }
            val newGross = if (adjustment.weightGrams > 0.0) {
                when (adjustment.type) {
                    StockAdjustmentType.CHARGE -> existing.grossWeightGrams + adjustment.weightGrams
                    StockAdjustmentType.DEDUCT -> (existing.grossWeightGrams - adjustment.weightGrams).coerceAtLeast(0.0)
                }
            } else {
                existing.grossWeightGrams
            }
            items[index] = existing.copy(
                quantity = newQty,
                grossWeightGrams = newGross
            )
            saveItems(items)
        }

        val adjustments = getAdjustments().toMutableList()
        adjustments.add(0, adjustment)
        saveAdjustments(adjustments)
    }

    override fun getAdjustments(): List<StockAdjustment> {
        val json = prefs.getString("stock_adjustments_json", null)
        if (json.isNullOrBlank()) return emptyList()
        return PersistenceJsonCodecs.decodeStockAdjustments(json)
    }

    private fun saveItems(items: List<InventoryItem>) {
        val json = PersistenceJsonCodecs.encodeInventoryItems(items)
        prefs.edit().putString("inventory_items_json", json).apply()
    }

    private fun saveAdjustments(adjustments: List<StockAdjustment>) {
        val json = PersistenceJsonCodecs.encodeStockAdjustments(adjustments)
        prefs.edit().putString("stock_adjustments_json", json).apply()
    }

    companion object {
        fun getInitialDefaultItems(): List<InventoryItem> = listOf(
            InventoryItem(
                id = "inv_vancleef_10492",
                code = "GLD-10492",
                title = "نیم‌ست طرح ون‌کلیف اونیکس و طلای زرد",
                category = InventoryCategory.SETS,
                location = "ویترین ۱ • سینی ۴",
                grossWeightGrams = 18.450,
                stoneWeightGrams = 2.100,
                karat = Karat.K18,
                workshop = "کارگاه زرین تهران",
                wageType = WageType.PERCENTAGE,
                wageValue = 16.0,
                wagePercent = 16.0,
                profitPercent = 7.0,
                taxPercent = 9.0,
                rfidTag = "RF-9942-VC",
                quantity = 1
            ),
            InventoryItem(
                id = "inv_emirati_08341",
                code = "GLD-08341",
                title = "النگو اماراتی سایز ۳ (۴ لنگه)",
                category = InventoryCategory.BANGLES,
                location = "گاوصندوق انبار داخلی",
                grossWeightGrams = 48.800,
                stoneWeightGrams = 0.0,
                karat = Karat.K18,
                workshop = "طلاسازی اصفهان",
                wageType = WageType.PERCENTAGE,
                wageValue = 9.5,
                wagePercent = 9.5,
                profitPercent = 7.0,
                taxPercent = 9.0,
                rfidTag = "RF-8341-EM",
                quantity = 4
            ),
            InventoryItem(
                id = "inv_coin_021",
                code = "COIN-021",
                title = "سکه تمام بهار آزادی (امامی)",
                category = InventoryCategory.COINS,
                location = "گاوصندوق انبار داخلی",
                grossWeightGrams = 8.133,
                stoneWeightGrams = 0.0,
                karat = Karat.K18,
                workshop = "بانک مرکزی",
                wageType = WageType.PERCENTAGE,
                wageValue = 0.0,
                wagePercent = 0.0,
                profitPercent = 0.0,
                taxPercent = 0.0,
                rfidTag = "RF-0021-CN",
                quantity = 18
            ),
            InventoryItem(
                id = "inv_ring_104",
                code = "RNG-104",
                title = "انگشتر تک‌نگین البرنادو ۱۸ عیار لوکس",
                category = InventoryCategory.RINGS,
                location = "سینی شماره ۱ ویترین اصلی",
                grossWeightGrams = 5.420,
                stoneWeightGrams = 0.150,
                karat = Karat.K18,
                workshop = "کارگاه زرین تهران",
                wageType = WageType.PERCENTAGE,
                wageValue = 12.0,
                wagePercent = 12.0,
                profitPercent = 7.0,
                taxPercent = 9.0,
                rfidTag = "RF-0104-RN",
                quantity = 5
            )
        )
    }
}

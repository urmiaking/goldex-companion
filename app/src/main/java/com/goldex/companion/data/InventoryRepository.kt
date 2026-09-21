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
            return emptyList()
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
}

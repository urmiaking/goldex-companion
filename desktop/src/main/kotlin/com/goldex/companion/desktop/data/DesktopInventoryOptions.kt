package com.goldex.companion.desktop.data

import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.presentation.inventory.InventoryForm
import org.json.JSONArray
import org.json.JSONObject

data class InventoryOption(val id: String, val name: String)
data class InventoryOptions(
    val categories: List<InventoryOption> = emptyList(),
    val locations: List<InventoryOption> = emptyList(),
    val categoryIds: Map<String, String> = emptyMap()
)

/** Desktop extensions retain the shared category as the item's financial policy. */
internal object DesktopInventoryOptions {
    const val CATEGORIES = "inventoryCategories"
    const val LOCATIONS = "inventoryLocations"
    val defaultLocations = listOf("ویترین اصلی", "گاوصندوق مرکزی", "خزانه طلا")

    fun read(document: JSONObject): InventoryOptions {
        if (document.has("inventoryCodeSequence")) {
            require(document.get("inventoryCodeSequence").toString().toIntOrNull()?.let { it in 999_999..9_999_999 } == true)
        }
        fun choices(key: String): List<InventoryOption> {
            if (!document.has(key)) return emptyList()
            val array = document.getJSONArray(key)
            require(array.length() <= 1000)
            val ids = mutableSetOf<String>()
            val names = mutableSetOf<String>()
            return (0 until array.length()).map { index ->
                val record = array.getJSONObject(index)
                val id = record.getString("id")
                val name = record.getString("name")
                require(id.isNotBlank() && ids.add(id))
                require(name.isNotBlank() && name == name.trim() && name.length <= 50 && names.add(name))
                InventoryOption(id, name)
            }
        }
        val categories = choices(CATEGORIES)
        val locations = choices(LOCATIONS)
        val assignments = buildMap {
            val inventory = document.optJSONArray("inventory") ?: JSONArray()
            for (index in 0 until inventory.length()) {
                val record = inventory.getJSONObject(index)
                if (record.has("customCategoryId") && !record.isNull("customCategoryId")) {
                    val id = record.getString("customCategoryId")
                    require(categories.any { it.id == id })
                    put(record.getString("id"), id)
                }
            }
        }
        return InventoryOptions(categories, locations, assignments)
    }

    fun nextCode(items: List<InventoryItem>, lastCode: Int = 999_999): String {
        val used = items.map { InventoryForm.normalize(it.code) }.toSet()
        // Bounded by store capacity; the durable sequence prevents recycling deleted codes.
        val code = ((lastCode + 1)..minOf(lastCode + 10_001, 9_999_999)).firstOrNull { it.toString() !in used }
        return requireNotNull(code) { "کد هفت‌رقمی آزاد وجود ندارد" }.toString()
    }
}

package com.goldex.companion.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["code"]),
        Index(value = ["rfidTag"])
    ]
)
data class InventoryItemEntity(
    @PrimaryKey
    val id: String,
    val code: String,
    val title: String,
    val category: String = "RINGS",
    val location: String = "سینی شماره ۱ ویترین اصلی",
    val grossWeightGrams: Double = 0.0,
    val stoneWeightGrams: Double = 0.0,
    val karat: String = "K18",
    val customKaratValue: Int = 750,
    val workshop: String = "کارگاه زرین تهران",
    val wageType: String = "PERCENTAGE",
    val wagePercent: Double = 0.0,
    val wageValue: Double = 0.0,
    val profitPercent: Double = 7.0,
    val taxPercent: Double = 9.0,
    val rfidTag: String = "",
    val quantity: Int = 1,
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

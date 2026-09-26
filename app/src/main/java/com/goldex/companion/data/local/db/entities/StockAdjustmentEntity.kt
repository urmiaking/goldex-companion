package com.goldex.companion.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_adjustments",
    indices = [
        Index(value = ["itemId"])
    ]
)
data class StockAdjustmentEntity(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val type: String = "CHARGE",
    val quantityChange: Int = 1,
    val weightGrams: Double = 0.0,
    val reason: String = "دریافت از کارگاه ساخت",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

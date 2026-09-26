package com.goldex.companion.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_items")
data class PortfolioItemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String = "GOLD",
    val weightGrams: Double = 0.0,
    val karat: String = "K18",
    val quantity: Int = 1,
    val coinType: String? = null,
    val purchasePriceTotal: Long = 0L,
    val purchaseDate: String = ""
)

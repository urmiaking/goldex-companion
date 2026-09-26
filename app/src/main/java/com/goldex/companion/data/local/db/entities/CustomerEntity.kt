package com.goldex.companion.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String = "",
    val nationalId: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val role: String = "بنکدار و همکار بازار",
    val goldDebtGrams: Double = 0.0,
    val cashDebtTomans: Long = 0L,
    val accountCode: String = "",
    val isVerified: Boolean = true,
    val cityOrMarket: String = "بازار بزرگ تهران",
    val lastActivityTime: String = "امروز"
)

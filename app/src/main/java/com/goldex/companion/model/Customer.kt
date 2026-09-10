package com.goldex.companion.model

import java.util.UUID

data class Customer(
    val id: String = UUID.randomUUID().toString(),
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


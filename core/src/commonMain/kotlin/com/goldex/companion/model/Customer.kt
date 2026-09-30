package com.goldex.companion.model

import com.goldex.companion.platform.SystemClock
import com.goldex.companion.platform.RandomIdGenerator

data class Customer(
    val id: String = RandomIdGenerator.newId(),
    val name: String,
    val phone: String = "",
    val nationalId: String = "",
    val note: String = "",
    val createdAt: Long = SystemClock.nowMillis(),
    val role: String = "بنکدار و همکار بازار",
    val goldDebtGrams: Double = 0.0,
    val cashDebtTomans: Long = 0L,
    val accountCode: String = "",
    val isVerified: Boolean = true,
    val cityOrMarket: String = "بازار بزرگ تهران",
    val lastActivityTime: String = "امروز"
)


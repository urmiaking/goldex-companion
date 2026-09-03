package com.goldex.companion.model

import java.util.UUID

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val nationalId: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

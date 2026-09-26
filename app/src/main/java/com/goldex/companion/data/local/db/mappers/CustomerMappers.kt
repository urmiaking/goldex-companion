package com.goldex.companion.data.local.db.mappers

import com.goldex.companion.data.local.db.entities.CustomerEntity
import com.goldex.companion.model.Customer

fun CustomerEntity.toDomain(): Customer {
    return Customer(
        id = id,
        name = name,
        phone = phone,
        nationalId = nationalId,
        note = note,
        createdAt = createdAt,
        role = role,
        goldDebtGrams = goldDebtGrams,
        cashDebtTomans = cashDebtTomans,
        accountCode = accountCode,
        isVerified = isVerified,
        cityOrMarket = cityOrMarket,
        lastActivityTime = lastActivityTime
    )
}

fun Customer.toEntity(): CustomerEntity {
    return CustomerEntity(
        id = id,
        name = name,
        phone = phone,
        nationalId = nationalId,
        note = note,
        createdAt = createdAt,
        role = role,
        goldDebtGrams = goldDebtGrams,
        cashDebtTomans = cashDebtTomans,
        accountCode = accountCode,
        isVerified = isVerified,
        cityOrMarket = cityOrMarket,
        lastActivityTime = lastActivityTime
    )
}

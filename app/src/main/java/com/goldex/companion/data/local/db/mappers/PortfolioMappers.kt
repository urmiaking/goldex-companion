package com.goldex.companion.data.local.db.mappers

import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.data.local.db.entities.PortfolioItemEntity
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat

fun PortfolioItemEntity.toDomain(): PortfolioItem {
    val cat = try {
        PortfolioCategory.valueOf(category)
    } catch (_: Exception) {
        PortfolioCategory.GOLD
    }

    val k = try {
        Karat.valueOf(karat)
    } catch (_: Exception) {
        Karat.K18
    }

    val coin = coinType?.let {
        try {
            CoinType.valueOf(it)
        } catch (_: Exception) {
            null
        }
    }

    return PortfolioItem(
        id = id,
        title = title,
        category = cat,
        weightGrams = weightGrams,
        karat = k,
        quantity = quantity,
        coinType = coin,
        purchasePriceTotal = purchasePriceTotal,
        purchaseDate = purchaseDate
    )
}

fun PortfolioItem.toEntity(): PortfolioItemEntity {
    return PortfolioItemEntity(
        id = id,
        title = title,
        category = category.name,
        weightGrams = weightGrams,
        karat = karat.name,
        quantity = quantity,
        coinType = coinType?.name,
        purchasePriceTotal = purchasePriceTotal,
        purchaseDate = purchaseDate
    )
}

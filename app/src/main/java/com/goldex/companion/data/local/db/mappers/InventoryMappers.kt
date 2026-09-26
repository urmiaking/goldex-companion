package com.goldex.companion.data.local.db.mappers

import com.goldex.companion.data.local.db.entities.InventoryItemEntity
import com.goldex.companion.data.local.db.entities.StockAdjustmentEntity
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import com.goldex.companion.model.WageType

fun InventoryItemEntity.toDomain(): InventoryItem {
    val cat = try {
        InventoryCategory.valueOf(category)
    } catch (_: Exception) {
        InventoryCategory.RINGS
    }

    val k = try {
        Karat.valueOf(karat)
    } catch (_: Exception) {
        Karat.K18
    }

    val wt = try {
        WageType.valueOf(wageType)
    } catch (_: Exception) {
        WageType.PERCENTAGE
    }

    return InventoryItem(
        id = id,
        code = code,
        title = title,
        category = cat,
        location = location,
        grossWeightGrams = grossWeightGrams,
        stoneWeightGrams = stoneWeightGrams,
        karat = k,
        customKaratValue = customKaratValue,
        workshop = workshop,
        wageType = wt,
        wagePercent = wagePercent,
        wageValue = wageValue,
        profitPercent = profitPercent,
        taxPercent = taxPercent,
        rfidTag = rfidTag,
        quantity = quantity,
        imageUrl = imageUrl,
        createdAt = createdAt
    )
}

fun InventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = id,
        code = code,
        title = title,
        category = category.name,
        location = location,
        grossWeightGrams = grossWeightGrams,
        stoneWeightGrams = stoneWeightGrams,
        karat = karat.name,
        customKaratValue = customKaratValue,
        workshop = workshop,
        wageType = wageType.name,
        wagePercent = wagePercent,
        wageValue = wageValue,
        profitPercent = profitPercent,
        taxPercent = taxPercent,
        rfidTag = rfidTag,
        quantity = quantity,
        imageUrl = imageUrl,
        createdAt = createdAt
    )
}

fun StockAdjustmentEntity.toDomain(): StockAdjustment {
    val adjustmentType = try {
        StockAdjustmentType.valueOf(type)
    } catch (_: Exception) {
        StockAdjustmentType.CHARGE
    }

    return StockAdjustment(
        id = id,
        itemId = itemId,
        itemTitle = itemTitle,
        type = adjustmentType,
        quantityChange = quantityChange,
        weightGrams = weightGrams,
        reason = reason,
        note = note,
        timestamp = timestamp
    )
}

fun StockAdjustment.toEntity(): StockAdjustmentEntity {
    return StockAdjustmentEntity(
        id = id,
        itemId = itemId,
        itemTitle = itemTitle,
        type = type.name,
        quantityChange = quantityChange,
        weightGrams = weightGrams,
        reason = reason,
        note = note,
        timestamp = timestamp
    )
}

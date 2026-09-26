package com.goldex.companion.data

import android.content.Context
import com.goldex.companion.data.local.db.GoldexDatabaseProvider
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.StockAdjustment

class InventoryRepository(
    private val delegate: InventoryStore
) : InventoryStore by delegate {

    constructor(context: Context) : this(
        GoldexDatabaseProvider.getInventoryStore(context)
    )
}

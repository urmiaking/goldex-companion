package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PortfolioRepository(
    private val delegate: PortfolioStore
) : PortfolioStore by delegate {

    constructor(context: Context) : this(
        com.goldex.companion.data.local.db.GoldexDatabaseProvider.getPortfolioStore(context)
    )
}

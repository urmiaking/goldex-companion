package com.goldex.companion.data

import android.content.Context
import com.goldex.companion.data.local.db.GoldexDatabaseProvider
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerTransaction

class CustomerRepository(
    private val delegate: CustomerStore
) : CustomerStore by delegate {

    constructor(context: Context) : this(
        GoldexDatabaseProvider.getCustomerStore(context)
    )
}

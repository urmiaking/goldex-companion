package com.goldex.companion.data

import android.content.Context
import com.goldex.companion.data.local.db.GoldexDatabaseProvider
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Invoice

class InvoiceRepository(
    private val delegate: InvoiceStore
) : InvoiceStore by delegate {

    constructor(context: Context) : this(
        GoldexDatabaseProvider.getInvoiceStore(context)
    )
}

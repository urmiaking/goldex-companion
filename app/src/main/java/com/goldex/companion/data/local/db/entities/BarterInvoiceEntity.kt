package com.goldex.companion.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.SettlementPaymentItem

@Entity(
    tableName = "barter_invoices",
    indices = [
        Index(value = ["invoiceNumber"]),
        Index(value = ["createdAt"])
    ]
)
data class BarterInvoiceEntity(
    @PrimaryKey
    val id: String,
    val invoiceNumber: String,
    val createdAt: Long,
    val customer: Customer? = null,
    val customerRole: String = "WHOLESALER",
    val spotPrice18k: Long = 0L,
    val salesItems: List<BarterItem> = emptyList(),
    val receivedItems: List<BarterItem> = emptyList(),
    val settlementMethod: String = "POS",
    val cashPosAmount: Long = 0L,
    val ledgerAmount: Long = 0L,
    val posTrackingCode: String = "",
    val ledgerDueDate: String = "",
    val bullionWeight: Double = 0.0,
    val bullionKarat: Int = 750,
    val bullionAngNumber: String = "",
    val thirdPartyCustomer: Customer? = null,
    val thirdPartyInvoiceId: String = "",
    val thirdPartyInvoiceNumber: String = "",
    val thirdPartyTransferWeight18k: Double = 0.0,
    val thirdPartyTransferAmount: Long = 0L,
    val thirdPartyTrackingCode: String = "",
    val note: String = "",
    val payments: List<SettlementPaymentItem> = emptyList(),
    val syncWithLedger: Boolean = true
)

package com.goldex.companion.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InvoiceItem

@Entity(tableName = "classic_invoices")
data class ClassicInvoiceEntity(
    @PrimaryKey
    val id: String,
    val invoiceNumber: String,
    val createdAt: Long,
    val customer: Customer? = null,
    val items: List<InvoiceItem> = emptyList(),
    val note: String = ""
)

package com.goldex.companion.data.local.db.converters

import androidx.room.TypeConverter
import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InvoiceItem
import com.goldex.companion.model.SettlementPaymentItem

class DatabaseConverters {

    @TypeConverter
    fun fromBarterItemList(items: List<BarterItem>?): String {
        if (items.isNullOrEmpty()) return "[]"
        return PersistenceJsonCodecs.encodeBarterItemsString(items)
    }

    @TypeConverter
    fun toBarterItemList(json: String?): List<BarterItem> {
        if (json.isNullOrBlank()) return emptyList()
        return PersistenceJsonCodecs.decodeBarterItemsString(json)
    }

    @TypeConverter
    fun fromSettlementPaymentList(payments: List<SettlementPaymentItem>?): String {
        if (payments.isNullOrEmpty()) return "[]"
        return PersistenceJsonCodecs.encodePaymentItemsString(payments)
    }

    @TypeConverter
    fun toSettlementPaymentList(json: String?): List<SettlementPaymentItem> {
        if (json.isNullOrBlank()) return emptyList()
        return PersistenceJsonCodecs.decodePaymentItemsString(json)
    }

    @TypeConverter
    fun fromInvoiceItemList(items: List<InvoiceItem>?): String {
        if (items.isNullOrEmpty()) return "[]"
        return PersistenceJsonCodecs.encodeInvoiceItemsString(items)
    }

    @TypeConverter
    fun toInvoiceItemList(json: String?): List<InvoiceItem> {
        if (json.isNullOrBlank()) return emptyList()
        return PersistenceJsonCodecs.decodeInvoiceItemsString(json)
    }

    @TypeConverter
    fun fromCustomer(customer: Customer?): String? {
        if (customer == null) return null
        return PersistenceJsonCodecs.encodeCustomerString(customer)
    }

    @TypeConverter
    fun toCustomer(json: String?): Customer? {
        if (json.isNullOrBlank()) return null
        return PersistenceJsonCodecs.decodeCustomerString(json)
    }
}

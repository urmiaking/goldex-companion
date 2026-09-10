package com.goldex.companion.data

import com.goldex.companion.model.Customer
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.InvoiceItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.WageType
import org.json.JSONArray
import org.json.JSONObject

internal object PersistenceJsonCodecs {
    fun decodeCustomers(json: String?): List<Customer> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            val customers = mutableListOf<Customer>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.stringValue("id").trim()
                val name = obj.stringValue("name").trim()
                if (id.isEmpty() || name.isEmpty()) continue
                val role = obj.optString("role", "بنکدار و همکار بازار").ifBlank { "بنکدار و همکار بازار" }
                val goldDebtGrams = obj.optDouble("goldDebtGrams", 0.0)
                val cashDebtTomans = obj.optLong("cashDebtTomans", 0L)
                val accountCode = obj.optString("accountCode", "")
                val isVerified = if (obj.has("isVerified")) obj.optBoolean("isVerified", true) else true
                val cityOrMarket = obj.optString("cityOrMarket", "بازار بزرگ تهران").ifBlank { "بازار بزرگ تهران" }
                val lastActivityTime = obj.optString("lastActivityTime", "امروز").ifBlank { "امروز" }
                customers += Customer(
                    id = id,
                    name = name,
                    phone = obj.stringValue("phone"),
                    nationalId = obj.stringValue("nationalId"),
                    note = obj.stringValue("note"),
                    createdAt = obj.longValue("createdAt", System.currentTimeMillis()),
                    role = role,
                    goldDebtGrams = goldDebtGrams,
                    cashDebtTomans = cashDebtTomans,
                    accountCode = accountCode,
                    isVerified = isVerified,
                    cityOrMarket = cityOrMarket,
                    lastActivityTime = lastActivityTime
                )
            }
            customers
        }.getOrDefault(emptyList())
    }

    fun encodeCustomers(customers: List<Customer>): String {
        val array = JSONArray()
        customers.forEach { customer ->
            array.put(JSONObject().apply {
                put("id", customer.id)
                put("name", customer.name)
                put("phone", customer.phone)
                put("nationalId", customer.nationalId)
                put("note", customer.note)
                put("createdAt", customer.createdAt)
                put("role", customer.role)
                put("goldDebtGrams", customer.goldDebtGrams)
                put("cashDebtTomans", customer.cashDebtTomans)
                put("accountCode", customer.accountCode)
                put("isVerified", customer.isVerified)
                put("cityOrMarket", customer.cityOrMarket)
                put("lastActivityTime", customer.lastActivityTime)
            })
        }
        return array.toString()
    }

    fun decodePortfolioItems(json: String?): List<PortfolioItem> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            val items = mutableListOf<PortfolioItem>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.stringValue("id").trim()
                val title = obj.stringValue("title").trim()
                if (id.isEmpty() || title.isEmpty()) continue
                val category = enumOrDefault(obj.stringValue("category"), PortfolioCategory.GOLD)
                val karat = enumOrDefault(obj.stringValue("karat"), Karat.K18)
                val coinType = obj.stringValue("coinType")
                    .takeIf { it.isNotBlank() }
                    ?.let { value -> runCatching { com.goldex.companion.model.CoinType.valueOf(value) }.getOrNull() }
                items += PortfolioItem(
                    id = id,
                    title = title,
                    category = category,
                    weightGrams = obj.doubleValue("weightGrams", 0.0),
                    karat = karat,
                    quantity = obj.intValue("quantity", 1),
                    coinType = coinType,
                    purchasePriceTotal = obj.longValue("purchasePriceTotal", 0L),
                    purchaseDate = obj.stringValue("purchaseDate")
                    )
            }
            items
        }.getOrDefault(emptyList())
    }

    fun encodePortfolioItems(items: List<PortfolioItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("category", item.category.name)
                put("weightGrams", item.weightGrams)
                put("karat", item.karat.name)
                put("quantity", item.quantity)
                item.coinType?.let { put("coinType", it.name) }
                put("purchasePriceTotal", item.purchasePriceTotal)
                put("purchaseDate", item.purchaseDate)
            })
        }
        return array.toString()
    }

    fun decodeInvoices(json: String?): List<Invoice> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            val invoices = mutableListOf<Invoice>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val customer = obj.optJSONObject("customer")?.let { customerObj ->
                    Customer(
                        id = customerObj.stringValue("id"),
                        name = customerObj.stringValue("name", "مشتری"),
                        phone = customerObj.stringValue("phone"),
                        nationalId = customerObj.stringValue("nationalId"),
                        note = customerObj.stringValue("note")
                    )
                }
                val items = decodeInvoiceItems(obj.optJSONArray("items"))
                invoices += Invoice(
                    id = obj.stringValue("id"),
                    invoiceNumber = obj.stringValue("invoiceNumber"),
                    createdAt = obj.longValue("createdAt", System.currentTimeMillis()),
                    customer = customer,
                    items = items,
                    note = obj.stringValue("note")
                )
            }
            invoices
        }.getOrDefault(emptyList())
    }

    fun encodeInvoices(invoices: List<Invoice>): String {
        val array = JSONArray()
        invoices.forEach { invoice ->
            array.put(JSONObject().apply {
                put("id", invoice.id)
                put("invoiceNumber", invoice.invoiceNumber)
                put("createdAt", invoice.createdAt)
                put("note", invoice.note)
                invoice.customer?.let { customer ->
                    put("customer", JSONObject().apply {
                        put("id", customer.id)
                        put("name", customer.name)
                        put("phone", customer.phone)
                        put("nationalId", customer.nationalId)
                        put("note", customer.note)
                    })
                }
                put("items", encodeInvoiceItems(invoice.items))
            })
        }
        return array.toString()
    }

    private fun decodeInvoiceItems(array: JSONArray?): List<InvoiceItem> {
        if (array == null) return emptyList()
        val items = mutableListOf<InvoiceItem>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            items += InvoiceItem(
                id = obj.stringValue("id"),
                title = obj.stringValue("title", "قطعه طلا"),
                karat = enumOrDefault(obj.stringValue("karat"), Karat.K18),
                grossWeight = obj.doubleValue("grossWeight", 0.0),
                stoneWeight = obj.doubleValue("stoneWeight", 0.0),
                netWeight = obj.doubleValue("netWeight", 0.0),
                spotPrice = obj.longValue("spotPrice", 0L),
                wageType = enumOrDefault(obj.stringValue("wageType"), WageType.PERCENTAGE),
                wageInput = obj.doubleValue("wageInput", 0.0),
                wageAmount = obj.doubleValue("wageAmount", 0.0),
                profitPercent = obj.doubleValue("profitPercent", 0.0),
                profitAmount = obj.doubleValue("profitAmount", 0.0),
                taxPercent = obj.doubleValue("taxPercent", 0.0),
                taxAmount = obj.doubleValue("taxAmount", 0.0),
                rawGoldValue = obj.doubleValue("rawGoldValue", 0.0),
                totalPayable = obj.doubleValue("totalPayable", 0.0),
                effectiveGramPrice = obj.doubleValue("effectiveGramPrice", 0.0)
                )
        }
        return items
    }

    private fun encodeInvoiceItems(items: List<InvoiceItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("karat", item.karat.name)
                put("grossWeight", item.grossWeight)
                put("stoneWeight", item.stoneWeight)
                put("netWeight", item.netWeight)
                put("spotPrice", item.spotPrice)
                put("wageType", item.wageType.name)
                put("wageInput", item.wageInput)
                put("wageAmount", item.wageAmount)
                put("profitPercent", item.profitPercent)
                put("profitAmount", item.profitAmount)
                put("taxPercent", item.taxPercent)
                put("taxAmount", item.taxAmount)
                put("rawGoldValue", item.rawGoldValue)
                put("totalPayable", item.totalPayable)
                put("effectiveGramPrice", item.effectiveGramPrice)
            })
        }
        return array
    }

    fun decodeLedgerTransactions(json: String?): List<LedgerTransaction> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            val transactions = mutableListOf<LedgerTransaction>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.stringValue("id").trim()
                val customerId = obj.stringValue("customerId").trim()
                if (id.isEmpty() || customerId.isEmpty()) continue
                val type = enumOrDefault(obj.stringValue("type"), LedgerEntryType.GOLD_WEIGHT)
                val direction = enumOrDefault(obj.stringValue("direction"), LedgerDirection.RECEIVE)
                transactions += LedgerTransaction(
                    id = id,
                    customerId = customerId,
                    documentNumber = obj.stringValue("documentNumber"),
                    title = obj.stringValue("title"),
                    dateTime = obj.stringValue("dateTime"),
                    type = type,
                    direction = direction,
                    goldCategory = obj.optString("goldCategory", "آبشده"),
                    scaleWeightGrams = obj.optDouble("scaleWeightGrams", 0.0),
                    karat = obj.optInt("karat", 750),
                    equivalent750WeightGrams = obj.optDouble("equivalent750WeightGrams", 0.0),
                    angNumber = obj.optString("angNumber", ""),
                    labName = obj.optString("labName", ""),
                    amountTomans = obj.optLong("amountTomans", 0L),
                    paymentMethod = obj.optString("paymentMethod", "حواله بانکی / پایا"),
                    destinationBank = obj.optString("destinationBank", ""),
                    trackingCode = obj.optString("trackingCode", ""),
                    note = obj.optString("note", ""),
                    tagBadge = obj.optString("tagBadge", ""),
                    resultingGoldBalance = obj.optDouble("resultingGoldBalance", 0.0),
                    resultingCashBalance = obj.optLong("resultingCashBalance", 0L),
                    timestamp = obj.longValue("timestamp", System.currentTimeMillis())
                )
            }
            transactions
        }.getOrDefault(emptyList())
    }

    fun encodeLedgerTransactions(transactions: List<LedgerTransaction>): String {
        val array = JSONArray()
        transactions.forEach { tx ->
            array.put(JSONObject().apply {
                put("id", tx.id)
                put("customerId", tx.customerId)
                put("documentNumber", tx.documentNumber)
                put("title", tx.title)
                put("dateTime", tx.dateTime)
                put("type", tx.type.name)
                put("direction", tx.direction.name)
                put("goldCategory", tx.goldCategory)
                put("scaleWeightGrams", tx.scaleWeightGrams)
                put("karat", tx.karat)
                put("equivalent750WeightGrams", tx.equivalent750WeightGrams)
                put("angNumber", tx.angNumber)
                put("labName", tx.labName)
                put("amountTomans", tx.amountTomans)
                put("paymentMethod", tx.paymentMethod)
                put("destinationBank", tx.destinationBank)
                put("trackingCode", tx.trackingCode)
                put("note", tx.note)
                put("tagBadge", tx.tagBadge)
                put("resultingGoldBalance", tx.resultingGoldBalance)
                put("resultingCashBalance", tx.resultingCashBalance)
                put("timestamp", tx.timestamp)
            })
        }
        return array.toString()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private fun JSONObject.stringValue(key: String, default: String = ""): String =
        optString(key).takeUnless { it == "" || it == "null" } ?: default

    private fun JSONObject.longValue(key: String, default: Long): Long =
        opt(key)?.toString()?.toLongOrNull() ?: default

    private fun JSONObject.intValue(key: String, default: Int): Int =
        opt(key)?.toString()?.toIntOrNull() ?: default

    private fun JSONObject.doubleValue(key: String, default: Double): Double =
        opt(key)?.toString()?.toDoubleOrNull() ?: default

}

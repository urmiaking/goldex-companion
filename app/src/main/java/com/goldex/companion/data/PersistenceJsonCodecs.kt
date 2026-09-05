package com.goldex.companion.data

import com.goldex.companion.model.Customer
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.InvoiceItem
import com.goldex.companion.model.Karat
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
                val id = obj.optString("id", "").trim()
                val name = obj.optString("name", "").trim()
                if (id.isEmpty() || name.isEmpty()) continue
                customers += Customer(
                    id = id,
                    name = name,
                    phone = obj.optString("phone", ""),
                    nationalId = obj.optString("nationalId", ""),
                    note = obj.optString("note", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
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
                val id = obj.optString("id", "").trim()
                val title = obj.optString("title", "").trim()
                if (id.isEmpty() || title.isEmpty()) continue
                val category = enumOrDefault(obj.optString("category"), PortfolioCategory.GOLD)
                val karat = enumOrDefault(obj.optString("karat"), Karat.K18)
                val coinType = obj.optString("coinType", "")
                    .takeIf { it.isNotBlank() }
                    ?.let { value -> runCatching { com.goldex.companion.model.CoinType.valueOf(value) }.getOrNull() }
                items += PortfolioItem(
                    id = id,
                    title = title,
                    category = category,
                    weightGrams = obj.optDouble("weightGrams", 0.0),
                    karat = karat,
                    quantity = obj.optInt("quantity", 1),
                    coinType = coinType,
                    purchasePriceTotal = obj.optLong("purchasePriceTotal", 0L),
                    purchaseDate = obj.optString("purchaseDate", "")
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
                        id = customerObj.optString("id", ""),
                        name = customerObj.optString("name", "مشتری"),
                        phone = customerObj.optString("phone", ""),
                        nationalId = customerObj.optString("nationalId", ""),
                        note = customerObj.optString("note", "")
                    )
                }
                val items = decodeInvoiceItems(obj.optJSONArray("items"))
                invoices += Invoice(
                    id = obj.optString("id", ""),
                    invoiceNumber = obj.optString("invoiceNumber", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    customer = customer,
                    items = items,
                    note = obj.optString("note", "")
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
                id = obj.optString("id", ""),
                title = obj.optString("title", "قطعه طلا"),
                karat = enumOrDefault(obj.optString("karat"), Karat.K18),
                grossWeight = obj.optDouble("grossWeight", 0.0),
                stoneWeight = obj.optDouble("stoneWeight", 0.0),
                netWeight = obj.optDouble("netWeight", 0.0),
                spotPrice = obj.optLong("spotPrice", 0L),
                wageType = enumOrDefault(obj.optString("wageType"), WageType.PERCENTAGE),
                wageInput = obj.optDouble("wageInput", 0.0),
                wageAmount = obj.optDouble("wageAmount", 0.0),
                profitPercent = obj.optDouble("profitPercent", 0.0),
                profitAmount = obj.optDouble("profitAmount", 0.0),
                taxPercent = obj.optDouble("taxPercent", 0.0),
                taxAmount = obj.optDouble("taxAmount", 0.0),
                rawGoldValue = obj.optDouble("rawGoldValue", 0.0),
                totalPayable = obj.optDouble("totalPayable", 0.0),
                effectiveGramPrice = obj.optDouble("effectiveGramPrice", 0.0)
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

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

}

package com.goldex.companion.data

import com.goldex.companion.model.BankCoinItem
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.InvoiceItem
import com.goldex.companion.model.InvoiceItemCategory
import com.goldex.companion.model.Karat
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.model.MeltGoldItem
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.SettlementPaymentItem
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
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
                customKaratValue = obj.optInt("customKaratValue", 750),
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
                put("customKaratValue", item.customKaratValue)
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
                    timestamp = obj.longValue("timestamp", System.currentTimeMillis()),
                    invoiceId = obj.stringValue("invoiceId").takeIf { it.isNotBlank() },
                    invoiceNumber = obj.stringValue("invoiceNumber").takeIf { it.isNotBlank() },
                    isAutoGenerated = obj.optBoolean("isAutoGenerated", false)
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
                if (tx.invoiceId != null) put("invoiceId", tx.invoiceId)
                if (tx.invoiceNumber != null) put("invoiceNumber", tx.invoiceNumber)
                put("isAutoGenerated", tx.isAutoGenerated)
            })
        }
        return array.toString()
    }

    fun decodeInventoryItems(json: String?): List<InventoryItem> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            val items = mutableListOf<InventoryItem>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.stringValue("id").trim()
                val code = obj.stringValue("code").trim()
                val title = obj.stringValue("title").trim()
                if (id.isEmpty() || title.isEmpty()) continue
                val category = enumOrDefault(obj.stringValue("category"), InventoryCategory.RINGS)
                val karat = enumOrDefault(obj.stringValue("karat"), Karat.K18)
                val defaultKaratNum = if (karat == Karat.K21) 875 else if (karat == Karat.K24) 999 else 750
                val customKaratValue = obj.intValue("customKaratValue", defaultKaratNum)
                val location = obj.stringValue("location", "سینی شماره ۱ ویترین اصلی")
                val grossWeightGrams = obj.doubleValue("grossWeightGrams", 0.0)
                val stoneWeightGrams = obj.doubleValue("stoneWeightGrams", 0.0)
                val workshop = obj.stringValue("workshop", "کارگاه زرین تهران")
                val wageType = enumOrDefault(obj.stringValue("wageType"), WageType.PERCENTAGE)
                val wageValue = obj.doubleValue("wageValue", obj.doubleValue("wagePercent", 0.0))
                val profitPercent = obj.doubleValue(
                    "profitPercent",
                    if (category == InventoryCategory.JEWELRY) 20.0 else if (category == InventoryCategory.COINS) 0.0 else 7.0
                )
                val taxPercent = obj.doubleValue("taxPercent", 9.0)
                val rfidTag = obj.stringValue("rfidTag", "")
                val quantity = obj.intValue("quantity", 1)
                val imageUrl = obj.stringValue("imageUrl", "")
                val createdAt = obj.longValue("createdAt", System.currentTimeMillis())

                items += InventoryItem(
                    id = id,
                    code = code,
                    title = title,
                    category = category,
                    location = location,
                    grossWeightGrams = grossWeightGrams,
                    stoneWeightGrams = stoneWeightGrams,
                    karat = karat,
                    customKaratValue = customKaratValue,
                    workshop = workshop,
                    wageType = wageType,
                    wageValue = wageValue,
                    wagePercent = if (wageType == WageType.PERCENTAGE) wageValue else 0.0,
                    profitPercent = profitPercent,
                    taxPercent = taxPercent,
                    rfidTag = rfidTag,
                    quantity = quantity,
                    imageUrl = imageUrl,
                    createdAt = createdAt
                )
            }
            items
        }.getOrDefault(emptyList())
    }

    fun encodeInventoryItems(items: List<InventoryItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("code", item.code)
                put("title", item.title)
                put("category", item.category.name)
                put("location", item.location)
                put("grossWeightGrams", item.grossWeightGrams)
                put("stoneWeightGrams", item.stoneWeightGrams)
                put("karat", item.karat.name)
                put("customKaratValue", item.customKaratValue)
                put("workshop", item.workshop)
                put("wageType", item.wageType.name)
                put("wageValue", item.wageValue)
                put("wagePercent", item.wagePercent)
                put("profitPercent", item.profitPercent)
                put("taxPercent", item.taxPercent)
                put("rfidTag", item.rfidTag)
                put("quantity", item.quantity)
                put("imageUrl", item.imageUrl)
                put("createdAt", item.createdAt)
            })
        }
        return array.toString()
    }

    fun decodeStockAdjustments(json: String?): List<StockAdjustment> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            val adjustments = mutableListOf<StockAdjustment>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.stringValue("id").trim()
                val itemId = obj.stringValue("itemId").trim()
                if (id.isEmpty() || itemId.isEmpty()) continue
                val itemTitle = obj.stringValue("itemTitle", "")
                val type = enumOrDefault(obj.stringValue("type"), StockAdjustmentType.CHARGE)
                val quantityChange = obj.intValue("quantityChange", 1)
                val weightGrams = obj.doubleValue("weightGrams", 0.0)
                val reason = obj.stringValue("reason", "دریافت از کارگاه ساخت")
                val note = obj.stringValue("note", "")
                val timestamp = obj.longValue("timestamp", System.currentTimeMillis())

                adjustments += StockAdjustment(
                    id = id,
                    itemId = itemId,
                    itemTitle = itemTitle,
                    type = type,
                    quantityChange = quantityChange,
                    weightGrams = weightGrams,
                    reason = reason,
                    note = note,
                    timestamp = timestamp
                )
            }
            adjustments
        }.getOrDefault(emptyList())
    }

    fun encodeStockAdjustments(adjustments: List<StockAdjustment>): String {
        val array = JSONArray()
        adjustments.forEach { adj ->
            array.put(JSONObject().apply {
                put("id", adj.id)
                put("itemId", adj.itemId)
                put("itemTitle", adj.itemTitle)
                put("type", adj.type.name)
                put("quantityChange", adj.quantityChange)
                put("weightGrams", adj.weightGrams)
                put("reason", adj.reason)
                put("note", adj.note)
                put("timestamp", adj.timestamp)
            })
        }
        return array.toString()
    }

    fun encodeMarketRates(rates: MarketRates): String {
        val obj = JSONObject().apply {
            put("gold18", rates.gold18)
            put("gold24", rates.gold24)
            put("goldMelt", rates.goldMelt)
            put("coinEmami", rates.coinEmami)
            put("coinBahar", rates.coinBahar)
            put("coinHalf", rates.coinHalf)
            put("coinQuarter", rates.coinQuarter)
            put("coinGerami", rates.coinGerami)
            put("usd", rates.usd)
            put("ons", rates.ons)
            put("lastUpdated", rates.lastUpdated)
            put("source", rates.source.name)
            put("isLive", rates.isLive)
        }
        return obj.toString()
    }

    fun decodeMarketRates(json: String?): MarketRates? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(json)
            val source = enumOrDefault(obj.stringValue("source"), PriceSource.ISIGNAL)
            MarketRates(
                gold18 = obj.longValue("gold18", 23_360_000L),
                gold24 = obj.longValue("gold24", 31_148_000L),
                goldMelt = obj.longValue("goldMelt", 101_500_000L),
                coinEmami = obj.longValue("coinEmami", 234_000_000L),
                coinBahar = obj.longValue("coinBahar", 230_000_000L),
                coinHalf = obj.longValue("coinHalf", 120_000_000L),
                coinQuarter = obj.longValue("coinQuarter", 66_000_000L),
                coinGerami = obj.longValue("coinGerami", 35_000_000L),
                usd = obj.longValue("usd", 221_500L),
                ons = obj.doubleValue("ons", 4435.0),
                lastUpdated = obj.stringValue("lastUpdated", "--:--:--"),
                source = source,
                isLive = obj.optBoolean("isLive", false)
            )
        }.getOrNull()
    }

    fun encodeMarketRateItemSummaries(summaries: Map<MarketRateItemType, MarketRateItemSummary>): String {
        val array = JSONArray()
        summaries.values.forEach { item ->
            array.put(JSONObject().apply {
                put("type", item.type.name)
                put("currentPrice", item.currentPrice)
                put("dayLow", item.dayLow)
                put("dayHigh", item.dayHigh)
                put("openPrice", item.openPrice)
                put("changeAmount", item.changeAmount)
                put("changePercent", item.changePercent)
                put("isPositive", item.isPositive)
                put("lastUpdated", item.lastUpdated)
            })
        }
        return array.toString()
    }

    fun decodeMarketRateItemSummaries(json: String?): Map<MarketRateItemType, MarketRateItemSummary> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val array = JSONArray(json)
            val map = mutableMapOf<MarketRateItemType, MarketRateItemSummary>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val typeStr = obj.stringValue("type")
                val type = runCatching { MarketRateItemType.valueOf(typeStr) }.getOrNull() ?: continue
                map[type] = MarketRateItemSummary(
                    type = type,
                    currentPrice = obj.longValue("currentPrice", 0L),
                    dayLow = obj.longValue("dayLow", 0L),
                    dayHigh = obj.longValue("dayHigh", 0L),
                    openPrice = obj.longValue("openPrice", 0L),
                    changeAmount = obj.longValue("changeAmount", 0L),
                    changePercent = obj.doubleValue("changePercent", 0.0),
                    isPositive = obj.optBoolean("isPositive", true),
                    lastUpdated = obj.longValue("lastUpdated", 0L)
                )
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun decodeBarterInvoices(json: String?): List<BarterInvoice> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            val invoices = mutableListOf<BarterInvoice>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.stringValue("id").trim()
                if (id.isEmpty()) continue
                val customerObj = obj.optJSONObject("customer")
                val customer = customerObj?.let { c ->
                    Customer(
                        id = c.stringValue("id"),
                        name = c.stringValue("name"),
                        phone = c.stringValue("phone"),
                        nationalId = c.stringValue("nationalId"),
                        note = c.stringValue("note"),
                        role = c.optString("role", "بنکدار و همکار بازار")
                    )
                }
                val thirdPartyObj = obj.optJSONObject("thirdPartyCustomer")
                val thirdPartyCustomer = thirdPartyObj?.let { c ->
                    Customer(
                        id = c.stringValue("id"),
                        name = c.stringValue("name"),
                        phone = c.stringValue("phone")
                    )
                }
                invoices += BarterInvoice(
                    id = id,
                    invoiceNumber = obj.stringValue("invoiceNumber"),
                    createdAt = obj.longValue("createdAt", System.currentTimeMillis()),
                    customer = customer,
                    customerRole = enumOrDefault(obj.stringValue("customerRole"), CustomerRole.WHOLESALER),
                    spotPrice18k = obj.longValue("spotPrice18k", 0L),
                    salesItems = decodeBarterItems(obj.optJSONArray("salesItems")),
                    receivedItems = decodeBarterItems(obj.optJSONArray("receivedItems")),
                    settlementMethod = enumOrDefault(obj.stringValue("settlementMethod"), SettlementMethod.POS),
                    cashPosAmount = obj.longValue("cashPosAmount", 0L),
                    ledgerAmount = obj.longValue("ledgerAmount", 0L),
                    posTrackingCode = obj.stringValue("posTrackingCode"),
                    ledgerDueDate = obj.optString("ledgerDueDate", "تسویه ماهانه"),
                    bullionWeight = obj.doubleValue("bullionWeight", 0.0),
                    bullionKarat = obj.optInt("bullionKarat", 750),
                    bullionAngNumber = obj.stringValue("bullionAngNumber"),
                    thirdPartyCustomer = thirdPartyCustomer,
                    thirdPartyInvoiceId = obj.stringValue("thirdPartyInvoiceId"),
                    thirdPartyInvoiceNumber = obj.stringValue("thirdPartyInvoiceNumber"),
                    thirdPartyTransferWeight18k = obj.doubleValue("thirdPartyTransferWeight18k", 0.0),
                    thirdPartyTransferAmount = obj.longValue("thirdPartyTransferAmount", 0L),
                    thirdPartyTrackingCode = obj.stringValue("thirdPartyTrackingCode"),
                    note = obj.stringValue("note"),
                    payments = decodePaymentItems(obj.optJSONArray("payments")),
                    syncWithLedger = obj.optBoolean("syncWithLedger", true)
                )
            }
            invoices
        }.getOrDefault(emptyList())
    }

    fun encodeBarterInvoices(invoices: List<BarterInvoice>): String {
        val array = JSONArray()
        invoices.forEach { invoice ->
            array.put(JSONObject().apply {
                put("id", invoice.id)
                put("invoiceNumber", invoice.invoiceNumber)
                put("createdAt", invoice.createdAt)
                invoice.customer?.let { c ->
                    put("customer", JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("nationalId", c.nationalId)
                        put("note", c.note)
                        put("role", c.role)
                    })
                }
                put("customerRole", invoice.customerRole.name)
                put("spotPrice18k", invoice.spotPrice18k)
                put("salesItems", encodeBarterItems(invoice.salesItems))
                put("receivedItems", encodeBarterItems(invoice.receivedItems))
                put("settlementMethod", invoice.settlementMethod.name)
                put("cashPosAmount", invoice.cashPosAmount)
                put("ledgerAmount", invoice.ledgerAmount)
                put("posTrackingCode", invoice.posTrackingCode)
                put("ledgerDueDate", invoice.ledgerDueDate)
                put("bullionWeight", invoice.bullionWeight)
                put("bullionKarat", invoice.bullionKarat)
                put("bullionAngNumber", invoice.bullionAngNumber)
                invoice.thirdPartyCustomer?.let { tc ->
                    put("thirdPartyCustomer", JSONObject().apply {
                        put("id", tc.id)
                        put("name", tc.name)
                        put("phone", tc.phone)
                    })
                }
                put("thirdPartyInvoiceId", invoice.thirdPartyInvoiceId)
                put("thirdPartyInvoiceNumber", invoice.thirdPartyInvoiceNumber)
                put("thirdPartyTransferWeight18k", invoice.thirdPartyTransferWeight18k)
                put("thirdPartyTransferAmount", invoice.thirdPartyTransferAmount)
                put("thirdPartyTrackingCode", invoice.thirdPartyTrackingCode)
                put("note", invoice.note)
                put("payments", encodePaymentItems(invoice.payments))
                put("syncWithLedger", invoice.syncWithLedger)
            })
        }
        return array.toString()
    }

    private fun decodeBarterItems(array: JSONArray?): List<BarterItem> {
        if (array == null) return emptyList()
        val items = mutableListOf<BarterItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val category = enumOrDefault(obj.stringValue("category"), InvoiceItemCategory.CRAFTED)
            val id = obj.stringValue("id")
            val title = obj.stringValue("title")
            val totalPayable = obj.doubleValue("totalPayable", 0.0)
            val equivalent18kWeight = obj.doubleValue("equivalent18kWeight", 0.0)
            when (category) {
                InvoiceItemCategory.CRAFTED -> {
                    items += CraftedGoldItem(
                        id = id,
                        title = title,
                        karat = enumOrDefault(obj.stringValue("karat"), Karat.K18),
                        customKaratValue = obj.optInt("customKaratValue", 750),
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
                        totalPayable = totalPayable,
                        equivalent18kWeight = equivalent18kWeight
                    )
                }
                InvoiceItemCategory.SCRAP -> {
                    items += ScrapGoldItem(
                        id = id,
                        title = title,
                        baseKarat = obj.optInt("baseKarat", 750),
                        karatDeficit = obj.optInt("karatDeficit", 15),
                        payableKarat = obj.optInt("payableKarat", 735),
                        grossWeight = obj.doubleValue("grossWeight", 0.0),
                        stoneWeight = obj.doubleValue("stoneWeight", 0.0),
                        netWeight = obj.doubleValue("netWeight", 0.0),
                        spotPrice = obj.longValue("spotPrice", 0L),
                        deductionPerGram = obj.longValue("deductionPerGram", 15000L),
                        exchangeCommissionPercent = obj.doubleValue("exchangeCommissionPercent", 0.0),
                        effectiveGramPrice = obj.longValue("effectiveGramPrice", 0L),
                        totalPayable = totalPayable,
                        equivalent18kWeight = equivalent18kWeight
                    )
                }
                InvoiceItemCategory.MELT -> {
                    items += MeltGoldItem(
                        id = id,
                        title = title,
                        weight = obj.doubleValue("weight", 0.0),
                        labKarat = obj.optInt("labKarat", 750),
                        angNumber = obj.stringValue("angNumber"),
                        labName = obj.stringValue("labName"),
                        spotPrice = obj.longValue("spotPrice", 0L),
                        totalPayable = totalPayable,
                        equivalent18kWeight = equivalent18kWeight
                    )
                }
                InvoiceItemCategory.COIN -> {
                    items += BankCoinItem(
                        id = id,
                        title = title,
                        coinType = enumOrDefault(obj.stringValue("coinType"), CoinType.EMAMI),
                        count = obj.intValue("count", 1),
                        hasHologram = obj.optBoolean("hasHologram", true),
                        unitPrice = obj.longValue("unitPrice", 0L),
                        totalPayable = totalPayable,
                        equivalent18kWeight = equivalent18kWeight
                    )
                }
            }
        }
        return items
    }

    private fun encodeBarterItems(items: List<BarterItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("category", item.category.name)
            obj.put("totalPayable", item.totalPayable)
            obj.put("equivalent18kWeight", item.equivalent18kWeight)
            when (item) {
                is CraftedGoldItem -> {
                    obj.put("karat", item.karat.name)
                    obj.put("customKaratValue", item.customKaratValue)
                    obj.put("grossWeight", item.grossWeight)
                    obj.put("stoneWeight", item.stoneWeight)
                    obj.put("netWeight", item.netWeight)
                    obj.put("spotPrice", item.spotPrice)
                    obj.put("wageType", item.wageType.name)
                    obj.put("wageInput", item.wageInput)
                    obj.put("wageAmount", item.wageAmount)
                    obj.put("profitPercent", item.profitPercent)
                    obj.put("profitAmount", item.profitAmount)
                    obj.put("taxPercent", item.taxPercent)
                    obj.put("taxAmount", item.taxAmount)
                    obj.put("rawGoldValue", item.rawGoldValue)
                }
                is ScrapGoldItem -> {
                    obj.put("baseKarat", item.baseKarat)
                    obj.put("karatDeficit", item.karatDeficit)
                    obj.put("payableKarat", item.payableKarat)
                    obj.put("grossWeight", item.grossWeight)
                    obj.put("stoneWeight", item.stoneWeight)
                    obj.put("netWeight", item.netWeight)
                    obj.put("spotPrice", item.spotPrice)
                    obj.put("deductionPerGram", item.deductionPerGram)
                    obj.put("exchangeCommissionPercent", item.exchangeCommissionPercent)
                    obj.put("effectiveGramPrice", item.effectiveGramPrice)
                }
                is MeltGoldItem -> {
                    obj.put("weight", item.weight)
                    obj.put("labKarat", item.labKarat)
                    obj.put("angNumber", item.angNumber)
                    obj.put("labName", item.labName)
                    obj.put("spotPrice", item.spotPrice)
                }
                is BankCoinItem -> {
                    obj.put("coinType", item.coinType.name)
                    obj.put("count", item.count)
                    obj.put("hasHologram", item.hasHologram)
                    obj.put("unitPrice", item.unitPrice)
                }
            }
            array.put(obj)
        }
        return array
    }

    private fun decodePaymentItems(array: JSONArray?): List<SettlementPaymentItem> {
        if (array == null) return emptyList()
        val items = mutableListOf<SettlementPaymentItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            items += SettlementPaymentItem(
                id = obj.stringValue("id"),
                method = enumOrDefault(obj.stringValue("method"), SettlementMethod.POS),
                amountTomans = obj.longValue("amountTomans", 0L),
                goldWeight18k = obj.doubleValue("goldWeight18k", 0.0),
                trackingCode = obj.stringValue("trackingCode"),
                description = obj.stringValue("description"),
                date = obj.stringValue("date"),
                thirdPartyCustomerName = obj.stringValue("thirdPartyCustomerName"),
                bullionKarat = obj.optInt("bullionKarat", 750),
                bullionAngNumber = obj.stringValue("bullionAngNumber")
            )
        }
        return items
    }

    private fun encodePaymentItems(payments: List<SettlementPaymentItem>): JSONArray {
        val array = JSONArray()
        payments.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id)
                put("method", p.method.name)
                put("amountTomans", p.amountTomans)
                put("goldWeight18k", p.goldWeight18k)
                put("trackingCode", p.trackingCode)
                put("description", p.description)
                put("date", p.date)
                put("thirdPartyCustomerName", p.thirdPartyCustomerName)
                put("bullionKarat", p.bullionKarat)
                put("bullionAngNumber", p.bullionAngNumber)
            })
        }
        return array
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

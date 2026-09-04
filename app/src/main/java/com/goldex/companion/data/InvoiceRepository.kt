package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.*
import org.json.JSONArray
import org.json.JSONObject

class InvoiceRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qirat_invoices_prefs", Context.MODE_PRIVATE)

    fun getInvoices(): List<Invoice> {
        val json = prefs.getString("invoices_json", null) ?: return emptyList()
        val list = mutableListOf<Invoice>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val customerObj = obj.optJSONObject("customer")
                val customer = customerObj?.let {
                    Customer(
                        id = it.optString("id", ""),
                        name = it.optString("name", "مشتری"),
                        phone = it.optString("phone", ""),
                        nationalId = it.optString("nationalId", ""),
                        note = it.optString("note", "")
                    )
                }

                val itemsList = mutableListOf<InvoiceItem>()
                val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                for (j in 0 until itemsArr.length()) {
                    val itm = itemsArr.getJSONObject(j)
                    val karatStr = itm.optString("karat", Karat.K18.name)
                    val karat = try { Karat.valueOf(karatStr) } catch (_: Exception) { Karat.K18 }
                    val wageTypeStr = itm.optString("wageType", WageType.PERCENTAGE.name)
                    val wageType = try { WageType.valueOf(wageTypeStr) } catch (_: Exception) { WageType.PERCENTAGE }

                    itemsList.add(
                        InvoiceItem(
                            id = itm.optString("id", ""),
                            title = itm.optString("title", "قطعه طلا"),
                            karat = karat,
                            grossWeight = itm.optDouble("grossWeight", 0.0),
                            stoneWeight = itm.optDouble("stoneWeight", 0.0),
                            netWeight = itm.optDouble("netWeight", 0.0),
                            spotPrice = itm.optLong("spotPrice", 0L),
                            wageType = wageType,
                            wageInput = itm.optDouble("wageInput", 0.0),
                            wageAmount = itm.optDouble("wageAmount", 0.0),
                            profitPercent = itm.optDouble("profitPercent", 0.0),
                            profitAmount = itm.optDouble("profitAmount", 0.0),
                            taxPercent = itm.optDouble("taxPercent", 0.0),
                            taxAmount = itm.optDouble("taxAmount", 0.0),
                            rawGoldValue = itm.optDouble("rawGoldValue", 0.0),
                            totalPayable = itm.optDouble("totalPayable", 0.0),
                            effectiveGramPrice = itm.optDouble("effectiveGramPrice", 0.0)
                        )
                    )
                }

                list.add(
                    Invoice(
                        id = obj.optString("id", ""),
                        invoiceNumber = obj.optString("invoiceNumber", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        customer = customer,
                        items = itemsList,
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveInvoice(invoice: Invoice) {
        val list = getInvoices().toMutableList()
        val index = list.indexOfFirst { it.id == invoice.id }
        if (index >= 0) {
            list[index] = invoice
        } else {
            list.add(0, invoice)
        }
        persist(list)
    }

    fun deleteInvoice(id: String) {
        val list = getInvoices().filterNot { it.id == id }
        persist(list)
    }

    private fun persist(list: List<Invoice>) {
        val arr = JSONArray()
        for (inv in list) {
            val obj = JSONObject()
            obj.put("id", inv.id)
            obj.put("invoiceNumber", inv.invoiceNumber)
            obj.put("createdAt", inv.createdAt)
            obj.put("note", inv.note)

            inv.customer?.let { c ->
                val cObj = JSONObject()
                cObj.put("id", c.id)
                cObj.put("name", c.name)
                cObj.put("phone", c.phone)
                cObj.put("nationalId", c.nationalId)
                cObj.put("note", c.note)
                obj.put("customer", cObj)
            }

            val itemsArr = JSONArray()
            for (itm in inv.items) {
                val itmObj = JSONObject()
                itmObj.put("id", itm.id)
                itmObj.put("title", itm.title)
                itmObj.put("karat", itm.karat.name)
                itmObj.put("grossWeight", itm.grossWeight)
                itmObj.put("stoneWeight", itm.stoneWeight)
                itmObj.put("netWeight", itm.netWeight)
                itmObj.put("spotPrice", itm.spotPrice)
                itmObj.put("wageType", itm.wageType.name)
                itmObj.put("wageInput", itm.wageInput)
                itmObj.put("wageAmount", itm.wageAmount)
                itmObj.put("profitPercent", itm.profitPercent)
                itmObj.put("profitAmount", itm.profitAmount)
                itmObj.put("taxPercent", itm.taxPercent)
                itmObj.put("taxAmount", itm.taxAmount)
                itmObj.put("rawGoldValue", itm.rawGoldValue)
                itmObj.put("totalPayable", itm.totalPayable)
                itmObj.put("effectiveGramPrice", itm.effectiveGramPrice)
                itemsArr.put(itmObj)
            }
            obj.put("items", itemsArr)
            arr.put(obj)
        }
        prefs.edit().putString("invoices_json", arr.toString()).apply()
    }
}

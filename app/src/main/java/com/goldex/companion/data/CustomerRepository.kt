package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.Customer
import org.json.JSONArray
import org.json.JSONObject

class CustomerRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("goldex_customers_prefs", Context.MODE_PRIVATE)

    fun getCustomers(): List<Customer> {
        val json = prefs.getString("customers_json", null)
        if (json.isNullOrBlank()) {
            val defaults = listOf(
                Customer(
                    id = "cust_default_1",
                    name = "حاج احمد کریمی",
                    phone = "۰۹۱۲۳۴۵۶۷۸۹",
                    nationalId = "۰۰۱۲۳۴۵۶۷۸",
                    note = "مشتری قدیمی بازار تهران"
                ),
                Customer(
                    id = "cust_default_2",
                    name = "خانم مهندس صادقی",
                    phone = "۰۹۱۴۱۱۱۱۲۲۳",
                    nationalId = "۱۳۷۰۹۸۷۶۵۴",
                    note = "سفارش ست و نیم‌ست عروس"
                )
            )
            saveCustomers(defaults)
            return defaults
        }

        val list = mutableListOf<Customer>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Customer(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        nationalId = obj.optString("nationalId", ""),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addCustomer(customer: Customer) {
        val list = getCustomers().toMutableList()
        list.add(0, customer)
        saveCustomers(list)
    }

    fun updateCustomer(customer: Customer) {
        val list = getCustomers().map { if (it.id == customer.id) customer else it }
        saveCustomers(list)
    }

    fun deleteCustomer(id: String) {
        val list = getCustomers().filter { it.id != id }
        saveCustomers(list)
    }

    private fun saveCustomers(customers: List<Customer>) {
        val arr = JSONArray()
        for (c in customers) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("nationalId", c.nationalId)
                put("note", c.note)
                put("createdAt", c.createdAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("customers_json", arr.toString()).apply()
    }
}

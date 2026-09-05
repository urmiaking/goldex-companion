package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.Customer
import org.json.JSONArray
import org.json.JSONObject

class CustomerRepository(context: Context) : CustomerStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("goldex_customers_prefs", Context.MODE_PRIVATE)

    override fun getCustomers(): List<Customer> {
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
        return PersistenceJsonCodecs.decodeCustomers(json)
    }

    override fun addCustomer(customer: Customer) {
        val list = getCustomers().toMutableList()
        list.add(0, customer)
        saveCustomers(list)
    }

    override fun updateCustomer(customer: Customer) {
        val list = getCustomers().map { if (it.id == customer.id) customer else it }
        saveCustomers(list)
    }

    override fun deleteCustomer(id: String) {
        val list = getCustomers().filter { it.id != id }
        saveCustomers(list)
    }

    private fun saveCustomers(customers: List<Customer>) {
        prefs.edit().putString("customers_json", PersistenceJsonCodecs.encodeCustomers(customers)).apply()
    }
}

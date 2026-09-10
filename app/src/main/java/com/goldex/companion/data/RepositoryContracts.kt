package com.goldex.companion.data

import com.goldex.companion.model.Customer
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.LedgerTransaction
import kotlinx.coroutines.flow.StateFlow

interface CustomerStore {
    fun getCustomers(): List<Customer>
    fun addCustomer(customer: Customer)
    fun updateCustomer(customer: Customer)
    fun deleteCustomer(id: String)
    fun getTransactions(customerId: String): List<LedgerTransaction>
    fun addTransaction(transaction: LedgerTransaction)
}

interface PortfolioStore {
    fun getItems(): List<PortfolioItem>
    fun addItem(item: PortfolioItem)
    fun deleteItem(id: String)
}

interface InvoiceStore {
    fun getInvoices(): List<Invoice>
    fun saveInvoice(invoice: Invoice)
    fun deleteInvoice(id: String)
}

interface SettingsStore {
    val settings: StateFlow<AppSettings>
    fun loadSettings(): AppSettings
    fun saveSettings(newSettings: AppSettings)
}

interface MarketRatesStore {
    val rates: StateFlow<MarketRates>
    val currentSource: StateFlow<PriceSource>
    suspend fun setSource(source: PriceSource)
    suspend fun cycleSource(): PriceSource
    suspend fun refreshRates(): MarketRates
}

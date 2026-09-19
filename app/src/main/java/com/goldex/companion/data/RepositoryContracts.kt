package com.goldex.companion.data

import com.goldex.companion.model.Customer
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.MarketCandle
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.TimeHorizon
import kotlinx.coroutines.flow.StateFlow

interface CustomerStore {
    fun getCustomers(): List<Customer>
    fun addCustomer(customer: Customer)
    fun updateCustomer(customer: Customer)
    fun deleteCustomer(id: String)
    fun getTransactions(customerId: String): List<LedgerTransaction>
    fun addTransaction(transaction: LedgerTransaction)
    fun updateTransaction(transaction: LedgerTransaction) {}
    fun deleteTransaction(id: String) {}
}

interface InventoryStore {
    fun getItems(): List<InventoryItem>
    fun addItem(item: InventoryItem)
    fun updateItem(item: InventoryItem)
    fun deleteItem(id: String)
    fun adjustStock(adjustment: StockAdjustment)
    fun getAdjustments(): List<StockAdjustment>
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
    fun loadDarkTheme(): Boolean = false
    fun saveDarkTheme(enabled: Boolean) = Unit
}

interface MarketRatesStore {
    val rates: StateFlow<MarketRates>
    val currentSource: StateFlow<PriceSource>
    suspend fun setSource(source: PriceSource)
    suspend fun cycleSource(): PriceSource
    suspend fun refreshRates(): MarketRates
}

interface MarketHistoryStore {
    suspend fun getHistory(
        type: MarketRateItemType,
        horizon: TimeHorizon,
        preferredSource: PriceSource = PriceSource.ISIGNAL
    ): List<MarketCandle>

    suspend fun getAllHorizonsHistory(
        type: MarketRateItemType,
        preferredSource: PriceSource = PriceSource.ISIGNAL
    ): Map<TimeHorizon, List<MarketCandle>>
}


package com.goldex.companion.desktop

import com.goldex.companion.data.*
import com.goldex.companion.desktop.customers.DesktopCustomers
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.DesktopInvoices
import com.goldex.companion.domain.customers.*
import com.goldex.companion.domain.invoice.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.invoices.*
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class DesktopInvoiceStateTest {
    @get:Rule val temporary = TemporaryFolder()
    private val customer = Customer(id = "contact", name = "مشتری آزمون", createdAt = 1)
    @Test fun calculatorTransferPreservesFinancialResultAndConvertsSelectedQuoteBasisExactlyOnce() {
        val calculator = com.goldex.companion.presentation.calculator.ManualGoldCalculator()
        calculator.setInput(com.goldex.companion.presentation.calculator.CalculatorField.SPOT, "۶۰۰۰۰۰۰")
        calculator.setInput(com.goldex.companion.presentation.calculator.CalculatorField.GROSS_WEIGHT, "۲")
        calculator.setInput(com.goldex.companion.presentation.calculator.CalculatorField.WAGE, "۱۰")
        calculator.setPriceBasis(PriceBasisTab.K24)
        val item = requireNotNull(com.goldex.companion.desktop.state.calculatorInvoiceItem(calculator.state.value))
        assertEquals(6_000_000L, item.spotPrice)
        assertEquals(14_315_160.0, item.totalPayable)
        assertEquals(191_160.0, item.taxAmount)
        assertEquals(2.0, item.equivalent18kWeight)
    }
    private fun crafted(id: String = "made") = CraftedGoldItem(id = id, title = "دستبند آزمون", grossWeight = 2.0,
        netWeight = 2.0, spotPrice = 6_000_000, wageInput = 10.0, wageAmount = 1_200_000.0,
        profitPercent = 7.0, profitAmount = 924_000.0, taxPercent = 9.0, taxAmount = 191_160.0,
        rawGoldValue = 12_000_000.0, totalPayable = 14_315_160.0, equivalent18kWeight = 2.0)
    private fun withFeature(action: (DesktopDataStore, DesktopInvoices) -> Unit) {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val customers = DesktopCustomers(store, scope) { DesktopMarketRepository.emptyRates() }
                customers.repository.addCustomer(customer)
                val feature = DesktopInvoices(customers, scope, store::loadSettings, Dispatchers.Unconfined) { 6_000_000L }
                action(store, feature)
            } finally { scope.cancel() }
        }
    }

    @Test fun issueEditReloadAndDeleteUseRealStoreAndReverseLedgerOnlyOnce() = withFeature { _, feature ->
        feature.newInvoice(customer)
        feature.manager.setDebtBasis(InvoiceDebtBasis.CASH)
        feature.manager.addSalesItem(crafted())
        feature.save()
        assertNull(feature.manager.uiState.value.error)
        val saved = feature.customers.invoices.getBarterInvoices().single()
        assertEquals(14_315_160L, feature.customers.repository.getCustomers().single().cashDebtTomans)
        assertEquals(1, feature.manager.uiState.value.invoicesList.size)
        assertEquals(InvoiceStatus.PARTIALLY_PAID, feature.manager.uiState.value.invoicesList.single().status)
        feature.manager.openInvoiceDetails(feature.manager.uiState.value.invoicesList.single())
        feature.manager.setNote("یادداشت محفوظ")
        feature.manager.reloadInvoices()
        assertEquals("یادداشت محفوظ", feature.manager.uiState.value.invoice.note)
        assertEquals("", feature.customers.invoices.getBarterInvoices().single().note)
        feature.save()
        assertEquals("یادداشت محفوظ", feature.customers.invoices.getBarterInvoices().single().note)
        assertEquals(14_315_160L, feature.customers.repository.getCustomers().single().cashDebtTomans)
        feature.manager.deleteInvoice(saved.id)
        feature.manager.deleteInvoice(saved.id)
        assertEquals(0L, feature.customers.repository.getCustomers().single().cashDebtTomans)
        assertTrue(feature.customers.repository.getTransactions(customer.id).isEmpty())
        assertTrue(feature.customers.invoices.getBarterInvoices().isEmpty())
    }

    @Test fun datedSettlementSurvivesEditingAndRemovalIsAppliedOnlyOnFinalSave() = withFeature { _, feature ->
        feature.newInvoice(customer)
        feature.manager.addSalesItem(crafted())
        feature.save()
        val saved = feature.customers.invoices.getBarterInvoices().single()
        val current = feature.customers.repository.getCustomers().single()
        val balance = invoiceOutstanding(feature.customers.repository.getTransactionsByInvoiceId(saved.id))
        val tx = RecordCustomerSettlementUseCase(feature.customers.repository, feature.customers.invoices, feature.customers.repository)
            .record(SettlementRequest(id = "dated", customerId = customer.id, invoiceId = saved.id,
                targetType = LedgerEntryType.GOLD_WEIGHT, paymentType = LedgerEntryType.CASH_RIAL,
                amountTomans = 6_000_000, rateTomans = 6_000_000), current, balance)
        feature.manager.reloadInvoices()
        feature.manager.openInvoiceDetails(feature.manager.uiState.value.invoicesList.single())
        assertEquals(tx.settlement, feature.manager.uiState.value.invoice.payments.single().settlement)
        val remaining = feature.customers.repository.getCustomers().single().goldDebtGrams
        feature.manager.setSettlementPayments(emptyList())
        assertEquals(remaining, feature.customers.repository.getCustomers().single().goldDebtGrams)
        feature.manager.navigateBackToList()
        feature.manager.reloadInvoices()
        assertEquals(1, feature.customers.invoices.getBarterInvoices().single().payments.size)
        feature.manager.openInvoiceDetails(feature.manager.uiState.value.invoicesList.single())
        feature.manager.setSettlementPayments(emptyList())
        feature.save()
        assertNull(feature.manager.uiState.value.error)
        assertEquals(balance.goldGrams, feature.customers.repository.getCustomers().single().goldDebtGrams, 1e-9)
        assertFalse(feature.customers.repository.getTransactions(customer.id).any { it.id == tx.id })
        feature.manager.deleteInvoice(saved.id)
        assertEquals(0.0, feature.customers.repository.getCustomers().single().goldDebtGrams, 1e-9)
    }

    @Test fun failureRollsBackInvoiceAndLedgerAndRetainsEditor() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val customers = DesktopCustomerStore(store)
            customers.addCustomer(customer)
            val failing = object : CustomerStore by customers { override fun updateCustomer(customer: Customer) { error("synthetic write failure") } }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val invoices = DesktopCustomerInvoiceStore(store, customers)
                val manager = BarterInvoiceViewModel(invoices, failing, customers, scope, Dispatchers.Unconfined) { 6_000_000 }
                manager.openNewInvoice(); manager.setCustomer(customer); manager.addSalesItem(crafted())
                manager.submitAndSaveCurrentInvoice()
                assertNotNull(manager.uiState.value.error)
                assertEquals(InvoicesSubScreen.EDITOR, manager.uiState.value.subScreen)
                assertEquals(1, manager.uiState.value.invoice.salesItems.size)
                assertTrue(invoices.getBarterInvoices().isEmpty())
                assertTrue(customers.getTransactions(customer.id).isEmpty())
                assertEquals(0.0, customers.getCustomers().single().goldDebtGrams)
            } finally { scope.cancel() }
        }
    }

    @Test fun zeroOfflineRateIsNeverReplacedWithAnInventedPriceAndEmptyInvoiceCannotBeSaved() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val customers = DesktopCustomers(store, scope) { DesktopMarketRepository.emptyRates() }
                val feature = DesktopInvoices(customers, scope, store::loadSettings, Dispatchers.Unconfined) { 0 }
                feature.newInvoice()
                assertEquals(0L, feature.manager.uiState.value.invoice.spotPrice18k)
                feature.save()
                assertTrue(feature.customers.invoices.getBarterInvoices().isEmpty())
                assertTrue(feature.manager.uiState.value.statusMessage.contains("قلم"))
            } finally { scope.cancel() }
        }
    }

    @Test fun everyAndroidItemAndPaymentCategoryRoundTripsInDesktopBackup() = withFeature { store, feature ->
        val items = listOf(crafted(), ScrapGoldItem(id = "scrap", grossWeight = 1.0, netWeight = 1.0, spotPrice = 6_000_000,
            effectiveGramPrice = 5_880_000, totalPayable = 5_880_000.0, equivalent18kWeight = .98),
            MeltGoldItem(id = "melt", weight = 1.0, spotPrice = 6_000_000, totalPayable = 6_000_000.0, equivalent18kWeight = 1.0),
            BankCoinItem(id = "coin", coinType = CoinType.EMAMI, count = 1, unitPrice = 75_000_000, totalPayable = 75_000_000.0, equivalent18kWeight = 9.759))
        val payments = SettlementMethod.values().map { SettlementPaymentItem(id = it.name, method = it, amountTomans = 1000) }
        val invoice = BarterInvoice(id = "all-categories", customer = customer, spotPrice18k = 6_000_000,
            debtBasis = InvoiceDebtBasis.CASH, salesItems = items, receivedItems = items, payments = payments)
        feature.customers.invoices.saveBarterInvoice(invoice)
        assertEquals(invoice, feature.customers.invoices.getBarterInvoices().single())
        val backup = temporary.newFolder().toPath().resolve("backup.json")
        store.exportBackup(backup)
        assertTrue(java.nio.file.Files.readString(backup).contains("all-categories"))
    }
}

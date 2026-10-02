package com.goldex.companion.ui.customers

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.data.*
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.domain.customers.RecordCustomerSettlementUseCase
import com.goldex.companion.model.*
import com.goldex.companion.ui.customers.modals.CustomerSettlementModal
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class CustomerSettlementModalTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cashToGoldFormAcceptsCustomRateAndCommitsOnlyAfterPreview() = verifyCashSettlement()

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun formRemainsUsableWithLargeTextOnSmallScreen() = verifyCashSettlement(fontScale = 1.5f)

    @Test @Config(qualifiers = "w800dp-h360dp-land")
    fun formRemainsUsableInLandscape() = verifyCashSettlement()

    private fun verifyCashSettlement(fontScale: Float = 1f) {
        val store = MemoryCustomers(Customer(id = "customer", name = "مشتری نمونه", goldDebtGrams = 10.0))
        val invoices = EmptyInvoices()
        val unit = object : SyncUnitOfWork { override fun <T> transaction(action: () -> T): T = action() }
        val vm = CustomerSettlementViewModel(store, invoices, RecordCustomerSettlementUseCase(store, invoices, unit)) {
            MarketRates(gold18 = 100_000_000, isLive = true, lastUpdated = "12:00:00")
        }
        vm.open(store.customer)
        compose.setContent {
            val state by vm.state.collectAsState()
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                GoldExCompanionTheme(isDarkTheme = true) {
                    CustomerSettlementModal(state, vm, onConfirm = { vm.confirm() }, onIndependentEntry = {})
                }
            }
        }
        compose.onNodeWithTag("confirmSettlement").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("نرخ دلخواه").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settlementRate")))
            .performScrollTo().performTextReplacement("۱۰۵۰۰۰۰۰۰")
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settlementAmount")))
            .performScrollTo().performTextReplacement("۵۰۰۰۰۰۰۰۰")
        compose.onNodeWithTag("settlementConversion").performScrollTo().assertExists()
        compose.onNodeWithTag("confirmSettlement").performScrollTo().assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(10.0 - 500_000_000.0 / 105_000_000, store.customer.goldDebtGrams, 1e-12)
        assertEquals(0L, store.customer.cashDebtTomans)
        assertEquals(105_000_000L, store.entries.single().settlement!!.rateTomans)
        compose.onNodeWithTag("confirmSettlement").assertDoesNotExist()
    }

    private class MemoryCustomers(var customer: Customer) : CustomerStore {
        val entries = mutableListOf<LedgerTransaction>()
        override fun getCustomers() = listOf(customer)
        override fun addCustomer(customer: Customer) { this.customer = customer }
        override fun updateCustomer(customer: Customer) { this.customer = customer }
        override fun deleteCustomer(id: String) = Unit
        override fun getTransactions(customerId: String) = entries.toList()
        override fun addTransaction(transaction: LedgerTransaction) { entries.add(transaction) }
    }
    private class EmptyInvoices : InvoiceStore {
        override fun getInvoices() = emptyList<Invoice>()
        override fun saveInvoice(invoice: Invoice) = Unit
        override fun deleteInvoice(id: String) = Unit
    }
}

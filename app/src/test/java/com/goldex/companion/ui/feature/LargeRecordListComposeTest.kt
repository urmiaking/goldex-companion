package com.goldex.companion.ui.feature

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.*
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.customers.CustomerLedgerScreen
import com.goldex.companion.ui.customers.CustomerStatementScreen
import com.goldex.companion.ui.inventory.InventoryScreen
import com.goldex.companion.ui.inventory.InventoryUiState
import com.goldex.companion.ui.invoices.*
import com.goldex.companion.ui.main.MainDestinationViewport
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class LargeRecordListComposeTest {
    @get:Rule val compose = createComposeRule()

    private fun invoices() = (0 until 2000).map { InvoiceListItem(id = "invoice-$it", invoiceNumber = "$it",
        customerName = "customer-$it", customerInitials = "م", createdAtText = "", itemsSummary = "invoice-item-$it",
        itemsCountText = "", line1Detail = "", line2Detail = "", finalAmount = 1000L) }

    private fun assertLastRowIsLazy(tag: String, text: String, index: Int) {
        compose.onNodeWithText(text).assertDoesNotExist()
        compose.onNodeWithTag(tag).performScrollToIndex(index)
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    @Test fun invoiceListComposesOnlyVisibleRowsAndCanReachRecordTwoThousand() {
        val state = BarterInvoiceUiState(invoicesList = invoices())
        compose.setContent { GoldExCompanionTheme { InvoicesManagementScreen(state, {}, {}, {}, {}, {}, {}) } }
        assertLastRowIsLazy("invoice-list", "invoice-item-1999", 2002)
    }

    @Test fun customerListComposesOnlyVisibleRowsAndCanReachRecordTwoThousand() {
        val state = CustomerManagerUiState(customerList = (0 until 2000).map { Customer(id = "$it", name = "customer-$it") })
        compose.setContent { GoldExCompanionTheme { CustomerLedgerScreen(state, {}, {}, {}, {}, {}, {}) } }
        assertLastRowIsLazy("customer-list", "customer-1999", 2002)
    }

    @Test fun statementComposesOnlyVisibleRowsAndCanReachRecordTwoThousand() {
        val customer = Customer(id = "c", name = "مشتری")
        val transactions = (0 until 2000).map { LedgerTransaction(id = "$it", customerId = "c", title = "transaction-$it") }
        compose.setContent { GoldExCompanionTheme { CustomerStatementScreen(customer, transactions,
            selectedFilter = StatementFilterTab.ALL, onFilterSelect = {}, onOpenAddEntry = {}, onBack = {}) } }
        assertLastRowIsLazy("statement-list", "transaction-1999", 2002)
    }

    @Test fun inventoryComposesOnlyVisibleRowsAndCanReachRecordTwoThousand() {
        val state = InventoryUiState(items = (0 until 2000).map { InventoryItem(id = "$it", code = "code-$it", title = "inventory-$it") })
        compose.setContent { GoldExCompanionTheme { InventoryScreen(state, MarketRates(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        assertLastRowIsLazy("inventory-list", "inventory-1999", 2003)
    }

    @Test fun outgoingInvoiceListKeepsFiniteConstraintsDuringTabAnimation() {
        val destination = mutableStateOf(AppTab.INVOICES)
        val state = BarterInvoiceUiState(invoicesList = invoices())
        compose.setContent {
            GoldExCompanionTheme {
                val scroll = rememberScrollState()
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(targetState = destination.value, modifier = Modifier.fillMaxSize(), label = "tab-test") { tab ->
                        MainDestinationViewport(tab, scroll, MarketRates()) {
                            if (tab == AppTab.INVOICES) InvoicesManagementScreen(state, {}, {}, {}, {}, {}, {})
                            else Text("home-destination")
                        }
                    }
                }
            }
        }
        compose.runOnIdle { destination.value = AppTab.HOME }
        compose.waitForIdle()
        compose.onNodeWithText("home-destination").assertIsDisplayed()
        compose.runOnIdle { destination.value = AppTab.INVOICES }
        compose.waitForIdle()
        compose.onNodeWithTag("invoice-list").assertIsDisplayed()
    }
}

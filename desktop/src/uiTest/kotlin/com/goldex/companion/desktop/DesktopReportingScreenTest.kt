package com.goldex.companion.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.domain.reporting.ReportingPeriod
import com.goldex.companion.model.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.*
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopReportingScreenTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace

    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        val market = DesktopMarketRepository(store, fetch = { error("offline test") })
        market.useManual(DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000, coinEmami = 60_000_000, goldMelt = 25_991_100))
        workspace = DesktopWorkspace(store, market)
    }

    @After fun cleanup() {
        workspace.close()
        store.close()
    }

    private fun render() = compose.setContent {
        val state by workspace.state.collectAsState()
        GoldExCompanionTheme(isDarkTheme = state.dark) {
            DesktopWorkspaceScreen(workspace, {}, "0.56.49")
        }
    }

    @Test
    fun reportingLoadsRealStoreRecordsAndComputesKpis() {
        // Seed inventory
        store.inventory.addItem(InventoryItem(code = "item-1", title = "دستبند کارتیه", grossWeightGrams = 10.0, karat = Karat.K18, location = "ویترین ۱"))
        store.inventory.addItem(InventoryItem(code = "item-2", title = "گردنبند ون‌کلیف", grossWeightGrams = 15.0, karat = Karat.K18, location = "گاوصندوق"))

        // Seed customer
        val customer = Customer(id = "cust-1", name = "علی محمدی", phone = "09121112233", cashDebtTomans = 5_000_000L)
        workspace.customers.repository.addCustomer(customer)

        // Seed invoice
        val now = System.currentTimeMillis()
        val invoice = BarterInvoice(
            id = "inv-1",
            invoiceNumber = "1001",
            createdAt = now,
            customer = customer,
            salesItems = listOf(
                CraftedGoldItem(
                    title = "دستبند کارتیه",
                    grossWeight = 10.0,
                    netWeight = 10.0,
                    spotPrice = 6_000_000L,
                    wageInput = 10.0,
                    wageAmount = 6_000_000.0,
                    profitPercent = 7.0,
                    profitAmount = 4_620_000.0,
                    taxPercent = 9.0,
                    taxAmount = 955_800.0,
                    rawGoldValue = 60_000_000.0,
                    totalPayable = 71_575_800.0,
                    equivalent18kWeight = 10.0
                )
            )
        )
        workspace.customers.invoices.saveBarterInvoice(invoice)

        workspace.reporting.loadData()

        compose.waitUntil(10_000) { !workspace.reporting.uiState.value.isLoading }
        val state = workspace.reporting.uiState.value

        assertEquals(25.0, state.goldInventory.totalWeight18k)
        assertEquals(2, state.goldInventory.piecesCount)
        assertEquals(5_000_000L, state.debtorsCreditors.totalReceivablesTomans)
        assertEquals(1, state.debtorsCreditors.debtorCount)
        assertTrue(state.salesPerformance.totalProfitTomans > 0)
    }

    @Test
    fun reportingPageRendersHeroKpisAndNavigatesToBreakdownDetails() {
        // Seed inventory
        store.inventory.addItem(InventoryItem(code = "item-1", title = "انگشتر فیوژن", grossWeightGrams = 8.5, karat = Karat.K18))

        render()

        // Navigate to REPORTING destination via sidebar
        compose.onNodeWithTag("nav-REPORTING").performClick()
        compose.waitUntil(10_000) { workspace.state.value.destination == DesktopDestination.REPORTING }

        // Verify Hero KPI card and 4 modules
        compose.onNodeWithTag("hero-kpi-card").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("module-sales").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("module-inventory").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("module-debtors").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("module-vat").assertExists().assertIsDisplayed()

        // Click on Sales module to drill down
        compose.onNodeWithTag("module-sales").performClick()
        compose.runOnIdle {
            assertEquals(ReportingBreakdownType.SALES_PERFORMANCE, workspace.reporting.uiState.value.activeBreakdown)
        }
        compose.onNodeWithTag("sales-categories-card").assertExists()

        // Header back button returns to overview
        compose.onNodeWithTag("reporting-back").performClick()
        compose.runOnIdle {
            assertEquals(null, workspace.reporting.uiState.value.activeBreakdown)
        }
        compose.onNodeWithTag("hero-kpi-card").assertExists()

        // Click on VAT module
        compose.onNodeWithTag("module-vat").performClick()
        compose.onNodeWithTag("vat-notice-card").assertExists()

        // Back to overview
        compose.onNodeWithTag("reporting-back").performClick()
        compose.runOnIdle {
            assertEquals(null, workspace.reporting.uiState.value.activeBreakdown)
        }
    }

    @Test
    fun periodSelectionAndCopyOverallSummary() {
        render()
        compose.onNodeWithTag("nav-REPORTING").performClick()

        // Select period THIS_WEEK
        compose.onNodeWithText("این هفته").performClick()
        compose.runOnIdle {
            assertEquals(ReportingPeriod.THIS_WEEK, workspace.reporting.uiState.value.selectedPeriod)
        }

        // Copy summary action
        compose.onNodeWithTag("reporting-copy-overall").performClick()
        compose.onNodeWithText("کپی شد ✓").assertExists()
    }
}

package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.domain.invoice.SaveBarterInvoiceUseCase
import com.goldex.companion.model.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopInvoicesScreenTest {
    @get:Rule val temporary = TemporaryFolder()
    private val customer = Customer(id = "synthetic", name = "مشتری فاکتور آزمون", phone = "09123456789")
    private fun item() = BankCoinItem(id = "synthetic-coin", title = "سکه بانکی آزمون", unitPrice = 75_000_000,
        totalPayable = 75_000_000.0, equivalent18kWeight = 9.759)
    private fun withWorkspace(action: (DesktopWorkspace) -> Unit) {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val customers = DesktopCustomerStore(store); customers.addCustomer(customer)
            SaveBarterInvoiceUseCase(DesktopCustomerInvoiceStore(store, customers), customers, customers)
                .save(BarterInvoice(id = "synthetic-invoice", invoiceNumber = "14051234", customer = customer,
                    spotPrice18k = 6_000_000, debtBasis = InvoiceDebtBasis.CASH, salesItems = listOf(item())))
            val workspace = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline test") }), isLicensedOverride = { true })
            try { action(workspace) } finally { workspace.close() }
        }
    }

    @Test fun headerMatchesCustomersAndRealInvoiceSearchPreviewAndDeletionWork() = withWorkspace { workspace ->
        workspace.navigate(DesktopDestination.INVOICES)
        runDesktopComposeUiTest(width = 1440, height = 1000) {
            setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "test") } }
            waitUntil(10000) { workspace.invoices.manager.uiState.value.invoicesList.size == 1 && !workspace.invoices.manager.uiState.value.isLoading }
            onNode(hasText("مدیریت فاکتورها") and hasTestTag("workspace-heading")).assertExists()
            onNodeWithText("مدیریت و بایگانی فاکتورها").assertDoesNotExist()
            val action = onNodeWithTag("invoices-new-btn").fetchSemanticsNode().boundsInRoot
            val calendar = onNodeWithTag("workspace-date").fetchSemanticsNode().boundsInRoot
            val status = onNodeWithTag("connection-chip").fetchSemanticsNode().boundsInRoot
            assertEquals(calendar.center.y, action.center.y)
            assertEquals(status.height, action.height)
            screenshot("invoices-list-light.png")
            onNodeWithTag("invoices-search").performTextReplacement("۱۴۰۵۱۲۳۴")
            onNodeWithTag("invoice-card-synthetic-invoice").assertIsDisplayed()
            onNodeWithTag("invoices-search").performTextReplacement("نام ناموجود")
            onNodeWithTag("invoice-empty").assertIsDisplayed()
            onNodeWithTag("invoices-search").performTextReplacement("")
            onNodeWithTag("invoice-preview-synthetic-invoice").performClick()
            onNodeWithTag("invoice-pdf-preview").assertIsDisplayed()
            waitUntil(10000) { onAllNodesWithTag("invoice-export").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithContentDescription("بستن پیش‌نمایش").performClick()
            runOnIdle { workspace.toggleTheme() }
            screenshot("invoices-list-dark.png")
            onNodeWithTag("invoice-delete-synthetic-invoice").performClick()
            onNodeWithTag("invoice-delete-final").performClick()
            waitUntil(10000) { workspace.invoices.manager.uiState.value.invoicesList.isEmpty() && !workspace.invoices.manager.uiState.value.isLoading }
            onNodeWithTag("invoice-empty").assertIsDisplayed()
            assertEquals(0L, workspace.customers.repository.getCustomers().single().cashDebtTomans)
        }
    }

    @Test fun invoiceCanBeIssuedFromCustomerAndDraftSurvivesPageNavigation() = withWorkspace { workspace ->
        workspace.navigate(DesktopDestination.CUSTOMERS)
        runDesktopComposeUiTest(width = 1100, height = 1000) {
            setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "test") } }
            waitUntil(10000) { workspace.customers.manager.uiState.value.customerList.isNotEmpty() && !workspace.invoices.manager.uiState.value.isLoading }
            onNodeWithTag("customer-invoice-synthetic").performClick()
            onNodeWithTag("invoice-editor").assertIsDisplayed()
            assertEquals(customer.id, workspace.invoices.manager.uiState.value.invoice.customer?.id)
            runOnIdle {
                workspace.invoices.manager.updateSpotPrice(6_000_000)
                workspace.invoices.manager.setDebtBasis(InvoiceDebtBasis.CASH)
                workspace.invoices.manager.addSalesItem(item())
            }
            screenshot("invoices-editor-light.png")
            onNodeWithTag("nav-RATES").performClick()
            onNodeWithTag("nav-INVOICES").performClick()
            onNodeWithTag("invoice-editor").assertIsDisplayed()
            assertEquals(1, workspace.invoices.manager.uiState.value.invoice.salesItems.size)
            onNodeWithText("ثبت نهایی و صدور فاکتور رسمی تهاتر").performScrollTo().performClick()
            waitUntil(10000) { workspace.invoices.manager.uiState.value.invoicesList.size == 2 && !workspace.invoices.manager.uiState.value.isLoading }
            onNodeWithTag("invoices-table").assertIsDisplayed()
        }
    }

    @Test fun itemAndPaymentFormsExposeAndroidCategoriesAndRemainBounded() = withWorkspace { workspace ->
        workspace.navigate(DesktopDestination.INVOICES)
        runDesktopComposeUiTest(width = 1100, height = 1000) {
            setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "test") } }
            waitUntil(10000) { workspace.invoices.manager.uiState.value.invoicesList.isNotEmpty() && !workspace.invoices.manager.uiState.value.isLoading }
            onNodeWithTag("invoices-new-btn").performClick()
            runOnIdle { workspace.invoices.manager.updateSpotPrice(6_000_000); workspace.invoices.manager.openAddItemModal(InvoiceItemCategory.CRAFTED, true) }
            onNodeWithText("طلای ساخته").assertExists()
            onNodeWithTag("invoice-item-save").assertIsNotEnabled()
            onNode(hasSetTextAction() and hasText("وزن ناخالص")).performTextReplacement("۲٫۵۰۰")
            onNodeWithTag("invoice-item-save").assertIsEnabled().performClick()
            waitUntil(10000) { !workspace.invoices.manager.uiState.value.isItemModalVisible }
            assertEquals(2.5, (workspace.invoices.manager.uiState.value.invoice.salesItems.single() as CraftedGoldItem).netWeight)
            runOnIdle { workspace.invoices.manager.openEditItemModal(workspace.invoices.manager.uiState.value.invoice.salesItems.single(), true) }
            screenshot("invoice-item-form.png")
            runOnIdle { workspace.invoices.manager.closeItemModal() }
            runOnIdle { workspace.invoices.manager.addSalesItem(item()) }
            onNodeWithText("ثبت پرداخت").performScrollTo().performClick()
            screenshot("invoice-payment-form.png")
        }
    }

    private fun ComposeUiTest.screenshot(name: String) {
        val roots = onAllNodes(isRoot())
        val bitmap = roots[roots.fetchSemanticsNodes().lastIndex].captureToImage().asSkiaBitmap()
        val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val encoded = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        try { File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }.resolve(name).writeBytes(encoded.bytes) }
        finally { encoded.close(); image.close(); bitmap.close() }
    }
}

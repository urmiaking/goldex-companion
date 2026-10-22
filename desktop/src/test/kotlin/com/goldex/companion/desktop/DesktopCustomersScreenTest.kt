package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.*
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class DesktopCustomersScreenTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        workspace = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline test") }), isLicensedOverride = { true })
    }
    @After fun cleanup() { workspace.close(); store.close() }
    private fun render() = compose.setContent {
        val state by workspace.state.collectAsState()
        GoldExCompanionTheme(isDarkTheme = state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.60") }
    }
    private fun field(tag: String) = compose.onNode(hasSetTextAction() and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag))))

    @Test fun customerCanBeCreatedWithPersianContactInformationAndSearched() {
        render()
        compose.onNodeWithTag("nav-CUSTOMERS").performClick()
        compose.onNodeWithText("مشتری جدید").performClick()
        compose.onNodeWithText("همکار و بنکدار").assertDoesNotExist()
        compose.onNodeWithTag("customer-role-selector").assertDoesNotExist()
        field("customer-name").performTextReplacement("مشتری آزمون")
        field("customer-phone").performTextReplacement("۰۹۱۲۳۴۵۶۷۸۹")
        screenshot("customers-add-light.png", "customer-form")
        compose.onNodeWithText("ثبت و انتخاب").performScrollTo().performClick()
        compose.waitUntil(10_000) { workspace.customers.manager.uiState.value.customerList.size == 1 && !workspace.customers.hasDialog }
        compose.onNodeWithText("مشتری آزمون").assertExists()
        field("customer-search").performTextReplacement("نام ناموجود")
        compose.onNodeWithText("طرف‌حسابی با این مشخصات یافت نشد").assertExists()
        field("customer-search").performTextReplacement("")
        compose.runOnIdle {
            assertEquals("09123456789", workspace.customers.repository.getCustomers().single().phone)
            assertEquals(Customer(name = "expected").role, workspace.customers.repository.getCustomers().single().role)
        }
    }

    @Test fun ledgerAndStatementUseWorkspaceHeaderInBothThemesAndNavigation() {
        val contact = Customer(id = "synthetic", name = "بنکدار آزمون", goldDebtGrams = 2.125, cashDebtTomans = -12_500_000, accountCode = "1042")
        workspace.customers.repository.addCustomer(contact)
        workspace.customers.repository.addTransaction(LedgerTransaction(id = "gold", customerId = contact.id,
            title = "پرداخت آبشده آزمون", documentNumber = "1042", dateTime = "۱۴۰۵/۰۷/۱۹", direction = LedgerDirection.PAY,
            scaleWeightGrams = 2.125, equivalent750WeightGrams = 2.125, timestamp = 1))
        workspace.customers.manager.loadCustomers()
        render()
        compose.onNodeWithTag("nav-CUSTOMERS").performClick()
        compose.waitUntil(10_000) { workspace.customers.manager.uiState.value.customerList.size == 1 }
        screenshot("customers-ledger-light.png")
        compose.runOnIdle { workspace.customers.manager.openCustomerStatement(contact) }
        compose.waitUntil(10_000) { workspace.customers.manager.uiState.value.activeCustomerTransactions.isNotEmpty() }
        compose.onNode(hasText(contact.name) and hasTestTag("workspace-heading")).assertExists()
        compose.onNodeWithTag("customer-payment").assertIsDisplayed()
        compose.onNodeWithTag("customer-independent-entry").assertIsDisplayed()
        compose.onNodeWithContentDescription("اشتراک‌گذاری").assertDoesNotExist()
        screenshot("customers-statement-light.png")
        compose.runOnIdle { workspace.toggleTheme() }
        compose.waitUntil(10_000) { workspace.state.value.dark }
        screenshot("customers-statement-dark.png")
        compose.onNodeWithTag("nav-RATES").performClick()
        compose.onNodeWithTag("nav-CUSTOMERS").performClick()
        compose.runOnIdle { assertEquals(contact.id, workspace.customers.manager.uiState.value.selectedCustomerForStatement!!.id) }
        compose.onNodeWithTag("customer-back").performClick()
        compose.onNodeWithTag("customer-add").assertIsDisplayed()
        screenshot("customers-ledger-dark.png")
    }

    @Test fun settlementShowsManualRatePreviewAndKeepsCancelOnRight() {
        val contact = Customer(id = "debt", name = "حساب آزمون", goldDebtGrams = 2.0)
        workspace.customers.repository.addCustomer(contact)
        render()
        compose.onNodeWithTag("nav-CUSTOMERS").performClick()
        compose.runOnIdle { workspace.customers.settlement.open(contact) }
        compose.waitUntil(10_000) { !workspace.customers.settlement.state.value.isLoading }
        field("settlementRate").performTextReplacement("۶۰۰۰۰۰۰")
        field("settlementAmount").performTextReplacement("۱۲۰۰۰۰۰۰")
        screenshot("customers-settlement-light.png", "settlement-dialog")
        val cancel = compose.onNodeWithText("انصراف").fetchSemanticsNode().boundsInRoot
        val confirm = compose.onNodeWithTag("confirmSettlement").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue(cancel.center.x > confirm.center.x)
        compose.onNodeWithTag("confirmSettlement").performClick()
        compose.waitUntil(10_000) { workspace.customers.settlement.state.value.customer == null }
        compose.runOnIdle { assertEquals(0.0, workspace.customers.repository.getCustomers().single().goldDebtGrams) }
    }

    @Test fun independentGoldReceiptAcceptsPersianDecimalWeightAndKeepsResultAfterReopen() {
        val contact = Customer(id = "manual", name = "حساب مستقل آزمون")
        workspace.customers.repository.addCustomer(contact)
        render()
        compose.onNodeWithTag("nav-CUSTOMERS").performClick()
        compose.runOnIdle { workspace.customers.manager.openAddLedgerEntry(contact) }
        compose.onNode(hasSetTextAction() and hasText("وزن ترازو")).performTextReplacement("۲٫۱۲۵")
        compose.onNode(hasSetTextAction() and hasText("عیار ری‌گیری")).performTextReplacement("۷۵۰")
        screenshot("customers-entry-light.png", "ledger-dialog")
        compose.onNodeWithTag("ledger-save").performClick()
        compose.waitUntil(10_000) { !workspace.customers.manager.uiState.value.isAddLedgerEntryModalVisible }
        compose.runOnIdle {
            val saved = workspace.customers.repository.getTransactions(contact.id).single()
            assertEquals(2.125, saved.equivalent750WeightGrams)
            assertEquals(-2.125, workspace.customers.repository.getCustomers().single().goldDebtGrams)
            workspace.customers.manager.openCustomerStatement(workspace.customers.repository.getCustomers().single())
        }
        compose.waitUntil(10_000) { workspace.customers.manager.uiState.value.activeCustomerTransactions.size == 1 }
    }

    @Test fun customerMetadataCanBeEditedAndUnsettledDeletionShowsErrorInConfirmation() {
        val contact = Customer(id = "edit", name = "مشتری اولیه", goldDebtGrams = 1.0, role = "همکار و بنکدار")
        workspace.customers.repository.addCustomer(contact)
        workspace.customers.manager.loadCustomers()
        render()
        compose.onNodeWithTag("nav-CUSTOMERS").performClick()
        compose.waitUntil(10_000) { workspace.customers.manager.uiState.value.customerList.size == 1 }
        compose.onNodeWithTag("customer-edit-edit").performClick()
        compose.onNodeWithTag("customer-role-selector").assertDoesNotExist()
        field("customer-name").performTextReplacement("مشتری ویرایش‌شده")
        compose.onNodeWithText("ذخیره تغییرات").performScrollTo().performClick()
        compose.waitUntil(10_000) { workspace.customers.repository.getCustomers().single().name == "مشتری ویرایش‌شده" && !workspace.customers.hasDialog }
        compose.runOnIdle { assertEquals(contact.role, workspace.customers.repository.getCustomers().single().role) }
        compose.onNodeWithTag("customer-delete-edit").performClick()
        compose.onNodeWithTag("customer-delete-final").assertIsNotEnabled()
        compose.onNode(hasText("پیش از حذف طرف‌حساب، مانده حساب را تسویه کنید") and hasAnyAncestor(hasTestTag("customer-delete-confirmation"))).assertExists()
        compose.runOnIdle { assertEquals(1.0, workspace.customers.repository.getCustomers().single().goldDebtGrams) }
        compose.onNodeWithTag("customer-delete-cancel").performClick()
        compose.onNodeWithTag("customer-delete-confirmation").assertDoesNotExist()
    }

    @Test fun settledDeletionRequiresConfirmationAndCancelPreservesCustomer() {
        workspace.customers.repository.addCustomer(Customer(id = "settled", name = "مشتری تسویه‌شده"))
        workspace.customers.manager.loadCustomers()
        render()
        compose.onNodeWithTag("nav-CUSTOMERS").performClick()
        compose.waitUntil(10_000) { workspace.customers.manager.uiState.value.customerList.size == 1 }
        compose.onNodeWithTag("customer-delete-settled").performClick()
        val cancel = compose.onNodeWithTag("customer-delete-cancel").fetchSemanticsNode().boundsInRoot
        val delete = compose.onNodeWithTag("customer-delete-final").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue(cancel.center.x > delete.center.x)
        compose.onNodeWithTag("customer-delete-cancel").performClick()
        compose.runOnIdle { assertEquals(1, workspace.customers.repository.getCustomers().size) }
        compose.onNodeWithTag("customer-delete-settled").performClick()
        compose.onNodeWithTag("customer-delete-final").performClick()
        compose.waitUntil(10_000) { workspace.customers.manager.uiState.value.customerList.isEmpty() && !workspace.customers.hasDialog }
        compose.onNodeWithTag("customer-delete-confirmation").assertDoesNotExist()
    }

    private fun screenshot(name: String, tag: String = "workspace-root") {
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asSkiaBitmap()
        val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val encoded = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }.resolve(name).writeBytes(encoded.bytes)
        encoded.close(); image.close(); bitmap.close()
    }
}

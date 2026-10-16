package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.model.Customer
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopCustomerLayoutTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun wideWorkspaceHasThreeCardsAndOneEqualHeightSearchFilterRow() = verifyLayout(1440, 3)
    @Test fun threeColumnsAtTheirMinimumWidthKeepReadableActions() = verifyLayout(1268, 3)

    @Test fun mediumWorkspaceHasTwoColumnsAndAccessibleHeaderActions() = verifyLayout(1100, 2)
    @Test fun narrowWorkspaceStacksCardsAndToolsWithoutLosingActions() = verifyLayout(760, 1)

    private fun verifyLayout(width: Int, expectedColumns: Int) {
        val store = DesktopDataStore(temporary.newFolder().toPath())
        val workspace = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline test") }))
        try {
            val contacts = (1..3).map { id ->
                Customer(id = "layout-$id", name = if (id == 1) "گالری طلا و جواهر نام بسیار طولانی برای بررسی نمایش" else "مشتری شماره $id",
                    role = "نقش مخفی", accountCode = if(id==2) "" else "104$id", phone = "09123456789",
                    cashDebtTomans = if(id==3) 0L else 123_456_789_012L, goldDebtGrams = if(id==3) 0.0 else 12345.678)
            }
            contacts.forEach(workspace.customers.repository::addCustomer)
            workspace.customers.manager.loadCustomers()
            workspace.navigate(DesktopDestination.CUSTOMERS)
            runDesktopComposeUiTest(width = width, height = 1000) {
                setContent {
                    val state by workspace.state.collectAsState()
                    GoldExCompanionTheme(isDarkTheme = state.dark) { DesktopWorkspaceScreen(workspace, {}, "test") }
                }
                waitUntil(10000) { workspace.customers.manager.uiState.value.customerList.size == 3 }
                onNodeWithTag("customer-add").assertIsDisplayed()
                onNodeWithTag("customer-invoice-layout-1").assertIsNotEnabled()
                onNodeWithTag("customer-contact-layout-1").assertTextEquals("۰۹۱۲۳۴۵۶۷۸۹ (۱۰۴۱)")
                onNodeWithTag("customer-contact-layout-2").assertTextEquals("۰۹۱۲۳۴۵۶۷۸۹")
                onNodeWithText("نقش مخفی").assertDoesNotExist()
                onNodeWithText("امروز").assertDoesNotExist()
                val statusId=if(expectedColumns>=2) 3 else 1
                val name=onNodeWithTag("customer-name-layout-$statusId").fetchSemanticsNode().boundsInRoot
                val status=onNodeWithTag("customer-status-layout-$statusId").fetchSemanticsNode().boundsInRoot
                assertEquals(name.center.y,status.center.y)
                assertTrue(status.right<=name.left)
                onNodeWithText("بدون کد حساب").assertDoesNotExist()
                for ((tag,label) in listOf("statement" to "ریز گردش","entry" to "ثبت دریافت/پرداخت","invoice" to "ثبت فاکتور")) {
                    val layouts=mutableListOf<TextLayoutResult>()
                    onNode(hasText(label) and hasAnyAncestor(hasTestTag("customer-$tag-layout-1")),useUnmergedTree=true)
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertTrue(layouts.isNotEmpty())
                    assertTrue(layouts.all { it.lineCount==1 && !it.isLineEllipsized(0) &&
                        it.getLineRight(0)-it.getLineLeft(0)<=it.size.width+1f && !it.didOverflowHeight },"$label must fit at $width")
                }
                val gold = onNodeWithTag("customer-gold-layout-1").fetchSemanticsNode().boundsInRoot
                val cash = onNodeWithTag("customer-cash-layout-1").fetchSemanticsNode().boundsInRoot
                assertEquals(gold.center.y, cash.center.y)
                assertTrue(gold.left > cash.right)
                val first = onNodeWithTag("customer-card-layout-1").fetchSemanticsNode().boundsInRoot
                val second = onNodeWithTag("customer-card-layout-2").fetchSemanticsNode().boundsInRoot
                if (expectedColumns >= 2) assertEquals(first.top, second.top)
                else assertTrue(second.top > first.top)
                if (expectedColumns >= 2) {
                    val third = onNodeWithTag("customer-card-layout-3").fetchSemanticsNode().boundsInRoot
                    if (expectedColumns == 3) assertEquals(first.top, third.top) else assertTrue(third.top > first.top)
                }
                val search = onNodeWithTag("customer-search-container").fetchSemanticsNode().boundsInRoot
                val filters = onNodeWithTag("customer-filters").fetchSemanticsNode().boundsInRoot
                assertEquals(search.height, filters.height)
                if (expectedColumns == 3) {
                    assertEquals(search.top, filters.top)
                    val toolbar = onNodeWithTag("customer-toolbar").fetchSemanticsNode().boundsInRoot
                    assertEquals(toolbar.left, filters.left)
                    assertEquals(toolbar.right, search.right)
                    assertEquals(12f, search.left - filters.right, .5f)
                    val lastFilter = onNode(hasText("تسویه‌شده") and hasAnyAncestor(hasTestTag("customer-filters"))).fetchSemanticsNode().boundsInRoot
                    assertTrue(lastFilter.left - filters.left < 15f, "Filter group must not reserve blank width away from search")
                }
                save("customers-layout-$width-light.png")
                onNodeWithTag("customer-statement-layout-1").performClick()
                onNodeWithTag("customer-payment").assertIsDisplayed()
                onNodeWithTag("customer-independent-entry").assertIsDisplayed()
                onNodeWithTag("customer-back").assertIsDisplayed()
                val back = onNodeWithTag("customer-back").fetchSemanticsNode().boundsInRoot
                val payment = onNodeWithTag("customer-payment").fetchSemanticsNode().boundsInRoot
                val independent = onNodeWithTag("customer-independent-entry").fetchSemanticsNode().boundsInRoot
                assertEquals(back.center.y, payment.center.y)
                assertEquals(back.center.y, independent.center.y)
                onNodeWithTag("customer-statement-summary").assertIsDisplayed()
                onNodeWithTag("customer-back").performClick()
                runOnIdle { workspace.toggleTheme() }
                save("customers-layout-$width-dark.png")
            }
        } finally { workspace.close(); store.close() }
    }

    private fun ComposeUiTest.save(name: String) {
        val bitmap = onNodeWithTag("workspace-root").captureToImage().asSkiaBitmap()
        val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val encoded = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }.resolve(name).writeBytes(encoded.bytes)
        encoded.close(); image.close(); bitmap.close()
    }
}


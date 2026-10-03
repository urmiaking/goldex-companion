package com.goldex.companion.ui.invoices

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.InvoiceStatus
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class InvoiceCardSettlementTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unsettledInvoiceCardRendersRemainingRowAndSettleButton() {
        var settleClicked = false
        val customer = Customer(id = "c1", name = "علی محمدی")
        val invoice = BarterInvoice(
            id = "inv-1",
            invoiceNumber = "IR-101",
            customer = customer,
            syncWithLedger = true
        )
        val item = InvoiceListItem(
            id = "inv-1",
            invoiceNumber = "101",
            customerName = "علی محمدی",
            customerInitials = "عم",
            createdAtText = "کد فاکتور: 101 • همین الان",
            status = InvoiceStatus.PARTIALLY_PAID,
            statusDetail = "تسویه نشده",
            itemsSummary = "طلای ۱۸ عیار",
            itemsCountText = "اقلام فاکتور (۱ قلم):",
            line1Detail = "وزن کل: ۱۰.۰۰۰ گرم",
            line2Detail = "نسیه / حساب دفتری",
            finalAmount = 50_000_000L,
            remainingDetail = "۵۰,۰۰۰,۰۰۰ تومان",
            barterInvoice = invoice
        )

        compose.setContent {
            GoldExCompanionTheme(isDarkTheme = true) {
                InvoiceTransactionCard(
                    item = item,
                    onCardClick = {},
                    onPrintClick = {},
                    onDeleteClick = {},
                    onSettleClick = { settleClicked = true }
                )
            }
        }

        // Verify remaining row is displayed
        compose.onNodeWithText("مانده فاکتور:").assertExists()
        compose.onNodeWithText("۵۰,۰۰۰,۰۰۰ تومان").assertExists()

        // Verify quick settle button is displayed in place of static chip
        val settleButton = compose.onNodeWithText("تسویه مانده")
        settleButton.assertExists()
        settleButton.performClick()
        assertTrue("onSettleClick callback must be invoked when clicking settle button", settleClicked)
    }
}

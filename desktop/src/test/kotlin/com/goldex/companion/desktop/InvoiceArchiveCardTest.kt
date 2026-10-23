package com.goldex.companion.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.goldex.companion.desktop.ui.InvoiceArchiveCard
import com.goldex.companion.model.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Test
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class InvoiceArchiveCardTest {
    private fun item() = InvoiceListItem(id = "fixture", invoiceNumber = "14051234",
        customerName = "مشتری آزمایشی با نام بلند", customerInitials = "م آ", createdAtText = "۱۴۰۵/۰۸/۰۱",
        status = InvoiceStatus.PARTIALLY_PAID, itemsSummary = "دستبند آزمایشی", itemsCountText = "۱ قلم",
        line1Detail = "وزن کل: ۱۲٫۳۴۵ گرم", line2Detail = "", finalAmount = 512_835_707,
        remainingDetail = "۱٫۴۰۷ گرم طلا", barterInvoice = BarterInvoice(id = "fixture"))

    @Test fun fiveActionsStayVisibleAndFourBodyButtonsShareOneRtlRowAtMinimumCardWidth() = runDesktopComposeUiTest(width = 458, height = 390) {
        var busy by mutableStateOf(false)
        var settled by mutableStateOf(false)
        var direction by mutableStateOf(LayoutDirection.Rtl)
        var dark by mutableStateOf(false)
        val events = mutableListOf<String>()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                GoldExCompanionTheme(dark) {
                    Box(Modifier.fillMaxSize().padding(14.dp)) {
                        InvoiceArchiveCard(item().let { if (settled) it.copy(status = InvoiceStatus.SETTLED) else it }, busy,
                            { events.add("view") }, { events.add("details") }, { events.add("delete") },
                            { events.add("settle") }, { events.add("print") }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        val tags = listOf("invoice-open-fixture", "invoice-delete-fixture", "invoice-settle-fixture", "invoice-print-fixture")
        val buttons = tags.map { onNodeWithTag(it).assertIsDisplayed().assertIsEnabled().fetchSemanticsNode().boundsInRoot }
        val card = onNodeWithTag("invoice-card-fixture").fetchSemanticsNode().boundsInRoot
        assertTrue(buttons.all { it.top == buttons.first().top && it.bottom == buttons.first().bottom })
        assertTrue(buttons.all { it.left >= card.left && it.right <= card.right })
        assertTrue(buttons.zipWithNext().all { (right, left) -> right.left > left.right })
        for (label in listOf("جزئیات", "حذف", "تسویه", "چاپ")) {
            val layouts = mutableListOf<TextLayoutResult>()
            onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            // As in WindowsControlContractTest, Skiko's RTL paragraph width can exceed a wrap-content Text.
            // Validate actual glyph advance and available button space instead of that paragraph flag.
            val advance = layout.getLineRight(0) - layout.getLineLeft(0)
            assertTrue(layout.lineCount == 1 && !layout.isLineEllipsized(0) && !layout.didOverflowHeight)
            assertTrue(advance <= layout.size.width + 1f, "$label must remain complete")
            assertTrue(advance + 20f + 6f + 24f <= buttons.first().width + 1f, "$label and icon must fit without clipping")
        }
        val view = onNodeWithTag("invoice-preview-fixture").fetchSemanticsNode().boundsInRoot
        assertTrue(view.bottom < buttons.first().top)
        val label = onNodeWithTag("desktop-navigation-label", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val chevron = onNodeWithTag("desktop-navigation-icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(chevron.right <= label.left, "RTL chevron follows the label at visual left")
        screenshot("invoice-card-compact-light")
        onNodeWithTag("invoice-preview-fixture").performClick()
        tags.forEach { onNodeWithTag(it).performClick() }
        assertEquals(listOf("view", "details", "delete", "settle", "print"), events)
        runOnIdle { busy = true }
        (tags + "invoice-preview-fixture").forEach { onNodeWithTag(it).assertIsNotEnabled() }
        runOnIdle { busy = false; settled = true; dark = true }
        onNodeWithTag("invoice-settle-fixture").assertIsDisplayed().assertIsNotEnabled()
        assertEquals(buttons.first().top, onNodeWithTag("invoice-open-fixture").fetchSemanticsNode().boundsInRoot.top)
        screenshot("invoice-card-compact-dark-settled")
        runOnIdle { direction = LayoutDirection.Ltr }
        val ltrLabel = onNodeWithTag("desktop-navigation-label", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val ltrChevron = onNodeWithTag("desktop-navigation-icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(ltrChevron.left >= ltrLabel.right)
    }

    private fun ComposeUiTest.screenshot(name: String) {
        val bitmap = onNodeWithTag("invoice-card-fixture").captureToImage()
        val skia = bitmap.asSkiaBitmap()
        val image = org.jetbrains.skia.Image.makeFromBitmap(skia)
        try {
            image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.use { data ->
                File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }.resolve("$name.png").writeBytes(data.bytes)
            }
        } finally { image.close(); skia.close() }
    }
}


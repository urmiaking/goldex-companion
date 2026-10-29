package com.goldex.companion.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.LayoutDirection
import com.goldex.companion.desktop.ui.DesktopField
import com.goldex.companion.desktop.ui.programErrorText
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.presentation.inventory.InventoryDraft
import com.goldex.companion.presentation.inventory.InventoryForm
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopCopyScreenTest {
    @Test fun sharedValidationCopyIsCleanWhileCustomerInputIsUntouched() = runDesktopComposeUiTest(width = 500, height = 250) {
        val entered = "سکه\u0654 آزمون \u0621"
        val sharedError = InventoryForm.validate(InventoryDraft(category = InventoryCategory.ALL))["category"]!!
        assertTrue(sharedError.contains('\u0654'))
        assertEquals("محدوده مجاز", programErrorText("محدوده\u0654 مجاز"))
        assertEquals("مظنه طلا", programErrorText("مظنه\u0621 طلا"))
        setContent {
            GoldExCompanionTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DesktopField(entered, {}, "نام کالا", error = sharedError)
                }
            }
        }
        onNodeWithText("دسته کالا را انتخاب کنید").assertExists()
        assertEquals(entered, onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }
}

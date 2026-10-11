package com.goldex.companion.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.ui.controls.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class WindowsControlContractTest {
    @Test fun selectedOverflowingOptionIsVisibleWithoutShrinkingLabels() = runDesktopComposeUiTest(width = 420, height = 240) {
        val choices = (1..12).map { "موقعیت شماره $it" }
        setContent {
            GoldExCompanionTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDesktopReduceMotion provides true) {
                    DesktopFilterRow(choices, choices.last(), {}, { it }, Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
        onNodeWithText(choices.last()).assertIsSelected().assertIsDisplayed()
    }

    @Test fun filtersRemainIndependentKeyboardSelectableAndSearchCanBeCleared() = runDesktopComposeUiTest(width = 940, height = 700) {
        var choice by mutableStateOf("همه")
        var query by mutableStateOf("")
        var clicks = 0
        setContent {
            GoldExCompanionTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDesktopReduceMotion provides true) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesktopFilterRow(listOf("همه", "بدهکار", "بستانکار"), choice, { choice = it }, { it },
                            Modifier.testTag("choices"), count = { if (it == "همه") 3 else 1 })
                        DesktopSearchField(query, { query = it }, "جستجوی مشتری", inputTag = "query")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesktopActionButton("ثبت سند", { clicks++ }, Modifier.testTag("save"), Icons.Outlined.NoteAdd)
                            DesktopActionButton("انصراف", { clicks++ }, Modifier.testTag("cancel"), Icons.Outlined.Close, isSecondary = true)
                            DesktopActionButton("فاکتور", { clicks++ }, Modifier.testTag("disabled"), Icons.Outlined.ReceiptLong, enabled = false)
                        }
                    }
                }
            }
        }
        onNodeWithText("همه").assertIsSelected()
        onNodeWithText("بدهکار").performClick().assertIsSelected()
        onNodeWithText("همه").assertIsNotSelected()
        onNodeWithText("بستانکار").performSemanticsAction(SemanticsActions.RequestFocus)
        onNodeWithText("بستانکار").performKeyInput { pressKey(Key.Enter) }
        onNodeWithText("بستانکار").assertIsSelected()
        onNodeWithTag("query").performTextReplacement("آزمون")
        onNodeWithContentDescription("پاک کردن جستجو").performClick()
        runOnIdle { assertEquals("", query); assertEquals("بستانکار", choice) }
        onNodeWithTag("disabled").assertIsNotEnabled()
        for (tag in listOf("save", "cancel", "disabled")) {
            onNode(hasTestTag("desktop-action-icon") and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true).assertExists()
            val layouts = mutableListOf<TextLayoutResult>()
            onNode(hasText(if (tag == "save") "ثبت سند" else if (tag == "cancel") "انصراف" else "فاکتور"), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
            // Skiko keeps the RTL paragraph's parent width even when the Text node wraps its glyphs.
            // Compare the actual line advance, allowing one physical pixel of font rounding.
            assertTrue(layouts.all { it.lineCount == 1 && !it.isLineEllipsized(0) &&
                it.getLineRight(0) - it.getLineLeft(0) <= it.size.width + 1f && !it.didOverflowHeight },
                "$tag: ${layouts.map { "size=${it.size}, paragraph=${it.multiParagraph.width}, right=${it.getLineRight(0)}, lines=${it.lineCount}, widthOverflow=${it.didOverflowWidth}, heightOverflow=${it.didOverflowHeight}" }}")
        }
        onNodeWithTag("save").performClick()
        runOnIdle { assertEquals(1, clicks) }
    }

    /** Architectural guard for the user's durable design rule, including future/hidden feature surfaces. */
    @Test fun featureScreensCannotBypassWindowsControlContract() {
        val sources = Path.of("src/main/kotlin")
        assertTrue(Files.isDirectory(sources))
        val violations = mutableListOf<String>()
        Files.walk(sources).use { paths -> paths.filter { it.toString().endsWith(".kt") }.forEach { path ->
            val source = Files.readString(path)
            val name = path.fileName.toString()
            if (name == "DesktopCalculatorScreen.kt" || path.toString().contains("controls")) return@forEach
            if (Regex("\\b(?:Button|OutlinedButton|TextButton|FilterChip|LuxurySegmentedControl|OutlinedTextField|GoldOutlinedTextField|BasicTextField)\\s*\\(").containsMatchIn(source))
                violations += "$name contains a raw action, filter or input control"
            if (Regex("\\bGoldButton\\s*\\(").containsMatchIn(source) &&
                !source.contains("import com.goldex.companion.desktop.ui.controls.DesktopActionButton as GoldButton"))
                violations += "$name uses GoldButton without the required-icon desktop adapter"
            if (Regex("\\bIconButton\\s*\\(").containsMatchIn(source) && name != "DesktopWorkspaceHeader.kt" &&
                !source.contains("import com.goldex.companion.desktop.ui.controls.DesktopIconButton as IconButton"))
                violations += "$name uses an icon action outside the desktop body-shape adapter"
            val headerOwners = mapOf(
                "DesktopWorkspaceHeader.kt" to setOf("WorkspaceHeader"),
                "DesktopWorkspaceScreen.kt" to setOf("DesktopWorkspaceScreen", "Sidebar"),
                "DesktopCustomersPage.kt" to setOf("CustomerHeaderActions"),
                "DesktopUpdateUi.kt" to setOf("WindowsUpdateControl")
            )
            Regex("\\bWorkspaceControl\\s*\\(").findAll(source).forEach { call ->
                val before = source.take(call.range.first)
                if (!before.trimEnd().endsWith("fun")) {
                    val owner = Regex("\\bfun\\s+([A-Za-z0-9_]+)\\s*\\(")
                        .findAll(before).lastOrNull()?.groupValues?.get(1)
                    if (owner !in headerOwners[name].orEmpty())
                        violations += "$name:$owner uses a header capsule in a body feature"
                }
            }
        } }
        assertEquals(emptyList(), violations, violations.joinToString("\n"))
    }
}

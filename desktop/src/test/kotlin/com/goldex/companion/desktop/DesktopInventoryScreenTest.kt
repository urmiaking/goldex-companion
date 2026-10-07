package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asSkiaBitmap
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.model.*
import com.goldex.companion.presentation.inventory.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopInventoryScreenTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        listOf("حلقه طلای آوا" to InventoryCategory.RINGS, "دستبند نگین‌دار" to InventoryCategory.BANGLES, "نیم‌ست مهتاب" to InventoryCategory.SETS).forEachIndexed { index, (title, category) ->
            store.inventory.addItem(InventoryForm.toItem(InventoryDraft(id = "item-$index", createdAt = 1, code = "RNG-12$index", title = title, category = category, gross = "4.125", stone = "0.125", quantity = "3", location = "سینی ${index + 1} ویترین اصلی", rfid = "RFID-$index")))
        }
        val market = DesktopMarketRepository(store, fetch = { error("offline") })
        market.useManual(DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000))
        workspace = DesktopWorkspace(store, market, history = DesktopGoldHistoryGateway { error("offline") })
        workspace.navigate(DesktopDestination.INVENTORY)
    }
    @After fun cleanup() { workspace.close(); store.close() }
    @Test fun masterDetailsSearchAndBothThemes() = runDesktopComposeUiTest(width = 1400, height = 980) {
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.41") } }
        onNodeWithTag("inventory-item-item-0").performClick()
        onNodeWithTag("inventory-details").assertIsDisplayed()
        onNodeWithTag("inventory-privacy").performClick()
        waitUntil(5000) { workspace.inventory.state.value.visible }
        save(onNodeWithTag("workspace-root"), "inventory-wide-light.png")
        onNodeWithTag("workspace-theme").performClick(); waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"), "inventory-wide-dark.png")
        onNodeWithText("النگو").performClick()
        onNodeWithTag("inventory-item-item-1").assertExists(); onNodeWithTag("inventory-item-item-0").assertDoesNotExist()
        onNodeWithText("همه").performClick()
        onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("inventory-search"))).performTextInput("RFID-1")
        onNodeWithTag("inventory-item-item-1").assertExists(); onNodeWithTag("inventory-item-item-0").assertDoesNotExist()
    }
    @Test fun fullFormValidationSaveAndStockMovement() = runDesktopComposeUiTest(width = 1400, height = 980) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.41") } }
        onNodeWithTag("inventory-add").performClick()
        onNodeWithTag("inventory-save").performClick()
        onNodeWithText("نام کالا را وارد کنید؛ حداکثر ۱۵۰ حرف").assertExists()
        fun field(tag: String) = onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))
        field("inventory-title").performTextInput("گردنبند آزمایشی")
        field("inventory-gross").performTextInput("۲٫۱۲۵")
        field("inventory-stone").performTextReplacement("۰٫۱۲۵")
        save(onNodeWithTag("inventory-dialog"), "inventory-form-light.png")
        onNodeWithTag("inventory-save").performClick()
        waitUntil(5000) { workspace.inventory.state.value.draft == null && !workspace.inventory.state.value.saving }
        assertEquals(4, workspace.inventory.state.value.items.size)
        val item = workspace.inventory.state.value.selected!!
        onNodeWithTag("inventory-details").performScrollToNode(hasTestTag("inventory-deduct"))
        onNodeWithTag("inventory-deduct").performClick()
        field("movement-count").performTextReplacement("۲")
        onNodeWithTag("movement-save").performClick()
        onNodeWithText("تعداد مثبت وارد کنید؛ خروج نباید بیشتر از موجودی باشد").assertExists()
        field("movement-count").performTextReplacement("۱")
        save(onNodeWithTag("inventory-movement-dialog"), "inventory-movement.png")
        onNodeWithTag("movement-save").performClick()
        waitUntil(5000) { workspace.inventory.state.value.movement == null && !workspace.inventory.state.value.saving }
        assertEquals(0, store.inventory.getItems().first { it.id == item.id }.quantity)
        assertEquals(1, store.inventory.getAdjustments().size)
        onNodeWithTag("inventory-history").performClick(); onNodeWithTag("inventory-history-list").assertIsDisplayed()
    }
    @Test fun compactWindowHasCompleteFormAndRetainsDraftAcrossNavigation() = runDesktopComposeUiTest(width = 940, height = 700) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.41") } }
        save(onNodeWithTag("workspace-root"), "inventory-compact.png")
        onNodeWithTag("inventory-item-item-0").performClick(); onNodeWithTag("inventory-details").assertIsDisplayed()
        runOnIdle { workspace.inventory.select(null) }
        onNodeWithTag("inventory-add").performClick()
        onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("inventory-title"))).performTextInput("پیش‌نویس محفوظ")
        save(onNodeWithTag("inventory-dialog"), "inventory-form-compact.png")
        runOnIdle { workspace.navigate(DesktopDestination.RATES) }
        onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("inventory-title"))).assertTextContains("پیش‌نویس محفوظ")
        onNodeWithText("انصراف").performClick(); onNodeWithText("بستن بدون ذخیره؟").assertExists()
        onNodeWithText("ادامه ویرایش").performClick(); assertNotNull(workspace.inventory.state.value.draft)
    }
    @Test fun importedPhotoRendersAndSurvivesRemovalOfSourceFile() = runDesktopComposeUiTest(width = 1400, height = 980) {
        val file = temporary.newFile("synthetic-photo.png")
        val fixture = java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_RGB)
        fixture.createGraphics().let { g -> try { g.color = java.awt.Color.YELLOW; g.fillRect(0, 0, 200, 200) } finally { g.dispose() } }
        javax.imageio.ImageIO.write(fixture, "png", file); fixture.flush()
        workspace.inventory.open(workspace.inventory.state.value.items.first())
        runBlocking { workspace.inventory.photo(file.toPath())!!.join(); workspace.inventory.save()!!.join() }
        assertTrue(file.delete())
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.41") } }
        onAllNodesWithContentDescription("تصویر کالا")[0].assertIsDisplayed()
        save(onNodeWithTag("workspace-root"), "inventory-photo.png")
        assertTrue(InventoryPhoto.isValid(store.inventory.getItems().first().imageUrl))
    }
    private fun save(node: SemanticsNodeInteraction, name: String) {
        val bitmap = node.captureToImage().asSkiaBitmap(); val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir"), name).apply { parentFile.mkdirs(); writeBytes(data.bytes) }
        data.close(); image.close(); bitmap.close()
    }
}

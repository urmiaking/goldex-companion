package com.goldex.companion.desktop

import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.*
import com.goldex.companion.presentation.inventory.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.*

class DesktopInventoryStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun item() = InventoryForm.toItem(InventoryDraft(id = "stable", createdAt = 1, code = "RNG-123", title = "حلقهٔ آزمون", gross = "2.125", stone = "0.125", quantity = "3"))
    private fun move(type: StockAdjustmentType = StockAdjustmentType.CHARGE, count: Int = 2) = StockAdjustment(id = "movement", itemId = "stable", itemTitle = item().title, type = type, quantityChange = count, weightGrams = 4.0, timestamp = 5)
    @Test fun additiveInventoryDefaultsAndRoundTripWithSettingsAndPortfolio() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store -> assertTrue(store.inventory.getItems().isEmpty()); store.inventory.addItem(item()); store.saveInventoryVisible(true) }
        DesktopDataStore(path).use { assertEquals(listOf(item()), it.inventory.getItems()); assertTrue(it.loadInventoryVisible()) }
    }
    @Test fun stockAndHistoryCommitTogetherAndRetryIsIdempotent() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            store.inventory.addItem(item()); store.inventory.adjustStock(move()); store.inventory.adjustStock(move())
            assertEquals(5, store.inventory.getItems().single().quantity)
            assertEquals(2.125, store.inventory.getItems().single().grossWeightGrams)
            assertEquals(listOf(move()), store.inventory.getAdjustments())
        }
        DesktopDataStore(path).use { assertEquals(5, it.inventory.getItems().single().quantity); assertEquals(listOf(move()), it.inventory.getAdjustments()) }
    }
    @Test fun insufficientStockAndConflictingRetryLeaveFileUnchanged() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            store.inventory.addItem(item())
            val original = Files.readString(store.documentPath)
            assertFailsWith<IllegalArgumentException> { store.inventory.adjustStock(move(StockAdjustmentType.DEDUCT, 4)) }
            assertEquals(original, Files.readString(store.documentPath)); assertTrue(store.inventory.getAdjustments().isEmpty())
            store.inventory.adjustStock(move())
            assertFailsWith<IllegalArgumentException> { store.inventory.adjustStock(move().copy(quantityChange = 1)) }
            assertEquals(5, store.inventory.getItems().single().quantity)
        }
    }
    @Test fun failedAtomicBackupDoesNotPublishQuantityOrMovement() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            store.inventory.addItem(item()); Files.deleteIfExists(store.backupPath); Files.createDirectory(store.backupPath); Files.writeString(store.backupPath.resolve("block"), "x")
            assertFails { store.inventory.adjustStock(move()) }
            assertEquals(3, store.inventory.getItems().single().quantity); assertTrue(store.inventory.getAdjustments().isEmpty())
            assertEquals(3, JSONObject(Files.readString(store.documentPath)).getJSONArray("inventory").getJSONObject(0).getInt("quantity"))
        }
    }
    @Test fun unknownFieldsRetainedAndDeletionRetainsAuditHistory() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { it.inventory.addItem(item()) }
        val file = path.resolve("workspace.json")
        val json = JSONObject(Files.readString(file)); json.getJSONArray("inventory").getJSONObject(0).put("future", "retained"); Files.writeString(file, json.toString())
        DesktopDataStore(path).use { store ->
            store.inventory.updateItem(item().copy(title = "حلقهٔ تازه"))
            assertEquals("retained", JSONObject(Files.readString(file)).getJSONArray("inventory").getJSONObject(0).getString("future"))
            store.inventory.adjustStock(move().copy(itemTitle = "حلقهٔ تازه")); store.inventory.deleteItem("stable")
            assertTrue(store.inventory.getItems().isEmpty()); assertEquals(1, store.inventory.getAdjustments().size)
        }
    }
    @Test fun malformedRecordsNeverDisappearThroughLenientCodec() {
        for ((field, bad) in listOf("quantity" to "bogus", "customKaratValue" to "bad", "grossWeightGrams" to "bad", "category" to "FUTURE", "imageUrl" to "not-a-photo")) {
            val path = temporary.newFolder().toPath(); DesktopDataStore(path).use { it.inventory.addItem(item()) }
            val file = path.resolve("workspace.json"); val json = JSONObject(Files.readString(file)); json.getJSONArray("inventory").getJSONObject(0).put(field, bad)
            val original = json.toString(); Files.writeString(file, original)
            assertFailsWith<IllegalStateException> { DesktopDataStore(path) }; assertEquals(original, Files.readString(file))
        }
    }
    @Test fun duplicateCodeCannotOverwriteExistingItem() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            store.inventory.addItem(item()); assertFailsWith<IllegalArgumentException> { store.inventory.addItem(item().copy(id = "another")) }
            assertEquals(listOf(item()), store.inventory.getItems())
        }
    }
    @Test fun metadataEditCannotBypassStockJournal() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            store.inventory.addItem(item())
            assertFailsWith<IllegalArgumentException> { store.inventory.updateItem(item().copy(quantity = 20)) }
            assertEquals(3, store.inventory.getItems().single().quantity)
            assertTrue(store.inventory.getAdjustments().isEmpty())
        }
    }
    @Test fun malformedJournalOrWrongArrayTypeBlocksOpenWithoutLoss() {
        for (field in listOf("weightGrams", "quantityChange", "type")) {
            val path = temporary.newFolder().toPath()
            DesktopDataStore(path).use { it.inventory.addItem(item()); it.inventory.adjustStock(move()) }
            val file = path.resolve("workspace.json"); val json = JSONObject(Files.readString(file))
            json.getJSONArray("stockAdjustments").getJSONObject(0).put(field, "invalid")
            val original = json.toString(); Files.writeString(file, original)
            assertFailsWith<IllegalStateException> { DesktopDataStore(path) }; assertEquals(original, Files.readString(file))
        }
        val path = temporary.newFolder().toPath(); DesktopDataStore(path).close()
        val file = path.resolve("workspace.json")
        val original = "{\"schemaVersion\":1,\"settings\":{},\"portfolio\":[],\"inventory\":{}}"
        Files.writeString(file, original); assertFailsWith<IllegalStateException> { DesktopDataStore(path) }; assertEquals(original, Files.readString(file))
    }
    @Test fun embeddedPhotoAndMovementsAreSelfContainedInBackup() {
        val source = BufferedImage(1000, 500, BufferedImage.TYPE_INT_RGB); val bytes = ByteArrayOutputStream(); ImageIO.write(source, "png", bytes)
        val image = InventoryPhoto.encode(bytes.toByteArray()); assertTrue(InventoryPhoto.isValid(image)); assertFails { InventoryPhoto.encode(byteArrayOf(1, 2, 3)) }
        val original = item().copy(imageUrl = image)
        val backup = temporary.root.toPath().resolve("export.json")
        DesktopDataStore(temporary.newFolder().toPath()).use { it.inventory.addItem(original); it.inventory.adjustStock(move()); it.exportBackup(backup) }
        val restored = temporary.newFolder().toPath(); Files.copy(backup, restored.resolve("workspace.json"))
        DesktopDataStore(restored).use { assertEquals(original.copy(quantity = 5), it.inventory.getItems().single()); assertEquals(listOf(move()), it.inventory.getAdjustments()) }
    }
    @Test fun stateRetainsInvalidDraftAndReportsExactStockResult() = runBlocking {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                store.inventory.addItem(item()); val feature = DesktopInventory(store, scope)
                feature.open(); feature.edit { it.copy(title = "حلقه", gross = "NaN") }; assertNull(feature.save()); assertTrue("gross" in feature.state.value.draft!!.errors)
                feature.dismiss(); assertTrue(feature.state.value.discard); feature.cancelDiscard(); assertNotNull(feature.state.value.draft); feature.discard()
                feature.openMovement(item(), StockAdjustmentType.DEDUCT); feature.editMovement { it.copy(count = "3") }
                assertEquals(0 to 0.0, feature.adjustedPreview()); feature.saveMovement()!!.join()
                assertEquals(0, feature.state.value.items.single().quantity); assertEquals(6.0, feature.state.value.history.single().weightGrams)
            } finally { scope.cancel() }
        }
    }
}

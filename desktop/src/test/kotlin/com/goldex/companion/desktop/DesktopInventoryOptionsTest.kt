package com.goldex.companion.desktop

import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.DesktopInventory
import com.goldex.companion.model.*
import com.goldex.companion.presentation.inventory.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.test.*

class DesktopInventoryOptionsTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun item(code: String = "RNG-OLD") = InventoryForm.toItem(InventoryDraft(
        id = "legacy", code = code, title = "حلقه", gross = "2.125", profit = "13.5", tax = "10"))

    @Test fun catalogsAndAssignmentsSurviveReopenBackupAndLegacyMetadataEdits() {
        val path = temporary.newFolder().toPath()
        lateinit var category: InventoryOption
        val original = item()
        val exported = temporary.root.toPath().resolve("export.json")
        DesktopDataStore(path).use { store ->
            store.inventory.addItem(original)
            assertEquals(InventoryOptions(), store.loadInventoryOptions())
            category = store.addInventoryOption(DesktopInventoryOptions.CATEGORIES, "آویز")
            assertEquals(category, store.addInventoryOption(DesktopInventoryOptions.CATEGORIES, " آویز "))
            store.addInventoryOption(DesktopInventoryOptions.LOCATIONS, "سینی جدید")
            val custom = original.copy(id = "new", category = InventoryCategory.MISC)
            val saved = store.saveInventoryProduct(custom, true, category.id)
            assertTrue(saved.code.matches(Regex("[0-9]{1,7}")))
            assertNotEquals(original.code, saved.code)
            store.exportBackup(exported)
        }
        val json = JSONObject(Files.readString(path.resolve("workspace.json")))
        json.getJSONArray("inventoryCategories").getJSONObject(0).put("future", "preserved")
        Files.writeString(path.resolve("workspace.json"), json.toString())
        DesktopDataStore(path).use { store ->
            assertEquals(category.id, store.loadInventoryOptions().categoryIds["new"])
            assertEquals("سینی جدید", store.loadInventoryOptions().locations.single().name)
            store.saveInventoryProduct(original.copy(title = "ویرایش"), false, null)
            assertEquals(original.copy(title = "ویرایش"), store.inventory.getItems().first())
            assertEquals("preserved", JSONObject(Files.readString(store.documentPath)).getJSONArray("inventoryCategories").getJSONObject(0).getString("future"))
        }
        val restored = temporary.newFolder().toPath()
        Files.copy(exported, restored.resolve("workspace.json"))
        DesktopDataStore(restored).use { assertEquals(category.id, it.loadInventoryOptions().categoryIds["new"]) }
    }

    @Test fun generatedCodesSkipOccupiedValuesAndCommitWithCategoryOrPreserveOriginalOnFailure() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            store.inventory.addItem(item("1000000"))
            val category = store.addInventoryOption(DesktopInventoryOptions.CATEGORIES, "آویز")
            val new = item().copy(id = "new", category = InventoryCategory.MISC)
            val saved = store.saveInventoryProduct(new, true, category.id)
            assertEquals("1000001", saved.code)
            val original = Files.readString(store.documentPath)
            Files.deleteIfExists(store.backupPath)
            Files.createDirectory(store.backupPath)
            Files.writeString(store.backupPath.resolve("block"), "x")
            assertFails { store.saveInventoryProduct(saved.copy(title = "تغییر"), false, null) }
            assertEquals(original, Files.readString(store.documentPath))
            assertEquals(category.id, store.loadInventoryOptions().categoryIds[saved.id])
            assertFails { store.addInventoryOption(DesktopInventoryOptions.LOCATIONS, "محل دیگر") }
            assertTrue(store.loadInventoryOptions().locations.isEmpty())
        }
    }

    @Test fun invalidCatalogOrDanglingReferenceBlocksOpenWithoutDiscardingData() {
        for (invalid in listOf("category", "location", "reference")) {
            val path = temporary.newFolder().toPath()
            DesktopDataStore(path).use { store ->
                val category = store.addInventoryOption(DesktopInventoryOptions.CATEGORIES, "آویز")
                store.addInventoryOption(DesktopInventoryOptions.LOCATIONS, "سینی")
                store.saveInventoryProduct(item().copy(category = InventoryCategory.MISC), true, category.id)
            }
            val file = path.resolve("workspace.json")
            val json = JSONObject(Files.readString(file))
            when (invalid) {
                "category" -> json.getJSONArray("inventoryCategories").getJSONObject(0).put("name", "")
                "location" -> json.put("inventoryLocations", JSONObject())
                else -> json.getJSONArray("inventory").getJSONObject(0).put("customCategoryId", "missing")
            }
            val original = json.toString()
            Files.writeString(file, original)
            assertFailsWith<IllegalStateException> { DesktopDataStore(path) }
            assertEquals(original, Files.readString(file))
        }
    }

    @Test fun deletionAndReopenDoNotRecycleGeneratedCodes() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            val first = store.saveInventoryProduct(item(), true, null)
            assertEquals("1000000", first.code)
            store.inventory.deleteItem(first.id)
        }
        DesktopDataStore(path).use { store ->
            assertEquals("1000001", store.saveInventoryProduct(item(), true, null).code)
        }
        assertFailsWith<IllegalArgumentException> { DesktopInventoryOptions.nextCode(emptyList(), 9_999_999) }
    }
    @Test fun rollbackWriterNativeCategoryEditPreservesCustomReferenceAndFinancialPolicy() {
        val path = temporary.newFolder().toPath()
        lateinit var saved: InventoryItem
        lateinit var category: InventoryOption
        DesktopDataStore(path).use { store ->
            category = store.addInventoryOption(DesktopInventoryOptions.CATEGORIES, "آویز")
            saved = store.saveInventoryProduct(item().copy(category = InventoryCategory.MISC), true, category.id)
            // Older clients merge the native record while retaining unknown customCategoryId.
            store.inventory.updateItem(saved.copy(category = InventoryCategory.JEWELRY, profitPercent = 20.0))
        }
        DesktopDataStore(path).use { store ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val feature = DesktopInventory(store, scope)
                feature.filterCustomCategory(category.id)
                assertEquals("آویز", feature.state.value.categoryTitle(feature.state.value.filtered.single()))
                assertEquals(InventoryCategory.JEWELRY, feature.state.value.filtered.single().category)
                assertEquals(20.0, feature.state.value.filtered.single().profitPercent)
            } finally { scope.cancel() }
        }
    }

    @Test fun featureRetainsCustomSelectionAndFinancialMetadataButClosesProductDirectly() = runBlocking {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                store.inventory.addItem(item())
                val feature = DesktopInventory(store, scope)
                feature.open(item())
                feature.category(InventoryCategory.RINGS)
                assertEquals("13.5", feature.state.value.draft!!.profit)
                feature.dismiss()
                assertNull(feature.state.value.draft)
                feature.open()
                feature.addCategory("آویز")!!.join()
                feature.addLocation("سینی جدید")!!.join()
                feature.edit { it.copy(title = "آویز جدید", gross = "1.125", purity = "735") }
                feature.save()!!.join()
                val saved = feature.state.value.selected!!
                assertEquals("آویز", feature.state.value.categoryTitle(saved))
                assertEquals("سینی جدید", saved.location)
                feature.filterCustomCategory(feature.state.value.options.categories.single().id)
                assertEquals(listOf(saved), feature.state.value.filtered)
                feature.open(saved)
                val categoryId = feature.state.value.draftCategoryId
                feature.edit { it.copy(title = "ویرایش آویز") }
                feature.save()!!.join()
                assertEquals(categoryId, feature.state.value.options.categoryIds[saved.id])
                feature.openMovement(saved)
                feature.editMovement { it.copy(count = "2") }
                feature.dismiss()
                assertTrue(feature.state.value.discard)
            } finally { scope.cancel() }
        }
    }
}

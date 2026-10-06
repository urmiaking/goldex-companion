package com.goldex.companion.desktop

import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.test.*

class DesktopDataStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun directory() = temporary.newFolder().toPath()
    private fun gold() = PortfolioItem(id = "stable-id", title = "طلای آزمون", category = PortfolioCategory.GOLD, weightGrams = 2.125, purchasePriceTotal = 12_345_678)

    @Test fun motionPreferenceDefaultsAndRoundTripsWithoutLosingFinancialRecords() {
        val path = directory()
        DesktopDataStore(path).use { store ->
            assertFalse(store.loadReduceMotion())
            store.addItem(gold())
            store.saveReduceMotion(true)
        }
        DesktopDataStore(path).use { store ->
            assertTrue(store.loadReduceMotion())
            assertEquals(listOf(gold()), store.getItems())
        }
    }

    @Test fun reopeningRetainsStableIdsSettingsThemeAndPreviousDocument() {
        val path = directory()
        DesktopDataStore(path).use { store ->
            store.addItem(gold())
            store.saveSettings(AppSettings(galleryName = "گالری آزمون", defaultTaxPercent = "10"))
            store.saveDarkTheme(true)
            assertEquals("گالری آزمون", JSONObject(Files.readString(store.backupPath)).getJSONObject("settings").getString("galleryName"))
        }
        DesktopDataStore(path).use { store ->
            assertEquals(listOf(gold()), store.getItems())
            assertEquals("10", store.loadSettings().defaultTaxPercent)
            assertTrue(store.loadDarkTheme())
        }
    }

    @Test fun unknownFieldsSurviveEditingWithoutChangingRecordId() {
        val path = directory()
        DesktopDataStore(path).use { it.addItem(gold()) }
        val file = path.resolve("workspace.json")
        val original = JSONObject(Files.readString(file)).put("futureRoot", "keep")
        original.getJSONObject("settings").put("futureSetting", 17)
        original.getJSONArray("portfolio").getJSONObject(0).put("futureRecord", "keep")
        Files.writeString(file, original.toString())
        DesktopDataStore(path).use { store ->
            store.addItem(gold().copy(title = "نام تازه"))
            store.saveSettings(store.loadSettings().copy(managerName = "آزمون"))
        }
        val saved = JSONObject(Files.readString(file))
        assertEquals("keep", saved.getString("futureRoot"))
        assertEquals(17, saved.getJSONObject("settings").getInt("futureSetting"))
        assertEquals("keep", saved.getJSONArray("portfolio").getJSONObject(0).getString("futureRecord"))
        assertEquals("stable-id", saved.getJSONArray("portfolio").getJSONObject(0).getString("id"))
    }

    @Test fun malformedFutureOrUnknownRecordsBlockOpenAndPreserveOriginalBytes() {
        for (content in listOf("{bad", """{"schemaVersion":2,"settings":{},"portfolio":[]}""", """{"schemaVersion":1.5,"settings":{},"portfolio":[]}""",
            """{"schemaVersion":1,"settings":{},"portfolio":[{"id":"a","title":"x","category":"FUTURE"}]}""")) {
            val path = directory()
            Files.writeString(path.resolve("workspace.json"), content)
            assertFailsWith<IllegalStateException> { DesktopDataStore(path) }
            assertEquals(content, Files.readString(path.resolve("workspace.json")))
        }
    }

    @Test fun anotherWriterCannotOpenTheSameDirectory() {
        val path = directory()
        DesktopDataStore(path).use { assertFailsWith<IllegalStateException> { DesktopDataStore(path) } }
        DesktopDataStore(path).use { it.addItem(gold()) }
    }

    @Test fun failedBackupWriteKeepsDiskAndMemoryUnchanged() {
        DesktopDataStore(directory()).use { store ->
            store.addItem(gold())
            val original = Files.readString(store.documentPath)
            Files.createDirectory(store.backupPath)
            assertFails { store.saveSettings(AppSettings(galleryName = "نباید ذخیره شود")) }
            assertEquals(original, Files.readString(store.documentPath))
            assertEquals("", store.settings.value.galleryName)
        }
    }

    @Test fun exportCreatesIndependentBackupAndNeverOverwritesExistingName() {
        DesktopDataStore(directory()).use { store ->
            store.addItem(gold())
            val export = temporary.root.toPath().resolve("export.json")
            store.exportBackup(export)
            val original = Files.readString(export)
            store.deleteItem(gold().id)
            assertFails { store.exportBackup(export) }
            assertEquals(original, Files.readString(export))
            assertEquals(1, JSONObject(original).getJSONArray("portfolio").length())
        }
    }

    @Test fun olderSettingsDefaultMissingFieldsAndRejectDuplicateIds() {
        val path = directory()
        val record = PersistenceJsonCodecs.encodePortfolioItems(listOf(gold(), gold()))
        Files.writeString(path.resolve("workspace.json"), """{"schemaVersion":1,"settings":{},"portfolio":$record}""")
        assertFailsWith<IllegalStateException> { DesktopDataStore(path) }
        assertEquals("7", PersistenceJsonCodecs.decodeSettings("{}").defaultProfitPercent)
        assertEquals(AppSettings(), PersistenceJsonCodecs.decodeSettings(PersistenceJsonCodecs.encodeSettings(AppSettings())))
    }
}

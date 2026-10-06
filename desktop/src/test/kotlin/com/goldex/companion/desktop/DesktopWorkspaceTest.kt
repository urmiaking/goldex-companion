package com.goldex.companion.desktop

import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.presentation.calculator.CalculatorField
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.test.*

class DesktopWorkspaceTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun workspace(store: DesktopDataStore) = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline test") }))

    @Test fun persianAssetInputsSaveEditAndReopenWithSameId() = runBlocking {
        val path = temporary.newFolder().toPath()
        var id = ""
        DesktopDataStore(path).use { store -> workspace(store).use { model ->
            model.openAsset()
            model.editDraft { it.copy(title = "شمش", weight = "۲٫۱۲۵", cost = "۱۲٬۳۴۵٬۶۷۸") }
            model.saveAsset()!!.join()
            val item = store.getItems().single()
            id = item.id
            assertEquals(2.125, item.weightGrams)
            model.openAsset(item)
            model.editDraft { it.copy(title = "شمش ویرایش‌شده") }
            model.saveAsset()!!.join()
            assertEquals(id, store.getItems().single().id)
        } }
        DesktopDataStore(path).use { assertEquals(id, it.getItems().single().id) }
    }

    @Test fun invalidInputAndCancelledDeleteNeverWritePortfolio() = runBlocking {
        DesktopDataStore(temporary.newFolder().toPath()).use { store -> workspace(store).use { model ->
            model.openAsset()
            model.editDraft { it.copy(title = "نام", weight = "1.1234", cost = "100") }
            assertNull(model.saveAsset())
            assertTrue(model.state.value.draft!!.errors.containsKey("weight"))
            assertTrue(store.getItems().isEmpty())
            model.editDraft { it.copy(weight = "1") }
            model.saveAsset()!!.join()
            model.requestDelete(store.getItems().single())
            model.requestDelete(null)
            assertEquals(1, store.getItems().size)
        } }
    }

    @Test fun settingsDraftSurvivesNavigationAndDefaultsOnlyAffectNextCalculation() = runBlocking {
        DesktopDataStore(temporary.newFolder().toPath()).use { store -> workspace(store).use { model ->
            model.calculator.setInput(CalculatorField.SPOT, "6000000")
            model.calculator.setInput(CalculatorField.GROSS_WEIGHT, "2")
            val quote = model.calculator.state.value.result
            model.editSettings { it.copy(galleryName = "گالری", defaultProfitPercent = "۸", defaultTaxPercent = "۱۰") }
            model.navigate(DesktopDestination.RATES)
            model.navigate(DesktopDestination.SETTINGS)
            assertEquals("گالری", model.state.value.settingsDraft!!.galleryName)
            model.saveSettings(model.state.value.settingsDraft!!)!!.join()
            assertEquals(quote, model.calculator.state.value.result)
            model.calculator.reset()
            assertEquals("8", model.calculator.state.value.input(CalculatorField.PROFIT))
            assertEquals("10", model.calculator.state.value.input(CalculatorField.TAX))
            model.toggleTheme()!!.join()
            assertTrue(store.loadDarkTheme())
        } }
    }

    @Test fun failedPersistenceRetainsDraftAndPreviouslySavedState() = runBlocking {
        DesktopDataStore(temporary.newFolder().toPath()).use { store -> workspace(store).use { model ->
            store.saveDarkTheme(false)
            Files.createDirectory(store.backupPath)
            model.openAsset()
            model.editDraft { it.copy(title = "طلا", weight = "1", cost = "100") }
            model.saveAsset()!!.join()
            assertNotNull(model.state.value.error)
            assertNotNull(model.state.value.draft)
            assertTrue(store.getItems().isEmpty())
        } }
    }
}

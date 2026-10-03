package com.goldex.companion.data.local

import androidx.test.core.app.ApplicationProvider
import com.goldex.companion.data.*
import com.goldex.companion.data.local.db.GoldexDatabase
import com.goldex.companion.data.local.repository.RoomInventoryRepository
import com.goldex.companion.data.local.repository.RoomPortfolioRepository
import com.goldex.companion.data.sync.*
import com.goldex.companion.domain.onboarding.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RoomOnboardingSafetyTest {
    private lateinit var db: GoldexDatabase
    private lateinit var unit: RoomSyncUnitOfWork
    private lateinit var inventory: InventoryStore
    private lateinit var portfolio: PortfolioStore
    private lateinit var settings: SettingsStore
    private lateinit var checkpoint: RoomOnboardingCheckpoint
    @Before fun setup() {
        db = GoldexDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        unit = RoomSyncUnitOfWork(db)
        inventory = RoomInventoryRepository(db.inventoryDao(), unit)
        portfolio = RoomPortfolioRepository(db.portfolioDao(), unit)
        checkpoint = RoomOnboardingCheckpoint(db.syncDao())
        // Stores business settings and outbox in the same real SQLite transaction.
        settings = object : SettingsStore {
            private var completed = false
            override val settings = MutableStateFlow(AppSettings())
            override fun loadSettings(): AppSettings = db.syncDao().business()?.let {
                SyncJson.applyBusiness(AppSettings(hasCompletedOnboarding = completed), JSONObject(it.payload))
            } ?: AppSettings(hasCompletedOnboarding = completed)
            override fun saveSettings(newSettings: AppSettings) {
                unit.transaction {
                    val payload = SyncJson.business(newSettings)
                    db.syncDao().business(BusinessSettings(payload = payload.toString()))
                    unit.changed("businessSettings", "business", payload)
                }
            }
            override fun setHasCompletedOnboarding(completed: Boolean) { this.completed = completed }
        }
    }
    @After fun close() { db.close() }

    @Test fun failedCompletionRollsBackBothStoresProfileMarkerAndOutboxBeforeRetry() {
        val failingPortfolio = object : PortfolioStore by portfolio {
            override fun addItem(item: PortfolioItem) {
                portfolio.addItem(item)
                error("simulated interruption after write")
            }
        }
        val failing = CompleteOnboardingUseCase(settings, inventory, failingPortfolio, checkpoint, unit)
        try {
            failing.complete(AppSettings(galleryName = "new"), OpeningInventoryInput(coinTamam = 2), false)
            fail("Completion must fail")
        } catch (_: IllegalStateException) { }
        assertTrue(inventory.getItems().isEmpty())
        assertTrue(portfolio.getItems().isEmpty())
        assertNull(db.syncDao().business())
        assertFalse(checkpoint.hasSeededOpeningInventory())
        assertFalse(settings.loadSettings().hasCompletedOnboarding)
        assertEquals(0, db.syncDao().pendingCount())
        assertTrue(db.syncDao().allMetadata().isEmpty())

        CompleteOnboardingUseCase(settings, inventory, portfolio, checkpoint, unit)
            .complete(AppSettings(galleryName = "new"), OpeningInventoryInput(coinTamam = 2), false)
        assertEquals(2, inventory.getItems().single().quantity)
        assertEquals(2, portfolio.getItems().single().quantity)
        assertTrue(checkpoint.hasSeededOpeningInventory())
        assertTrue(settings.loadSettings().hasCompletedOnboarding)
        assertEquals(1, db.syncDao().pendingCount())
        val changes = JSONObject(db.syncDao().first()!!.payload).getJSONArray("changes")
        assertEquals(setOf("businessSettings", "inventory", "portfolio"),
            (0 until changes.length()).map { changes.getJSONObject(it).getString("type") }.toSet())
    }

    @Test fun reopeningWizardKeepsRecordIdsAndDoesNotEnqueueDuplicateStock() {
        val useCase = CompleteOnboardingUseCase(settings, inventory, portfolio, checkpoint, unit)
        val input = OpeningInventoryInput(vitrinWeight = "۰٫۰۰۱", coinNim = 2)
        useCase.complete(AppSettings(galleryName = "same"), input, false)
        val firstInventory = inventory.getItems()
        val firstPortfolio = portfolio.getItems()
        val queued = db.syncDao().pendingCount()
        useCase.complete(AppSettings(galleryName = "same"), input, false)
        assertEquals(firstInventory, inventory.getItems())
        assertEquals(firstPortfolio, portfolio.getItems())
        assertEquals(queued, db.syncDao().pendingCount())
    }

    @Test fun onboardingCompletionWithCloudLoginPreservesAndStagesLocalWizardData() {
        val useCase = CompleteOnboardingUseCase(settings, inventory, portfolio, checkpoint, unit)
        val input = OpeningInventoryInput(vitrinWeight = "۵٫۵۰۰", coinTamam = 1)
        val wizardSettings = AppSettings(
            galleryName = "طلافروشی نمونه",
            managerName = "مدیر آزمایشی",
            unionCode = "۱۲۳۴۵",
            defaultProfitPercent = "۷",
            defaultTaxPercent = "۹"
        )
        // Normal cloud login without backup restoration passes restoredFromCloud = false
        useCase.complete(wizardSettings, input, restoredFromCloud = false)
        val savedSettings = settings.loadSettings()
        assertEquals("طلافروشی نمونه", savedSettings.galleryName)
        assertEquals("مدیر آزمایشی", savedSettings.managerName)
        assertEquals("۱۲۳۴۵", savedSettings.unionCode)
        assertEquals("۷", savedSettings.defaultProfitPercent)
        assertEquals("۹", savedSettings.defaultTaxPercent)
        assertTrue(savedSettings.hasCompletedOnboarding)
        assertEquals(2, inventory.getItems().size)
        assertEquals(2, portfolio.getItems().size)
        assertTrue(checkpoint.hasSeededOpeningInventory())
        assertEquals(1, db.syncDao().pendingCount())
    }
}

package com.goldex.companion.domain.onboarding

import com.goldex.companion.data.*
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.*

class CompleteOnboardingUseCaseTest {
    @Test fun openingPlanPreservesPersianInputsFeesPurityAndCoinWeights() {
        val plan = OpeningInventoryPolicy.build(
            AppSettings(galleryName = "زر", defaultProfitPercent = "8", defaultTaxPercent = "10"),
            OpeningInventoryInput(
                vitrinWeight = "۱۲٫۳۴۵", vitrinOjrat = "۶٫۵", meltWeight = "۲٫۱۲۳", meltAyar = "۹۰۰",
                coinTamam = 2, coinNim = 3, coinRob = 4, coinQadim = 5, coinGerami = 6
            )
        )
        assertEquals(7, plan.inventory.size)
        assertEquals(7, plan.portfolio.size)
        val vitrin = plan.inventory.first()
        assertEquals("مصنوعات ویترین (زر)", vitrin.title)
        assertEquals(12.345, vitrin.grossWeightGrams)
        assertEquals(6.5, vitrin.wageValue)
        assertEquals(8.0, vitrin.profitPercent)
        assertEquals(10.0, vitrin.taxPercent)
        val melt = plan.inventory[1]
        assertEquals(900, melt.customKaratValue)
        assertEquals(Karat.K21, melt.karat)
        assertEquals(0.0, melt.wageValue)
        assertEquals(0.0, melt.taxPercent)
        assertEquals(listOf(8.133, 4.066, 2.033, 8.133, 1.01), plan.inventory.drop(2).map { it.grossWeightGrams })
        assertEquals(listOf(2, 3, 4, 5, 6), plan.portfolio.drop(2).map { it.quantity })
        assertEquals(listOf(CoinType.EMAMI, CoinType.HALF, CoinType.QUARTER, CoinType.BAHAR, CoinType.GERAMI), plan.portfolio.drop(2).map { it.coinType })
        assertTrue(plan.inventory.drop(2).all { it.customKaratValue == 900 && it.taxPercent == 0.0 })
        assertTrue(plan.portfolio.all { it.purchaseDate == "موجودی اول دوره" && it.purchasePriceTotal == 0L })
    }

    @Test fun blankInvalidAndNonPositiveOpeningInventoryDoNotCreateRecords() {
        val plan = OpeningInventoryPolicy.build(AppSettings(), OpeningInventoryInput(
            vitrinWeight = "bad", meltWeight = "-1", coinTamam = -2, cashTankhah = "1000", bankBalances = "2000"
        ))
        assertTrue(plan.inventory.isEmpty())
        assertTrue(plan.portfolio.isEmpty())
    }

    @Test fun repeatingCompletionKeepsIdsAndCountsWhileAllowingProfileUpdate() {
        val stores = Stores()
        val useCase = stores.useCase()
        useCase.complete(AppSettings(galleryName = "first"), OpeningInventoryInput(coinTamam = 2), false)
        val originalInventory = stores.inventory.toList()
        val originalPortfolio = stores.portfolio.toList()
        useCase.complete(AppSettings(galleryName = "second"), OpeningInventoryInput(coinTamam = 9), false)
        assertEquals(originalInventory, stores.inventory)
        assertEquals(originalPortfolio, stores.portfolio)
        assertEquals("second", stores.settings.value.galleryName)
        assertTrue(stores.settings.value.hasCompletedOnboarding)
    }

    @Test fun restoredCloudDataBypassesFinancialTransactionAndPreservesProfileAndStock() {
        val stores = Stores()
        stores.settings.value = AppSettings(galleryName = "restored")
        stores.inventory += InventoryItem(id = "stable", code = "OLD", title = "existing")
        stores.rejectWrites = true
        stores.useCase().complete(AppSettings(galleryName = "wizard"), OpeningInventoryInput(coinTamam = 9), true)
        assertEquals(0, stores.transactions)
        assertEquals("restored", stores.settings.value.galleryName)
        assertEquals("stable", stores.inventory.single().id)
        assertTrue(stores.settings.value.hasCompletedOnboarding)
    }

    @Test fun portfolioFailureRollsBackSettingsStockAndMarkerAndAllowsSafeRetry() {
        val stores = Stores()
        stores.settings.value = AppSettings(galleryName = "original")
        stores.failPortfolio = true
        assertFailsWith<IllegalStateException> {
            stores.useCase().complete(AppSettings(galleryName = "new"), OpeningInventoryInput(coinTamam = 1), false)
        }
        assertEquals("original", stores.settings.value.galleryName)
        assertFalse(stores.settings.value.hasCompletedOnboarding)
        assertTrue(stores.inventory.isEmpty())
        assertTrue(stores.portfolio.isEmpty())
        assertFalse(stores.marked)
        stores.failPortfolio = false
        stores.useCase().complete(AppSettings(), OpeningInventoryInput(coinTamam = 1), false)
        assertEquals(1, stores.inventory.size)
        assertEquals(1, stores.portfolio.size)
        assertTrue(stores.marked)
    }

    @Test fun legacyCompletionFlagPreventsSeedingPreviouslyInitializedAccounts() {
        val stores = Stores()
        stores.settings.value = AppSettings(hasCompletedOnboarding = true)
        stores.useCase().complete(AppSettings(), OpeningInventoryInput(coinTamam = 10), false)
        assertTrue(stores.inventory.isEmpty())
        assertTrue(stores.portfolio.isEmpty())
        assertTrue(stores.marked)
    }

    private class Stores : SettingsStore, OnboardingCheckpoint, SyncUnitOfWork {
        override val settings = MutableStateFlow(AppSettings())
        val inventory = mutableListOf<InventoryItem>()
        val portfolio = mutableListOf<PortfolioItem>()
        var marked = false
        var failPortfolio = false
        var rejectWrites = false
        var transactions = 0
        override fun loadSettings() = settings.value
        override fun saveSettings(newSettings: AppSettings) { settings.value = newSettings }
        override fun hasSeededOpeningInventory() = marked
        override fun markOpeningInventorySeeded() { marked = true }
        override fun <T> transaction(action: () -> T): T {
            check(!rejectWrites)
            transactions++
            val beforeSettings = settings.value
            val beforeInventory = inventory.toList()
            val beforePortfolio = portfolio.toList()
            val beforeMarker = marked
            return try { action() } catch (failure: Exception) {
                settings.value = beforeSettings
                inventory.clear(); inventory.addAll(beforeInventory)
                portfolio.clear(); portfolio.addAll(beforePortfolio)
                marked = beforeMarker
                throw failure
            }
        }
        fun useCase() = CompleteOnboardingUseCase(
            this,
            object : InventoryStore {
                override fun getItems() = inventory.toList()
                override fun addItem(item: InventoryItem) { inventory += item }
                override fun updateItem(item: InventoryItem) = error("unused")
                override fun deleteItem(id: String) = error("unused")
                override fun adjustStock(adjustment: StockAdjustment) = error("unused")
                override fun getAdjustments() = emptyList<StockAdjustment>()
            },
            object : PortfolioStore {
                override fun getItems() = portfolio.toList()
                override fun addItem(item: PortfolioItem) { check(!failPortfolio); portfolio += item }
                override fun deleteItem(id: String) = error("unused")
            },
            this, this
        )
    }
}

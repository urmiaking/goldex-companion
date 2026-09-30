package com.goldex.companion.domain.onboarding

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.PortfolioStore
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.data.sync.SyncUnitOfWork

/** Must share the same transaction as the inventory and portfolio stores. */
interface OnboardingCheckpoint {
    fun hasSeededOpeningInventory(): Boolean
    fun markOpeningInventorySeeded()
}

class CompleteOnboardingUseCase(
    private val settings: SettingsStore,
    private val inventory: InventoryStore,
    private val portfolio: PortfolioStore,
    private val checkpoint: OnboardingCheckpoint,
    private val unitOfWork: SyncUnitOfWork
) {
    fun complete(updatedSettings: AppSettings, input: OpeningInventoryInput, restoredFromCloud: Boolean) {
        // Restored profiles and balances are authoritative; retired writers must not attempt a write.
        if (!restoredFromCloud) {
            unitOfWork.transaction {
                val previouslyCompleted = settings.loadSettings().hasCompletedOnboarding
                settings.saveSettings(updatedSettings)
                // A retry or a manually reopened wizard may update settings, but never doubles stock.
                if (!checkpoint.hasSeededOpeningInventory()) {
                    if (!previouslyCompleted) {
                        val plan = OpeningInventoryPolicy.build(updatedSettings, input)
                        plan.inventory.forEach(inventory::addItem)
                        plan.portfolio.forEach(portfolio::addItem)
                    }
                    checkpoint.markOpeningInventorySeeded()
                }
            }
        }
        // This flag is device-owned. A failed financial transaction must never complete the wizard.
        settings.setHasCompletedOnboarding(true)
    }
}

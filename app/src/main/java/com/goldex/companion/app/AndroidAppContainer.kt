package com.goldex.companion.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.GoldMarketRepository
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.NetworkMonitor
import com.goldex.companion.data.SettingsRepository
import com.goldex.companion.data.license.LicenseRepository
import com.goldex.companion.data.local.db.GoldexDatabaseProvider
import com.goldex.companion.data.local.RoomOnboardingCheckpoint
import com.goldex.companion.domain.onboarding.CompleteOnboardingUseCase
import com.goldex.companion.ui.wizard.OnboardingViewModel
import com.goldex.companion.data.sync.SyncCoordinator
import com.goldex.companion.ui.calculator.KaratConvertViewModel
import com.goldex.companion.ui.inventory.InventoryViewModel
import com.goldex.companion.ui.invoices.BarterInvoiceViewModel
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import com.goldex.companion.ui.invoices.InvoiceManagerViewModel
import com.goldex.companion.ui.license.LicenseViewModel
import com.goldex.companion.ui.main.MainViewModel
import com.goldex.companion.ui.portfolio.PortfolioManagerViewModel
import com.goldex.companion.ui.reporting.ReportingViewModel
import com.goldex.companion.ui.settings.SettingsViewModel
import com.goldex.companion.ui.sync.CloudSyncViewModel
import com.goldex.companion.ui.update.UpdateViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Android composition root. Uses the existing providers, database and migration path.
 * Each adapter is created once and never retains an Activity.
 */
class AndroidAppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    val settings by lazy { SettingsRepository.getInstance(applicationContext) }
    private val customers by lazy { GoldexDatabaseProvider.getCustomerStore(applicationContext) }
    private val invoices by lazy { GoldexDatabaseProvider.getInvoiceStore(applicationContext) }
    private val inventory by lazy { GoldexDatabaseProvider.getInventoryStore(applicationContext) }
    private val portfolio by lazy { GoldexDatabaseProvider.getPortfolioStore(applicationContext) }
    private val syncUnit by lazy { GoldexDatabaseProvider.getSyncUnit(applicationContext) }
    private val market by lazy { GoldMarketRepository.also { it.init(applicationContext) } }
    private val connectivity by lazy { NetworkMonitor(applicationContext) }
    private val license by lazy { LicenseRepository(applicationContext) }
    private val cloud by lazy { SyncCoordinator.get(applicationContext) }

    /** The host collects the existing quote stream; forms receive immutable snapshots. */
    val marketRates: StateFlow<MarketRates> get() = market.rates

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val instance: ViewModel = when (modelClass) {
                MainViewModel::class.java -> MainViewModel(settings, market, market, market, market, connectivity)
                CustomerManagerViewModel::class.java -> CustomerManagerViewModel(customers, syncUnit)
                InvoiceManagerViewModel::class.java -> InvoiceManagerViewModel(invoices)
                PortfolioManagerViewModel::class.java -> PortfolioManagerViewModel(portfolio)
                SettingsViewModel::class.java -> SettingsViewModel(settings)
                OnboardingViewModel::class.java -> OnboardingViewModel(
                    CompleteOnboardingUseCase(
                        settings, inventory, portfolio,
                        RoomOnboardingCheckpoint(GoldexDatabaseProvider.getDatabase(applicationContext).syncDao()),
                        syncUnit
                    )
                )
                InventoryViewModel::class.java -> InventoryViewModel(inventory)
                ReportingViewModel::class.java -> ReportingViewModel(invoices, customers, inventory, settings)
                BarterInvoiceViewModel::class.java -> BarterInvoiceViewModel(invoices, customers, syncUnit) { market.rates.value.gold18 }
                LicenseViewModel::class.java -> LicenseViewModel(license)
                CloudSyncViewModel::class.java -> CloudSyncViewModel(cloud)
                UpdateViewModel::class.java -> UpdateViewModel()
                KaratConvertViewModel::class.java -> KaratConvertViewModel()
                else -> throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
            }
            return instance as T
        }
    }
}

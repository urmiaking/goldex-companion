package com.goldex.companion.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.PriceSource
import com.goldex.companion.data.SettingsRepository
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.model.WageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val appSettings: AppSettings = AppSettings(),
    val isTaxProfitModalVisible: Boolean = false,
    val isPriceSourceModalVisible: Boolean = false,
    val isJewelerProfileModalVisible: Boolean = false,
    val isSettingsDialogVisible: Boolean = false
)

class SettingsViewModel(
    private val repository: SettingsStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val s = repository.loadSettings()
        _uiState.update { it.copy(appSettings = s) }
    }

    fun updateSettings(newSettings: AppSettings) {
        repository.saveSettings(newSettings)
        _uiState.update { it.copy(appSettings = newSettings) }
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isSettingsDialogVisible = visible) }
    }

    fun setTaxProfitModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isTaxProfitModalVisible = visible) }
    }

    fun setPriceSourceModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isPriceSourceModalVisible = visible) }
    }

    fun setJewelerProfileModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isJewelerProfileModalVisible = visible) }
    }

    fun updateTaxAndProfit(profitPercent: String, taxPercent: String, wageType: WageType) {
        val updated = _uiState.value.appSettings.copy(
            defaultProfitPercent = profitPercent,
            defaultTaxPercent = taxPercent,
            defaultWageType = wageType
        )
        updateSettings(updated)
    }

    fun updatePriceSource(source: PriceSource, autoSync: Boolean) {
        val updated = _uiState.value.appSettings.copy(
            priceSource = source,
            autoSyncRates = autoSync
        )
        updateSettings(updated)
    }

    fun updateJewelerProfile(
        galleryName: String,
        managerName: String,
        unionCode: String,
        phone: String,
        address: String
    ) {
        val updated = _uiState.value.appSettings.copy(
            galleryName = galleryName,
            managerName = managerName,
            unionCode = unionCode,
            galleryPhone = phone,
            galleryAddress = address,
            galleryLicense = "صنف طلا و جواهر: $unionCode"
        )
        updateSettings(updated)
        setJewelerProfileModalVisible(false)
    }

    fun toggleBiometricLock(enabled: Boolean) {
        val updated = _uiState.value.appSettings.copy(
            isBiometricLockEnabled = enabled
        )
        updateSettings(updated)
    }
}

class SettingsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = SettingsRepository(application.applicationContext)
        return SettingsViewModel(repository) as T
    }
}

package com.goldex.companion.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.domain.security.AppLockState
import com.goldex.companion.domain.security.BiometricAuthManager
import com.goldex.companion.domain.security.BiometricAuthResult
import com.goldex.companion.domain.security.BiometricStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State holder for application security lock and biometric authentication workflows.
 */
class AppLockViewModel(
    private val settingsStore: SettingsStore,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val isLockFeatureEnabled: Boolean
        get() = settingsStore.settings.value.isBiometricLockEnabled

    private val _uiState = MutableStateFlow(
        AppLockState(
            isLocked = isLockFeatureEnabled && biometricAuthManager.isBiometricAvailable,
            authErrorMessage = null
        )
    )
    val uiState: StateFlow<AppLockState> = _uiState.asStateFlow()

    fun onAppForegrounded() {
        if (_uiState.value.isLocked) {
            requestUnlock()
        }
    }

    fun onAppBackgrounded() {
        if (isLockFeatureEnabled && biometricAuthManager.isBiometricAvailable) {
            _uiState.update {
                it.copy(
                    isLocked = true,
                    authErrorMessage = null
                )
            }
        }
    }

    fun requestUnlock() {
        if (!_uiState.value.isLocked) return

        biometricAuthManager.authenticate(
            title = "قفل امنیتی قیراط",
            subtitle = "برای ورود به برنامه، اثر انگشت خود را تأیید کنید",
            cancelButtonText = "انصراف"
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLocked = false,
                            authErrorMessage = null
                        )
                    }
                }
                is BiometricAuthResult.Failed -> {
                    _uiState.update {
                        it.copy(authErrorMessage = "اثر انگشت شناسایی نشد. دوباره تلاش کنید.")
                    }
                }
                is BiometricAuthResult.Canceled -> {
                    _uiState.update {
                        it.copy(authErrorMessage = "احراز هویت لغو شد. برای ورود، دکمه زیر را لمس کنید.")
                    }
                }
                is BiometricAuthResult.Error -> {
                    _uiState.update {
                        it.copy(authErrorMessage = "خطا در احراز هویت: ${result.message}")
                    }
                }
            }
        }
    }

    fun toggleBiometricLock(
        requested: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (requested) {
            when (biometricAuthManager.checkBiometricStatus()) {
                BiometricStatus.NO_HARDWARE -> {
                    onResult(false, "این دستگاه فاقد حسگر اثر انگشت است.")
                    return
                }
                BiometricStatus.NOT_ENROLLED -> {
                    onResult(false, "ابتدا اثر انگشت خود را در تنظیمات امنیتی گوشی ثبت کنید.")
                    return
                }
                BiometricStatus.UNAVAILABLE -> {
                    onResult(false, "حسگر اثر انگشت در حال حاضر در دسترس نیست.")
                    return
                }
                BiometricStatus.AVAILABLE -> {
                    biometricAuthManager.authenticate(
                        title = "فعال‌سازی قفل اثر انگشت",
                        subtitle = "برای فعال‌سازی قفل، اثر انگشت خود را تأیید کنید",
                        cancelButtonText = "انصراف"
                    ) { result ->
                        if (result is BiometricAuthResult.Success) {
                            settingsStore.setBiometricLockEnabled(true)
                            onResult(true, "قفل امنیتی با اثر انگشت فعال شد.")
                        } else {
                            onResult(false, "تأیید اثر انگشت انجام نشد.")
                        }
                    }
                }
            }
        } else {
            if (biometricAuthManager.isBiometricAvailable) {
                biometricAuthManager.authenticate(
                    title = "غیرفعال‌سازی قفل اثر انگشت",
                    subtitle = "برای غیرفعال‌سازی قفل، اثر انگشت خود را تأیید کنید",
                    cancelButtonText = "انصراف"
                ) { result ->
                    if (result is BiometricAuthResult.Success) {
                        settingsStore.setBiometricLockEnabled(false)
                        _uiState.update { it.copy(isLocked = false, authErrorMessage = null) }
                        onResult(true, "قفل امنیتی غیرفعال شد.")
                    } else {
                        onResult(false, "تأیید هویت ناموفق بود؛ قفل امنیتی فعال باقی ماند.")
                    }
                }
            } else {
                settingsStore.setBiometricLockEnabled(false)
                _uiState.update { it.copy(isLocked = false, authErrorMessage = null) }
                onResult(true, "قفل امنیتی غیرفعال شد.")
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(authErrorMessage = null) }
    }
}

class AppLockViewModelFactory(
    private val settingsStore: SettingsStore,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppLockViewModel::class.java)) {
            return AppLockViewModel(settingsStore, biometricAuthManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

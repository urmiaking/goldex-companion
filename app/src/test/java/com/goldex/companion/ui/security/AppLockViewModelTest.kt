package com.goldex.companion.ui.security

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.domain.security.BiometricAuthManager
import com.goldex.companion.domain.security.BiometricAuthResult
import com.goldex.companion.domain.security.BiometricStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockViewModelTest {

    private class FakeSettingsStore(initialSettings: AppSettings = AppSettings()) : SettingsStore {
        private val _settings = MutableStateFlow(initialSettings)
        override val settings: StateFlow<AppSettings> = _settings
        override fun loadSettings(): AppSettings = _settings.value
        override fun saveSettings(newSettings: AppSettings) {
            _settings.value = newSettings
        }
        override fun loadDarkTheme(): Boolean = false
        override fun saveDarkTheme(enabled: Boolean) = Unit
    }

    private class FakeBiometricAuthManager(
        var status: BiometricStatus = BiometricStatus.AVAILABLE,
        var nextAuthResult: BiometricAuthResult = BiometricAuthResult.Success
    ) : BiometricAuthManager {
        var authenticateCallCount = 0

        override fun checkBiometricStatus(): BiometricStatus = status

        override fun authenticate(
            title: String,
            subtitle: String,
            cancelButtonText: String,
            onResult: (BiometricAuthResult) -> Unit
        ) {
            authenticateCallCount++
            onResult(nextAuthResult)
        }
    }

    @Test
    fun defaultAppSettings_hasBiometricLockAndTipDismissedDisabledByDefault() {
        val defaultSettings = AppSettings()
        assertFalse(defaultSettings.isBiometricLockEnabled)
        assertFalse(defaultSettings.isBiometricTipDismissed)

        val settingsStore = FakeSettingsStore(defaultSettings)
        val authManager = FakeBiometricAuthManager(status = BiometricStatus.AVAILABLE)

        val viewModel = AppLockViewModel(settingsStore, authManager)
        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun initialState_whenBiometricEnabledAndAvailable_startsLocked() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(status = BiometricStatus.AVAILABLE)

        val viewModel = AppLockViewModel(settingsStore, authManager)

        assertTrue(viewModel.uiState.value.isLocked)
        assertNull(viewModel.uiState.value.authErrorMessage)
    }

    @Test
    fun initialState_whenBiometricDisabled_startsUnlocked() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = false))
        val authManager = FakeBiometricAuthManager(status = BiometricStatus.AVAILABLE)

        val viewModel = AppLockViewModel(settingsStore, authManager)

        assertFalse(viewModel.uiState.value.isLocked)
        assertNull(viewModel.uiState.value.authErrorMessage)
    }

    @Test
    fun initialState_whenBiometricEnabledButNoHardware_startsUnlockedToPreventLockout() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(status = BiometricStatus.NO_HARDWARE)

        val viewModel = AppLockViewModel(settingsStore, authManager)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun onAppBackgrounded_whenLockEnabled_transitionsToLocked() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(
            status = BiometricStatus.AVAILABLE,
            nextAuthResult = BiometricAuthResult.Success
        )

        val viewModel = AppLockViewModel(settingsStore, authManager)
        assertTrue(viewModel.uiState.value.isLocked)

        viewModel.requestUnlock()
        assertFalse(viewModel.uiState.value.isLocked)

        viewModel.onAppBackgrounded()
        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun requestUnlock_onSuccess_unlocksAppAndClearsError() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(
            status = BiometricStatus.AVAILABLE,
            nextAuthResult = BiometricAuthResult.Success
        )

        val viewModel = AppLockViewModel(settingsStore, authManager)
        assertTrue(viewModel.uiState.value.isLocked)

        viewModel.requestUnlock()

        assertFalse(viewModel.uiState.value.isLocked)
        assertNull(viewModel.uiState.value.authErrorMessage)
    }

    @Test
    fun requestUnlock_onFailed_retainsLockedStateWithErrorMessage() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(
            status = BiometricStatus.AVAILABLE,
            nextAuthResult = BiometricAuthResult.Failed
        )

        val viewModel = AppLockViewModel(settingsStore, authManager)
        assertTrue(viewModel.uiState.value.isLocked)

        viewModel.requestUnlock()

        assertTrue(viewModel.uiState.value.isLocked)
        assertNotNull(viewModel.uiState.value.authErrorMessage)
    }

    @Test
    fun requestUnlock_onCanceled_retainsLockedStateWithErrorMessage() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(
            status = BiometricStatus.AVAILABLE,
            nextAuthResult = BiometricAuthResult.Canceled
        )

        val viewModel = AppLockViewModel(settingsStore, authManager)
        assertTrue(viewModel.uiState.value.isLocked)

        viewModel.requestUnlock()

        assertTrue(viewModel.uiState.value.isLocked)
        assertNotNull(viewModel.uiState.value.authErrorMessage)
    }

    @Test
    fun toggleBiometricLock_whenNoHardware_failsWithoutChangingSettings() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = false))
        val authManager = FakeBiometricAuthManager(status = BiometricStatus.NO_HARDWARE)

        val viewModel = AppLockViewModel(settingsStore, authManager)

        var callbackSuccess: Boolean? = null
        var callbackMessage: String? = null
        viewModel.toggleBiometricLock(true) { success, msg ->
            callbackSuccess = success
            callbackMessage = msg
        }

        assertEquals(false, callbackSuccess)
        assertNotNull(callbackMessage)
        assertFalse(settingsStore.settings.value.isBiometricLockEnabled)
    }

    @Test
    fun toggleBiometricLock_whenNotEnrolled_failsWithoutChangingSettings() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = false))
        val authManager = FakeBiometricAuthManager(status = BiometricStatus.NOT_ENROLLED)

        val viewModel = AppLockViewModel(settingsStore, authManager)

        var callbackSuccess: Boolean? = null
        var callbackMessage: String? = null
        viewModel.toggleBiometricLock(true) { success, msg ->
            callbackSuccess = success
            callbackMessage = msg
        }

        assertEquals(false, callbackSuccess)
        assertNotNull(callbackMessage)
        assertFalse(settingsStore.settings.value.isBiometricLockEnabled)
    }

    @Test
    fun toggleBiometricLock_whenEnablingAndAuthSucceeds_updatesSettings() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = false))
        val authManager = FakeBiometricAuthManager(
            status = BiometricStatus.AVAILABLE,
            nextAuthResult = BiometricAuthResult.Success
        )

        val viewModel = AppLockViewModel(settingsStore, authManager)

        var callbackSuccess: Boolean? = null
        viewModel.toggleBiometricLock(true) { success, _ ->
            callbackSuccess = success
        }

        assertEquals(true, callbackSuccess)
        assertTrue(settingsStore.settings.value.isBiometricLockEnabled)
    }

    @Test
    fun toggleBiometricLock_whenDisablingAndAuthSucceeds_updatesSettings() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(
            status = BiometricStatus.AVAILABLE,
            nextAuthResult = BiometricAuthResult.Success
        )

        val viewModel = AppLockViewModel(settingsStore, authManager)

        var callbackSuccess: Boolean? = null
        viewModel.toggleBiometricLock(false) { success, _ ->
            callbackSuccess = success
        }

        assertEquals(true, callbackSuccess)
        assertFalse(settingsStore.settings.value.isBiometricLockEnabled)
    }

    @Test
    fun toggleBiometricLock_whenDisablingAndAuthFails_keepsSettingsEnabled() {
        val settingsStore = FakeSettingsStore(AppSettings(isBiometricLockEnabled = true))
        val authManager = FakeBiometricAuthManager(
            status = BiometricStatus.AVAILABLE,
            nextAuthResult = BiometricAuthResult.Failed
        )

        val viewModel = AppLockViewModel(settingsStore, authManager)

        var callbackSuccess: Boolean? = null
        viewModel.toggleBiometricLock(false) { success, _ ->
            callbackSuccess = success
        }

        assertEquals(false, callbackSuccess)
        assertTrue(settingsStore.settings.value.isBiometricLockEnabled)
    }
}

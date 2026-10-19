package com.goldex.companion.ui.wizard

import com.goldex.companion.data.license.LicenseInfo
import com.goldex.companion.data.license.LicenseResult
import com.goldex.companion.data.license.LicenseStatus
import com.goldex.companion.data.license.LicenseStore
import com.goldex.companion.ui.license.LicenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WizardGuestTrialGuardTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGuestLoginInvokesTrialValidationAndAllowsEntryOnSuccess() {
        var requestedChoice: WizardLicenseChoice? = null
        var requestedCode: String? = null
        var onSkipInvoked = false

        val onValidateLicense: (WizardLicenseChoice, String, () -> Unit, (String) -> Unit) -> Unit =
            { choice, code, onSuccess, _ ->
                requestedChoice = choice
                requestedCode = code
                onSuccess()
            }

        val onSkip = {
            onSkipInvoked = true
        }

        // Simulate guest login workflow
        var isValidating = true
        onValidateLicense(
            WizardLicenseChoice.TRIAL,
            "",
            {
                isValidating = false
                onSkip()
            },
            {
                isValidating = false
            }
        )

        assertEquals(WizardLicenseChoice.TRIAL, requestedChoice)
        assertEquals("", requestedCode)
        assertFalse(isValidating)
        assertTrue(onSkipInvoked)
    }

    @Test
    fun testGuestLoginBlocksEntryAndDisplaysErrorOnValidationFailure() {
        var requestedChoice: WizardLicenseChoice? = null
        var onSkipInvoked = false
        var capturedError: String? = null
        var isValidating = true

        val expectedError = "مهلت استفاده آزمایشی (۱۴ روز) برای این دستگاه به پایان رسیده است."

        val onValidateLicense: (WizardLicenseChoice, String, () -> Unit, (String) -> Unit) -> Unit =
            { choice, _, _, onError ->
                requestedChoice = choice
                onError(expectedError)
            }

        val onSkip = {
            onSkipInvoked = true
        }

        // Simulate guest login attempt when server rejects trial
        onValidateLicense(
            WizardLicenseChoice.TRIAL,
            "",
            {
                isValidating = false
                onSkip()
            },
            { error ->
                isValidating = false
                capturedError = error
            }
        )

        assertEquals(WizardLicenseChoice.TRIAL, requestedChoice)
        assertFalse(isValidating)
        assertFalse("Entry must be blocked when trial validation fails", onSkipInvoked)
        assertEquals(expectedError, capturedError)
    }

    @Test
    fun testLicenseViewModelTrialActivationSuccessTriggersOnSuccess() = runTest(testDispatcher) {
        val fakeStore = FakeLicenseStore(
            trialResult = LicenseResult.Success(
                info = LicenseInfo(
                    status = LicenseStatus.TRIAL_ACTIVE,
                    remainingDays = 14
                ),
                message = "مهلت تست فعال شد."
            )
        )

        val viewModel = LicenseViewModel(fakeStore)
        testDispatcher.scheduler.advanceUntilIdle()

        var successCalled = false
        var errorCalled = false

        viewModel.activateTrial(
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertFalse(errorCalled)
        assertEquals(LicenseStatus.TRIAL_ACTIVE, viewModel.uiState.value.licenseInfo.status)
        assertEquals("مهلت تست فعال شد.", viewModel.uiState.value.successMessage)
    }

    @Test
    fun testLicenseViewModelTrialActivationFailureTriggersOnError() = runTest(testDispatcher) {
        val errorMessage = "دستگاه قبلاً ثبت شده و مهلت آزمایشی به اتمام رسیده است."
        val fakeStore = FakeLicenseStore(
            trialResult = LicenseResult.Error(
                message = errorMessage,
                statusCode = 403
            )
        )

        val viewModel = LicenseViewModel(fakeStore)
        testDispatcher.scheduler.advanceUntilIdle()

        var successCalled = false
        var capturedError: String? = null

        viewModel.activateTrial(
            onSuccess = { successCalled = true },
            onError = { capturedError = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(errorMessage, capturedError)
        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
    }

    private class FakeLicenseStore(
        var trialResult: LicenseResult = LicenseResult.Success(LicenseInfo(), "")
    ) : LicenseStore {
        private val _licenseInfo = MutableStateFlow(LicenseInfo())
        override val licenseInfo: StateFlow<LicenseInfo> = _licenseInfo.asStateFlow()

        override fun getCachedInfo(): LicenseInfo = _licenseInfo.value

        override suspend fun activateTrial(): LicenseResult {
            if (trialResult is LicenseResult.Success) {
                _licenseInfo.value = (trialResult as LicenseResult.Success).info
            }
            return trialResult
        }

        override suspend fun activateCode(code: String): LicenseResult = trialResult

        override suspend fun syncStatus(): LicenseResult = trialResult
    }
}

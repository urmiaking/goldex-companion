package com.goldex.companion.ui.wizard

import androidx.lifecycle.ViewModel
import com.goldex.companion.data.AppSettings
import com.goldex.companion.domain.onboarding.CompleteOnboardingUseCase
import com.goldex.companion.domain.onboarding.OpeningInventoryInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnboardingCompletionState(val errorMessage: String? = null)

class OnboardingViewModel(private val completeOnboarding: CompleteOnboardingUseCase) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingCompletionState())
    val state = _state.asStateFlow()

    fun complete(settings: AppSettings, inventory: OpeningInventoryInput, restoredFromCloud: Boolean): Boolean {
        return try {
            completeOnboarding.complete(settings, inventory, restoredFromCloud)
            _state.value = OnboardingCompletionState()
            true
        } catch (_: Exception) {
            _state.value = OnboardingCompletionState("راه‌اندازی انجام نشد؛ داده‌های قبلی محفوظ است")
            false
        }
    }
}

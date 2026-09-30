package com.goldex.companion.ui.license

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.data.license.LicenseInfo
import com.goldex.companion.data.license.LicenseStore
import com.goldex.companion.data.license.LicenseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LicenseUiState(
    val licenseInfo: LicenseInfo = LicenseInfo(),
    val isActivationDialogVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class LicenseViewModel(private val repository: LicenseStore) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LicenseUiState(licenseInfo = repository.getCachedInfo())
    )
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.licenseInfo.collect { info ->
                _uiState.update { it.copy(licenseInfo = info) }
            }
        }
        // استعلام خاموش وضعیت اشتراک در پس‌زمینه
        syncStatus()
    }

    fun setActivationDialogVisible(visible: Boolean) {
        _uiState.update {
            it.copy(
                isActivationDialogVisible = visible,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun activateTrial(
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.activateTrial()) {
                is LicenseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            licenseInfo = result.info,
                            successMessage = result.message
                        )
                    }
                    onSuccess?.invoke()
                }
                is LicenseResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    onError?.invoke(result.message)
                }
            }
        }
    }

    fun activateCode(
        code: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.activateCode(code)) {
                is LicenseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            licenseInfo = result.info,
                            successMessage = result.message,
                            isActivationDialogVisible = false
                        )
                    }
                    onSuccess?.invoke()
                }
                is LicenseResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    onError?.invoke(result.message)
                }
            }
        }
    }

    fun syncStatus() {
        viewModelScope.launch {
            repository.syncStatus()
        }
    }
}

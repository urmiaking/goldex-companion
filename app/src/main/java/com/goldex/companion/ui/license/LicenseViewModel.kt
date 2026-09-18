package com.goldex.companion.ui.license

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.goldex.companion.data.license.LicenseInfo
import com.goldex.companion.data.license.LicenseRepository
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

class LicenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LicenseRepository(application.applicationContext)

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

class LicenseViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LicenseViewModel::class.java)) {
            return LicenseViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

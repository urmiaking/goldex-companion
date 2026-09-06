package com.goldex.companion.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.data.AppUpdateChecker
import com.goldex.companion.data.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val updateInfo: UpdateInfo? = null,
    val isCheckingForUpdate: Boolean = false,
    val isUpdateDialogDismissed: Boolean = false
)

class UpdateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdates(@Suppress("UNUSED_PARAMETER") manual: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isCheckingForUpdate = true, isUpdateDialogDismissed = false) }
            val info = try {
                AppUpdateChecker.check()
            } catch (_: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    updateInfo = info,
                    isCheckingForUpdate = false
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogDismissed = true) }
    }

    fun resetUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogDismissed = false) }
    }
}

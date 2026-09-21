package com.goldex.companion

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.data.SettingsRepository
import com.goldex.companion.data.security.AndroidBiometricAuthManager
import com.goldex.companion.ui.main.MainScreen
import com.goldex.companion.ui.main.MainViewModel
import com.goldex.companion.ui.security.AppLockScreen
import com.goldex.companion.ui.security.AppLockViewModel
import com.goldex.companion.ui.security.AppLockViewModelFactory
import com.goldex.companion.ui.theme.GoldExCompanionTheme

class MainActivity : FragmentActivity() {

    private val appLockViewModel: AppLockViewModel by viewModels {
        AppLockViewModelFactory(
            SettingsRepository.getInstance(applicationContext),
            AndroidBiometricAuthManager(applicationContext) { this }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val appLockState by appLockViewModel.uiState.collectAsState()

            GoldExCompanionTheme(isDarkTheme = uiState.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (appLockState.isLocked) {
                        AppLockScreen(
                            state = appLockState,
                            onUnlockClick = { appLockViewModel.requestUnlock() }
                        )
                    } else {
                        MainScreen(
                            mainViewModel = viewModel,
                            appLockViewModel = appLockViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLockViewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        appLockViewModel.onAppBackgrounded()
    }
}

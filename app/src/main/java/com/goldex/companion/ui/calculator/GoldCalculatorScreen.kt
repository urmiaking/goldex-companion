package com.goldex.companion.ui.calculator

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.ui.main.MainScreen
import com.goldex.companion.ui.main.MainViewModel

@Composable
fun GoldCalculatorScreen(
    viewModel: MainViewModel,
    viewModelFactory: ViewModelProvider.Factory
) {
    MainScreen(mainViewModel = viewModel, viewModelFactory = viewModelFactory)
}

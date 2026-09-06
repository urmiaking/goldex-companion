package com.goldex.companion.ui.calculator

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.ui.main.MainScreen
import com.goldex.companion.ui.main.MainViewModel

@Composable
fun GoldCalculatorScreen(
    viewModel: MainViewModel = viewModel()
) {
    MainScreen(mainViewModel = viewModel)
}

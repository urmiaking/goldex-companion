package com.goldex.companion.ui.calculator

import com.goldex.companion.ui.main.MainUiState
import com.goldex.companion.ui.main.MainViewModel

enum class AppTab(val titleFa: String) {
    HOME("خانه"),
    RATES("تابلوی مظنه"),
    CALCULATOR("ماشین‌حساب"),
    INVOICES("فاکتورها"),
    MORE("بیشتر")
}

typealias CalculatorUiState = MainUiState
typealias GoldCalculatorViewModel = MainViewModel

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

enum class PriceBasisTab(val labelFa: String) {
    K18("۱۸ عیار (۷۵۰)"),
    K24("۲۴ عیار (۹۹۹)"),
    MESGHAL("مظنه (مثقال)")
}

typealias CalculatorUiState = MainUiState
typealias GoldCalculatorViewModel = MainViewModel

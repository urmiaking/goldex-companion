package com.goldex.companion.desktop.calculator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modes of the GoldEx specialized multi-mode gold calculator and reference center.
 */
enum class DesktopCalculatorTab(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    JEWELRY(
        title = "طلای ساخته و جواهر",
        subtitle = "محاسبه ارزش طلا، اجرت، سود قانونی و ارزش افزوده",
        icon = Icons.Outlined.Diamond
    ),
    MELT(
        title = "مظنه و آبشده سنتی",
        subtitle = "تبدیل ۱۷ به ۱۸ و محاسبه قطعه آبشده و مثقال بازار",
        icon = Icons.Outlined.Scale
    ),
    COIN_BUBBLE(
        title = "تحلیل حباب مسکوکات",
        subtitle = "محاسبه ارزش ذاتی و حباب سکه بانکی بر پایه انس و دلار",
        icon = Icons.Outlined.MonetizationOn
    ),
    KARAT_CONVERT(
        title = "تبدیل عیار و ری‌گیری",
        subtitle = "تبدیل مستقیم عیار و محاسبه شرطی آزمایشگاه طلا",
        icon = Icons.Outlined.SwapHoriz
    ),
    STANDARD_FORMULAS(
        title = "فرمول‌های مصوب اتحادیه",
        subtitle = "مرجع رسمی قوانین محاسبه، مالیات و تبدیل‌های زرگری",
        icon = Icons.Outlined.MenuBook
    )
}

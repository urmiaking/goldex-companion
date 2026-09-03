package com.goldex.companion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.goldex.companion.R

val VazirmatnFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

val VazirmatnFeatureSettings = "ss01"

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings),
    headlineMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings),

    titleLarge = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),

    labelLarge = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VazirmatnFamily,
        fontFeatureSettings = VazirmatnFeatureSettings,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

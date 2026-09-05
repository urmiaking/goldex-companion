package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.theme.goldButtonGradient
import com.goldex.companion.ui.theme.goldButtonText
import com.goldex.companion.ui.theme.heroCardGradient

/**
 * Pixel-perfect Jewelry Calculator Tab adhering strictly to Google Stitch
 * Design Screen ID: 5e8f08bf3eea421a888f20d95c28262c and user reference.
 */
@Composable
fun JewelryTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    val result = uiState.jewelryResult
    val netWeight = result?.netWeight ?: (PersianNumberFormatter.parsePersianOrEnglish(uiState.grossWeightInput) ?: 0.0)
    val rawGoldValue = result?.rawGoldValue ?: 0.0
    val wageAmount = result?.wageAmount ?: 0.0
    val profitAmount = result?.profitAmount ?: 0.0
    val taxAmount = result?.taxAmount ?: 0.0
    val totalPayable = result?.totalPayable ?: 0.0

    // Custom Profit & Tax Dialog States (per Items 4 and 5)
    var isCustomProfitDialogVisible by remember { mutableStateOf(false) }
    var isCustomTaxDialogVisible by remember { mutableStateOf(false) }
    var tempProfitInput by remember(uiState.profitPercentInput) { mutableStateOf(uiState.profitPercentInput) }
    var tempTaxInput by remember(uiState.taxPercentInput) { mutableStateOf(uiState.taxPercentInput) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ─── Header: Icon + Title & Subtitle ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .border(0.6.dp, colors.goldBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CalcCalculate,
                    contentDescription = null,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "ماشین‌حساب تخصصی طلا و جواهر",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = "فرمول استاندارد اتحادیه طلا و جواهر ایران",
                    fontSize = 10.5.sp,
                    color = colors.textMuted
                )
            }
        }

        // ─── Card 1: Benchmark Rate (نرخ مبنا) ────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: نرخ مبنا & تغییر نرخ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = CalcToll,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "نرخ مبنا:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        val spotVal = PersianNumberFormatter.parseToCleanLong(uiState.spotPriceInput) ?: 0L
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(spotVal.toDouble())} تومان",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    // Button تغییر نرخ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setManualSpotDialogVisible(true) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تغییر نرخ",
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "تغییر نرخ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }

                // Segmented Pill Container: ۱۸ عیار / ۲۴ عیار / مظنه (Animated)
                LuxurySegmentedControl(
                    items = PriceBasisTab.values().toList(),
                    selectedItem = uiState.priceBasisTab,
                    onItemSelected = { viewModel.setPriceBasisTab(it) },
                    label = { it.labelFa },
                    modifier = Modifier.fillMaxWidth(),
                    height = 36.dp,
                    fontSize = 11.sp
                )
            }
        }

        // ─── Card 2: Gold Weight (وزن طلای کارشده) ────────────────────
        // ─── Card 2: Gold Weight (وزن طلای کارشده) ────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = CalcScale,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "وزن طلای کارشده",
                            fontFamily = VazirmatnFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }
                    Text(
                        text = "عیار ۷۵۰",
                        fontFamily = VazirmatnFamily,
                        fontSize = 11.5.sp,
                        color = colors.textMuted
                    )
                }

                // Large Input Box with "گرم"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "گرم",
                            fontFamily = VazirmatnFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        BasicTextField(
                            value = PersianNumberFormatter.toPersianDigits(uiState.grossWeightInput),
                            onValueChange = { viewModel.onGrossWeightChanged(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = VazirmatnFamily,
                                fontFeatureSettings = VazirmatnFeatureSettings,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Rtl
                            ),
                            cursorBrush = SolidColor(colors.goldPrimary),
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        // ─── Card 3: Wage (اجرت ساخت طلا) ─────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Title + Toggle (درصدی / تومانی)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = CalcHandyman,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "اجرت ساخت طلا",
                            fontFamily = VazirmatnFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    // Animated Mode Toggle (درصدی / تومانی)
                    LuxurySegmentedControl(
                        items = listOf(WageType.PERCENTAGE, WageType.TOMAN_PER_GRAM),
                        selectedItem = uiState.wageType,
                        onItemSelected = { viewModel.onWageTypeChanged(it) },
                        label = { if (it == WageType.PERCENTAGE) "درصدی (٪)" else "تومانی / گرم" },
                        modifier = Modifier.width(176.dp),
                        height = 32.dp,
                        fontSize = 10.5.sp
                    )
                }

                if (uiState.wageType == WageType.PERCENTAGE) {
                    // Stepper + Equivalent Wage Row (درصدی)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stepper: [-]  Value  [+]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surface,
                                    border = BorderStroke(0.5.dp, colors.border),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { viewModel.decrementWage() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CalcRemove,
                                            contentDescription = "کاهش",
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${PersianNumberFormatter.toPersianDigits(uiState.wageInput)}٪",
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surface,
                                    border = BorderStroke(0.5.dp, colors.border),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { viewModel.incrementWage() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "افزایش",
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Equivalent Toman Wage
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(wageAmount)} تومان",
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary
                                )
                                Text(
                                    text = "معادل ریالی اجرت",
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                } else {
                    // Direct Toman Input Box (تومانی) - Steppers removed per Item 3!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تومان / گرم",
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted
                            )
                            val cleanWage = PersianNumberFormatter.toEnglishDigits(uiState.wageInput).filter { it.isDigit() }
                            val displayWage = if (cleanWage.isNotBlank()) {
                                PersianNumberFormatter.formatPrice(cleanWage.toDoubleOrNull() ?: 0.0)
                            } else ""

                            BasicTextField(
                                value = displayWage,
                                onValueChange = { input ->
                                    val digitsOnly = PersianNumberFormatter.toEnglishDigits(input).filter { it.isDigit() }
                                    viewModel.onWageChanged(digitsOnly)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    textAlign = TextAlign.Right,
                                    textDirection = TextDirection.Rtl
                                ),
                                cursorBrush = SolidColor(colors.goldPrimary),
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                                decorationBox = { innerTextField ->
                                    if (displayWage.isEmpty()) {
                                        Text(
                                            text = "مبلغ اجرت هر گرم طلا...",
                                            fontFamily = VazirmatnFamily,
                                            fontSize = 12.5.sp,
                                            color = colors.textMuted.copy(alpha = 0.55f),
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }
            }
        }

        // ─── 2-Columns Grid: Profit (سود) & Tax (مالیات) ──────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 4A: سود
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = CalcPercent,
                                contentDescription = null,
                                tint = colors.goldSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "سود",
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = colors.goldContainer.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "اتحادیه",
                                fontFamily = VazirmatnFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(uiState.profitPercentInput)}٪",
                            fontFamily = VazirmatnFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(profitAmount)} ت",
                            fontFamily = VazirmatnFamily,
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }

                    // Quick Preset Chips (۷٪, ۲۰٪ و مداد ویرایش سفارشی per Item 4)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(7.0 to "۷٪", 20.0 to "۲۰٪").forEach { (preset, label) ->
                            val isSel = (PersianNumberFormatter.parsePersianOrEnglish(uiState.profitPercentInput) ?: 0.0) == preset
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                border = if (isSel) BorderStroke(0.6.dp, colors.goldPrimary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.applyPresetProfit(preset) }
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) colors.goldPrimary else colors.textMain,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        // Pencil icon button for custom profit
                        val isCustom = (PersianNumberFormatter.parsePersianOrEnglish(uiState.profitPercentInput) ?: 0.0) !in listOf(7.0, 20.0)
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = if (isCustom) colors.goldContainer else colors.surfaceElevated,
                            border = if (isCustom) BorderStroke(0.6.dp, colors.goldPrimary) else null,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    tempProfitInput = uiState.profitPercentInput
                                    isCustomProfitDialogVisible = true
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "سود دلخواه",
                                    tint = if (isCustom) colors.goldPrimary else colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Card 4B: مالیات
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = CalcAccountBalance,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "مالیات",
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = colors.profitGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "قانونی",
                                fontFamily = VazirmatnFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.profitGreen,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(uiState.taxPercentInput)}٪",
                            fontFamily = VazirmatnFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(taxAmount)} ت",
                            fontFamily = VazirmatnFamily,
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }

                    // Subtitle & Pencil Edit Button (per Item 5)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "روی اجرت و سود",
                            fontFamily = VazirmatnFamily,
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = colors.surfaceElevated,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    tempTaxInput = uiState.taxPercentInput
                                    isCustomTaxDialogVisible = true
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش مالیات",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── Card 5: Stone Deduction (کسر وزن سنگ و نگین) ──────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CalcDiamond,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "کسر وزن سنگ و نگین",
                            fontFamily = VazirmatnFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "کسر خودکار از وزن کل",
                            fontFamily = VazirmatnFamily,
                            fontSize = 9.5.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stoneVal = PersianNumberFormatter.parsePersianOrEnglish(uiState.stoneWeightInput) ?: 0.0
                    Text(
                        text = "${PersianNumberFormatter.formatWeight(stoneVal)} گرم",
                        fontFamily = VazirmatnFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.5.dp, colors.border),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { viewModel.setStoneWeightDialogVisible(true) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش سنگ",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── Card 6: Hero Obsidian Dark Monitor (مبلغ نهایی قابل پرداخت) ─
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f)),
            shadowElevation = if (colors.isDark) 0.dp else 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.heroCardGradient)
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Header of Hero Card: Receipt Icon + Title + Share Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.goldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CalcReceipt,
                                contentDescription = null,
                                tint = colors.goldSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = "مبلغ نهایی قابل پرداخت (فروش طلا)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                val text = "مبلغ قابل پرداخت فاکتور طلا: ${PersianNumberFormatter.formatPrice(totalPayable)} تومان\nوزن خالص: ${PersianNumberFormatter.formatWeight(netWeight)} گرم"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری فاکتور سریع"))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = CalcShare,
                                contentDescription = "اشتراک‌گذاری",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Center Price Display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = PersianNumberFormatter.formatPrice(totalPayable),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "تومان",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (totalPayable > 0) PersianWordsFormatter.toWords(totalPayable.toLong()) + " تومان" else "صفر تومان",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                // 3-Columns Metrics Breakdown
                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Box 1: وزن خالص
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("وزن خالص", fontSize = 10.sp, color = Color.White.copy(alpha = 0.55f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(netWeight)} گرم",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Box 2: طلای خام
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("طلای خام", fontSize = 10.sp, color = Color.White.copy(alpha = 0.55f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PersianNumberFormatter.formatPrice(rawGoldValue),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Box 3: اجرت+سود+مالیات
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("اجرت+سود+مالیات", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.55f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PersianNumberFormatter.formatPrice(wageAmount + profitAmount + taxAmount),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldSecondary
                            )
                        }
                    }
                }
            }
        }

        // ─── Action Buttons ──────────────────────────────────────────
        // 1. Primary Button: صدور و ثبت فاکتور رسمی (پیل زرین با متن برنزی تیره)
        GoldButton(
            text = "صدور و ثبت فاکتور رسمی",
            icon = CalcPostAdd,
            onClick = {
                viewModel.addItemToInvoice()
                viewModel.selectTab(AppTab.INVOICES)
                Toast.makeText(context, "قطعه به سبد فاکتور افزوده شد ✓", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Secondary Row: صفر کردن مقادیر & کپی خلاصه محاسبه
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable {
                        viewModel.resetJewelry()
                        Toast.makeText(context, "مقادیر بازنشانی شدند", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = CalcRestartAlt,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "صفر کردن مقادیر",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val summary = buildString {
                            appendLine("📋 محاسبه طلا و جواهر (قیراط)")
                            appendLine("─────────────────────")
                            appendLine("وزن کارشده: ${PersianNumberFormatter.formatWeight(netWeight)} گرم")
                            appendLine("مبنای مظنه: ${PersianNumberFormatter.formatPrice(rawGoldValue)} ت")
                            appendLine("اجرت ساخت: ${PersianNumberFormatter.formatPrice(wageAmount)} ت")
                            appendLine("سود فروشنده: ${PersianNumberFormatter.formatPrice(profitAmount)} ت")
                            appendLine("مالیات قانونی: ${PersianNumberFormatter.formatPrice(taxAmount)} ت")
                            appendLine("مبلغ نهایی: ${PersianNumberFormatter.formatPrice(totalPayable)} تومان")
                        }
                        clipboard.setPrimaryClip(ClipData.newPlainText("Gold Calculation", summary))
                        Toast.makeText(context, "خلاصه محاسبه در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = CalcContentCopy,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "کپی خلاصه محاسبه",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ─── Dialog: تغییر نرخ دستی طلا ──────────────────────────────
    if (uiState.isManualSpotDialogVisible) {
        Dialog(onDismissRequest = { viewModel.setManualSpotDialogVisible(false) }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تنظیم و تغییر نرخ مبنا",
                            fontFamily = VazirmatnFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        if (uiState.rates.gold18 > 0L) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.profitGreen.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, colors.profitGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    viewModel.applyPresetSpotPrice(uiState.rates.gold18)
                                    viewModel.setManualSpotDialogVisible(false)
                                }
                            ) {
                                Text(
                                    text = "نرخ زنده: ${PersianNumberFormatter.formatPrice(uiState.rates.gold18.toDouble())}",
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.profitGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    GoldInputField(
                        value = uiState.spotPriceInput,
                        onValueChange = { viewModel.onSpotPriceChanged(it) },
                        label = "نرخ هر گرم به تومان",
                        trailingText = "تومان",
                        useThousandsSeparator = true
                    )

                    GoldButton(
                        text = "تایید نرخ مبنا",
                        onClick = { viewModel.setManualSpotDialogVisible(false) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // ─── Dialog: ویرایش کسر وزن سنگ و نگین ────────────────────────
    if (uiState.isStoneWeightDialogVisible) {
        Dialog(onDismissRequest = { viewModel.setStoneWeightDialogVisible(false) }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "کسر وزن سنگ، نگین و چرم",
                        fontFamily = VazirmatnFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )

                    GoldInputField(
                        value = uiState.stoneWeightInput,
                        onValueChange = { viewModel.onStoneWeightChanged(it) },
                        label = "وزن سنگ و نگین بر حسب گرم",
                        trailingText = "گرم",
                        isDecimal = true,
                        useThousandsSeparator = false
                    )

                    GoldButton(
                        text = "تایید کسر وزن",
                        onClick = { viewModel.setStoneWeightDialogVisible(false) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // ─── Dialog: تنظیم سود دلخواه (per Item 4) ────────────────────
    if (isCustomProfitDialogVisible) {
        Dialog(onDismissRequest = { isCustomProfitDialogVisible = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.goldContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CalcMonetizationOn,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "تنظیم سود فروشندگی",
                            fontFamily = VazirmatnFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    Text(
                        text = "درصد سود مورد نظر را وارد کرده یا از مقادیر پیش‌فرض انتخاب فرمایید:",
                        fontFamily = VazirmatnFamily,
                        fontSize = 11.5.sp,
                        color = colors.textMuted
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "٪",
                                fontFamily = VazirmatnFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                            BasicTextField(
                                value = PersianNumberFormatter.toPersianDigits(tempProfitInput),
                                onValueChange = { tempProfitInput = PersianNumberFormatter.toEnglishDigits(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    textAlign = TextAlign.Right,
                                    textDirection = TextDirection.Rtl
                                ),
                                cursorBrush = SolidColor(colors.goldPrimary),
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                        }
                    }

                    // Quick Chips in dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("۵", "۷", "۱۰", "۱۵", "۲۰").forEach { chip ->
                            val isSel = tempProfitInput == chip || tempProfitInput.toDoubleOrNull() == chip.toDoubleOrNull()
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                border = if (isSel) BorderStroke(0.6.dp, colors.goldPrimary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tempProfitInput = chip }
                            ) {
                                Text(
                                    text = "${PersianNumberFormatter.toPersianDigits(chip)}٪",
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) colors.goldPrimary else colors.textMain,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoldButton(
                            text = "ثبت و تایید",
                            onClick = {
                                viewModel.onProfitPercentChanged(tempProfitInput)
                                isCustomProfitDialogVisible = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { isCustomProfitDialogVisible = false },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(0.7f).height(48.dp),
                            border = BorderStroke(0.6.dp, colors.border)
                        ) {
                            Text(
                                text = "انصراف",
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // ─── Dialog: تنظیم مالیات دلخواه (per Item 5) ───────────────────
    if (isCustomTaxDialogVisible) {
        Dialog(onDismissRequest = { isCustomTaxDialogVisible = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.goldContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CalcAccountBalance,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "تنظیم مالیات بر ارزش افزوده",
                            fontFamily = VazirmatnFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    Text(
                        text = "درصد مالیات ارزش افزوده (طبق قانون مالیات، ۹٪ روی اجرت و سود اعمال می‌شود):",
                        fontFamily = VazirmatnFamily,
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        lineHeight = 16.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "٪",
                                fontFamily = VazirmatnFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                            BasicTextField(
                                value = PersianNumberFormatter.toPersianDigits(tempTaxInput),
                                onValueChange = { tempTaxInput = PersianNumberFormatter.toEnglishDigits(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    textAlign = TextAlign.Right,
                                    textDirection = TextDirection.Rtl
                                ),
                                cursorBrush = SolidColor(colors.goldPrimary),
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                        }
                    }

                    // Quick Chips for Tax (۰٪ معاف, ۹٪ قانونی)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("0" to "۰٪ (معافیت)", "9" to "۹٪ (نرخ مصوب)").forEach { (chip, label) ->
                            val isSel = tempTaxInput == chip || (chip == "0" && tempTaxInput.toDoubleOrNull() == 0.0) || (chip == "9" && tempTaxInput.toDoubleOrNull() == 9.0)
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                border = if (isSel) BorderStroke(0.6.dp, colors.goldPrimary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tempTaxInput = chip }
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = VazirmatnFamily,
                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) colors.goldPrimary else colors.textMain,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoldButton(
                            text = "ثبت و تایید",
                            onClick = {
                                viewModel.onTaxPercentChanged(tempTaxInput)
                                isCustomTaxDialogVisible = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { isCustomTaxDialogVisible = false },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(0.7f).height(48.dp),
                            border = BorderStroke(0.6.dp, colors.border)
                        ) {
                            Text(
                                text = "انصراف",
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

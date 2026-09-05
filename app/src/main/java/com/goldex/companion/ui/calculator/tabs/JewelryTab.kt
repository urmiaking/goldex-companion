package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.theme.heroCardGradient

/**
 * Rebuilt Jewelry Calculator Tab adhering strictly to Google Stitch
 * Design Screen ID: 5e8f08bf3eea421a888f20d95c28262c
 * ("ماشین‌حساب تخصصی طلا و جواهر - استاندارد اتحادیه طلا و جواهر ایران")
 */
@Composable
fun JewelryTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current
    var isManualSpotExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ─── Subheader / Navigation Bar ──────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.goldBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ماشین‌حساب تخصصی طلا و جواهر",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "فرمول رسمی مصوب اتحادیه طلا و جواهر کشور",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Quick Reset Action
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border),
                    modifier = Modifier.clickable {
                        viewModel.resetJewelry()
                        Toast.makeText(context, "مقادیر محاسبه بازنشانی شدند", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "بازنشانی",
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "صفر کردن",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        // ─── 1. Benchmark Rate Card (مبنای مظنه طلا خام) ──────────────
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hairline gold top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(colors.goldGradient)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "مبنای قیمت طلا خام ۱۸ عیار",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                        }

                        // Sync / Manual Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.autoSyncPrice) colors.profitGreen.copy(alpha = 0.12f) else colors.goldContainer,
                            border = BorderStroke(
                                0.6.dp,
                                if (uiState.autoSyncPrice) colors.profitGreen.copy(alpha = 0.4f) else colors.goldPrimary
                            ),
                            modifier = Modifier.clickable {
                                isManualSpotExpanded = !isManualSpotExpanded
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.autoSyncPrice) colors.profitGreen else colors.goldPrimary)
                                )
                                Text(
                                    text = if (uiState.autoSyncPrice) "زنده بازار" else "دستی",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.autoSyncPrice) colors.profitGreen else colors.goldPrimary
                                )
                                Icon(
                                    imageVector = if (isManualSpotExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (uiState.autoSyncPrice) colors.profitGreen else colors.goldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Spot Rate Hero Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "نرخ هر گرم طلای ۱۸ عیار (۷۵۰):",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                                val priceLong = PersianNumberFormatter.parseToCleanLong(uiState.spotPriceInput) ?: 0L
                                Text(
                                    text = if (priceLong > 0) PersianWordsFormatter.toWords(priceLong) + " تومان" else "",
                                    fontSize = 9.sp,
                                    color = colors.textMuted
                                )
                            }
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatPrice(uiState.spotPriceInput.toDoubleOrNull() ?: 0.0)} تومان",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }

                    // Collapsible manual spot price input
                    AnimatedVisibility(
                        visible = isManualSpotExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoldInputField(
                                value = uiState.spotPriceInput,
                                onValueChange = { viewModel.onSpotPriceChanged(it) },
                                label = "قیمت دستی هر گرم طلا ۱۸ عیار",
                                trailingText = "تومان",
                                useThousandsSeparator = true
                            )

                            // Quick button to re-sync with live feed
                            if (!uiState.autoSyncPrice && uiState.rates.gold18 > 0L) {
                                Button(
                                    onClick = {
                                        viewModel.toggleAutoSyncPrice(true)
                                        isManualSpotExpanded = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.profitGreen
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.6.dp, colors.profitGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "بازگشت به نرخ زنده تابلوی بازار (${PersianNumberFormatter.formatPrice(uiState.rates.gold18.toDouble())} ت)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Karat Selection Chips (750 / 875 / 999)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "عیار قطعه کار شده:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Karat.values().forEach { karat ->
                                val isSelected = uiState.selectedKarat == karat
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colors.goldContainer else colors.surfaceElevated,
                                    border = BorderStroke(
                                        if (isSelected) 1.2.dp else 0.5.dp,
                                        if (isSelected) colors.goldPrimary else colors.border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.onKaratSelected(karat) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = karat.labelFa,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.goldPrimary else colors.textMain
                                        )
                                        Text(
                                            text = "خلوص ${karat.karatNumber}/۲۴",
                                            fontSize = 9.sp,
                                            color = if (isSelected) colors.goldSecondary else colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── 2. Worked Gold Weight Card (وزن کار طلا) ────────────────
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "وزن کار طلا (ترازو زرگری)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }
                    Text(
                        text = "دقت ۳ رقم اعشار (میلی‌گرم)",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }

                // Gross Weight Input
                GoldInputField(
                    value = uiState.grossWeightInput,
                    onValueChange = { viewModel.onGrossWeightChanged(it) },
                    label = "وزن ناخالص قطعه کار شده",
                    trailingText = "گرم",
                    isDecimal = true,
                    useThousandsSeparator = false
                )

                // Quick Weight Increment Stepper Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1.0 to "+۱ گرم", 5.0 to "+۵ گرم", 10.0 to "+۱۰ گرم", 20.0 to "+۲۰ گرم").forEach { (step, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.5.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.addGrossWeight(step) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.goldPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── 3. Crafting Wage Card (اجرت ساخت کارگاه) ────────────────
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Architecture,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "اجرت ساخت کارگاه",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    // Wage Type Segmented Control (درصدی vs تومانی)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .border(0.6.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        WageType.values().forEach { type ->
                            val isSelected = uiState.wageType == type
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) colors.goldPrimary else Color.Transparent,
                                modifier = Modifier.clickable { viewModel.onWageTypeChanged(type) }
                            ) {
                                Text(
                                    text = if (type == WageType.PERCENTAGE) "درصدی (٪)" else "تومانی / گرم",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Wage Input with Minus/Plus Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decrement Stepper
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.goldBorder),
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                val current = PersianNumberFormatter.parsePersianOrEnglish(uiState.wageInput) ?: 0.0
                                val step = if (uiState.wageType == WageType.PERCENTAGE) 1.0 else 10_000.0
                                val next = (current - step).coerceAtLeast(0.0)
                                viewModel.onWageChanged(if (next % 1.0 == 0.0) next.toLong().toString() else next.toString())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "کاهش", tint = colors.goldPrimary)
                        }
                    }

                    // Wage Input Field
                    Box(modifier = Modifier.weight(1f)) {
                        GoldInputField(
                            value = uiState.wageInput,
                            onValueChange = { viewModel.onWageChanged(it) },
                            label = if (uiState.wageType == WageType.PERCENTAGE) "درصد اجرت ساخت" else "مبلغ اجرت هر گرم",
                            trailingText = if (uiState.wageType == WageType.PERCENTAGE) "٪" else "تومان",
                            isDecimal = uiState.wageType == WageType.PERCENTAGE,
                            useThousandsSeparator = uiState.wageType == WageType.TOMAN_PER_GRAM
                        )
                    }

                    // Increment Stepper
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.goldBorder),
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                val current = PersianNumberFormatter.parsePersianOrEnglish(uiState.wageInput) ?: 0.0
                                val step = if (uiState.wageType == WageType.PERCENTAGE) 1.0 else 10_000.0
                                val next = current + step
                                viewModel.onWageChanged(if (next % 1.0 == 0.0) next.toLong().toString() else next.toString())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "افزایش", tint = colors.goldPrimary)
                        }
                    }
                }

                // Quick Wage Presets (5%, 7%, 9%, 12%, 15%, 18%, 22%)
                if (uiState.wageType == WageType.PERCENTAGE) {
                    val wageScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(wageScrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("5" to "۵٪", "7" to "۷٪", "9" to "۹٪", "12" to "۱۲٪", "15" to "۱۵٪", "18" to "۱۸٪", "22" to "۲۲٪").forEach { (wage, label) ->
                            val isSel = PersianNumberFormatter.toEnglishDigits(uiState.wageInput) == wage
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                border = BorderStroke(
                                    if (isSel) 1.dp else 0.5.dp,
                                    if (isSel) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier.clickable { viewModel.onWageChanged(wage) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) colors.goldPrimary else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Dynamic Rial Equivalent Container
                uiState.jewelryResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معادل ریالی اجرت کل:",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatPrice(res.wageAmount)} تومان",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldSecondary
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Profit & Legal VAT Card (سود زرگر و مالیات قانونی) ───────
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Percent,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "سود مصوب اتحادیه و مالیات بر ارزش افزوده",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }
                }

                // Profit Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سود قانونی فروشنده (زرگر):",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        uiState.jewelryResult?.let { res ->
                            Text(
                                text = "مبلغ: ${PersianNumberFormatter.formatPrice(res.profitAmount)} تومان",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldSecondary
                            )
                        }
                    }

                    // Profit Presets (5%, 7% مصوب اتحادیه, 9%, 10%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5.0 to "۵٪", 7.0 to "۷٪ (مصوب)", 9.0 to "۹٪", 10.0 to "۱۰٪").forEach { (profit, label) ->
                            val isSel = uiState.profitPercentInput == (if (profit % 1.0 == 0.0) profit.toLong().toString() else profit.toString())
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                border = BorderStroke(
                                    if (isSel) 1.dp else 0.5.dp,
                                    if (isSel) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.applyPresetProfit(profit) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) colors.goldPrimary else colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                Divider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

                // Legal VAT Section (9% on wage + profit only)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "مالیات بر ارزش افزوده (۹٪ قانونی):",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                            // Legal Exemption Badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colors.profitGreen.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, colors.profitGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "معافیت اصل طلا",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.profitGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        uiState.jewelryResult?.let { res ->
                            Text(
                                text = "مبلغ: ${PersianNumberFormatter.formatPrice(res.taxAmount)} تومان",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldSecondary
                            )
                        }
                    }

                    Text(
                        text = "مطابق ماده ۲۶ قانون مالیات بر ارزش افزوده، اصل طلا ۱۰۰٪ از مالیات معاف بوده و مالیات ۹٪ منحصراً به اجرت ساخت و سود زرگر تعلق می‌گیرد.",
                        fontSize = 9.sp,
                        color = colors.textMuted,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // ─── 5. Stone/Gem Deduction Card (کسر وزن سنگ و متعلقات) ────────
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "کسر وزن سنگ، نگین و متعلقات",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }
                    uiState.jewelryResult?.let { res ->
                        Text(
                            text = "وزن خالص: ${PersianNumberFormatter.formatWeight(res.netWeight)} گرم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }

                GoldInputField(
                    value = uiState.stoneWeightInput,
                    onValueChange = { viewModel.onStoneWeightChanged(it) },
                    label = "وزن سنگ یا متعلقات غیرطلا برای کسر",
                    trailingText = "گرم",
                    isDecimal = true,
                    useThousandsSeparator = false
                )
            }
        }

        // ─── 6. Hero Dark Card (صورتحساب قطعه طلا) ───────────────────
        uiState.jewelryResult?.let { res ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                border = BorderStroke(0.8.dp, colors.goldBorder),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.heroCardGradient)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header of Hero Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "مبلغ نهایی و قابل پرداخت",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Share / Copy Icon Action
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val invoiceText = res.formatInvoice(
                                        spotPrice = PersianNumberFormatter.parseToCleanLong(uiState.spotPriceInput) ?: 0L,
                                        karat = uiState.selectedKarat
                                    )
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Gold Invoice", invoiceText))
                                    Toast.makeText(context, "فاکتور محاسبه در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Large Total Price Ticker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedPriceTicker(
                            text = "${PersianNumberFormatter.formatPrice(res.totalPayable)} تومان",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.goldPrimary
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Amount in Persian written words
                        Text(
                            text = PersianWordsFormatter.toWords(res.totalPayable.toLong()) + " تومان",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // 3-Column Detailed Breakdown Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Net Weight
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("وزن خالص", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(res.netWeight)} گ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // 2. Raw Gold Value
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ارزش اصل طلا", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(res.rawGoldValue)} ت",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldSecondary
                            )
                        }

                        // 3. Wage + Profit + Tax
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("اجرت+سود+مالیات", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(2.dp))
                            val overhead = res.wageAmount + res.profitAmount + res.taxAmount
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(overhead)} ت",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.profitGreen
                            )
                        }
                    }

                    // Effective Price per Gram Footer
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نرخ تمام‌شده هر گرم برای خریدار:",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(res.effectiveGramPrice)} تومان",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }
                }
            }
        }

        // ─── 7. Action Buttons ───────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Primary Button: صدور و ثبت فاکتور رسمی
            Button(
                onClick = {
                    viewModel.addItemToInvoice()
                    Toast.makeText(context, "قطعه با موفقیت به پیش‌نویس فاکتورها افزوده شد ✓", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.goldPrimary,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PostAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "صدور و ثبت فاکتور رسمی",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary Button: کپی خلاصه فاکتور
            OutlinedButton(
                onClick = {
                    val res = uiState.jewelryResult
                    if (res != null) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val invoiceText = res.formatInvoice(
                            spotPrice = PersianNumberFormatter.parseToCleanLong(uiState.spotPriceInput) ?: 0L,
                            karat = uiState.selectedKarat
                        )
                        clipboard.setPrimaryClip(ClipData.newPlainText("Gold Invoice", invoiceText))
                        Toast.makeText(context, "خلاصه محاسبه کپی شد ✓", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                border = BorderStroke(0.8.dp, colors.goldBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "کپی خلاصه محاسبه به عنوان فاکتور",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

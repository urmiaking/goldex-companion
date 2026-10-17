package com.goldex.companion.desktop.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopFilterChip
import com.goldex.companion.desktop.ui.controls.DesktopOutlinedTextField
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.heroCardGradient
import com.goldex.companion.ui.util.ThousandsSeparatorVisualTransformation

@Composable
fun DesktopMeltCalculatorPane(
    rates: MarketRates,
    onNavigateToInvoice: () -> Unit = {}
) {
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current

    var mesghalInput by remember { mutableStateOf(if (rates.goldMelt > 0) rates.goldMelt.toString() else "101500000") }
    var weightInput by remember { mutableStateOf("10.000") }
    var karatInput by remember { mutableStateOf("705") }
    var copied by remember { mutableStateOf(false) }

    val mesghalPrice = PersianNumberFormatter.toEnglishDigits(mesghalInput).toLongOrNull() ?: 0L
    val weightGrams = PersianNumberFormatter.toEnglishDigits(weightInput).toDoubleOrNull() ?: 0.0
    val karatValue = PersianNumberFormatter.toEnglishDigits(karatInput).toIntOrNull() ?: 705

    // Calculations based on official gold guild standards
    val gram18kPrice = if (mesghalPrice > 0) (mesghalPrice / GoldCalculationUseCases.MESGHAL_TO_GRAM_18K_RATIO).toLong() else 0L
    val equivalent18kWeight = if (karatValue > 0) weightGrams * (karatValue.toDouble() / 750.0) else weightGrams
    val totalValue = equivalent18kWeight * gram18kPrice
    val weightDiff = equivalent18kWeight - weightGrams

    fun buildSummaryText(): String = buildString {
        appendLine("📜 صورت‌حساب معامله آبشده و مثقال (قیراط)")
        appendLine("----------------------------------------")
        appendLine("مظنه روز (مثقال ۱۷): ${PersianNumberFormatter.formatPrice(mesghalPrice.toDouble())} تومان")
        appendLine("وزن قطعه آبشده: ${PersianNumberFormatter.formatAccountWeight(weightGrams)} گرم")
        appendLine("عیار قطعه (انگ): ${PersianNumberFormatter.toPersianDigits(karatValue)}")
        appendLine("----------------------------------------")
        appendLine("نرخ معادل هر گرم ۱۸ عیار: ${PersianNumberFormatter.formatPrice(gram18kPrice.toDouble())} تومان")
        appendLine("وزن شرطی (معادل ۱۸ عیار): ${PersianNumberFormatter.formatAccountWeight(equivalent18kWeight)} گرم")
        appendLine("ارزش کل قطعه: ${PersianNumberFormatter.formatPrice(totalValue)} تومان")
        if (totalValue > 0) {
            appendLine("به حروف: ${PersianWordsFormatter.toWords(totalValue.toLong())}")
        }
        appendLine("========================================")
        appendLine("محاسبه‌شده با فرمول رسمی اتحادیه طلا و جواهر")
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 940.dp
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MeltInputCard(
                        mesghalInput = mesghalInput,
                        onMesghalChange = { mesghalInput = it; copied = false },
                        weightInput = weightInput,
                        onWeightChange = { weightInput = it; copied = false },
                        karatInput = karatInput,
                        onKaratChange = { karatInput = it; copied = false },
                        liveMeltRate = rates.goldMelt,
                        onUseLiveRate = {
                            if (rates.goldMelt > 0) {
                                mesghalInput = rates.goldMelt.toString()
                                copied = false
                            }
                        }
                    )
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MeltResultCard(
                        totalValue = totalValue,
                        gram18kPrice = gram18kPrice,
                        equivalent18kWeight = equivalent18kWeight,
                        weightDiff = weightDiff,
                        copied = copied,
                        onCopy = {
                            try {
                                clipboard.setText(AnnotatedString(buildSummaryText()))
                            } catch (_: Throwable) {}
                            copied = true
                        },
                        onReset = {
                            mesghalInput = if (rates.goldMelt > 0) rates.goldMelt.toString() else "101500000"
                            weightInput = ""
                            karatInput = "705"
                            copied = false
                        },
                        onNavigateToInvoice = onNavigateToInvoice
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                MeltInputCard(
                    mesghalInput = mesghalInput,
                    onMesghalChange = { mesghalInput = it; copied = false },
                    weightInput = weightInput,
                    onWeightChange = { weightInput = it; copied = false },
                    karatInput = karatInput,
                    onKaratChange = { karatInput = it; copied = false },
                    liveMeltRate = rates.goldMelt,
                    onUseLiveRate = {
                        if (rates.goldMelt > 0) {
                            mesghalInput = rates.goldMelt.toString()
                            copied = false
                        }
                    }
                )

                MeltResultCard(
                    totalValue = totalValue,
                    gram18kPrice = gram18kPrice,
                    equivalent18kWeight = equivalent18kWeight,
                    weightDiff = weightDiff,
                    copied = copied,
                    onCopy = {
                        try {
                            clipboard.setText(AnnotatedString(buildSummaryText()))
                        } catch (_: Throwable) {}
                        copied = true
                    },
                    onReset = {
                        mesghalInput = if (rates.goldMelt > 0) rates.goldMelt.toString() else "101500000"
                        weightInput = ""
                        karatInput = "705"
                        copied = false
                    },
                    onNavigateToInvoice = onNavigateToInvoice
                )
            }
        }
    }
}

@Composable
private fun MeltInputCard(
    mesghalInput: String,
    onMesghalChange: (String) -> Unit,
    weightInput: String,
    onWeightChange: (String) -> Unit,
    karatInput: String,
    onKaratChange: (String) -> Unit,
    liveMeltRate: Long,
    onUseLiveRate: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("melt-input-card")
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Scale, null, Modifier.size(20.dp), tint = colors.goldPrimary)
                    }
                    Column {
                        Text("ورودی‌های معامله آبشده", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textMain)
                        Text("مظنه مثقال ۱۷ و مشخصات قطعه آبشده", fontSize = 11.5.sp, color = colors.textMuted)
                    }
                }

                if (liveMeltRate > 0) {
                    DesktopActionButton(
                        text = "مظنه زنده بازار",
                        onClick = onUseLiveRate,
                        icon = Icons.Outlined.Sync,
                        isSecondary = true,
                        height = 38.dp,
                        modifier = Modifier.testTag("melt-use-live-rate")
                    )
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            // Mesghal price input
            DesktopOutlinedTextField(
                value = mesghalInput,
                onValueChange = onMesghalChange,
                label = { Text("مظنه آبشده (یک مثقال ۱۷)") },
                prefix = { Text("تومان ", color = colors.textMuted, fontSize = 13.sp) },
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("melt-mesghal-field")
            )

            // Weight input
            DesktopOutlinedTextField(
                value = weightInput,
                onValueChange = onWeightChange,
                label = { Text("وزن قطعه آبشده (ترازوی طلافروشی)") },
                prefix = { Text("گرم ", color = colors.textMuted, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("melt-weight-field")
            )

            // Karat input and preset chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DesktopOutlinedTextField(
                    value = karatInput,
                    onValueChange = onKaratChange,
                    label = { Text("عیار قطعه (انگ ری‌گیری یا استاندارد)") },
                    prefix = { Text("عیار ", color = colors.textMuted, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("melt-karat-field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("عیارهای رایج:", fontSize = 12.sp, color = colors.textMuted)
                    DesktopFilterChip(
                        text = "۷۰۵ (مظنه سنتی)",
                        selected = karatInput == "705",
                        onClick = { onKaratChange("705") },
                        modifier = Modifier.testTag("melt-karat-705")
                    )
                    DesktopFilterChip(
                        text = "۷۳۵ (متفرقه مستعمل)",
                        selected = karatInput == "735",
                        onClick = { onKaratChange("735") },
                        modifier = Modifier.testTag("melt-karat-735")
                    )
                    DesktopFilterChip(
                        text = "۷۵۰ (استاندارد ۱۸)",
                        selected = karatInput == "750",
                        onClick = { onKaratChange("750") },
                        modifier = Modifier.testTag("melt-karat-750")
                    )
                }
            }
        }
    }
}

@Composable
private fun MeltResultCard(
    totalValue: Double,
    gram18kPrice: Long,
    equivalent18kWeight: Double,
    weightDiff: Double,
    copied: Boolean,
    onCopy: () -> Unit,
    onReset: () -> Unit,
    onNavigateToInvoice: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth().testTag("melt-result-card")
    ) {
        Column(
            Modifier.fillMaxWidth().background(colors.heroCardGradient).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors.profitGreen))
                    Text("ارزش کل قطعه آبشده ۱۸ عیار", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.goldPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        "ارزش تسویه معاملاتی",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedPriceTicker(
                    text = "${PersianNumberFormatter.formatPrice(totalValue)} تومان",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                if (totalValue > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = PersianWordsFormatter.toWords(totalValue.toLong()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Breakdown card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("قیمت هر گرم ۱۸ عیار (مظنه ÷ ۴.۳۳۱۸):", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "${PersianNumberFormatter.formatPrice(gram18kPrice.toDouble())} تومان",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("وزن شرطی (معادل طلای ۱۸ عیار):", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "${PersianNumberFormatter.formatAccountWeight(equivalent18kWeight)} گرم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (kotlin.math.abs(weightDiff) > 1e-4) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تفاوت وزنی با عیار ۷۵۰:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                            Text(
                                "${if (weightDiff > 0) "+" else ""}${PersianNumberFormatter.formatAccountWeight(weightDiff)} گرم",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weightDiff >= 0) colors.profitGreen else colors.errorRed
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DesktopActionButton(
                    text = if (copied) "پیام معامله کپی شد ✓" else "کپی پیام معامله برای طرف‌حساب",
                    onClick = onCopy,
                    icon = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    modifier = Modifier.fillMaxWidth().testTag("melt-copy-summary")
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopActionButton(
                        text = "پاک‌سازی فرم",
                        onClick = onReset,
                        icon = Icons.Outlined.RestartAlt,
                        isSecondary = true,
                        modifier = Modifier.weight(1f).testTag("melt-reset-form")
                    )

                    DesktopActionButton(
                        text = "صدور فاکتور تهاتر",
                        onClick = onNavigateToInvoice,
                        icon = Icons.Outlined.ReceiptLong,
                        isSecondary = true,
                        modifier = Modifier.weight(1.2f).testTag("melt-to-invoice")
                    )
                }
            }
        }
    }
}

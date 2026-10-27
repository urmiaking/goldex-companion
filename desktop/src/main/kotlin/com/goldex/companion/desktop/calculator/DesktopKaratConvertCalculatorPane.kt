package com.goldex.companion.desktop.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopFilterChip
import com.goldex.companion.desktop.ui.controls.DesktopOutlinedTextField
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

enum class KaratMode(val title: String) {
    DIRECT("تبدیل مستقیم عیار"),
    ASSAY("محاسبه شرطی ری‌گیری (آزمایشگاه)")
}

@Composable
fun DesktopKaratConvertCalculatorPane(
    rates: MarketRates
) {
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current

    var selectedMode by remember { mutableStateOf(KaratMode.DIRECT) }

    // Direct conversion state
    var weightInput by remember { mutableStateOf("10.000") }
    var fromFinenessInput by remember { mutableStateOf("750") }
    var toFinenessInput by remember { mutableStateOf("999") }

    // Assay calculation state
    var assayWeightInput by remember { mutableStateOf("100.000") }
    var agreedFinenessInput by remember { mutableStateOf("750") }
    var labAssayFinenessInput by remember { mutableStateOf("740") }

    var copied by remember { mutableStateOf(false) }

    // Direct Conversion math
    val weightGrams = PersianNumberFormatter.toEnglishDigits(weightInput).toDoubleOrNull() ?: 0.0
    val fromFineness = PersianNumberFormatter.toEnglishDigits(fromFinenessInput).toDoubleOrNull() ?: 750.0
    val toFineness = PersianNumberFormatter.toEnglishDigits(toFinenessInput).toDoubleOrNull() ?: 999.0

    val convertedWeight = if (toFineness > 0) (weightGrams * fromFineness) / toFineness else 0.0
    val pureGold24k = if (fromFineness > 0) (weightGrams * fromFineness) / 1000.0 else 0.0
    val standard18k = if (fromFineness > 0) (weightGrams * fromFineness) / 750.0 else 0.0

    // Assay Calculation math
    val assayWeight = PersianNumberFormatter.toEnglishDigits(assayWeightInput).toDoubleOrNull() ?: 0.0
    val agreedFineness = PersianNumberFormatter.toEnglishDigits(agreedFinenessInput).toIntOrNull() ?: 750
    val labFineness = PersianNumberFormatter.toEnglishDigits(labAssayFinenessInput).toIntOrNull() ?: 740

    val finenessDiff = labFineness - agreedFineness
    val actual18kWeight = if (agreedFineness > 0) assayWeight * (labFineness.toDouble() / 750.0) else 0.0
    val agreed18kWeight = if (agreedFineness > 0) assayWeight * (agreedFineness.toDouble() / 750.0) else 0.0
    val weightDiffGrams = actual18kWeight - agreed18kWeight
    val spot18k = rates.gold18.toDouble()
    val settlementTomans = weightDiffGrams * spot18k

    fun buildSummaryText(): String = buildString {
        if (selectedMode == KaratMode.DIRECT) {
            appendLine("⚖️ نتیجه تبدیل عیار طلا (قیراط)")
            appendLine("----------------------------------------")
            appendLine("وزن مبدا: ${PersianNumberFormatter.formatAccountWeight(weightGrams)} گرم")
            appendLine("عیار مبدا: ${PersianNumberFormatter.toPersianDigits(fromFineness.toInt())} | عیار مقصد: ${PersianNumberFormatter.toPersianDigits(toFineness.toInt())}")
            appendLine("----------------------------------------")
            appendLine("وزن در عیار مقصد: ${PersianNumberFormatter.formatAccountWeight(convertedWeight)} گرم")
            appendLine("معادل طلای خالص (۲۴ عیار): ${PersianNumberFormatter.formatAccountWeight(pureGold24k)} گرم")
            appendLine("معادل طلای استاندارد (۱۸ عیار): ${PersianNumberFormatter.formatAccountWeight(standard18k)} گرم")
        } else {
            appendLine("🔬 صورت‌حساب شرطی ری‌گیری و آزمایشگاه طلا (قیراط)")
            appendLine("----------------------------------------")
            appendLine("وزن قطعه ذوب‌شده: ${PersianNumberFormatter.formatAccountWeight(assayWeight)} گرم")
            appendLine("عیار شرط معامله: ${PersianNumberFormatter.toPersianDigits(agreedFineness)} | عیار آزمایشگاه (انگ): ${PersianNumberFormatter.toPersianDigits(labFineness)}")
            appendLine("اختلاف عیار (خط ری‌گیری): ${if (finenessDiff > 0) "+" else ""}${PersianNumberFormatter.toPersianDigits(finenessDiff)} خط")
            appendLine("----------------------------------------")
            appendLine("اختلاف وزنی ۱۸ عیار: ${if (weightDiffGrams > 0) "+" else ""}${PersianNumberFormatter.formatAccountWeight(weightDiffGrams)} گرم")
            appendLine("مظنه روز ۱۸ عیار: ${PersianNumberFormatter.formatPrice(spot18k)} تومان")
            appendLine("مبلغ تسویه ریالی: ${PersianNumberFormatter.formatPrice(kotlin.math.abs(settlementTomans))} تومان")
            appendLine("وضعیت تسویه: ${if (settlementTomans >= 0) "بستانکار (دریافت شود)" else "بدهکار (پرداخت شود)"}")
        }
        appendLine("========================================")
        appendLine("سامانه هوشمند مدیریت زرگری قیراط")
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Mode Selection Switcher
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                DesktopFilterChip(
                    text = KaratMode.DIRECT.title,
                    selected = selectedMode == KaratMode.DIRECT,
                    onClick = { selectedMode = KaratMode.DIRECT; copied = false },
                    modifier = Modifier.testTag("karat-mode-direct")
                )
                DesktopFilterChip(
                    text = KaratMode.ASSAY.title,
                    selected = selectedMode == KaratMode.ASSAY,
                    onClick = { selectedMode = KaratMode.ASSAY; copied = false },
                    modifier = Modifier.testTag("karat-mode-assay")
                )
            }

            DesktopActionButton(
                text = if (copied) "گزارش کپی شد ✓" else "کپی نتیجه و صورت‌حساب",
                onClick = {
                    try {
                        clipboard.setText(AnnotatedString(buildSummaryText()))
                    } catch (_: Throwable) {}
                    copied = true
                },
                icon = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                isSecondary = true,
                height = 42.dp,
                modifier = Modifier.testTag("karat-copy-summary")
            )
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            val wide = maxWidth >= 940.dp

            if (selectedMode == KaratMode.DIRECT) {
                // DIRECT CONVERSION
                if (wide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DirectConvertInputCard(
                                weightInput = weightInput,
                                onWeightChange = { weightInput = it; copied = false },
                                fromFinenessInput = fromFinenessInput,
                                onFromFinenessChange = { fromFinenessInput = it; copied = false },
                                toFinenessInput = toFinenessInput,
                                onToFinenessChange = { toFinenessInput = it; copied = false },
                                onSwap = {
                                    val temp = fromFinenessInput
                                    fromFinenessInput = toFinenessInput
                                    toFinenessInput = temp
                                    copied = false
                                }
                            )
                        }

                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DirectConvertResultCard(
                                convertedWeight = convertedWeight,
                                pureGold24k = pureGold24k,
                                standard18k = standard18k,
                                toFineness = toFineness.toInt()
                            )
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        DirectConvertInputCard(
                            weightInput = weightInput,
                            onWeightChange = { weightInput = it; copied = false },
                            fromFinenessInput = fromFinenessInput,
                            onFromFinenessChange = { fromFinenessInput = it; copied = false },
                            toFinenessInput = toFinenessInput,
                            onToFinenessChange = { toFinenessInput = it; copied = false },
                            onSwap = {
                                val temp = fromFinenessInput
                                fromFinenessInput = toFinenessInput
                                toFinenessInput = temp
                                copied = false
                            }
                        )

                        DirectConvertResultCard(
                            convertedWeight = convertedWeight,
                            pureGold24k = pureGold24k,
                            standard18k = standard18k,
                            toFineness = toFineness.toInt()
                        )
                    }
                }
            } else {
                // ASSAY & LAB SETTLEMENT
                if (wide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            AssayInputCard(
                                assayWeightInput = assayWeightInput,
                                onAssayWeightChange = { assayWeightInput = it; copied = false },
                                agreedFinenessInput = agreedFinenessInput,
                                onAgreedFinenessChange = { agreedFinenessInput = it; copied = false },
                                labAssayFinenessInput = labAssayFinenessInput,
                                onLabAssayFinenessChange = { labAssayFinenessInput = it; copied = false }
                            )
                        }

                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            AssayResultCard(
                                finenessDiff = finenessDiff,
                                weightDiffGrams = weightDiffGrams,
                                settlementTomans = settlementTomans,
                                spot18k = spot18k
                            )
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        AssayInputCard(
                            assayWeightInput = assayWeightInput,
                            onAssayWeightChange = { assayWeightInput = it; copied = false },
                            agreedFinenessInput = agreedFinenessInput,
                            onAgreedFinenessChange = { agreedFinenessInput = it; copied = false },
                            labAssayFinenessInput = labAssayFinenessInput,
                            onLabAssayFinenessChange = { labAssayFinenessInput = it; copied = false }
                        )

                        AssayResultCard(
                            finenessDiff = finenessDiff,
                            weightDiffGrams = weightDiffGrams,
                            settlementTomans = settlementTomans,
                            spot18k = spot18k
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectConvertInputCard(
    weightInput: String,
    onWeightChange: (String) -> Unit,
    fromFinenessInput: String,
    onFromFinenessChange: (String) -> Unit,
    toFinenessInput: String,
    onToFinenessChange: (String) -> Unit,
    onSwap: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("karat-direct-input-card")
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceElevated), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SwapHoriz, null, Modifier.size(20.dp), tint = colors.goldPrimary)
                }
                Column {
                    Text("مشخصات تبدیل عیار طلا", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textMain)
                    Text("فرمول رسمی: وزن مقصد = (وزن مبدا × عیار مبدا) ÷ عیار مقصد", fontSize = 11.5.sp, color = colors.textMuted)
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            DesktopOutlinedTextField(
                value = weightInput,
                onValueChange = onWeightChange,
                label = { Text("وزن اولیه طلا") },
                trailingIcon = { Text("گرم ", color = colors.textMuted, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontFamily = VazirmatnFamily,
                    fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp,
                    color = colors.textMain, textDirection = TextDirection.Ltr),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("karat-direct-weight-field")
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    DesktopOutlinedTextField(
                        value = fromFinenessInput,
                        onValueChange = onFromFinenessChange,
                        label = { Text("عیار مبدا (از)") },
                        trailingIcon = { Text("عیار ", color = colors.textMuted, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontFamily = VazirmatnFamily,
                            fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp,
                            color = colors.textMain, textDirection = TextDirection.Ltr),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("karat-from-field")
                    )
                }

                DesktopActionButton(
                    text = "جابجایی",
                    onClick = onSwap,
                    icon = Icons.Outlined.SwapHoriz,
                    isSecondary = true,
                    height = 48.dp,
                    modifier = Modifier.testTag("karat-swap-btn")
                )

                Box(Modifier.weight(1f)) {
                    DesktopOutlinedTextField(
                        value = toFinenessInput,
                        onValueChange = onToFinenessChange,
                        label = { Text("عیار مقصد (به)") },
                        trailingIcon = { Text("عیار ", color = colors.textMuted, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontFamily = VazirmatnFamily,
                            fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp,
                            color = colors.textMain, textDirection = TextDirection.Ltr),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("karat-to-field")
                    )
                }
            }

            // Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("عیارهای پرکاربرد بازار:", fontSize = 12.sp, color = colors.textMuted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("705", "735", "750", "875", "995", "999").forEach { k ->
                        DesktopFilterChip(
                            text = PersianNumberFormatter.toPersianDigits(k),
                            selected = fromFinenessInput == k,
                            onClick = { onFromFinenessChange(k) },
                            modifier = Modifier.testTag("preset-karat-$k")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectConvertResultCard(
    convertedWeight: Double,
    pureGold24k: Double,
    standard18k: Double,
    toFineness: Int
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth().testTag("karat-direct-result-card")
    ) {
        Column(
            Modifier.fillMaxWidth().background(colors.heroCardGradient).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors.profitGreen))
                    Text(
                        "وزن تبدیل‌شده در عیار ${PersianNumberFormatter.toPersianDigits(toFineness)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.goldPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        "نتیجه تبدیل",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedPriceTicker(
                    text = "${PersianNumberFormatter.formatAccountWeight(convertedWeight)} گرم",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("معادل طلای خالص (۲۴ عیار ۹۹۹):", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "${PersianNumberFormatter.formatAccountWeight(pureGold24k)} گرم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("معادل طلای استاندارد (۱۸ عیار ۷۵۰):", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "${PersianNumberFormatter.formatAccountWeight(standard18k)} گرم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssayInputCard(
    assayWeightInput: String,
    onAssayWeightChange: (String) -> Unit,
    agreedFinenessInput: String,
    onAgreedFinenessChange: (String) -> Unit,
    labAssayFinenessInput: String,
    onLabAssayFinenessChange: (String) -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("karat-assay-input-card")
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceElevated), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Biotech, null, Modifier.size(20.dp), tint = colors.goldPrimary)
                }
                Column {
                    Text("محاسبه شرطی ری‌گیری و آزمایشگاه", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textMain)
                    Text("محاسبه اختلاف وزنی و تسویه ریالی بر اساس انگ ری‌گیری", fontSize = 11.5.sp, color = colors.textMuted)
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            DesktopOutlinedTextField(
                value = assayWeightInput,
                onValueChange = onAssayWeightChange,
                label = { Text("وزن قطعه مورد آزمایش (ترازو)") },
                trailingIcon = { Text("گرم ", color = colors.textMuted, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontFamily = VazirmatnFamily,
                    fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp,
                    color = colors.textMain, textDirection = TextDirection.Ltr),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("assay-weight-field")
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    DesktopOutlinedTextField(
                        value = agreedFinenessInput,
                        onValueChange = onAgreedFinenessChange,
                        label = { Text("عیار شرط معامله (معمولاً ۷۵۰)") },
                        trailingIcon = { Text("عیار ", color = colors.textMuted, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontFamily = VazirmatnFamily,
                            fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp,
                            color = colors.textMain, textDirection = TextDirection.Ltr),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("assay-agreed-field")
                    )
                }

                Box(Modifier.weight(1f)) {
                    DesktopOutlinedTextField(
                        value = labAssayFinenessInput,
                        onValueChange = onLabAssayFinenessChange,
                        label = { Text("عیار برگه ری‌گیری (انگ آزمایشگاه)") },
                        trailingIcon = { Text("عیار ", color = colors.textMuted, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontFamily = VazirmatnFamily,
                            fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp,
                            color = colors.textMain, textDirection = TextDirection.Ltr),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("assay-lab-field")
                    )
                }
            }
        }
    }
}

@Composable
private fun AssayResultCard(
    finenessDiff: Int,
    weightDiffGrams: Double,
    settlementTomans: Double,
    spot18k: Double
) {
    val colors = LocalGoldExColors.current
    val isPositive = finenessDiff >= 0

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth().testTag("karat-assay-result-card")
    ) {
        Column(
            Modifier.fillMaxWidth().background(colors.heroCardGradient).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isPositive) colors.profitGreen else colors.errorRed))
                    Text(
                        if (isPositive) "مازاد عیار (بستانکاری زرگر)" else "کسری عیار (بدهکاری طرف‌حساب)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (isPositive) colors.profitGreen else colors.errorRed).copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, (if (isPositive) colors.profitGreen else colors.errorRed).copy(alpha = 0.5f))
                ) {
                    Text(
                        "${if (finenessDiff > 0) "+" else ""}${PersianNumberFormatter.toPersianDigits(finenessDiff)} خط ری‌گیری",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) colors.profitGreen else colors.errorRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("مبلغ تسویه ریالی ری‌گیری", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                AnimatedPriceTicker(
                    text = "${PersianNumberFormatter.formatPrice(kotlin.math.abs(settlementTomans))} تومان",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                if (kotlin.math.abs(settlementTomans) > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${if (isPositive) "دریافت شود از" else "پرداخت شود به"} طرف‌حساب",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) colors.profitGreen else colors.errorRed
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("اختلاف وزنی معادل ۱۸ عیار:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "${if (weightDiffGrams > 0) "+" else ""}${PersianNumberFormatter.formatAccountWeight(weightDiffGrams)} گرم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) colors.profitGreen else colors.errorRed
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("مبنای مظنه هر گرم ۱۸ عیار:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "${PersianNumberFormatter.formatPrice(spot18k)} تومان",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary
                        )
                    }
                }
            }
        }
    }
}

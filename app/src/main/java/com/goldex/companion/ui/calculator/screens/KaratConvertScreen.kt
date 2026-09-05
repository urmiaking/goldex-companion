package com.goldex.companion.ui.calculator.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.theme.heroCardGradient

/**
 * Dedicated Full Screen: Karat Converter & Assay Lab Settlement
 * Adheres strictly to Stitch Screen ID: 3d1b87d2ad7d4d659884f0a454e9aab3
 */
@Composable
fun KaratConvertScreen(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()

    // Calculations
    val inputWeight = PersianNumberFormatter.parsePersianOrEnglish(uiState.convertWeightInput) ?: 0.0
    val fromRatio = uiState.convertFromKarat.purityRatio
    val toRatio = uiState.convertToKarat.purityRatio
    val targetWeight = if (toRatio > 0) inputWeight * (fromRatio / toRatio) else 0.0
    val pureGold24k = inputWeight * fromRatio // pure 24k (1000/1000 ratio)

    // Conditional Assay Lab Settlement Math
    val agreedKaratNum = PersianNumberFormatter.parsePersianOrEnglish(uiState.agreedKaratInput) ?: 750.0
    val assayKaratNum = PersianNumberFormatter.parsePersianOrEnglish(uiState.assayKaratInput) ?: 742.0
    val karatDiff = assayKaratNum - agreedKaratNum
    val settlementWeight = if (agreedKaratNum > 0) inputWeight * (assayKaratNum / agreedKaratNum) else inputWeight
    val weightDiff = settlementWeight - inputWeight
    val spot18k = uiState.rates.gold18.toDouble()
    val settlementRial = (weightDiff * spot18k)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            containerColor = colors.background,
            topBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(0.6.dp, colors.goldBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = HubArrowRight,
                                    contentDescription = "بازگشت",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(colors.goldPrimary)
                                    )
                                    Text(
                                        text = "تبدیل عیار و محاسبه شرطی",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                }
                                Text(
                                    text = "فرمول استاندارد ری‌گیری و تبدیل ۷۵۰ به ۹۹۹",
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Action معکوس
                        GoldButton(
                            text = "معکوس",
                            icon = CalcSwapHoriz,
                            onClick = { viewModel.swapConvertKarats() },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                // ─── 1. Dark Card: مانیتور تبدیل زنده ─────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(0.6.dp, colors.goldBorder),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.heroCardGradient)
                            .padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.profitGreen)
                                )
                                Text(
                                    text = "خروجی تبدیل وزن معادل استاندارد",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.goldPrimary.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "پایه عیار ۷۵۰",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 2 Columns in boxes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Column 1: عیار مقصد
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "وزن در عیار مقصد (${uiState.convertToKarat.karatNumber})",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberFormatter.formatWeight(targetWeight),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = colors.goldPrimary
                                        )
                                        Text(
                                            text = "گرم",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = "تراز استاندارد صنف",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.profitGreen
                                    )
                                }
                            }

                            // Column 2: شمش خالص ۲۴
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "معادل در شمش خالص (۹۹۹)",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberFormatter.formatWeight(pureGold24k),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "گرم",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = "طلای ناب ۲۴ عیار",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.goldSecondary
                                    )
                                }
                            }
                        }

                        // Formula Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "فرمول صنف زرگری:",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                                Text(
                                    text = "وزن × (عیار مبدا ÷ عیار مقصد)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary
                                )
                            }
                        }
                    }
                }

                // ─── 2. Mode Selector: مستقیم vs شرطی ری‌گیری ────────────────
                LuxurySegmentedControl(
                    items = listOf(KaratConvertMode.DIRECT, KaratConvertMode.SETTLEMENT),
                    selectedItem = uiState.convertMode,
                    onItemSelected = { viewModel.setConvertMode(it) },
                    label = { it.labelFa },
                    modifier = Modifier.fillMaxWidth()
                )

                // ─── 3. Inputs Card: مشخصات طلای مبدا ─────────────────────────
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "مشخصات طلای مبدا",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }
                            Text(
                                text = "ترازو دقیق آزمایشگاهی",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        // Weight Input Box
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "وزن دقیق مبدا (گرم)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
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
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BasicTextField(
                                        value = PersianNumberFormatter.toPersianDigits(uiState.convertWeightInput),
                                        onValueChange = { viewModel.onConvertWeightChanged(it) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = VazirmatnFamily,
                                            fontFeatureSettings = VazirmatnFeatureSettings,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain,
                                            textAlign = TextAlign.Right,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        cursorBrush = SolidColor(colors.goldPrimary),
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "گرم",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }

                        // Karat From & To
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // عیار مبدا
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "عیار مبدا",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Karat.values().forEach { k ->
                                        val isSel = uiState.convertFromKarat == k
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                            border = if (isSel) BorderStroke(0.8.dp, colors.goldPrimary) else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.onConvertFromKarat(k) }
                                        ) {
                                            Text(
                                                text = "${k.karatNumber}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) colors.goldPrimary else colors.textMain,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // عیار مقصد
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "عیار مقصد",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Karat.values().forEach { k ->
                                        val isSel = uiState.convertToKarat == k
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                            border = if (isSel) BorderStroke(0.8.dp, colors.goldPrimary) else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.onConvertToKarat(k) }
                                        ) {
                                            Text(
                                                text = "${k.karatNumber}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) colors.goldPrimary else colors.textMain,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── 4. Card: محاسبه معامله شرطی و ری‌گیری ───────────────────
                AnimatedVisibility(visible = uiState.convertMode == KaratConvertMode.SETTLEMENT) {
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
                                        imageVector = CalcFactCheck,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "محاسبه معامله شرطی (تصفیه ری‌گیری)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.goldContainer.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "انگ قطعی",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Input row for assay
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("عیار شرط‌شده:", fontSize = 10.sp, color = colors.textSecondary)
                                    BasicTextField(
                                        value = PersianNumberFormatter.toPersianDigits(uiState.agreedKaratInput),
                                        onValueChange = { viewModel.onAgreedKaratChanged(it) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = VazirmatnFamily,
                                            fontFeatureSettings = VazirmatnFeatureSettings,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain,
                                            textAlign = TextAlign.Right,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.surfaceElevated)
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("عیار جواب آزمایشگاه:", fontSize = 10.sp, color = colors.textSecondary)
                                    BasicTextField(
                                        value = PersianNumberFormatter.toPersianDigits(uiState.assayKaratInput),
                                        onValueChange = { viewModel.onAssayKaratChanged(it) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = VazirmatnFamily,
                                            fontFeatureSettings = VazirmatnFeatureSettings,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary,
                                            textAlign = TextAlign.Right,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.surfaceElevated)
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Settlement breakdown box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("اختلاف عیار ری‌گیری:", fontSize = 11.sp, color = colors.textSecondary)
                                        Text(
                                            text = "${PersianNumberFormatter.toPersianDigits(karatDiff.toInt().toString())} خط (${if (karatDiff >= 0) "مازاد" else "کسری"})",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (karatDiff >= 0) colors.profitGreen else Color(0xFFEF4444)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("اختلاف وزن طلا:", fontSize = 11.sp, color = colors.textSecondary)
                                        Text(
                                            text = "${PersianNumberFormatter.formatWeight(kotlin.math.abs(weightDiff))} گرم طلا",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (weightDiff >= 0) colors.profitGreen else Color(0xFFEF4444)
                                        )
                                    }
                                    Divider(color = colors.border, thickness = 0.5.dp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ارزش ریالی تسویه اختلاف:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                        Text(
                                            text = "${PersianNumberFormatter.formatPrice(kotlin.math.abs(settlementRial))} تومان",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (settlementRial >= 0) colors.profitGreen else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── 5. Bottom Action Buttons ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldButton(
                        text = "انتقال به فاکتور",
                        onClick = {
                            viewModel.addItemToInvoice()
                            viewModel.selectTab(AppTab.INVOICES)
                            Toast.makeText(context, "به فاکتور منتقل شد ✓", Toast.LENGTH_SHORT).show()
                        },
                        icon = CalcReceiptLong,
                        modifier = Modifier.weight(1f)
                    )

                    GoldButton(
                        text = "اشتراک گزارش",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val report = buildString {
                                appendLine("📋 گزارش تبدیل عیار قیراط")
                                appendLine("─────────────────")
                                appendLine("وزن ورودی: ${PersianNumberFormatter.formatWeight(inputWeight)} گرم (${uiState.convertFromKarat.labelFa})")
                                appendLine("وزن معادل: ${PersianNumberFormatter.formatWeight(targetWeight)} گرم (${uiState.convertToKarat.labelFa})")
                                appendLine("شمش خالص: ${PersianNumberFormatter.formatWeight(pureGold24k)} گرم (۲۴ عیار)")
                                if (uiState.convertMode == KaratConvertMode.SETTLEMENT) {
                                    appendLine("تسویه ری‌گیری: ${PersianNumberFormatter.formatPrice(kotlin.math.abs(settlementRial))} ت")
                                }
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Karat Report", report))
                            Toast.makeText(context, "گزارش در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
                        },
                        icon = CalcShare,
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

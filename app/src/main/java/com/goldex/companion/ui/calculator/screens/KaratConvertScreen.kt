package com.goldex.companion.ui.calculator.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.theme.heroCardGradient

private val Karat.standardCode: String
    get() = when (this) {
        Karat.K18 -> "۷۵۰"
        Karat.K21 -> "۸۷۵"
        Karat.K24 -> "۹۹۹"
    }

/**
 * Dedicated Full Screen: Karat Converter & Lab Settlement
 * Adheres strictly to Google Stitch Screen ID: 3d1b87d2ad7d4d659884f0a454e9aab3
 * ("قیراط - تبدیل عیار و محاسبه شرطی طلا")
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

    var swapRotationTarget by remember { mutableFloatStateOf(0f) }
    val swapRotation by animateFloatAsState(
        targetValue = swapRotationTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swapRotation"
    )

    // Calculations
    val inputWeight = PersianNumberFormatter.parsePersianOrEnglish(uiState.convertWeightInput) ?: 0.0
    val pureGold24k = if (Karat.K24.purityRatio > 0) inputWeight * (uiState.convertFromKarat.purityRatio / Karat.K24.purityRatio) else 0.0
    val standardGold18k = if (Karat.K18.purityRatio > 0) inputWeight * (uiState.convertFromKarat.purityRatio / Karat.K18.purityRatio) else 0.0

    // Conditional lab calculation (اختلاف ری‌گیری نسبت به استاندارد ۷۵۰)
    val labDiffWeight = standardGold18k - inputWeight
    val spot18k = uiState.rates.gold18.toDouble()
    val labDiffRial = labDiffWeight * spot18k

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
                    shadowElevation = 3.dp
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
                                            .background(Color(0xFFF59E0B))
                                    )
                                    Text(
                                        text = "تبدیل عیار و محاسبه شرطی",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.textMain
                                    )
                                }
                                Text(
                                    text = "فرمول استاندارد ری‌گیری و تبدیل ۷۵۰ به ۹۹۹",
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Swap Button
                        Button(
                            onClick = {
                                swapRotationTarget += 180f
                                viewModel.swapConvertKarats()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.goldPrimary,
                                contentColor = Color(0xFF141B2B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = CalcSwapHoriz,
                                contentDescription = "معکوس",
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(swapRotation)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "معکوس", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── 1. Hero Dark Card (کارت مشکی لوکس خلاصه / مانیتور تبدیل زنده) ─────
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                )
                                Text(
                                    text = "خروجی تبدیل وزن معادل استاندارد",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.goldPrimary.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "پایه عیار ۷۵۰",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 2 Columns: Converted Weight + Pure 24K
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Dest Karat Result
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "وزن در عیار ${uiState.convertToKarat.displayName} (${uiState.convertToKarat.standardCode}):",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberFormatter.formatWeight(uiState.convertedWeight),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFBBF24)
                                        )
                                        Text(
                                            text = "گرم",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "تراز استاندارد صنف",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }

                            // Pure 24K Gold Equivalent
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "معادل در شمش خالص (۹۹۹):",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberFormatter.formatWeight(pureGold24k),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "گرم",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "طلای آبشده ۲۴ عیار",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        // Formula Banner inside Dark Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "فرمول تبدیل صنف:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "وزن × عیار مبدا ÷ عیار مقصد",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary
                                )
                            }
                        }
                    }
                }

                // ─── 2. Input Weight Card ────────────────────────────────────
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
                                    imageVector = CalcScale,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "وزن قطعه یا طلای آبشده",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }
                            Text(
                                text = "دقت میلی‌گرم (۰.۰۰۱g)",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        GoldInputField(
                            value = uiState.convertWeightInput,
                            onValueChange = { viewModel.onConvertWeightChanged(it) },
                            label = "وزن طلا جهت معادل‌سازی",
                            trailingText = "گرم",
                            isDecimal = true,
                            useThousandsSeparator = false
                        )

                        // Quick Increment Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10.0 to "+۱۰ گ", 50.0 to "+۵۰ گ", 100.0 to "+۱۰۰ گ", 500.0 to "+۵۰۰ گ").forEach { (step, label) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.5.dp, colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val current = PersianNumberFormatter.parsePersianOrEnglish(uiState.convertWeightInput) ?: 0.0
                                            val next = current + step
                                            viewModel.onConvertWeightChanged(
                                                if (next % 1.0 == 0.0) next.toLong().toString() else next.toString()
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
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

                // ─── 3. Source & Target Karat Selectors ───────────────────────
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Source Karat (عیار مبدا)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "عیار مبدا (عیار فعلی طلا):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Karat.values().forEach { karat ->
                                    val isSelected = uiState.convertFromKarat == karat
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) colors.goldContainer else colors.surfaceElevated,
                                        border = BorderStroke(
                                            if (isSelected) 1.2.dp else 0.5.dp,
                                            if (isSelected) colors.goldPrimary else colors.border
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.onConvertFromKaratChanged(karat) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "${karat.value} عیار",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.goldPrimary else colors.textMain
                                            )
                                            Text(
                                                text = "(${karat.standardCode})",
                                                fontSize = 9.sp,
                                                color = if (isSelected) colors.goldPrimary else colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

                        // Target Karat (عیار مقصد)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "عیار مقصد (عیار تبدیل):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Karat.values().forEach { karat ->
                                    val isSelected = uiState.convertToKarat == karat
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) colors.goldContainer else colors.surfaceElevated,
                                        border = BorderStroke(
                                            if (isSelected) 1.2.dp else 0.5.dp,
                                            if (isSelected) colors.goldPrimary else colors.border
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.onConvertToKaratChanged(karat) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "${karat.value} عیار",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.goldPrimary else colors.textMain
                                            )
                                            Text(
                                                text = "(${karat.standardCode})",
                                                fontSize = 9.sp,
                                                color = if (isSelected) colors.goldPrimary else colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── 4. Assay Lab Cash Settlement Card (تسویه نقدی ری‌گیری) ────
                if (labDiffWeight != 0.0 && spot18k > 0) {
                    val isSurplus = labDiffWeight > 0
                    val badgeColor = if (isSurplus) colors.profitGreen else Color(0xFFEF4444)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surface,
                        border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.5f)),
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = CalcFactCheck,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "تسویه نقدی ری‌گیری نسبت به عیار ۷۵۰",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = if (isSurplus) "مازاد عیار (طلب)" else "کسری عیار (بدهی)",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceElevated)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "اختلاف وزنی شرطی:",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = "${if (isSurplus) "+" else ""}${PersianNumberFormatter.formatWeight(labDiffWeight)} گرم ۱۸",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "ارزش ریالی روز بازار:",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary
                                    )
                                    AnimatedPriceTicker(
                                        text = "${PersianNumberFormatter.formatPrice(kotlin.math.abs(labDiffRial))} تومان",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── 5. Action Buttons ───────────────────────────────────────
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val summaryText = buildString {
                            appendLine("📋 گزارش تبدیل عیار و محاسبه شرطی قیراط")
                            appendLine("─────────────────────────")
                            appendLine("وزن ورودی: ${PersianNumberFormatter.formatWeight(inputWeight)} گرم (${uiState.convertFromKarat.displayName})")
                            appendLine("وزن معادل مقصد: ${PersianNumberFormatter.formatWeight(uiState.convertedWeight)} گرم (${uiState.convertToKarat.displayName})")
                            appendLine("معادل شمش خالص ۲۴: ${PersianNumberFormatter.formatWeight(pureGold24k)} گرم")
                            if (labDiffWeight != 0.0) {
                                appendLine("تفاوت با عیار ۷۵۰: ${PersianNumberFormatter.formatWeight(labDiffWeight)} گرم")
                                appendLine("ارزش ریالی تفاوت: ${PersianNumberFormatter.formatPrice(kotlin.math.abs(labDiffRial))} تومان")
                            }
                        }
                        clipboard.setPrimaryClip(ClipData.newPlainText("Karat Convert Summary", summaryText))
                        Toast.makeText(context, "گزارش تبدیل عیار در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.goldPrimary,
                        contentColor = Color(0xFF141B2B)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = CalcContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "کپی گزارش رسمی تبدیل عیار",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

package com.goldex.companion.ui.calculator.tabs

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldInputField
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
 * Rebuilt Karat Converter Tab adhering strictly to Google Stitch
 * Design Screen ID: 3d1b87d2ad7d4d659884f0a454e9aab3
 * ("قیراط - تبدیل عیار و محاسبه شرطی طلا")
 */
@Composable
fun KaratConvertTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    var swapRotationTarget by remember { mutableFloatStateOf(0f) }
    val swapRotation by animateFloatAsState(
        targetValue = swapRotationTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swapRotation"
    )

    // Calculate pure 24K equivalent and standard 18K equivalent
    val inputWeight = PersianNumberFormatter.parsePersianOrEnglish(uiState.convertWeightInput) ?: 0.0
    val pureGold24k = if (Karat.K24.purityRatio > 0) inputWeight * (uiState.convertFromKarat.purityRatio / Karat.K24.purityRatio) else 0.0
    val standardGold18k = if (Karat.K18.purityRatio > 0) inputWeight * (uiState.convertFromKarat.purityRatio / Karat.K18.purityRatio) else 0.0

    // Conditional lab calculation (اختلاف ری‌گیری نسبت به استاندارد ۷۵۰)
    val labDiffWeight = standardGold18k - inputWeight
    val spot18k = uiState.rates.gold18.toDouble()
    val labDiffRial = labDiffWeight * spot18k

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ─── Subheader / Action Bar ──────────────────────────────────
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
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تبدیل عیار و محاسبه شرطی",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "فرمول استاندارد ری‌گیری و تبدیل ۷۵۰ به ۹۹۹",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Swap Button: معکوس
                Button(
                    onClick = {
                        swapRotationTarget += 180f
                        viewModel.swapConvertKarats()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.goldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
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

        // ─── 1. Hero Dark Card (مانیتور تبدیل زنده) ───────────────────
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
                // Header row with pulsing dot
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
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "مقصد: ${uiState.convertToKarat.labelFa}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // 2 Large Metric Cards in a Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Col 1: Converted weight in target karat
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "وزن در عیار مقصد (${uiState.convertToKarat.standardCode})",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatWeight(uiState.convertedWeight)} گرم",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.goldPrimary
                            )
                            Text(
                                text = "تراز استاندارد صنف",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.profitGreen
                            )
                        }
                    }

                    // Col 2: Equivalent in pure gold bullion (24K / 999.9)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "معادل در شمش خالص (۹۹۹)",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatWeight(pureGold24k)} گرم",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "طلای ناب ۲۴ عیار",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.goldSecondary
                            )
                        }
                    }
                }

                // Formula pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فرمول صنف زرگری:",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "وزن × (${uiState.convertFromKarat.standardCode} ÷ ${uiState.convertToKarat.standardCode})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }
            }
        }

        // ─── 2. Input Weight & Karats Card ───────────────────────────
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

                // Weight Input
                GoldInputField(
                    value = uiState.convertWeightInput,
                    onValueChange = { viewModel.onConvertWeightChanged(it) },
                    label = "وزن دقیق مبدا",
                    trailingText = "گرم",
                    isDecimal = true,
                    useThousandsSeparator = false
                )

                // Quick weight presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1" to "۱ گرم", "5" to "۵ گرم", "10" to "۱۰ گرم", "25" to "۲۵ گرم", "50" to "۵۰ گرم").forEach { (w, label) ->
                        val isSel = PersianNumberFormatter.toEnglishDigits(uiState.convertWeightInput) == w
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                            border = BorderStroke(
                                if (isSel) 1.dp else 0.5.dp,
                                if (isSel) colors.goldPrimary else colors.border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.onConvertWeightChanged(w) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) colors.goldPrimary else colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                // From Karat Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "عیار مبدا (عیار فعلی طلا / ری‌گیری):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Karat.values().forEach { k ->
                            val isSel = uiState.convertFromKarat == k
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                border = BorderStroke(
                                    if (isSel) 1.dp else 0.5.dp,
                                    if (isSel) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.onConvertFromKarat(k) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = k.labelFa,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) colors.goldPrimary else colors.textMain
                                    )
                                    Text(
                                        text = "کد ${k.standardCode}",
                                        fontSize = 9.sp,
                                        color = if (isSel) colors.goldSecondary else colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // To Karat Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "عیار مقصد (پایه تبدیل):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Karat.values().forEach { k ->
                            val isSel = uiState.convertToKarat == k
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                                border = BorderStroke(
                                    if (isSel) 1.dp else 0.5.dp,
                                    if (isSel) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.onConvertToKarat(k) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = k.labelFa,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) colors.goldPrimary else colors.textMain
                                    )
                                    Text(
                                        text = "کد ${k.standardCode}",
                                        fontSize = 9.sp,
                                        color = if (isSel) colors.goldSecondary else colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── 3. Conditional Melt / Lab Settlement Card ───────────────
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
                            imageVector = Icons.Default.FactCheck,
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
                        color = colors.goldContainer,
                        border = BorderStroke(0.5.dp, colors.goldPrimary)
                    ) {
                        Text(
                            text = "انگ قطعی",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.5.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("عیار شرط‌شده اولیه:", fontSize = 11.sp, color = colors.textSecondary)
                            Text("۷۵۰ خط (۱۸ عیار استاندارد)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("عیار جواب آزمایشگاه (ری‌گیری):", fontSize = 11.sp, color = colors.textSecondary)
                            Text(
                                "${uiState.convertFromKarat.standardCode} خط",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("اختلاف وزن طلا (کسر / اضافه):", fontSize = 11.sp, color = colors.textSecondary)
                            val isDeficit = labDiffWeight < 0
                            val sign = if (labDiffWeight > 0) "+" else ""
                            Text(
                                "$sign${PersianNumberFormatter.formatWeight(labDiffWeight)} گرم",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeficit) colors.errorRed else if (labDiffWeight > 0) colors.profitGreen else colors.textMain
                            )
                        }

                        if (spot18k > 0) {
                            Divider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ارزش ریالی تسویه اختلاف:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                val absRial = kotlin.math.abs(labDiffRial)
                                val isPayable = labDiffRial < 0
                                Text(
                                    "${PersianNumberFormatter.formatPrice(absRial)} تومان ${if (isPayable) "(کسری)" else if (labDiffRial > 0) "(طلب)" else ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPayable) colors.errorRed else colors.profitGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── 4. Action Buttons ───────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipText = """
                        🧾 گزارش تبدیل عیار طلا (قیراط)
                        • وزن مبدا: ${PersianNumberFormatter.formatWeight(inputWeight)} گرم (${uiState.convertFromKarat.labelFa})
                        • وزن معادل عیار مقصد: ${PersianNumberFormatter.formatWeight(uiState.convertedWeight)} گرم (${uiState.convertToKarat.labelFa})
                        • معادل در شمش ناب ۲۴ عیار: ${PersianNumberFormatter.formatWeight(pureGold24k)} گرم
                        • فرمول صنف: وزن × (${uiState.convertFromKarat.standardCode} ÷ ${uiState.convertToKarat.standardCode})
                    """.trimIndent()
                    clipboard.setPrimaryClip(ClipData.newPlainText("Karat Convert", clipText))
                    Toast.makeText(context, "گزارش تبدیل عیار در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.goldPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("کپی نتیجه تبدیل و گزارش تراز", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

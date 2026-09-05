package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

/**
 * Rebuilt Melt Tab (مظنه آبشده و مثقال) harmonized with Stitch Sovereign Aurum
 */
@Composable
fun MeltTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

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
                            imageVector = CalcScale,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "مظنه آبشده و مثقال بازار",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "فرمول تبدیل ۱۷ به ۱۸ و مثقال ۴.۶۰۸۳ گرم",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Bind Live Melt Rate
                GoldButton(
                    text = "مظنه زنده",
                    icon = CalcSync,
                    onClick = {
                        viewModel.onMesghalPriceChanged(uiState.rates.goldMelt.toString())
                        Toast.makeText(context, "مظنه زنده آبشده درج شد ✓", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(36.dp)
                )
            }
        }

        // ─── 1. Hero Dark Card (ارزش کل قطعه آبشده) ─────────────────
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
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.profitGreen)
                        )
                        Text(
                            text = "ارزش کل قطعه آبشده ۱۸ عیار",
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
                            text = "ارزش معاملاتی",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedPriceTicker(
                        text = "${PersianNumberFormatter.formatPrice(uiState.meltTotalValue)} تومان",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = PersianWordsFormatter.toWords(uiState.meltTotalValue.toLong()) + " تومان",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }

                // Breakdown: Equivalent 18k Gram price
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
                            text = "قیمت معادل هر گرم ۱۸ عیار (مظنه ÷ ۴.۳۳۱۸):",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(uiState.meltGram18kPrice.toDouble())} تومان",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary
                        )
                    }
                }
            }
        }

        // ─── 2. Inputs Card ──────────────────────────────────────────
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
                // Mesghal price input
                GoldInputField(
                    value = uiState.mesghalPriceInput,
                    onValueChange = { viewModel.onMesghalPriceChanged(it) },
                    label = "قیمت یک مثقال طلای ۱۷ عیار (مظنه آبشده)",
                    trailingText = "تومان",
                    useThousandsSeparator = true
                )

                // Melt piece weight input
                GoldInputField(
                    value = uiState.meltWeightInput,
                    onValueChange = { viewModel.onMeltWeightChanged(it) },
                    label = "وزن قطعه آبشده (گرم)",
                    trailingText = "گرم",
                    isDecimal = true,
                    useThousandsSeparator = false
                )

                // Quick weight preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "4.608" to "۱ مثقال",
                        "10" to "۱۰ گرم",
                        "20" to "۲۰ گرم",
                        "50" to "۵۰ گرم"
                    ).forEach { (weight, label) ->
                        val isSel = PersianNumberFormatter.toEnglishDigits(uiState.meltWeightInput) == weight
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) colors.goldContainer else colors.surfaceElevated,
                            border = BorderStroke(
                                if (isSel) 1.dp else 0.5.dp,
                                if (isSel) colors.goldPrimary else colors.border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.onMeltWeightChanged(weight) }
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
            }
        }

        // ─── 3. Action Buttons ───────────────────────────────────────
        GoldButton(
            text = "کپی نتیجه محاسبه آبشده",
            icon = CalcContentCopy,
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipText = "ارزش قطعه آبشده (${PersianNumberFormatter.toPersianDigits(uiState.meltWeightInput)} گرم با مظنه ${PersianNumberFormatter.formatPrice(uiState.mesghalPriceInput.toDoubleOrNull() ?: 0.0)} ت): ${PersianNumberFormatter.formatPrice(uiState.meltTotalValue)} تومان"
                clipboard.setPrimaryClip(ClipData.newPlainText("Melt Value", clipText))
                Toast.makeText(context, "ارزش آبشده در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

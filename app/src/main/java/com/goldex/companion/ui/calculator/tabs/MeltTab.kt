package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.theme.heroCardGradient

@Composable
fun MeltTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gold gradient accent line on top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(colors.goldGradient)
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with luxury icon container
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.goldBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "مظنه آبشده و مثقال (فرمول ۱۷ به ۱۸)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "محاسبه ارزش طلا بر مبنای مثقال ۴.۶۰۸۳ گرم و تبدیل عیار ۷۰۵ به ۷۵۰",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Mesghal price input with live badge button
                GoldInputField(
                    value = uiState.mesghalPriceInput,
                    onValueChange = { viewModel.onMesghalPriceChanged(it) },
                    label = "قیمت یک مثقال طلای ۱۷ عیار (مظنه آبشده)",
                    trailingText = "تومان",
                    useThousandsSeparator = true
                )

                // Live rate quick bind chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.goldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onMesghalPriceChanged(uiState.rates.goldMelt.toString()) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.profitGreen)
                            )
                            Text(text = "درج مظنه زنده بازار:", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(uiState.rates.goldMelt.toDouble())} تومان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }

                // Equivalent 18k Gram rate pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("قیمت معادل هر گرم ۱۸ عیار:", fontSize = 12.sp, color = colors.textSecondary)
                            Text("محاسبه: مظنه ÷ ۴.۳۳۱۸", fontSize = 9.sp, color = colors.textMuted)
                        }
                        AnimatedPriceTicker(
                            text = "${PersianNumberFormatter.formatPrice(uiState.meltGram18kPrice.toDouble())} تومان",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary
                        )
                    }
                }

                // Melt piece weight input
                GoldInputField(
                    value = uiState.meltWeightInput,
                    onValueChange = { viewModel.onMeltWeightChanged(it) },
                    label = "وزن قطعه آبشده",
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.goldContainer else colors.surfaceElevated)
                                .border(
                                    if (isSel) 1.dp else 0.5.dp,
                                    if (isSel) colors.goldPrimary else colors.border,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.onMeltWeightChanged(weight) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) colors.goldPrimary else colors.textSecondary
                            )
                        }
                    }
                }

                // Final Lot Value Result (Stitch Sovereign Aurum Box)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.heroCardGradient)
                        .border(0.8.dp, colors.goldBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ارزش کل قطعه آبشده ۱۸ عیار", fontSize = 12.sp, color = colors.textSecondary)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.goldPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "ارزش معاملاتی", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedPriceTicker(
                        text = "${PersianNumberFormatter.formatPrice(uiState.meltTotalValue)} تومان",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${PersianWordsFormatter.toWords(uiState.meltTotalValue.toLong())} تومان",
                        fontSize = 11.sp,
                        color = colors.textMain,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Perforated tear line
                    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                        drawLine(
                            color = colors.border,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                            strokeWidth = 1.2f
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = "ارزش قطعه آبشده (${PersianNumberFormatter.toPersianDigits(uiState.meltWeightInput)} گرم): ${PersianNumberFormatter.formatPrice(uiState.meltTotalValue)} تومان"
                            clipboard.setPrimaryClip(ClipData.newPlainText("Melt Value", clipText))
                            Toast.makeText(context, "ارزش آبشده کپی شد ✓", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.goldBorder),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "کپی سریع ارزش آبشده", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

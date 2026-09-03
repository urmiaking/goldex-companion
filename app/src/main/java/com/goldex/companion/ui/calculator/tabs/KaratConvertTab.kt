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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تبدیل وزن بین عیارهای مختلف طلا",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "محاسبه دقیق طلا بر مبنای عیار جهانی (۹۹۹) و سنتی (۷۰۵، ۷۵۰، ۸۴۰)",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Input gold weight
                GoldInputField(
                    value = uiState.convertWeightInput,
                    onValueChange = { viewModel.onConvertWeightChanged(it) },
                    label = "وزن قطعه طلا",
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
                                .clickable { viewModel.onConvertWeightChanged(w) }
                                .padding(vertical = 6.dp),
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

                // Karat selectors with Swap button
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عیار مبدأ (عیار فعلی طلا):", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Karat.values().forEach { k ->
                            val isSel = uiState.convertFromKarat == k
                            Box(
                                modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) colors.goldContainer else colors.surfaceElevated)
                                .border(
                                    if (isSel) 1.dp else 0.5.dp,
                                    if (isSel) colors.goldPrimary else colors.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.onConvertFromKarat(k) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = k.labelFa,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) colors.goldPrimary else colors.textMain
                                )
                                Text(
                                    text = k.standardCode,
                                    fontSize = 9.sp,
                                    color = if (isSel) colors.goldSecondary else colors.textMuted
                                )
                            }
                        }
                    }
                }

                // Swap button between karats
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.goldBorder),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                swapRotationTarget += 180f
                                val currentFrom = uiState.convertFromKarat
                                val currentTo = uiState.convertToKarat
                                viewModel.onConvertFromKarat(currentTo)
                                viewModel.onConvertToKarat(currentFrom)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تعویض مبدأ و مقصد",
                                tint = colors.goldPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(swapRotation)
                            )
                        }
                    }
                }

                    Text("عیار مقصد (عیار تبدیل):", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Karat.values().forEach { k ->
                            val isSel = uiState.convertToKarat == k
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) colors.goldContainer else colors.surfaceElevated)
                                    .border(
                                        if (isSel) 1.dp else 0.5.dp,
                                        if (isSel) colors.goldPrimary else colors.border,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.onConvertToKarat(k) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = k.labelFa,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) colors.goldPrimary else colors.textMain
                                    )
                                    Text(
                                        text = k.standardCode,
                                        fontSize = 9.sp,
                                        color = if (isSel) colors.goldSecondary else colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Formula helper pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "فرمول تبدیل عیار:", fontSize = 11.sp, color = colors.textMuted)
                        Text(
                            text = "وزن × (${uiState.convertFromKarat.standardCode} ÷ ${uiState.convertToKarat.standardCode})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.goldSecondary
                        )
                    }
                }

                // Converted Output Result (Stitch Golden Box)
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
                        Text("وزن معادل در عیار مقصد", fontSize = 12.sp, color = colors.textSecondary)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.goldPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = uiState.convertToKarat.labelFa, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedPriceTicker(
                        text = "${PersianNumberFormatter.formatWeight(uiState.convertedWeight)} گرم",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "معادل طلا با استاندارد خلوص ${uiState.convertToKarat.standardCode}",
                        fontSize = 10.sp,
                        color = colors.textSecondary,
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
                            val clipText = "${PersianNumberFormatter.toPersianDigits(uiState.convertWeightInput)} گرم طلا ${uiState.convertFromKarat.labelFa} = ${PersianNumberFormatter.formatWeight(uiState.convertedWeight)} گرم طلا ${uiState.convertToKarat.labelFa}"
                            clipboard.setPrimaryClip(ClipData.newPlainText("Karat Conversion", clipText))
                            Toast.makeText(context, "نتیجه تبدیل کپی شد ✓", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.goldBorder),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "کپی نتیجه تبدیل", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

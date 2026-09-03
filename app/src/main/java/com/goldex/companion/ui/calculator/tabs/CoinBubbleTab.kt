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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.ResultRow
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.theme.heroCardGradient

private val CoinIconVector: ImageVector = ImageVector.Builder(
    name = "CoinIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 19.5f)
        curveTo(7.86f, 19.5f, 4.5f, 16.14f, 4.5f, 12f)
        curveTo(4.5f, 7.86f, 7.86f, 4.5f, 12f, 4.5f)
        curveTo(16.14f, 4.5f, 19.5f, 7.86f, 19.5f, 12f)
        curveTo(19.5f, 16.14f, 16.14f, 19.5f, 12f, 19.5f)
        close()
    }
}.build()

@Composable
fun CoinBubbleTab(
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
                // Header with luxury coin badge
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
                            imageVector = CoinIconVector,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "حباب‌سنج تخصصی انواع سکه بانکی",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "مقایسه ارزش طلای واقعی سکه با قیمت روز بازار آزاد",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Coins Selector List
                Text(
                    text = "نوع سکه بهار آزادی:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoinType.values().forEach { coin ->
                        val isSelected = uiState.selectedCoin == coin
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colors.goldContainer else colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.dp else 0.5.dp,
                                if (isSelected) colors.goldPrimary else colors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onCoinTypeSelected(coin) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) colors.goldPrimary else colors.border)
                                    )
                                    Text(
                                        text = coin.titleFa,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.goldPrimary else colors.textMain
                                    )
                                }

                                Text(
                                    text = "${PersianNumberFormatter.toPersianDigits(coin.totalWeightGrams.toString())} گرم (۹۰۰)",
                                    fontSize = 11.sp,
                                    color = if (isSelected) colors.goldSecondary else colors.textMuted
                                )
                            }
                        }
                    }
                }

                // Market Price Input
                GoldInputField(
                    value = uiState.coinMarketPriceInput,
                    onValueChange = { viewModel.onCoinMarketPriceChanged(it) },
                    label = "قیمت روز بازار سکه",
                    trailingText = "تومان",
                    useThousandsSeparator = true
                )

                // Quick Bind Live Coin Rate Chip
                val currentCoinLiveRate = when (uiState.selectedCoin) {
                    CoinType.EMAMI -> uiState.rates.coinEmami
                    CoinType.BAHAR -> uiState.rates.coinBahar
                    CoinType.HALF -> uiState.rates.coinHalf
                    CoinType.QUARTER -> uiState.rates.coinQuarter
                    CoinType.GERAMI -> uiState.rates.coinGerami
                }
                if (currentCoinLiveRate > 0L) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.goldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onCoinMarketPriceChanged(currentCoinLiveRate.toString()) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "درج مظنه زنده ${uiState.selectedCoin.titleFa}:",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatPrice(currentCoinLiveRate.toDouble())} تومان",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }
                }

                // Bubble Result Card (Stitch Component)
                uiState.coinBubbleResult?.let { bubble ->
                    val bubblePercent = bubble.bubblePercent
                    val statusColor = when {
                        bubblePercent > 25.0 -> colors.errorRed
                        bubblePercent > 15.0 -> Color(0xFFF59E0B)
                        else -> colors.profitGreen
                    }
                    val statusText = when {
                        bubblePercent > 25.0 -> "ریسک حباب بسیار بالا"
                        bubblePercent > 15.0 -> "حباب متوسط و محتاطانه"
                        else -> "حباب منطقی و کم‌ریسک"
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.goldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.heroCardGradient)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("وضعیت ریسک حباب بازاری", fontSize = 12.sp, color = colors.textSecondary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }

                            // Prominent Bubble Amount Display
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "مبلغ حباب قیمت بازاری (${uiState.selectedCoin.titleFa})",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                AnimatedPriceTicker(
                                    text = "${PersianNumberFormatter.formatPrice(bubble.bubbleAmount)} تومان",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    contentAlignment = Alignment.Center
                                )
                            }

                            // Visual Bubble Risk Meter (Progress bar)
                            val normalizedProgress = (bubblePercent / 40.0).coerceIn(0.0, 1.0).toFloat()
                            val animatedProgress by animateFloatAsState(
                                targetValue = normalizedProgress,
                                animationSpec = tween(400, easing = FastOutSlowInEasing),
                                label = "bubbleProgress"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = statusColor,
                                    trackColor = colors.surface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("۰٪ (بدون حباب)", fontSize = 9.sp, color = colors.textMuted)
                                    Text("۲۰٪ (تعادل)", fontSize = 9.sp, color = colors.textMuted)
                                    Text("۴۰٪+ (بحرانی)", fontSize = 9.sp, color = colors.textMuted)
                                }
                            }

                            HorizontalDivider(color = colors.border, thickness = 0.6.dp)

                            ResultRow(
                                label = "ارزش ذاتی طلای سکه:",
                                value = "${PersianNumberFormatter.formatPrice(bubble.intrinsicValue)} تومان"
                            )
                            ResultRow(
                                label = "مبلغ حباب سکه:",
                                value = "${PersianNumberFormatter.formatPrice(bubble.bubbleAmount)} تومان",
                                valueColor = statusColor
                            )
                            ResultRow(
                                label = "درصد حباب بازاری:",
                                value = "${PersianNumberFormatter.toPersianDigits("%.1f".format(bubblePercent))}٪",
                                valueColor = statusColor,
                                isHighlight = true
                            )

                            // Perforated tear line
                            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).padding(vertical = 2.dp)) {
                                drawLine(
                                    color = colors.border,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                                    strokeWidth = 1.2f
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val report = "تحلیل حباب ${uiState.selectedCoin.titleFa}: حباب ${PersianNumberFormatter.formatPrice(bubble.bubbleAmount)} تومان (${PersianNumberFormatter.toPersianDigits("%.1f".format(bubblePercent))}٪) | ارزش ذاتی: ${PersianNumberFormatter.formatPrice(bubble.intrinsicValue)} تومان"
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Coin Bubble", report))
                                    Toast.makeText(context, "گزارش حباب کپی شد ✓", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                                border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.goldBorder),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(text = "کپی خلاصه حباب‌سنج", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

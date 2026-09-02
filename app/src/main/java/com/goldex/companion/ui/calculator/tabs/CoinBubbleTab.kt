package com.goldex.companion.ui.calculator.tabs

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.ResultRow
import com.goldex.companion.ui.theme.*

@Composable
fun CoinBubbleTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "حباب‌سنج تخصصی انواع سکه بانکی",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            // Coins Selector Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CoinType.values().forEach { coin ->
                    val isSelected = uiState.selectedCoin == coin
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) GoldContainer else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.dp else 0.5.dp,
                            if (isSelected) GoldPrimary else DarkBorder
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
                            Text(
                                text = coin.titleFa,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GoldPrimary else TextMain
                            )
                            Text(
                                text = "${coin.totalWeightGrams} گرم",
                                fontSize = 11.sp,
                                color = if (isSelected) GoldLight else TextMuted
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

            // Bubble Result Card
            uiState.coinBubbleResult?.let { bubble ->
                val bubblePercent = bubble.bubblePercent
                val statusColor = when {
                    bubblePercent > 25.0 -> ErrorRed
                    bubblePercent > 15.0 -> Color(0xFFF59E0B)
                    else -> ProfitGreen
                }
                val statusText = when {
                    bubblePercent > 25.0 -> "ریسک حباب بسیار بالا"
                    bubblePercent > 15.0 -> "حباب متوسط"
                    else -> "حباب منطقی و کم"
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("وضعیت حباب بازاری", fontSize = 12.sp, color = TextSecondary)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            }
                        }

                        Divider(color = DarkBorder, thickness = 0.5.dp)

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
                    }
                }
            }
        }
    }
}

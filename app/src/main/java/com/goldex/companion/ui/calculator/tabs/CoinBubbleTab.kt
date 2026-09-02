package com.goldex.companion.ui.calculator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "حباب‌سنج تخصصی انواع سکه بانکی",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CoinType.values().forEach { coin ->
                    val selected = uiState.selectedCoin == coin
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onCoinTypeSelected(coin) },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(coin.titleFa, fontSize = 12.sp)
                                Text(
                                    "${coin.totalWeightGrams}g",
                                    fontSize = 11.sp,
                                    color = if (selected) DarkBg else TextMuted
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = DarkBg,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            GoldInputField(
                value = uiState.coinMarketPriceInput,
                onValueChange = { viewModel.onCoinMarketPriceChanged(it) },
                label = "قیمت بازار سکه (تومان)",
                trailingText = "تومان"
            )

            uiState.coinBubbleResult?.let { bubble ->
                val bubblePercent = bubble.bubblePercent
                val statusColor = when {
                    bubblePercent > 25.0 -> Color(0xFFEF4444)
                    bubblePercent > 15.0 -> Color(0xFFF59E0B)
                    else -> ProfitGreen
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            valueColor = statusColor
                        )
                    }
                }
            }
        }
    }
}

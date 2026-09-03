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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun MeltTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "مظنه آبشده و مثقال (تبدیل ۱۷ به ۱۸ عیار)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.goldPrimary
            )

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
                    Text(text = "درج مظنه زنده بازار:", fontSize = 11.sp, color = colors.textSecondary)
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قیمت معادل هر گرم ۱۸ عیار:", fontSize = 12.sp, color = colors.textSecondary)
                    Text(
                        text = "${PersianNumberFormatter.formatPrice(uiState.meltGram18kPrice.toDouble())} تومان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldSecondary
                    )
                }
            }

            // Melt piece weight
            GoldInputField(
                value = uiState.meltWeightInput,
                onValueChange = { viewModel.onMeltWeightChanged(it) },
                label = "وزن قطعه آبشده",
                trailingText = "گرم",
                isDecimal = true,
                useThousandsSeparator = false
            )

            // Final Lot Value Result
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.goldContainer)
                    .border(0.6.dp, colors.goldBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ارزش کل قطعه آبشده ۱۸ عیار", fontSize = 12.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${PersianNumberFormatter.formatPrice(uiState.meltTotalValue)} تومان",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${PersianWordsFormatter.toWords(uiState.meltTotalValue.toLong())} تومان",
                    fontSize = 11.sp,
                    color = colors.textMain
                )
            }
        }
    }
}

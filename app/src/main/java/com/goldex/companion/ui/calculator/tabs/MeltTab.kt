package com.goldex.companion.ui.calculator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.*

@Composable
fun MeltTab(
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
                text = "محاسبه مظنه آبشده و مثقال (۱۷ به ۱۸ عیار)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            GoldInputField(
                value = uiState.mesghalPriceInput,
                onValueChange = { viewModel.onMesghalPriceChanged(it) },
                label = "قیمت یک مثقال طلای ۱۷ عیار (مظنه آبشده)",
                trailingText = "تومان"
            )

            OutlinedButton(
                onClick = { viewModel.onMesghalPriceChanged(uiState.rates.goldMelt.toString()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSecondary)
            ) {
                Text(
                    text = "درج مظنه زنده بازار: ${PersianNumberFormatter.formatPrice(uiState.rates.goldMelt.toDouble())} تومان",
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قیمت معادل هر گرم ۱۸ عیار:", fontSize = 13.sp, color = TextMuted)
                    Text(
                        text = "${PersianNumberFormatter.formatPrice(uiState.meltGram18kPrice.toDouble())} تومان",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }

            GoldInputField(
                value = uiState.meltWeightInput,
                onValueChange = { viewModel.onMeltWeightChanged(it) },
                label = "وزن قطعه آبشده (گرم)",
                trailingText = "گرم",
                isDecimal = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ارزش کل قطعه آبشده ۱۸ عیار:", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = "${PersianNumberFormatter.formatPrice(uiState.meltTotalValue)} تومان",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }
        }
    }
}

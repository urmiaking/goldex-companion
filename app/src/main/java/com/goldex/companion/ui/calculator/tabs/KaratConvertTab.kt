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
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.*

@Composable
fun KaratConvertTab(
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
                text = "تبدیل وزن بین عیارهای مختلف طلا",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            GoldInputField(
                value = uiState.convertWeightInput,
                onValueChange = { viewModel.onConvertWeightChanged(it) },
                label = "وزن قطعه طلا",
                trailingText = "گرم",
                isDecimal = true,
                useThousandsSeparator = false
            )

            Text("عیار مبدأ:", fontSize = 11.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Karat.values().forEach { k ->
                    val isSel = uiState.convertFromKarat == k
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) GoldContainer else DarkSurfaceElevated)
                            .border(
                                if (isSel) 1.dp else 0.5.dp,
                                if (isSel) GoldPrimary else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onConvertFromKarat(k) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = k.labelFa,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) GoldPrimary else TextSecondary
                        )
                    }
                }
            }

            Text("عیار مقصد:", fontSize = 11.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Karat.values().forEach { k ->
                    val isSel = uiState.convertToKarat == k
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) GoldContainer else DarkSurfaceElevated)
                            .border(
                                if (isSel) 1.dp else 0.5.dp,
                                if (isSel) GoldPrimary else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onConvertToKarat(k) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = k.labelFa,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) GoldPrimary else TextSecondary
                        )
                    }
                }
            }

            // Converted Output
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GoldContainer)
                    .border(0.6.dp, GoldBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("وزن معادل در عیار مقصد", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${PersianNumberFormatter.formatWeight(uiState.convertedWeight)} گرم",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
        }
    }
}

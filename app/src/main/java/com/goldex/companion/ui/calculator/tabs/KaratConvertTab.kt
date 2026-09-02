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
                text = "تبدیل وزن بین عیارهای مختلف",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )

            GoldInputField(
                value = uiState.convertWeightInput,
                onValueChange = { viewModel.onConvertWeightChanged(it) },
                label = "وزن قطعه",
                trailingText = "گرم",
                isDecimal = true
            )

            Text("تبدیل از عیار مبدا:", fontSize = 12.sp, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Karat.values().forEach { k ->
                    FilterChip(
                        selected = uiState.convertFromKarat == k,
                        onClick = { viewModel.onConvertFromKarat(k) },
                        label = { Text(k.labelFa, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("تبدیل به عیار مقصد:", fontSize = 12.sp, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Karat.values().forEach { k ->
                    FilterChip(
                        selected = uiState.convertToKarat == k,
                        onClick = { viewModel.onConvertToKarat(k) },
                        label = { Text(k.labelFa, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("وزن معادل در عیار مقصد:", fontSize = 12.sp, color = TextMuted)
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
}

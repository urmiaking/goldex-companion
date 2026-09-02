package com.goldex.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.theme.TextMain
import com.goldex.companion.ui.theme.TextSecondary

@Composable
fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = TextMain,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 13.sp else 12.sp,
            color = if (isHighlight) TextMain else TextSecondary,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 14.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

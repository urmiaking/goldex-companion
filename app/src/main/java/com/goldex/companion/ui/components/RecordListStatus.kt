package com.goldex.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
fun RecordListStatus(isLoading: Boolean, error: String?, onRetry: () -> Unit = {}) {
    val colors = LocalGoldExColors.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.goldPrimary, strokeWidth = 2.dp)
                Text("در حال بارگذاری…", color = colors.textMuted, fontFamily = VazirmatnFamily)
            }
        }
        if (error != null) {
            Text(error, color = colors.errorRed, fontFamily = VazirmatnFamily)
            GoldButton(text = "تلاش دوباره", onClick = onRetry, isSecondary = true, enabled = !isLoading)
        }
    }
}

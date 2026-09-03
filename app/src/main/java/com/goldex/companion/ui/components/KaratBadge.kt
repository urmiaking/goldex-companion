package com.goldex.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.ui.theme.LocalGoldExColors

val Karat.purityCodeFa: String
    get() = when (this) {
        Karat.K18 -> "۷۵۰"
        Karat.K21 -> "۸۷۵"
        Karat.K24 -> "۹۹۹"
    }

val Karat.shortLabelFa: String
    get() = when (this) {
        Karat.K18 -> "۱۸ عیار"
        Karat.K21 -> "۲۱ عیار"
        Karat.K24 -> "۲۴ عیار"
    }

/**
 * Compact luxury badge for displaying gold karat purity.
 */
@Composable
fun KaratBadge(
    karat: Karat,
    modifier: Modifier = Modifier,
    showPurityCode: Boolean = true,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalGoldExColors.current
    val text = if (showPurityCode) {
        "${karat.shortLabelFa} (${karat.purityCodeFa})"
    } else {
        karat.shortLabelFa
    }

    val clickableModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) colors.goldContainer else colors.surfaceElevated,
        border = BorderStroke(
            width = if (isSelected) 1.dp else 0.5.dp,
            color = if (isSelected) colors.goldPrimary else colors.border
        )
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.goldPrimary else colors.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Segmented selection row for standard Iranian gold karats:
 * 18K (۷۵۰), 21K (۸۷۵), 24K (۹۹۹).
 */
@Composable
fun KaratSelector(
    selectedKarat: Karat,
    onKaratSelected: (Karat) -> Unit,
    modifier: Modifier = Modifier,
    availableKarats: List<Karat> = listOf(Karat.K18, Karat.K21, Karat.K24),
    enabled: Boolean = true
) {
    val colors = LocalGoldExColors.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableKarats.forEach { karat ->
            val isSelected = selectedKarat == karat
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { onKaratSelected(karat) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    if (colors.isDark) colors.goldContainer.copy(alpha = 0.35f) else colors.goldContainer
                } else {
                    colors.surfaceElevated
                },
                border = BorderStroke(
                    width = if (isSelected) 1.2.dp else 0.6.dp,
                    color = if (isSelected) colors.goldPrimary else colors.border
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = karat.shortLabelFa,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) colors.goldPrimary else colors.textMain
                    )
                    Text(
                        text = karat.purityCodeFa,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) colors.goldSecondary else colors.textMuted
                    )
                }
            }
        }
    }
}

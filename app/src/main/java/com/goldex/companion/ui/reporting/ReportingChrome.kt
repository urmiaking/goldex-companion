package com.goldex.companion.ui.reporting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.domain.reporting.ReportingPeriod
import com.goldex.companion.domain.reporting.ReportingUiState
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily

/** The header and period controls are shared by the gateway and every report page. */
@Composable
internal fun ReportingTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    val colors = LocalGoldExColors.current
    Surface(
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp).clip(ButtonShape)
                        .background(colors.surfaceElevated)
                        .border(0.6.dp, colors.goldBorder, ButtonShape)
                ) {
                    Icon(HubArrowRight, contentDescription = "بازگشت", tint = colors.goldPrimary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(colors.goldPrimary))
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain,
                            fontFamily = VazirmatnFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(subtitle, fontSize = 10.5.sp, color = colors.textMuted,
                        fontFamily = VazirmatnFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun ReportingPeriodFilters(
    uiState: ReportingUiState,
    onSelectPeriod: (ReportingPeriod) -> Unit,
    onOpenCustomDateDialog: () -> Unit
) {
    val colors = LocalGoldExColors.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReportingPeriod.entries.forEach { period ->
            val selected = uiState.selectedPeriod == period
            val chipBg = animateColorAsState(
                if (selected) { if (colors.isDark) colors.goldPrimary else Color(0xFF1E2333) }
                else colors.surfaceElevated,
                label = "reportPeriodBackground"
            ).value
            val chipText = animateColorAsState(
                if (selected) { if (colors.isDark) Color(0xFF141B2B) else Color.White }
                else colors.textSecondary,
                label = "reportPeriodText"
            ).value
            Surface(
                shape = RoundedCornerShape(20.dp), color = chipBg,
                border = BorderStroke(0.6.dp, if (selected) colors.goldPrimary else colors.border.copy(alpha = 0.5f)),
                modifier = Modifier.clickable {
                    if (period == ReportingPeriod.CUSTOM) onOpenCustomDateDialog() else onSelectPeriod(period)
                }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (period == ReportingPeriod.CUSTOM) {
                        Icon(ReportingCalendar, contentDescription = null, tint = chipText, modifier = Modifier.size(14.dp))
                    }
                    val label = if (period == ReportingPeriod.CUSTOM && uiState.customStartDateShamsi.isNotBlank()) {
                        "${PersianNumberFormatter.toPersianDigits(uiState.customStartDateShamsi)} تا ${PersianNumberFormatter.toPersianDigits(uiState.customEndDateShamsi)}"
                    } else period.labelFa
                    AnimatedContent(targetState = label,
                        transitionSpec = { LuxuryMotion.numberSlideSpec(isIncreasing = true) },
                        label = "reportPeriodLabel") { current ->
                        Text(current, fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = chipText, fontFamily = VazirmatnFamily, maxLines = 1)
                    }
                }
            }
        }
    }
}

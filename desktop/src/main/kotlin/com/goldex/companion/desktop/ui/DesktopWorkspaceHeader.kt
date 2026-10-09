package com.goldex.companion.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.MarketHistoryConverter
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.*
import java.time.Instant
import java.time.ZoneId

// Global desktop controls use the requested capsule treatment, without changing mobile tokens.
internal val WorkspaceControlShape = RoundedCornerShape(24.dp)
internal val WorkspaceControlHeight = 48.dp

internal fun workspaceDate(now: Long): String {
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Tehran"))
    val (year, month, day) = MarketHistoryConverter.gregorianToShamsi(today.year, today.monthValue, today.dayOfMonth)
    val months = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val weekdays = listOf("دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه", "یکشنبه")
    return PersianNumberFormatter.toPersianDigits("${weekdays[today.dayOfWeek.value - 1]} $day ${months[month - 1]} $year")
}

@Composable internal fun WorkspaceHeader(state: WorkspaceState, actions: (@Composable () -> Unit)? = null) {
    val colors = LocalGoldExColors.current
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("workspace-header")) {
        val heading: @Composable () -> Unit = {
            Column {
                val title = when (state.destination) {
                    DesktopDestination.CALCULATOR -> "ماشین‌حساب تخصصی طلا"
                    DesktopDestination.RATES -> "تابلوی نرخ‌ها"
                    else -> state.destination.title
                }
                val subtitle = when (state.destination) {
                    DesktopDestination.INVENTORY -> "مدیریت جامع انبار، ویترین و ترازوی طلا"
                    else -> state.destination.subtitle
                }
                Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.textMain, fontWeight = FontWeight.Bold)
                Text(subtitle, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val status: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val online = state.connection == ConnectionStatus.ONLINE
                val statusColor = if (online) colors.marketGainText else colors.textMuted
                Surface(shape = WorkspaceControlShape, color = colors.surfaceElevated, border = colors.hairlineBorder,
                    modifier = Modifier.height(WorkspaceControlHeight).testTag("connection-chip")) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                        Text(if (online) "آنلاین" else "آفلاین", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                Surface(shape = WorkspaceControlShape, color = colors.surfaceElevated, border = colors.hairlineBorder,
                    modifier = Modifier.height(WorkspaceControlHeight).testTag("workspace-date")) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp), tint = colors.goldPrimary)
                        Text(workspaceDate(state.now), color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        if (maxWidth < 950.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { heading() }
                    status()
                }
                if (actions != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        actions()
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { heading() }
                status()
                if (actions != null) actions()
            }
        }
    }
}

@Composable internal fun WorkspaceControl(text: String, onClick: () -> Unit, modifier: Modifier,
    icon: ImageVector? = null, secondary: Boolean = false, enabled: Boolean = true, compact: Boolean = false) {
    val colors = LocalGoldExColors.current
    if (secondary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(WorkspaceControlHeight),
            enabled = enabled,
            shape = WorkspaceControlShape,
            border = colors.hairlineBorder,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceElevated,
                contentColor = colors.textMain,
                disabledContainerColor = colors.surfaceElevated,
                disabledContentColor = colors.textMuted
            ),
            contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null && !compact) {
                    Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) colors.goldPrimary else colors.textMuted)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) colors.textMain else colors.textMuted,
                    maxLines = 1
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(WorkspaceControlHeight),
            enabled = enabled,
            shape = WorkspaceControlShape,
            border = if (enabled) null else colors.hairlineBorder,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colors.goldButtonText,
                disabledContainerColor = colors.surfaceElevated,
                disabledContentColor = colors.textMuted
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (!enabled) Modifier.background(colors.surfaceElevated)
                        else Modifier.background(colors.goldButtonGradient)
                    )
                    .padding(horizontal = if (compact) 10.dp else 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null && !compact) {
                        Icon(icon, null, Modifier.size(18.dp), tint = colors.goldButtonText)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = text,
                        fontSize = if (compact) 11.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.goldButtonText,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

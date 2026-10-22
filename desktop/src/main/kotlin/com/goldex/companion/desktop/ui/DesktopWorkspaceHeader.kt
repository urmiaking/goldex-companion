package com.goldex.companion.desktop.ui

import com.goldex.companion.desktop.ui.controls.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.*
import com.goldex.companion.desktop.sync.DesktopSyncStatus
import com.goldex.companion.desktop.sync.DesktopSyncUiState
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
internal val WorkspaceControlShape = DesktopControls.CapsuleShape
internal val WorkspaceControlHeight = DesktopControls.Height

internal fun workspaceDate(now: Long): String {
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Tehran"))
    val (year, month, day) = MarketHistoryConverter.gregorianToShamsi(today.year, today.monthValue, today.dayOfMonth)
    val months = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val weekdays = listOf("دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه", "یکشنبه")
    return PersianNumberFormatter.toPersianDigits("${weekdays[today.dayOfWeek.value - 1]} $day ${months[month - 1]} $year")
}

@Composable internal fun WorkspaceHeader(state: WorkspaceState, actions: (@Composable () -> Unit)? = null,
    titleOverride: String? = null, subtitleOverride: String? = null, onBack: (() -> Unit)? = null,
    backTag: String = "customer-back", backDescription: String = "بازگشت به مشتریان",
    cloudState: DesktopSyncUiState? = null, onCloudClick: (() -> Unit)? = null) {
    val colors = LocalGoldExColors.current
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("workspace-header")) {
        val heading: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onBack != null) Surface(shape = WorkspaceControlShape, color = colors.surfaceElevated, border = colors.hairlineBorder) {
                    IconButton(onBack, Modifier.size(WorkspaceControlHeight).testTag(backTag)) {
                        Icon(Icons.Outlined.ArrowForward, backDescription, Modifier.size(20.dp), tint = colors.goldPrimary)
                    }
                }
                Column(Modifier.weight(1f)) {
                val title = titleOverride ?: when (state.destination) {
                    DesktopDestination.CALCULATOR -> "ماشین‌حساب تخصصی طلا"
                    DesktopDestination.RATES -> "تابلوی نرخ‌ها"
                    else -> state.destination.title
                }
                val subtitle = subtitleOverride ?: when (state.destination) {
                    DesktopDestination.INVENTORY -> "مدیریت جامع انبار، ویترین و ترازوی طلا"
                    else -> state.destination.subtitle
                }
                Text(title, modifier = Modifier.testTag("workspace-heading"), style = MaterialTheme.typography.headlineMedium,
                    color = colors.textMain, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val status: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (cloudState != null) {
                    val statusColor = when (cloudState.status) {
                        DesktopSyncStatus.SYNCED -> colors.marketGainText
                        DesktopSyncStatus.SYNCING -> colors.syncBlue
                        DesktopSyncStatus.PENDING, DesktopSyncStatus.AUTH_REQUIRED -> colors.goldPrimary
                        DesktopSyncStatus.OFFLINE, DesktopSyncStatus.DISABLED -> colors.textMuted
                        DesktopSyncStatus.ERROR -> colors.errorRed
                    }
                    val icon = when (cloudState.status) {
                        DesktopSyncStatus.DISABLED -> Icons.Outlined.CloudOff
                        DesktopSyncStatus.AUTH_REQUIRED, DesktopSyncStatus.PENDING, DesktopSyncStatus.SYNCING -> Icons.Outlined.CloudSync
                        DesktopSyncStatus.SYNCED -> Icons.Outlined.CloudDone
                        DesktopSyncStatus.OFFLINE, DesktopSyncStatus.ERROR -> Icons.Outlined.CloudOff
                    }
                    val label = when (cloudState.status) {
                        DesktopSyncStatus.DISABLED -> "ابری: غیرفعال"
                        DesktopSyncStatus.AUTH_REQUIRED -> "نیاز به ورود ابری"
                        DesktopSyncStatus.PENDING -> if (cloudState.pendingCount > 0) "${PersianNumberFormatter.toPersianDigits(cloudState.pendingCount.toString())} سند در انتظار" else "در انتظار همگام‌سازی"
                        DesktopSyncStatus.SYNCING -> "در حال همگام‌سازی..."
                        DesktopSyncStatus.SYNCED -> "ابری: همگام"
                        DesktopSyncStatus.OFFLINE -> "ابری: آفلاین"
                        DesktopSyncStatus.ERROR -> "خطای همگام‌سازی"
                    }
                    Surface(
                        shape = WorkspaceControlShape,
                        color = colors.surfaceElevated,
                        border = colors.hairlineBorder,
                        modifier = Modifier
                            .height(WorkspaceControlHeight)
                            .testTag("connection-chip")
                            .then(if (onCloudClick != null) Modifier.clickable(onClick = onCloudClick) else Modifier)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(icon, contentDescription = label, tint = statusColor, modifier = Modifier.size(17.dp))
                            Text(label, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                } else {
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
                    if (actions != null) actions()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    status()
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
    icon: ImageVector, secondary: Boolean = false, enabled: Boolean = true) {
    DesktopHeaderButton(text, onClick, modifier, icon, isSecondary = secondary, enabled = enabled)
}

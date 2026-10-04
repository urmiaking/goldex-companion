package com.goldex.companion.ui.sync

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.sync.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalcReceiptLong
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.hub.HubCamera
import com.goldex.companion.ui.hub.HubCloud
import com.goldex.companion.ui.hub.HubCloudDownload
import com.goldex.companion.ui.hub.HubCloudOff
import com.goldex.companion.ui.hub.HubCloudSync
import com.goldex.companion.ui.hub.HubCopy
import com.goldex.companion.ui.hub.HubDevices
import com.goldex.companion.ui.hub.HubMenuBook
import com.goldex.companion.ui.hub.HubPhoneInTalk
import com.goldex.companion.ui.hub.HubShieldCheck
import com.goldex.companion.ui.hub.HubShowcase
import com.goldex.companion.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun SyncStatus.title()=when(this) {
    SyncStatus.DISABLED -> "همگام‌سازی خاموش است"
    SyncStatus.AUTH_REQUIRED -> "ورود به حساب لازم است"
    SyncStatus.PENDING -> "در انتظار همگام‌سازی"
    SyncStatus.SYNCING -> "در حال همگام‌سازی"
    SyncStatus.SYNCED -> "اطلاعات همگام است"
    SyncStatus.OFFLINE -> "آفلاین؛ اطلاعات محلی محفوظ است"
    SyncStatus.LICENSE_REQUIRED -> "مجوز معتبر لازم است"
    SyncStatus.CONFLICT -> "اختلاف نسخه؛ بررسی لازم است"
    SyncStatus.RESTORE_REQUIRED -> "بازیابی اطلاعات لازم است"
    SyncStatus.WRITER_CHANGED -> "دستگاه نویسنده تغییر کرده است"
    SyncStatus.ERROR -> "ارتباط با ابر ناموفق بود"
}

fun formatBytesPersian(bytes: Long): String {
    if (bytes <= 0) return "۰ بایت"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> {
            val v = bytes / gb
            if (v % 1.0 == 0.0) {
                "${PersianNumberFormatter.toPersianDigits(v.toLong())} گیگابایت"
            } else {
                "${PersianNumberFormatter.formatDouble(v, 2).trimEnd('۰').trimEnd('.').trimEnd('٫')} گیگابایت"
            }
        }
        bytes >= mb -> {
            val v = bytes / mb
            if (v % 1.0 == 0.0) {
                "${PersianNumberFormatter.toPersianDigits(v.toLong())} مگابایت"
            } else {
                "${PersianNumberFormatter.formatDouble(v, 1).trimEnd('۰').trimEnd('.').trimEnd('٫')} مگابایت"
            }
        }
        bytes >= kb -> {
            val v = bytes / kb
            if (v % 1.0 == 0.0) {
                "${PersianNumberFormatter.toPersianDigits(v.toLong())} کیلوبایت"
            } else {
                "${PersianNumberFormatter.formatDouble(v, 1).trimEnd('۰').trimEnd('.').trimEnd('٫')} کیلوبایت"
            }
        }
        else -> "${PersianNumberFormatter.toPersianDigits(bytes)} بایت"
    }
}

fun formatLastBackupPersian(timestamp: Long): String {
    if (timestamp <= 0) return "هنوز انجام نشده"
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val isToday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    val timeStr = PersianNumberFormatter.toPersianDigits(timeFormat.format(Date(timestamp)))
    return if (isToday) {
        "امروز، $timeStr"
    } else {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        "${PersianNumberFormatter.toPersianDigits(dateFormat.format(Date(timestamp)))}، $timeStr"
    }
}

@Composable
fun CloudSyncButton(state: SyncUiState, reducedMotion: Boolean? = null, onClick: () -> Unit) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    val reduced = reducedMotion ?: (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f)
    val contentDesc = state.status.title()

    Surface(
        onClick = onClick,
        shape = ButtonShape,
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier
            .padding(end = 12.dp)
            .size(40.dp)
            .semantics { contentDescription = contentDesc }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!state.enabled || state.status == SyncStatus.DISABLED) {
                Icon(
                    imageVector = HubCloudOff,
                    contentDescription = contentDesc,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = HubCloud,
                    contentDescription = contentDesc,
                    tint = if (state.status == SyncStatus.SYNCING) colors.syncBlue else colors.goldPrimary,
                    modifier = Modifier.size(20.dp)
                )

                // Status Badge at bottom-right (in RTL layout, Alignment.BottomStart is the bottom-right corner)
                AnimatedContent(
                    targetState = state.status,
                    transitionSpec = {
                        if (reduced) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.5f) + fadeIn(tween(180)))
                                .togetherWith(scaleOut(tween(160, easing = FastOutSlowInEasing), targetScale = 0.5f) + fadeOut(tween(140)))
                        }
                    },
                    label = "cloud_status_badge",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                ) { targetStatus ->
                    when (targetStatus) {
                        SyncStatus.SYNCING -> {
                            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                            val rotation by if (!reduced) {
                                infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "sync_angle"
                                )
                            } else {
                                remember { mutableFloatStateOf(0f) }
                            }

                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(0.8.dp)
                                    .clip(CircleShape)
                                    .background(colors.syncBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = contentDesc,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(8.5.dp)
                                        .rotate(rotation)
                                )
                            }
                        }

                        SyncStatus.SYNCED -> {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(0.8.dp)
                                    .clip(CircleShape)
                                    .background(colors.profitGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = contentDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(8.5.dp)
                                )
                            }
                        }

                        SyncStatus.ERROR -> {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(0.8.dp)
                                    .clip(CircleShape)
                                    .background(colors.errorRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = contentDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        }

                        SyncStatus.CONFLICT, SyncStatus.LICENSE_REQUIRED, SyncStatus.RESTORE_REQUIRED, SyncStatus.AUTH_REQUIRED, SyncStatus.WRITER_CHANGED -> {
                            val badgeIcon = when (targetStatus) {
                                SyncStatus.AUTH_REQUIRED -> Icons.Default.AccountCircle
                                SyncStatus.LICENSE_REQUIRED, SyncStatus.WRITER_CHANGED -> Icons.Default.Lock
                                else -> Icons.Default.Warning
                            }
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(0.8.dp)
                                    .clip(CircleShape)
                                    .background(colors.syncWarning),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = contentDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        }

                        SyncStatus.OFFLINE -> {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(0.8.dp)
                                    .clip(CircleShape)
                                    .background(colors.errorRed.copy(alpha = 0.85f))
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(0.8.dp)
                                    .clip(CircleShape)
                                    .background(colors.goldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = contentDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(8.5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudMasterSyncCard(
    enabled: Boolean,
    shopName: String,
    interactive: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f)),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.goldContainer, colors.goldPrimary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HubCloudSync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "همگام‌سازی خودکار",
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontSize = 15.sp
                        )
                        Text(
                            text = shopName.ifBlank { "حساب تجاری زرگری مروارید" },
                            fontWeight = FontWeight.Medium,
                            color = colors.goldPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = interactive,
                    modifier = Modifier.semantics { contentDescription = "همگام‌سازی خودکار" },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.goldPrimary,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surface
                    )
                )
            }
            HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)
            Text(
                text = "پشتیبان‌گیری امن و آنی فاکتورها و دفاتر معین با رمزنگاری اختصاصی.",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun CloudServerMetricsCard(
    state: SyncUiState,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val storage = state.storage

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.6.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = HubShieldCheck,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "شاخص‌های سرور خزانه مرکزی",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.profitGreen)
                    )
                    Text(
                        text = "پینگ: ms ${PersianNumberFormatter.toPersianDigits(state.pingMs)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.profitGreen
                    )
                }
            }

            // 2-Column Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Last backup (Right side in RTL)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = BorderStroke(0.5.dp, colors.border.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "آخرین پشتیبان‌گیری",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = formatLastBackupPersian(state.lastSuccessAt),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                    }
                }

                // Metric 2: Encryption (Left side in RTL)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = BorderStroke(0.5.dp, colors.border.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "رمزنگاری داده‌ها",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "سرتاسری AES-۲۵۶",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                    }
                }
            }

            // Storage Metrics Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فضای ابری",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${formatBytesPersian(storage.usedBytes)} از ${formatBytesPersian(storage.quotaBytes)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                }

                val progressFraction = if (storage.quotaBytes > 0) {
                    (storage.usedBytes.toFloat() / storage.quotaBytes.toFloat()).coerceIn(0.01f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(colors.goldSecondary, colors.goldPrimary)
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatBytesPersian(storage.freeBytes)} آزاد",
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = if (storage.tier.equals("PERMANENT", ignoreCase = true)) "نسخه دائمی (۵ گیگابایت)" else "نسخه آزمایشی (۵۰ مگابایت)",
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
fun CloudSyncPreferencesList(
    wifiOnly: Boolean,
    onToggleWifiOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "دفاتر و داده‌های تحت پوشش همگام‌سازی",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        SyncPreferenceItem(
            icon = CalcReceiptLong,
            title = "فاکتورها و معاملات",
            subtitle = "خرید، فروش و مظنه"
        )

        SyncPreferenceItem(
            icon = HubMenuBook,
            title = "دفاتر معین و حساب‌ها",
            subtitle = "بدهکاران و بستانکاران"
        )

        SyncPreferenceItem(
            icon = HubShowcase,
            title = "تراز وزنی و ویترین",
            subtitle = "موجودی ۷۵۰ و مسکوکات"
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(0.5.dp, colors.border.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HubCamera,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "تصاویر اتیکت و فاکتور",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain
                        )
                        Text(
                            text = "فقط با Wi-Fi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.goldPrimary
                        )
                    }
                }
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = onToggleWifiOnly,
                    modifier = Modifier.semantics { contentDescription = "همگام‌سازی تصاویر فقط با Wi-Fi" },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.goldPrimary,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surface
                    )
                )
            }
        }
    }
}

@Composable
private fun SyncPreferenceItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.5.dp, colors.border.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMain
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.profitGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.profitGreen,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun CloudSettingsContent(
    viewModel: CloudSyncViewModel,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true
) {
    val state by viewModel.state.collectAsState()
    val form by viewModel.form.collectAsState()
    val colors = LocalGoldExColors.current
    val context = LocalContext.current

    LaunchedEffect(form.backupPath) {
        form.backupPath?.let { path ->
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(path))
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                .setType("application/json")
                .putExtra(android.content.Intent.EXTRA_STREAM, uri)
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(android.content.Intent.createChooser(intent, "ذخیرهٔ پشتیبان محلی"))
            viewModel.backupShared()
        }
    }

    var phone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(form.phone) }
    var code by androidx.compose.runtime.saveable.rememberSaveable(form.challenge?.optString("challengeId")) { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(scrollModifier)
            .padding(if (scrollable) 16.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CloudMasterSyncCard(
            enabled = state.enabled,
            shopName = state.shopName,
            interactive = !form.busy && !state.busy,
            onToggle = viewModel::setEnabled
        )

        if (state.enabled) {
            if (state.phone.isNotBlank() && state.status != SyncStatus.AUTH_REQUIRED) {
                CloudServerMetricsCard(state = state)
                CloudSyncPreferencesList(
                    wifiOnly = state.storage.isWifiOnlyAssets,
                    onToggleWifiOnly = viewModel::setWifiOnlyAssets
                )
            }

            if (state.message.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.syncWarning.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, colors.syncWarning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.syncWarning, modifier = Modifier.size(18.dp))
                        Text(state.message, color = colors.syncWarning, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }

            if (state.phone.isBlank() || state.status == SyncStatus.AUTH_REQUIRED) {
                CloudSignInForm(
                    phone = phone,
                    code = code,
                    form = form,
                    onPhoneChange = { phone = it },
                    onCodeChange = { code = it },
                    onRequest = { viewModel.requestCode(phone) },
                    onVerify = { viewModel.verifyCode(code) },
                    onChangePhone = viewModel::changePhone,
                    onResend = { viewModel.requestCode(form.phone) }
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.goldContainer.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, colors.goldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HubShieldCheck, contentDescription = null, tint = colors.goldPrimary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "دورهٔ آزمایشی ۱۴ روزه رایگان",
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "امکان استفاده کامل از خدمات ابری و همگام‌سازی بدون نیاز به پرداخت.",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        GoldButton(
                            text = "فعال‌سازی دورهٔ آزمایشی",
                            icon = HubShieldCheck,
                            onClick = viewModel::activateTrial,
                            isLoading = form.busy,
                            enabled = !form.busy,
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        CloudAccountActions(
            state = state,
            formBusy = form.busy,
            onSync = viewModel::sync,
            onReviewConflict = viewModel::reviewConflict,
            onRestore = { confirm = "restore" },
            onTakeover = { confirm = "takeover" },
            onReauthenticate = viewModel::reauthenticate,
            onListDevices = viewModel::listDevices,
            onExportBackup = viewModel::exportBackup,
            onLogout = { confirm = "logout" },
            onDetach = { confirm = "detach" }
        )

        if (form.error.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.errorRed.copy(alpha = 0.15f),
                border = BorderStroke(0.8.dp, colors.errorRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = colors.errorRed, modifier = Modifier.size(18.dp))
                    Text(form.error, color = colors.errorRed, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            if (form.error.contains("مجوز") || state.status == SyncStatus.LICENSE_REQUIRED) {
                GoldButton(
                    text = "فعال‌سازی مهلت تست ۱۴ روزه رایگان",
                    onClick = { viewModel.activateTrial() },
                    isLoading = form.busy,
                    enabled = !form.busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceElevated.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "خاموش‌کردن سینک یا پایان مجوز، اطلاعات محلی و نسخهٔ ابری را حذف نمی‌کند. سینک جایگزین پشتیبان مستقل نیست.",
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    form.review?.let { review ->
        Dialog(onDismissRequest = viewModel::closeReview) {
            Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f)), modifier = Modifier.heightIn(max = 680.dp)) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("مقایسهٔ اختلاف‌ها", style = MaterialTheme.typography.titleLarge, color = colors.textMain, fontWeight = FontWeight.Bold)
                    Text("انتخاب نسخهٔ محلی برای تمام رکوردهای این گروه اعمال می‌شود. محاسبات مالی دوباره اجرا نمی‌شوند. هر دو نسخه در پشتیبان محفوظ می‌مانند. برای انتخاب نسخهٔ ابری، بازیابی کامل با تأیید جداگانه انجام دهید.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                    val local = review.getJSONArray("local")
                    val remote = review.getJSONArray("remote")
                    for (i in 0 until local.length()) {
                        val c = local.getJSONObject(i)
                        val r = remote.getJSONObject(i)
                        Text("${cloudRecordTitle(c.getString("type"))} · ${PersianNumberFormatter.toPersianDigits(c.getString("id"))}", color = colors.goldPrimary, fontWeight = FontWeight.Bold)
                        val l = c.optJSONObject("localPayload")
                        val server = r.optJSONObject("payload")
                        val fields = ((l?.keys()?.asSequence()?.toList() ?: emptyList()) + (server?.keys()?.asSequence()?.toList() ?: emptyList())).distinct()
                        if (l == null) Text("نسخهٔ محلی: حذف رکورد", color = colors.syncWarning)
                        if (r.getBoolean("deleted")) Text("نسخهٔ ابری: حذف رکورد؛ شناسهٔ حذف‌شده قابل بازگردانی نیست", color = colors.syncWarning)
                        for (field in fields) if (SyncJson.canonical(l?.opt(field)) != SyncJson.canonical(server?.opt(field))) {
                            Text(field, color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
                            Text("محلی: ${PersianNumberFormatter.toPersianDigits(l?.opt(field)?.toString() ?: "حذف‌شده")}", color = colors.textMain)
                            Text("ابری: ${PersianNumberFormatter.toPersianDigits(server?.opt(field)?.toString() ?: "حذف‌شده")}", color = colors.textSecondary)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton("بستن", onClick = viewModel::closeReview, isSecondary = true, modifier = Modifier.weight(1f))
                        GoldButton("انتخاب محلی کل گروه", onClick = { confirm = "local" }, enabled = !form.busy, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    form.devices?.let { devices ->
        Dialog(onDismissRequest = viewModel::closeReview) {
            Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f))) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("انتقال به دستگاه دیگر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Text("دستگاه مقصد باید قبلاً با همین حساب وارد شده باشد. صف این گوشی پیش از انتقال کامل ارسال می‌شود.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                    val list = devices.getJSONArray("devices")
                    for (i in 0 until list.length()) {
                        val device = list.getJSONObject(i)
                        if (!device.getBoolean("isWriter")) {
                            GoldButton(
                                text = "انتقال به ${device.getString("id").take(8)}",
                                icon = HubDevices,
                                onClick = { confirm = "release:${device.getString("id")}" },
                                isSecondary = true,
                                enabled = !form.busy,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    GoldButton("بستن", onClick = viewModel::closeReview, isSecondary = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (confirm.isNotBlank()) {
        Dialog(onDismissRequest = { confirm = "" }) {
            Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = when {
                            confirm == "detach" -> "جداسازی داده از حساب ابری"
                            confirm == "local" -> "تأیید نسخهٔ محلی"
                            confirm.startsWith("release:") -> "انتقال نویسندگی"
                            confirm == "restore" -> "بازیابی اطلاعات از ابر"
                            confirm == "takeover" -> "انتقال نویسندگی به این گوشی"
                            else -> "خروج از حساب ابری"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = when {
                            confirm == "detach" -> "پس از ساخت پشتیبان، دادهٔ فعلی به‌صورت محلی نگهداری و ارتباط آن با حساب و صف قبلی جدا می‌شود. برای حساب دارای داده، بازیابی با تأیید جداگانه لازم است. پیش از ادامه پشتیبان را در جای امن ذخیره کنید."
                            confirm == "local" -> "نسخهٔ محلی برای تمام رکوردهای گروه انتخاب شود؟ هر اختلاف مالی این گروه باید با هم بررسی شود. در صورت تغییر دوبارهٔ سرور، ارسال مجدداً متوقف خواهد شد."
                            confirm.startsWith("release:") -> "پس از ارسال کامل صف، نویسندگی به دستگاه انتخاب‌شده منتقل می‌شود. ویرایش و ارسال این گوشی متوقف می‌شود."
                            confirm == "restore" -> "دادهٔ فعال این نصب با نسخهٔ ابری جایگزین می‌شود. پیش از جایگزینی، پشتیبان اطلاعات و صف محلی در حافظهٔ برنامه نگهداری می‌شود. در صورت اختلاف مالی، کل مجموعه بررسی شود."
                            confirm == "takeover" -> "تغییرات ارسال‌نشدهٔ گوشی قبلی در نسخهٔ ابری نیستند. گوشی قبلی حق ارسال را از دست می‌دهد. تأیید تازهٔ موبایل و سپس بازیابی نسخهٔ ابری لازم است."
                            else -> "سینک خاموش می‌شود و اطلاعات و صف ارسال روی این دستگاه باقی می‌مانند."
                        },
                        color = colors.textSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    )
                    // RTL dialog buttons: Cancel on RIGHT (first child), Confirm on LEFT (second child)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton("انصراف", onClick = { confirm = "" }, isSecondary = true, modifier = Modifier.weight(1f))
                        GoldButton(
                            "تأیید",
                            onClick = {
                                val action = confirm
                                confirm = ""
                                when {
                                    action == "detach" -> viewModel.detach()
                                    action == "local" -> viewModel.chooseLocal()
                                    action.startsWith("release:") -> viewModel.releaseWriter(action.removePrefix("release:"))
                                    action == "restore" -> viewModel.restore()
                                    action == "takeover" -> viewModel.takeover()
                                    else -> viewModel.logout()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** Keeps recovery actions visible while account maintenance stays collapsed by default. */
@Composable
internal fun CloudAccountActions(
    state: SyncUiState,
    formBusy: Boolean,
    onSync: () -> Unit,
    onReviewConflict: () -> Unit,
    onRestore: () -> Unit,
    onTakeover: () -> Unit,
    onReauthenticate: () -> Unit,
    onListDevices: () -> Unit,
    onExportBackup: () -> Unit,
    onLogout: () -> Unit,
    onDetach: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val signedIn = state.enabled && state.phone.isNotBlank() && state.status != SyncStatus.AUTH_REQUIRED
    val hasBackup = state.phone.isNotBlank() || state.readOnly
    val canDetach = state.readOnly || state.phone.isNotBlank() || state.status == SyncStatus.WRITER_CHANGED
    var expanded by androidx.compose.runtime.saveable.rememberSaveable(state.phone) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (signedIn) {
            GoldButton(
                text = if (state.busy || formBusy || state.status == SyncStatus.SYNCING) "در حال همگام‌سازی..." else "همگام‌سازی دستی همین حالا",
                icon = HubCloudSync,
                onClick = onSync,
                isLoading = state.busy || formBusy || state.status == SyncStatus.SYNCING,
                enabled = !state.busy && !formBusy,
                modifier = Modifier.fillMaxWidth()
            )
            GoldButton(
                text = "دریافت نسخه پشتیبان آفلاین (Excel / JSON)",
                icon = HubCloudDownload,
                onClick = onExportBackup,
                isLoading = false,
                enabled = !state.busy && !formBusy,
                isSecondary = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.status == SyncStatus.CONFLICT) {
                CloudActionRow("مقایسهٔ نسخه‌های کل گروه", HubCopy, !formBusy, onReviewConflict)
            }
            if (state.status in listOf(SyncStatus.RESTORE_REQUIRED, SyncStatus.CONFLICT)) {
                CloudActionRow("بررسی و بازیابی نسخهٔ ابری", HubCloudDownload, !formBusy, onRestore)
            }
            if (state.status == SyncStatus.WRITER_CHANGED) {
                GoldButton(
                    text = "انتقال نویسندگی به این دستگاه",
                    icon = HubDevices,
                    onClick = onTakeover,
                    enabled = !formBusy,
                    modifier = Modifier.fillMaxWidth()
                )
                CloudActionRow("تأیید تازهٔ شماره برای انتقال", HubPhoneInTalk, !formBusy, onReauthenticate)
            }
        }

        if (signedIn || hasBackup || canDetach) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { expanded = !expanded }
                            .semantics { stateDescription = if (expanded) "باز" else "بسته" }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        Text(
                            "گزینه‌های بیشتر",
                            color = colors.textMain,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "chevronRotation"
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp).rotate(chevronRotation)
                        )
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(durationMillis = 150))
                    ) {
                        Column {
                            HorizontalDivider(color = colors.border)
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (signedIn && state.status == SyncStatus.SYNCED) {
                                    CloudActionRow("انتقال به دستگاه دیگر", HubDevices, !formBusy, onListDevices)
                                }
                                if (hasBackup) {
                                    CloudActionRow("ذخیرهٔ پشتیبان محلی و اختلاف‌ها", HubCloudDownload, !formBusy && !state.busy, onExportBackup)
                                }
                                if (canDetach || signedIn) {
                                    HorizontalDivider(color = colors.border)
                                }
                                if (canDetach) {
                                    CloudActionRow(
                                        "جداسازی داده برای اتصال به حساب دیگر", HubCloudOff,
                                        !formBusy && !state.busy, onDetach
                                    )
                                }
                                if (signedIn) {
                                    CloudActionRow("خروج از حساب", HubArrowRight, !formBusy && !state.busy, onLogout, caution = true)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security Micro-Trust Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.profitGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "مطابق با استاندارد امنیت اتحادیه طلا و جواهر و بانک مرکزی",
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
    }
}

/** A wrapping label and a 48dp minimum touch target keep compact actions usable at large font sizes. */
@Composable
private fun CloudActionRow(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    caution: Boolean = false
) {
    val colors = LocalGoldExColors.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        color = colors.surfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (!enabled) colors.textMuted else if (caution) colors.syncWarning else colors.goldPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                color = if (!enabled) colors.textMuted else colors.textMain,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Dedicated, luxury Cloud Sync Section designed specifically for the Onboarding Wizard.
 * Features dual selectable option cards (Offline by default, Online with animated expansion),
 * phone authentication, and a prominent celebratory cloud data restoration experience.
 */
@Composable
fun WizardCloudSyncSection(
    viewModel: CloudSyncViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val form by viewModel.form.collectAsState()
    val colors = LocalGoldExColors.current

    // Offline is default unless user already enabled cloud or has entered phone
    var isOnlineSelected by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(state.enabled || state.phone.isNotBlank())
    }

    var phone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(form.phone) }
    var code by androidx.compose.runtime.saveable.rememberSaveable(form.challenge?.optString("challengeId")) { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LuxuryCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "نحوه ذخیره و همگام‌سازی اطلاعات",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border)
                ) {
                    Text(
                        text = "اختیاری",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "محل نگهداری اطلاعات را تعیین کنید؛ برنامه به صورت پیش‌فرض کاملاً آفلاین کار می‌کند.",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }

        // Dual Square Selection Cards in One Row (No RadioButtons, No Extra Text)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Offline Mode Card (Right side in RTL, first child)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        isOnlineSelected = false
                        viewModel.setEnabled(false)
                    },
                shape = RoundedCornerShape(16.dp),
                color = if (!isOnlineSelected) colors.goldContainer.copy(alpha = 0.35f) else colors.surfaceElevated,
                border = BorderStroke(
                    if (!isOnlineSelected) 1.5.dp else 0.8.dp,
                    if (!isOnlineSelected) colors.goldPrimary else colors.border
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isOnlineSelected) colors.goldPrimary.copy(alpha = 0.15f) else colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HubCloudOff,
                            contentDescription = null,
                            tint = if (!isOnlineSelected) colors.goldPrimary else colors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ذخیره در دستگاه",
                        fontSize = 12.sp,
                        fontWeight = if (!isOnlineSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isOnlineSelected) colors.textMain else colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "(آفلاین)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (!isOnlineSelected) colors.goldPrimary else colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Online Mode Card (Left side in RTL, second child)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        isOnlineSelected = true
                        viewModel.setEnabled(true)
                    },
                shape = RoundedCornerShape(16.dp),
                color = if (isOnlineSelected) colors.goldContainer.copy(alpha = 0.35f) else colors.surfaceElevated,
                border = BorderStroke(
                    if (isOnlineSelected) 1.5.dp else 0.8.dp,
                    if (isOnlineSelected) colors.goldPrimary else colors.border
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOnlineSelected) colors.goldPrimary.copy(alpha = 0.15f) else colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HubCloud,
                            contentDescription = null,
                            tint = if (isOnlineSelected) colors.goldPrimary else colors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "همگام‌سازی ابری",
                        fontSize = 12.sp,
                        fontWeight = if (isOnlineSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isOnlineSelected) colors.textMain else colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "(آنلاین)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isOnlineSelected) colors.goldPrimary else colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Animated Content for Online Mode
        AnimatedVisibility(
            visible = isOnlineSelected,
            enter = expandVertically(animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

                // If backup / restore is detected on cloud server
                if (state.status in listOf(SyncStatus.RESTORE_REQUIRED, SyncStatus.CONFLICT)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.goldContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.2.dp, colors.goldPrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.goldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HubCloudDownload,
                                        contentDescription = null,
                                        tint = colors.surface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "نسخهٔ پشتیبان ابری شما پیدا شد! 🎉",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = "اطلاعات گالری و فاکتورهای حساب شماره ${PersianNumberFormatter.toPersianDigits(state.phone)} در سرور موجود است.",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Text(
                                text = "آیا مایلید اطلاعات گذشته شما بر روی این دستگاه بازیابی و جایگزین شود؟",
                                fontSize = 11.5.sp,
                                color = colors.textMain,
                                fontWeight = FontWeight.Medium
                            )

                            // RTL buttons: Secondary (cancel/ignore) on RIGHT, Primary (restore) on LEFT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoldButton(
                                    text = "شروع تازه",
                                    onClick = { confirm = "local" },
                                    isSecondary = true,
                                    enabled = !form.busy && !state.busy,
                                    modifier = Modifier.weight(1f)
                                )
                                GoldButton(
                                    text = if (state.busy || form.busy) "در حال بازیابی..." else "بازیابی کامل اطلاعات",
                                    icon = HubCloudDownload,
                                    onClick = { confirm = "restore" },
                                    isLoading = state.busy || form.busy,
                                    enabled = !state.busy && !form.busy,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }
                } else if (state.restoredGeneration > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.profitGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.profitGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "اطلاعات ابری با موفقیت بازیابی شد ✓",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.profitGreen
                                )
                                Text(
                                    text = "کلیه سوابق، دفاتر و فاکتورها روی این دستگاه قرار گرفتند.",
                                    fontSize = 10.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // If not signed in / phone needed
                if (state.phone.isBlank() || state.status == SyncStatus.AUTH_REQUIRED) {
                    CloudSignInForm(
                        phone = phone,
                        code = code,
                        form = form,
                        onPhoneChange = { phone = it },
                        onCodeChange = { code = it },
                        onRequest = { viewModel.requestCode(phone) },
                        onVerify = { viewModel.verifyCode(code) },
                        onChangePhone = viewModel::changePhone,
                        onResend = { viewModel.requestCode(form.phone) }
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.goldContainer.copy(alpha = 0.25f),
                        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💡", fontSize = 16.sp)
                            Text(
                                text = "در صورت داشتن حساب قبلی، پس از تأیید شماره، نسخه پشتیبان به صورت خودکار شناسایی و پیشنهاد بازیابی داده می‌شود.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else if (state.status !in listOf(SyncStatus.RESTORE_REQUIRED, SyncStatus.CONFLICT)) {
                    CloudAccountSummary(state)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.goldContainer.copy(alpha = 0.25f),
                        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✓", fontSize = 16.sp, color = colors.profitGreen, fontWeight = FontWeight.Bold)
                            Text(
                                text = "حساب ابری متصل شد. پس از فشردن دکمه «ورود به داشبورد قیراط»، اطلاعات این راه‌اندازی به صورت خودکار در فضای ابری بارگذاری و همگام‌سازی خواهند شد.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldButton(
                            text = "خروج از حساب",
                            onClick = { confirm = "logout" },
                            isSecondary = true,
                            enabled = !form.busy && !state.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (form.error.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.errorRed.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, colors.errorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = colors.errorRed, modifier = Modifier.size(16.dp))
                            Text(form.error, color = colors.errorRed, fontSize = 11.5.sp)
                        }
                    }
                    if (form.error.contains("مجوز") || state.status == SyncStatus.LICENSE_REQUIRED) {
                        GoldButton(
                            text = "فعال‌سازی مهلت تست ۱۴ روزه رایگان",
                            onClick = { viewModel.activateTrial() },
                            isLoading = form.busy,
                            enabled = !form.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialogs (RTL: Cancel on RIGHT, Confirm on LEFT)
    if (confirm.isNotBlank()) {
        Dialog(
            onDismissRequest = { confirm = "" },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.goldBorder)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = when (confirm) {
                            "restore" -> "بازیابی اطلاعات از حساب ابری"
                            "local" -> "شروع تازه"
                            else -> "خروج از حساب ابری"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = when (confirm) {
                            "restore" -> "اطلاعات موجود روی سرور ابری بر روی این دستگاه بازیابی و جایگزین خواهد شد. آیا ادامه می‌دهید؟"
                            "local" -> "نسخهٔ ابری قبلی نادیده گرفته شده و اتصال قطع می‌شود تا اطلاعات محلی جدیدی که در مراحل راه‌اندازی وارد کرده‌اید حفظ گردند. آیا ادامه می‌دهید؟"
                            else -> "سینک خاموش می‌شود و ارتباط با حساب ابری قطع خواهد شد."
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldButton(
                            text = "انصراف",
                            onClick = { confirm = "" },
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                        GoldButton(
                            text = if (confirm == "restore") "تأیید و بازیابی" else "تأیید",
                            onClick = {
                                val action = confirm
                                confirm = ""
                                when (action) {
                                    "restore" -> viewModel.restore()
                                    "local" -> {
                                        viewModel.detach()
                                        isOnlineSelected = false
                                    }
                                    else -> {
                                        viewModel.logout()
                                        isOnlineSelected = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSettingsDialog(viewModel: CloudSyncViewModel, onDismiss: () -> Unit, canDismiss: Boolean = true) {
    CloudSettingsModal(onDismiss = onDismiss, canDismiss = canDismiss) { CloudSettingsContent(viewModel) }
}

@Composable
fun CloudConnectionToggle(enabled: Boolean, interactive: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.6.dp, colors.goldBorder)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) colors.goldContainer else colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enabled) HubCloud else HubCloudOff,
                    contentDescription = null,
                    tint = if (enabled) colors.goldPrimary else colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("اتصال به ابر", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("فاکتورها، حساب‌ها و موجودی با حساب شما همگام می‌شوند.", color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                enabled = interactive,
                modifier = Modifier.semantics { contentDescription = "اتصال به ابر" },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.goldPrimary,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surface
                )
            )
        }
    }
}

@Composable
fun CloudAccountSummary(state: SyncUiState) {
    val colors = LocalGoldExColors.current
    val statusColor = when (state.status) {
        SyncStatus.SYNCED -> colors.profitGreen
        SyncStatus.SYNCING -> colors.syncBlue
        SyncStatus.ERROR, SyncStatus.CONFLICT, SyncStatus.LICENSE_REQUIRED -> colors.syncWarning
        else -> colors.goldPrimary
    }

    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceElevated, border = BorderStroke(0.6.dp, colors.border)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = state.status.title(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (state.pending > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, colors.goldBorder)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(state.pending)} تغییر",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("شمارهٔ حساب", color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
                Text(PersianNumberFormatter.toPersianDigits(state.phone), color = colors.textMain, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (state.lastSuccessAt > 0) "آخرین اتصال: ${PersianNumberFormatter.toPersianDigits(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(state.lastSuccessAt)))}" else "هنوز همگام‌سازی انجام نشده",
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
fun CloudSignInForm(phone: String, code: String, form: CloudFormState, onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit, onRequest: () -> Unit, onVerify: () -> Unit,
    onChangePhone: () -> Unit, onResend: () -> Unit) {
    val colors=LocalGoldExColors.current
    val challenge=form.challenge
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var seconds by remember(challenge) { mutableIntStateOf(0) }
    LaunchedEffect(challenge,form.requestedAtMillis) {
        if(challenge!=null) do {
            seconds=((challenge.optInt("retryAfter",60)*1000L-(System.currentTimeMillis()-form.requestedAtMillis)+999)/1000).toInt().coerceAtLeast(0)
            if(seconds>0) delay(1000)
        } while(seconds>0)
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if(challenge==null) "ورود به حساب ابری" else "تأیید شماره موبایل",color=colors.textMain,fontWeight=FontWeight.Bold)
        Text(if(challenge==null) "شمارهٔ حساب می‌تواند با شمارهٔ تماس فروشگاه متفاوت باشد." else
            "کد ورود برای ${PersianNumberFormatter.toPersianDigits(form.phone)}",color=colors.textSecondary,style=MaterialTheme.typography.bodySmall)
        if(challenge==null) {
            GoldInputField(value=phone,onValueChange={ onPhoneChange(PersianNumberFormatter.toEnglishDigits(it).take(14)) },
                label="شماره موبایل",keyboardType=KeyboardType.Phone,useThousandsSeparator=false,enabled=!form.busy,modifier=Modifier.fillMaxWidth())
            GoldButton("دریافت کد ورود",onClick={ keyboard?.hide(); onRequest() },isLoading=form.busy,
                enabled=!form.busy && Regex("^(09[0-9]{9}|(?:[+]|00)?989[0-9]{9})$").matches(phone.trim()),modifier=Modifier.fillMaxWidth())
        } else {
            if(challenge.optString("otpMode")=="temporary") Surface(shape=ButtonShape,color=colors.goldContainer) {
                Text("کد ورود موقت: ${PersianNumberFormatter.toPersianDigits(challenge.optString("temporaryCode"))}",
                    modifier=Modifier.fillMaxWidth().padding(12.dp),color=colors.goldPrimary,style=MaterialTheme.typography.bodyMedium)
            }
            GoldInputField(value=code,onValueChange={
                val digits=PersianNumberFormatter.toEnglishDigits(it).filter(Char::isDigit).take(6)
                onCodeChange(digits)
                if(digits.length==6) keyboard?.hide()
            },
                label="کد ورود شش‌رقمی",keyboardType=KeyboardType.NumberPassword,useThousandsSeparator=false,enabled=!form.busy,modifier=Modifier.fillMaxWidth())
            GoldButton("تأیید و اتصال",onClick={ keyboard?.hide(); onVerify() },isLoading=form.busy,enabled=!form.busy && code.length==6,modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick=onChangePhone,enabled=!form.busy) { Text("تغییر شماره",color=colors.textMuted) }
                TextButton(onClick=onResend,enabled=!form.busy && seconds==0) {
                    Text(if(seconds>0) "درخواست مجدد · ${PersianNumberFormatter.toPersianDigits(seconds)} ثانیه" else "دریافت کد جدید",
                        color=if(seconds>0) colors.textMuted else colors.goldPrimary)
                }
            }
        }
    }
}

/** Matches the bottom-anchored settings modals with Stitch luxury styling. */
@Composable
fun CloudSettingsModal(onDismiss: () -> Unit, canDismiss: Boolean = true, content: @Composable () -> Unit) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    val reduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        if (canDismiss && visible) scope.launch {
            visible = false
            if (!reduced) delay(LuxuryMotion.DURATION_MODAL_EXIT.toLong())
            onDismiss()
        }
    }
    LaunchedEffect(Unit) { visible = true }
    val scrim by animateFloatAsState(if (visible) 0.65f else 0f, tween(if (reduced) 0 else LuxuryMotion.DURATION_MODAL_ENTER), label = "cloud_scrim")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val pulseAlpha by if (!reduced) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrim))
                    .imePadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss),
                contentAlignment = Alignment.BottomCenter
            ) {
                val availableHeight = maxHeight * 0.90f
                AnimatedVisibility(
                    visible = visible,
                    enter = if (reduced) EnterTransition.None else LuxuryMotion.ModalEnter,
                    exit = if (reduced) ExitTransition.None else LuxuryMotion.ModalExit
                ) {
                    Surface(
                        Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .heightIn(max = availableHeight)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(colors.goldPrimary.copy(alpha = 0.6f), colors.border.copy(alpha = 0.3f))))
                    ) {
                        Column(Modifier.navigationBarsPadding()) {
                            // Handle Bar (Bottom Sheet Anchor)
                            Box(
                                Modifier
                                    .padding(top = 10.dp, bottom = 6.dp)
                                    .size(48.dp, 4.dp)
                                    .clip(CircleShape)
                                    .background(colors.goldBorder.copy(alpha = 0.6f))
                                    .align(Alignment.CenterHorizontally)
                            )
                            // Modal Header with Pulse Dot
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.goldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(HubCloudSync, null, tint = colors.goldPrimary, modifier = Modifier.size(24.dp))
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.profitGreen.copy(alpha = pulseAlpha))
                                            )
                                            Text(
                                                "همگام‌سازی ابری و پشتیبان‌گیری",
                                                color = colors.textMain,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            "بایگانی امن و خودکار فاکتورها، حساب‌های معین و دفاتر",
                                            color = colors.textSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (canDismiss) {
                                    IconButton(onClick = dismiss, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Close, "بستن همگام‌سازی ابری", tint = colors.textMuted)
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.35f))
                            Box(Modifier.weight(1f, false)) { content() }
                        }
                    }
                }
            }
        }
    }
}

private fun cloudRecordTitle(type: String)=when(type) {
    "customer"->"طرف‌حساب"; "ledger"->"دفتر حساب"; "invoice"->"فاکتور"; "barterInvoice"->"فاکتور تهاتر"
    "inventory"->"موجودی"; "stockAdjustment"->"گردش موجودی"; "portfolio"->"پرتفوی"; else->"تنظیمات کسب‌وکار"
}

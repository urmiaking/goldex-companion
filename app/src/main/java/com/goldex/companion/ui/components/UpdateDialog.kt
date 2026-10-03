package com.goldex.companion.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.BuildConfig
import com.goldex.companion.R
import com.goldex.companion.data.UpdateInfo
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalcReceiptLong
import com.goldex.companion.ui.calculator.CalcStars
import com.goldex.companion.ui.hub.HubCloudDownload
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current
    val appName = stringResource(R.string.app_name)
    val coroutineScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    val buttonHeight = 48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)

    val handleDismiss: () -> Unit = {
        if (isVisible && !isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                isVisible = false
                delay(LuxuryMotion.DURATION_DIALOG_EXIT.toLong())
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) { isVisible = true }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AnimatedVisibility(
                visible = isVisible,
                enter = LuxuryMotion.DialogEnter,
                exit = LuxuryMotion.DialogExit
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.goldBorder),
                    shadowElevation = 16.dp,
                    modifier = modifier
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                ) {
                    Column {
                        // Top Decorative Gold Accent Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(colors.goldSecondary, colors.goldPrimary, colors.goldBullion)
                                    )
                                )
                        )

                        // Scrollable content area
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Official App Emblem with Live Pulse Badge
                            Box(
                                modifier = Modifier.size(62.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(colors.goldBullion, colors.goldPrimary, colors.goldSecondary)
                                            )
                                        )
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_logo_raw),
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                // Pulsing badge dot on top-right of emblem
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                        .padding(1.5.dp)
                                        .clip(CircleShape)
                                        .background(colors.goldPrimary)
                                )
                            }

                            // Version & Identity Badges Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.goldContainer,
                                    border = BorderStroke(0.6.dp, colors.goldBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(colors.goldPrimary)
                                        )
                                        Text(
                                            text = "نسخه جدید ${PersianNumberFormatter.toPersianDigits(updateInfo.latestVersion.removePrefix("v"))}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.6.dp, colors.border)
                                ) {
                                    Text(
                                        text = "نسخه رسمی و پایدار",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Title & Description
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "نسخه جدید $appName آماده است!",
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "ارتقای سرعت محاسبات مظنه زنده، استاندارد فاکتور زرگری و هماهنگی یکپارچه با سامانه مالی.",
                                    fontSize = 11.5.sp,
                                    lineHeight = 18.sp,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Version Migration Card (Current -> Target)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Current Version (Right in RTL)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.surface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = colors.textMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(
                                                text = "نسخه فعلی",
                                                fontSize = 10.sp,
                                                color = colors.textMuted
                                            )
                                            Text(
                                                text = PersianNumberFormatter.toPersianDigits(BuildConfig.VERSION_NAME),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                        }
                                    }

                                    // Arrow pointing Left (from current to target in RTL)
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    // Target Version (Left in RTL)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.goldContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CalcStars,
                                                contentDescription = null,
                                                tint = colors.goldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(
                                                text = "نسخه جدید",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.goldPrimary
                                            )
                                            Text(
                                                text = PersianNumberFormatter.toPersianDigits(updateInfo.latestVersion.removePrefix("v")),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Changelog Section
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = CalcReceiptLong,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "گزارش انتشار نسخه ${PersianNumberFormatter.toPersianDigits(updateInfo.latestVersion.removePrefix("v"))}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.profitGreen.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, colors.profitGreen.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.profitGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "آخرین نسخه",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.profitGreen
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceElevated.copy(alpha = 0.7f),
                                    border = BorderStroke(0.6.dp, colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        if (updateInfo.releaseNotes.isNotBlank()) {
                                            Text(
                                                text = updateInfo.releaseNotes,
                                                fontSize = 12.sp,
                                                lineHeight = 21.sp,
                                                color = colors.textSecondary
                                            )
                                        } else {
                                            Text(
                                                text = "جزئیات این نسخه در صفحه انتشار در دسترس است.",
                                                fontSize = 12.5.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions Row (RTL: cancel on right, download on left)
                        HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Secondary action (Cancel / Later) on the right
                            GoldButton(
                                text = "بعداً",
                                onClick = handleDismiss,
                                isSecondary = true,
                                height = buttonHeight,
                                modifier = Modifier.weight(1f)
                            )

                            // Primary action (Download & Install) on the left
                            GoldButton(
                                text = "دانلود و نصب",
                                icon = HubCloudDownload,
                                height = buttonHeight,
                                modifier = Modifier.weight(1.5f),
                                onClick = {
                                    val targetUrl = updateInfo.downloadUrl.ifBlank { updateInfo.releasePageUrl }
                                    if (targetUrl.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            val fallbackUrl = updateInfo.releasePageUrl.ifBlank { targetUrl }
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                })
                                            } catch (_: Exception) {
                                                // No browser is available on this device.
                                            }
                                        }
                                    }
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

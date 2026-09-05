package com.goldex.companion.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.BuildConfig
import com.goldex.companion.data.UpdateInfo
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
    val coroutineScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    val handleDismiss: () -> Unit = {
        if (isVisible) {
            coroutineScope.launch {
                isVisible = false
                delay(LuxuryMotion.DURATION_DIALOG_EXIT.toLong())
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

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
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.goldPrimary.copy(alpha = 0.35f)),
                    shadowElevation = 12.dp,
                    modifier = modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 24.dp)
                ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Icon Badge
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(colors.goldContainer, colors.surfaceElevated)
                                ),
                                shape = CircleShape
                            )
                            .border(BorderStroke(1.dp, colors.goldBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بروزرسانی نرم‌افزار",
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Title and Version Information
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "نسخه جدید گلدکس پرو",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "نسخه ${updateInfo.latestVersion} منتشر شد (نسخه فعلی: v${BuildConfig.VERSION_NAME})",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Release notes box (if available)
                    if (updateInfo.releaseNotes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "تغییرات و قابلیت‌های جدید:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.goldPrimary
                                )
                                Text(
                                    text = updateInfo.releaseNotes,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Direct Download Button (Luxury Gold Gradient)
                        GoldButton(
                            text = "دانلود و نصب مستقیم نسخه جدید",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                val targetUrl = updateInfo.downloadUrl.ifBlank { updateInfo.releasePageUrl }
                                if (targetUrl.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val fallbackUrl = updateInfo.releasePageUrl.ifBlank { targetUrl }
                                        try {
                                            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(fallbackIntent)
                                        } catch (_: Exception) {
                                            // Handle case where no browser app is installed
                                        }
                                    }
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Secondary Dismiss Button
                        GoldButton(
                            text = "بعداً یادآوری کن",
                            onClick = handleDismiss,
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
}

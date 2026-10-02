package com.goldex.companion.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.BuildConfig
import com.goldex.companion.R
import com.goldex.companion.data.UpdateInfo
import com.goldex.companion.model.PersianNumberFormatter
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
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.goldBorder),
                    shadowElevation = 12.dp,
                    modifier = modifier
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                ) {
                    Column {
                        // A single flexible reading area; actions stay visible even with long notes.
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Brush.linearGradient(listOf(colors.goldContainer, colors.surfaceElevated)),
                                            CircleShape
                                        )
                                        .border(1.dp, colors.goldBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_logo_raw),
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "به‌روزرسانی $appName",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = "نسخه جدید آماده دریافت است",
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.goldContainer,
                                border = BorderStroke(0.6.dp, colors.goldBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "نسخه جدید: ${PersianNumberFormatter.toPersianDigits(updateInfo.latestVersion.removePrefix("v"))}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = "نسخه نصب‌شده: ${PersianNumberFormatter.toPersianDigits(BuildConfig.VERSION_NAME)}",
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            if (updateInfo.releaseNotes.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "تغییرات این نسخه",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = updateInfo.releaseNotes,
                                        fontSize = 14.sp,
                                        lineHeight = 24.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            } else {
                                Text(
                                    text = "جزئیات این نسخه در صفحه انتشار در دسترس است.",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        HorizontalDivider(color = colors.border)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // RTL invariant: secondary on the right, primary on the left.
                            GoldButton(
                                text = "بعداً",
                                onClick = handleDismiss,
                                isSecondary = true,
                                height = buttonHeight,
                                modifier = Modifier.weight(1f)
                            )
                            GoldButton(
                                text = "دانلود و نصب",
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

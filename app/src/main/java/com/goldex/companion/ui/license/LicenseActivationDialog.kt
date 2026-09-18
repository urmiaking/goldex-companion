package com.goldex.companion.ui.license

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.license.DeviceIdentityManager
import com.goldex.companion.data.license.LicenseInfo
import com.goldex.companion.data.license.LicenseStatus
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun LicenseActivationDialog(
    licenseInfo: LicenseInfo,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onDismiss: () -> Unit,
    onActivateCode: (String) -> Unit,
    onActivateTrial: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current
    var codeInput by remember { mutableStateOf(licenseInfo.licenseCode ?: "") }
    val identity = remember { DeviceIdentityManager.getDeviceIdentity(context) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    colors.goldPrimary.copy(alpha = 0.6f),
                                    colors.goldPrimary.copy(alpha = 0.15f)
                                )
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                color = colors.surface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row with Close Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.goldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "مدیریت اشتراک و فعال‌سازی",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.textMain
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = colors.textMuted
                            )
                        }
                    }

                    HorizontalDivider(
                        color = colors.goldBorder.copy(alpha = 0.2f),
                        thickness = 0.8.dp
                    )

                    // Current License Status Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = when (licenseInfo.status) {
                            LicenseStatus.LIFETIME -> colors.profitGreen.copy(alpha = 0.12f)
                            LicenseStatus.TRIAL_ACTIVE -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                            LicenseStatus.TRIAL_EXPIRED, LicenseStatus.REVOKED -> colors.errorRed.copy(alpha = 0.12f)
                            LicenseStatus.NONE -> colors.surfaceVariant
                        },
                        border = BorderStroke(
                            0.8.dp,
                            when (licenseInfo.status) {
                                LicenseStatus.LIFETIME -> colors.profitGreen.copy(alpha = 0.4f)
                                LicenseStatus.TRIAL_ACTIVE -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                                LicenseStatus.TRIAL_EXPIRED, LicenseStatus.REVOKED -> colors.errorRed.copy(alpha = 0.4f)
                                LicenseStatus.NONE -> colors.goldBorder.copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val (badgeText, badgeColor) = when (licenseInfo.status) {
                                LicenseStatus.LIFETIME -> "اشتراک دائمی فعال است" to colors.profitGreen
                                LicenseStatus.TRIAL_ACTIVE -> "مهلت تست فعال (${PersianNumberFormatter.toPersianDigits(licenseInfo.remainingDays)} روز باقی‌مانده)" to Color(0xFFF59E0B)
                                LicenseStatus.TRIAL_EXPIRED -> "مهلت تست ۱۴ روزه به پایان رسیده است" to colors.errorRed
                                LicenseStatus.REVOKED -> "این حساب توسط مدیریت مسدود شده است" to colors.errorRed
                                LicenseStatus.NONE -> "نسخه رایگان (مهلت تست فعال نشده)" to colors.textMuted
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Text(
                                    text = badgeText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = badgeColor
                                )
                            }

                            Text(
                                text = "شناسه دستگاه: ${identity.fingerprint.take(8)}...${identity.fingerprint.takeLast(4)}",
                                fontSize = 10.5.sp,
                                color = colors.textMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Success / Error Banners
                    AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = colors.errorRed.copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, colors.errorRed.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 11.5.sp,
                                color = colors.errorRed,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    AnimatedVisibility(visible = !successMessage.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = colors.profitGreen.copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, colors.profitGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = successMessage ?: "",
                                fontSize = 11.5.sp,
                                color = colors.profitGreen,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // License Code Input Section (if not lifetime)
                    if (!licenseInfo.isLifetime) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "کد فعال‌سازی اختصاصی (لایسنس دائم):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textMain
                            )

                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it.uppercase() },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "مثال: QIR-91C7-2M4P",
                                        fontSize = 12.sp,
                                        color = colors.textMuted
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (codeInput.isNotBlank()) onActivateCode(codeInput)
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.goldPrimary,
                                    unfocusedBorderColor = colors.goldBorder.copy(alpha = 0.4f),
                                    focusedContainerColor = colors.surfaceVariant,
                                    unfocusedContainerColor = colors.surfaceVariant
                                )
                            )
                        }

                        // Button for 14-day free trial if not used yet
                        if (licenseInfo.status == LicenseStatus.NONE) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLoading) { onActivateTrial() },
                                shape = RoundedCornerShape(12.dp),
                                color = colors.goldPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, colors.goldPrimary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🎁 فعال‌سازی ۱۴ روز مهلت تست رایگان",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Website / Support Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qirato.ir/#pricing"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "خرید یا تمدید اشتراک در تارنمای رسمی qirato.ir",
                            fontSize = 11.sp,
                            color = colors.goldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Dialog Button Invariant:
                    // RTL Layout: First child in Row = Right = Secondary (Cancel/Close)
                    // Second child in Row = Left = Primary (Activate/Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. دکمه فرعی / انصراف (در چینش RTL در سمت راست نمایش داده می‌شود)
                        GoldButton(
                            text = "بستن",
                            onClick = onDismiss,
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )

                        // 2. دکمه اصلی / فعال‌سازی (در چینش RTL در سمت چپ نمایش داده می‌شود)
                        if (!licenseInfo.isLifetime) {
                            GoldButton(
                                text = if (isLoading) "در حال بررسی..." else "ثبت کد لایسنس",
                                onClick = {
                                    if (codeInput.isNotBlank()) onActivateCode(codeInput)
                                },
                                isSecondary = false,
                                icon = Icons.Default.Check,
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

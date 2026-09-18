package com.goldex.companion.ui.license

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LicenseActivationModal: Bottom-sheet style modal for subscription management and code activation.
 * Adheres strictly to Persian Sovereign Aurum tokens and RTL dialog button layout invariants.
 */
@Composable
fun LicenseActivationModal(
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
    val coroutineScope = rememberCoroutineScope()
    var codeInput by remember { mutableStateOf(licenseInfo.licenseCode ?: "") }
    val identity = remember { DeviceIdentityManager.getDeviceIdentity(context) }

    var isVisible by remember { mutableStateOf(false) }

    val handleDismiss: () -> Unit = {
        if (isVisible) {
            coroutineScope.launch {
                isVisible = false
                delay(LuxuryMotion.DURATION_MODAL_EXIT.toLong())
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.65f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) LuxuryMotion.DURATION_MODAL_ENTER else LuxuryMotion.DURATION_MODAL_EXIT,
            easing = FastOutSlowInEasing
        ),
        label = "scrimAlpha"
    )

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = handleDismiss
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = LuxuryMotion.ModalEnter,
                    exit = LuxuryMotion.ModalExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Consume clicks
                            ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.goldPrimary.copy(alpha = 0.65f),
                                    colors.border.copy(alpha = 0.25f)
                                )
                            )
                        ),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(max = 680.dp)
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Grabber Handle
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(colors.border.copy(alpha = 0.6f))
                            )

                            // Header Row with Close Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
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
                                    Column {
                                        Text(
                                            text = "مدیریت اشتراک و فعال‌سازی",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = "دسترسی به خدمات صدور فاکتور رسمی و دفتر معین",
                                            fontSize = 10.5.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = handleDismiss,
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
                                color = colors.border.copy(alpha = 0.3f),
                                thickness = 0.8.dp
                            )

                            // Current License Status Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
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
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            fontSize = 14.sp,
                                            color = badgeColor
                                        )
                                    }

                                    Text(
                                        text = "شناسه قفل سخت‌افزاری دستگاه: ${identity.fingerprint.take(8)}...${identity.fingerprint.takeLast(4)}",
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Error & Success Banners
                            AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.errorRed.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, colors.errorRed.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        fontSize = 12.sp,
                                        color = colors.errorRed,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            AnimatedVisibility(visible = !successMessage.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.profitGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, colors.profitGreen.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = successMessage ?: "",
                                        fontSize = 12.sp,
                                        color = colors.profitGreen,
                                        modifier = Modifier.padding(12.dp),
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
                                        fontSize = 12.5.sp,
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

                                // 14-Day Free Trial Button (if trial not used yet)
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
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "🎁 فعال‌سازی ۱۴ روز مهلت تست رایگان",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Website / Purchase Link
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
                                    fontSize = 11.5.sp,
                                    color = colors.goldPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Dialog Button Invariant (RTL):
                            // 1. Right (first in Row): Secondary Action / Cancel ("بستن")
                            // 2. Left (second in Row): Primary Action / Save ("ثبت کد لایسنس")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoldButton(
                                    text = "بستن",
                                    onClick = handleDismiss,
                                    isSecondary = true,
                                    modifier = Modifier.weight(1f)
                                )

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
    }
}

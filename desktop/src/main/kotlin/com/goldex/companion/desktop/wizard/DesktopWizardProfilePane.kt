package com.goldex.companion.desktop.wizard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.DesktopBrandAssetTile
import com.goldex.companion.desktop.ui.DesktopField
import com.goldex.companion.desktop.ui.DesktopSignaturePadDialog
import com.goldex.companion.desktop.ui.controls.DesktopDialogHost
import com.goldex.companion.desktop.ui.rememberBrandAssetBitmap
import com.goldex.companion.desktop.ui.saveSignatureStrokesToPng
import com.goldex.companion.desktop.ui.selectBrandLogoFile
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Composable
fun DesktopWizardProfilePane(
    profileState: DesktopWizardProfileState,
    onProfileChange: (DesktopWizardProfileState) -> Unit,
    profileErrors: DesktopWizardProfileErrors,
    showErrors: Boolean = true,
    dataDirectory: Path = Path.of(System.getProperty("user.home"), ".qirato"),
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    var showSignaturePad by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("wizard-profile-pane")) {
        val isWide = maxWidth >= 960.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProfileFormCard(
                    profileState = profileState,
                    onProfileChange = onProfileChange,
                    profileErrors = profileErrors,
                    showErrors = showErrors,
                    modifier = Modifier.weight(1.2f).fillMaxHeight()
                )

                ProfileBrandCard(
                    profileState = profileState,
                    onProfileChange = onProfileChange,
                    onOpenSignaturePad = { showSignaturePad = true },
                    dataDirectory = dataDirectory,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileFormCard(
                    profileState = profileState,
                    onProfileChange = onProfileChange,
                    profileErrors = profileErrors,
                    showErrors = showErrors,
                    modifier = Modifier.fillMaxWidth()
                )

                ProfileBrandCard(
                    profileState = profileState,
                    onProfileChange = onProfileChange,
                    onOpenSignaturePad = { showSignaturePad = true },
                    dataDirectory = dataDirectory,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Signature Pad Modal
    DesktopDialogHost(showSignaturePad) {
        DesktopSignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSaveSignature = { strokes, w, h ->
                val brandDir = dataDirectory.resolve("brand_assets")
                Files.createDirectories(brandDir)
                val dest = brandDir.resolve("wizard_signature.png").toFile()
                saveSignatureStrokesToPng(strokes, w, h, dest)
                onProfileChange(profileState.copy(signatureUri = dest.absolutePath))
                showSignaturePad = false
            }
        )
    }
}

@Composable
private fun ProfileFormCard(
    profileState: DesktopWizardProfileState,
    onProfileChange: (DesktopWizardProfileState) -> Unit,
    profileErrors: DesktopWizardProfileErrors,
    showErrors: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier.testTag("profile-form-card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.goldPrimary)
                )
                Text(
                    text = "مشخصات واحد صنفی و مدیریت",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }

            Text(
                text = "این اطلاعات در بالای اسناد حسابداری، اتیکت‌ها و سربرگ فاکتورهای رسمی درج خواهند شد.",
                fontSize = 12.5.sp,
                color = colors.textSecondary
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            // Gallery Name
            DesktopField(
                value = profileState.galleryName,
                onChange = { onProfileChange(profileState.copy(galleryName = it)) },
                label = "نام تجاری طلافروشی / گالری / بنکداری *",
                adornment = Icons.Outlined.Store,
                error = if (showErrors) profileErrors.galleryNameError else null,
                modifier = Modifier.fillMaxWidth().testTag("profile-gallery-name")
            )

            // Manager Name
            DesktopField(
                value = profileState.managerName,
                onChange = { onProfileChange(profileState.copy(managerName = it)) },
                label = "نام و نام خانوادگی مدیر مسئول *",
                adornment = Icons.Outlined.Person,
                error = if (showErrors) profileErrors.managerNameError else null,
                modifier = Modifier.fillMaxWidth().testTag("profile-manager-name")
            )

            // Union Code & Phone in row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DesktopField(
                    value = profileState.unionCode,
                    onChange = { onProfileChange(profileState.copy(unionCode = it)) },
                    label = "شماره پروانه کسب اتحادیه طلا *",
                    adornment = Icons.Outlined.Badge,
                    numeric = true,
                    error = if (showErrors) profileErrors.unionCodeError else null,
                    modifier = Modifier.weight(1f).testTag("profile-union-code")
                )

                DesktopField(
                    value = profileState.phone,
                    onChange = { onProfileChange(profileState.copy(phone = it)) },
                    label = "شماره تماس واحد صنفی *",
                    adornment = Icons.Outlined.Phone,
                    numeric = true,
                    error = if (showErrors) profileErrors.phoneError else null,
                    modifier = Modifier.weight(1f).testTag("profile-phone")
                )
            }

            // Address
            DesktopField(
                value = profileState.address,
                onChange = { onProfileChange(profileState.copy(address = it)) },
                label = "نشانی کامل واحد صنفی / بازار طلا *",
                adornment = Icons.Outlined.LocationOn,
                singleLine = false,
                error = if (showErrors) profileErrors.addressError else null,
                modifier = Modifier.fillMaxWidth().testTag("profile-address")
            )
        }
    }
}

@Composable
private fun ProfileBrandCard(
    profileState: DesktopWizardProfileState,
    onProfileChange: (DesktopWizardProfileState) -> Unit,
    onOpenSignaturePad: () -> Unit,
    dataDirectory: Path,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier.testTag("profile-brand-card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.goldPrimary)
                )
                Text(
                    text = "هویت بصری و سربرگ اسناد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }

            Text(
                text = "بارگذاری لوگو و امضای دیجیتال زرگر برای فاکتورهای رسمی و رسیدهای تسویه (اختیاری).",
                fontSize = 12.5.sp,
                color = colors.textSecondary
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            // Logo & Signature Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DesktopBrandAssetTile(
                    title = "لوگوی واحد صنفی",
                    actionLabel = if (profileState.logoUri.isNotBlank()) "تغییر لوگو" else "انتخاب لوگو",
                    bitmap = rememberBrandAssetBitmap(profileState.logoUri),
                    fallback = profileState.galleryName.trim().take(2).ifBlank { "لوگو" },
                    onPick = {
                        val picked = selectBrandLogoFile()
                        if (picked != null) {
                            val brandDir = dataDirectory.resolve("brand_assets")
                            Files.createDirectories(brandDir)
                            val dest = brandDir.resolve("wizard_logo.png")
                            Files.copy(picked.toPath(), dest, StandardCopyOption.REPLACE_EXISTING)
                            onProfileChange(profileState.copy(logoUri = dest.toFile().absolutePath))
                        }
                    },
                    onClear = if (profileState.logoUri.isNotBlank()) ({ onProfileChange(profileState.copy(logoUri = "")) }) else null,
                    modifier = Modifier.weight(1f).testTag("wizard-logo-tile")
                )

                DesktopBrandAssetTile(
                    title = "امضای دیجیتال زرگر",
                    actionLabel = if (profileState.signatureUri.isNotBlank()) "تغییر امضا" else "ثبت امضا",
                    bitmap = rememberBrandAssetBitmap(profileState.signatureUri),
                    fallback = "امضا",
                    onPick = onOpenSignaturePad,
                    onClear = if (profileState.signatureUri.isNotBlank()) ({ onProfileChange(profileState.copy(signatureUri = "")) }) else null,
                    modifier = Modifier.weight(1f).testTag("wizard-signature-tile")
                )
            }

            Spacer(Modifier.height(8.dp))

            // Live Certificate Preview Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.goldContainer.copy(alpha = 0.35f),
                border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("profile-cert-preview")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پیش‌نمایش سربرگ فاکتور رسمی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.3f))

                    Text(
                        text = profileState.galleryName.ifBlank { "نام گالری طلا و جواهر" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = "مدیریت: ${profileState.managerName.ifBlank { "استاد زرگر" }} • پروانه: ${PersianNumberFormatter.toPersianDigits(profileState.unionCode.ifBlank { "۱۲۳۴۵" })}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "تلفن: ${PersianNumberFormatter.toPersianDigits(profileState.phone.ifBlank { "۰۲۱-۸۸۸۸۸۸۸" })}",
                        fontSize = 11.5.sp,
                        color = colors.textMuted
                    )
                    if (profileState.address.isNotBlank()) {
                        Text(
                            text = "نشانی: ${profileState.address}",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

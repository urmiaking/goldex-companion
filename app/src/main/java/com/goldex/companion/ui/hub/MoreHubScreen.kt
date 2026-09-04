package com.goldex.companion.ui.hub

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * MoreHubScreen: The Central Command Hub & Operations Center.
 *
 * Faithfully designed after Google Stitch Screen ID: a4fb5e02179f4ce0b4ac213ee29bca16
 *
 * Replaces the obsolete Navigation Drawer with an ergonomic, luxury Persian Sovereign Aurum hub:
 * 1. Jeweler Profile & Guild Hero Card with Live Union Accreditation Badge
 * 2. Quick Metrics Ribbon (Active Counterparties, Bullion Inventory, License Validity)
 * 3. 2x2 Specialized Operations Grid (Ledger, Melt Lab, Workshop Orders, Vault Inventory)
 * 4. Supplementary Calculation Tools Group (Karat Conversion, Coin Bubble, Cloud Backup)
 * 5. Union Settings & Invoice Configuration Group (VAT/Profit margin, Official Letterhead, Biometrics)
 * 6. Official Guild Support & Industry Formula Reference Group
 * 7. Versioning & Account Security Actions
 */
@Composable
fun MoreHubScreen(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState,
    onNavigateLedger: () -> Unit,
    onNavigateMelt: () -> Unit,
    onNavigateWorkshop: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateConvert: () -> Unit,
    onNavigateCoinBubble: () -> Unit,
    onNavigateInvoices: () -> Unit,
    onNavigateSettings: () -> Unit,
    onOpenJewelerProfile: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    val settings = uiState.appSettings

    // Calculate 2-letter monogram from gallery name
    val monogram = remember(settings.galleryName) {
        val parts = settings.galleryName.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
        } else {
            settings.galleryName.take(2).ifBlank { "جا" }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. Jeweler Profile & Guild Hero Card
        // ==========================================
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF131722),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.45f),
                        Color(0xFFD97706).copy(alpha = 0.15f)
                    )
                )
            ),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Ambient gold decorative atmospheric glows
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(x = 30.dp, y = (-20.dp))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFF59E0B).copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopEnd)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Profile Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Monogram avatar
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFBBF24),
                                                Color(0xFFD97706)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color(0xFFFDE68A).copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monogram,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = settings.galleryName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = HubCheckCircle,
                                        contentDescription = "معتبر",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "کد صنفی: ${settings.unionCode}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFDE68A).copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF34D399))
                                        )
                                        Text(
                                            text = "پروانه معتبر",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }

                        // Edit Profile Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onOpenJewelerProfile() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ویرایش",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFDE68A)
                                )
                                Icon(
                                    imageVector = HubChevronLeft,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Metrics Ribbon (Compact & Balanced 3-column)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.8.dp,
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "حساب‌های فعال",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${uiState.customerList.size.coerceAtLeast(84)} مشتری",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "موجودی بنکداری",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "۱,۸۴۲.۶ گرم",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "اعتبار اشتراک",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "۱۱ ماه باقی",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. Specialized Operations Grid (4 سامانه‌ها)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدیریت تخصصی طلا و کارگاه",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                Text(
                    text = "۴ سامانه فعال",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: دفتر حساب و معین
                ManagementGridCard(
                    title = "دفتر حساب و معین",
                    subtitle = "بدهکاران، بستانکاران و مانده‌ها",
                    badgeText = "۴ مانده‌دار",
                    badgeColor = colors.errorRed,
                    icon = HubMenuBook,
                    iconBg = Color(0xFFFFF1F2),
                    iconTint = Color(0xFFE11D48),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateLedger
                )

                // Card 2: آبشده و ری‌گیری
                ManagementGridCard(
                    title = "آبشده و ری‌گیری",
                    subtitle = "ثبت شماره انگ، آزمایشگاه و عیار",
                    badgeText = "۳ پاکت",
                    badgeColor = colors.goldPrimary,
                    icon = HubInbox,
                    iconBg = Color(0xFFFFFBEB),
                    iconTint = Color(0xFFB45309),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateMelt
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: سفارشات و کارگاه
                ManagementGridCard(
                    title = "سفارشات و کارگاه",
                    subtitle = "پیگیری ریخته‌گری، مخراجی و تحویل",
                    badgeText = "در حال ساخت",
                    badgeColor = Color(0xFF2563EB),
                    icon = HubHandyman,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF1D4ED8),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateWorkshop
                )

                // Card 4: انبار و ویترین طلا
                ManagementGridCard(
                    title = "انبار و ویترین طلا",
                    subtitle = "موجودی کارهای ساخته، سکه و سنگ",
                    badgeText = "بارکدخوان",
                    badgeColor = colors.profitGreen,
                    icon = HubShowcase,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateInventory
                )
            }
        }

        // ==========================================
        // 3. Supplementary Calculation Tools Group
        // ==========================================
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Group Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ابزارهای محاسباتی تکمیلی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.textMain
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(0.6.dp, colors.goldBorder)
                    ) {
                        Text(
                            text = "پیشرفته",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Divider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                // Item 1: Karat conversion
                HubListRowItem(
                    title = "تبدیل عیار و محاسبه شرطی (۷۵۰ به ۷۰۵ و ۹۹۹)",
                    subtitle = "فرمول صنف همراه با کسر خطای ری‌گیری",
                    icon = HubKaratSync,
                    iconTint = Color(0xFFD97706),
                    iconBg = Color(0xFFFEF3C7),
                    onClick = onNavigateConvert
                )

                Divider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Item 2: Coin bubble
                HubListRowItem(
                    title = "تحلیلگر حباب انواع سکه و انس جهانی",
                    subtitle = "محاسبه ارزش ذاتی بر پایه دلار نیما و آزاد",
                    icon = HubTrending,
                    iconTint = Color(0xFF059669),
                    iconBg = Color(0xFFD1FAE5),
                    onClick = onNavigateCoinBubble
                )

                Divider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Item 3: Cloud & Excel backup
                HubListRowItem(
                    title = "پشتیبان‌گیری ابری و اکسل فاکتورها",
                    subtitle = "همگام‌سازی ابری و گزارش جامع مالیاتی",
                    icon = HubCloudSync,
                    iconTint = Color(0xFF4F46E5),
                    iconBg = Color(0xFFE0E7FF),
                    onClick = onNavigateInvoices
                )
            }
        }

        // ==========================================
        // 4. Settings & Invoice Customization Group
        // ==========================================
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تنظیمات و سفارشی‌سازی فاکتور",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.textMain
                    )
                    Text(
                        text = "قوانین اتحادیه زرگران",
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )
                }

                Divider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                // Setting 1: Profit and VAT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateSettings() }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HubPercent,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سود مصوب و مالیات ارزش افزوده",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                            Text(
                                text = "سود مغازه ۷٪ • مالیات اجرت ۹٪ (قانون جدید)",
                                fontSize = 10.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldContainer.copy(alpha = 0.4f),
                        border = BorderStroke(0.6.dp, colors.goldBorder)
                    ) {
                        Text(
                            text = "پیش‌فرض صنف",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Divider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Setting 2: Letterhead & QR Code
                HubListRowItem(
                    title = "طراحی سربرگ، مهر و QR سامانه جامع",
                    subtitle = "تنظیم لوگو، آدرس، تلفن و بارکد اصالت کالا",
                    icon = HubQrCode,
                    iconTint = colors.textSecondary,
                    iconBg = colors.surfaceElevated,
                    onClick = onOpenJewelerProfile
                )

                Divider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Setting 3: Biometric Lock Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HubFingerprint,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "قفل امنیتی با Face ID / اثر انگشت",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                            Text(
                                text = "محافظت از مبالغ فروش و دفاتر مالی هنگام خروج",
                                fontSize = 10.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = settings.isBiometricLockEnabled,
                        onCheckedChange = { viewModel.toggleBiometricLock(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.goldPrimary,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surfaceElevated
                        )
                    )
                }
            }
        }

        // ==========================================
        // 5. Official Guild Support & Guide Group
        // ==========================================
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Support Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:02155623481"))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "تماس با پشتیبانی: ۰۲۱-۵۵۶۲۳۴۸۱", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD1FAE5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HubPhoneInTalk,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پشتیبانی تلفنی و تلگرام همکاران قیراط",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                            Text(
                                text = "شنبه تا چهارشنبه ۹ تا ۱۹ • پنج‌شنبه‌ها تا ۱۴",
                                fontSize = 10.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD1FAE5).copy(alpha = 0.8f),
                        border = BorderStroke(0.6.dp, Color(0xFF059669).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "پاسخگوی آنلاین",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Divider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Gold Guild Standards Guide
                HubListRowItem(
                    title = "راهنمای فرمول‌های استاندارد اتحادیه طلا",
                    subtitle = "توضیح وزن نگین، کسر ناخالصی و اجرت",
                    icon = HubMenuBook,
                    iconTint = Color(0xFFD97706),
                    iconBg = Color(0xFFFEF3C7),
                    onClick = {
                        Toast.makeText(context, "استاندارد عیار ۷۵۰ و فرمول معافیت اصل طلا مصوب صنف", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // ==========================================
        // 6. Versioning, Updates & Logout Actions
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Check Update Action Button
            OutlinedButton(
                onClick = { viewModel.checkForUpdates(manual = true) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.goldBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(
                    imageVector = HubCloudDownload,
                    contentDescription = null,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بررسی بروزرسانی نرم‌افزار",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // App Version Badge
            Text(
                text = "قیراط - نگارش ۲.۴.۰ پرو (ویژه بنکداران و فروشگاه‌های طلا و جواهر)",
                fontSize = 10.5.sp,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun ManagementGridCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border.copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg)
                        .border(0.6.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = colors.textMain,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = colors.textMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HubListRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = colors.textMuted
                )
            }
        }

        Icon(
            imageVector = HubChevronLeft,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

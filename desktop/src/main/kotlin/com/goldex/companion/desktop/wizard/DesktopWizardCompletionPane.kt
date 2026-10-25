package com.goldex.companion.desktop.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.DesktopField
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopChoiceRow
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun DesktopWizardCompletionPane(
    profileState: DesktopWizardProfileState,
    financialState: DesktopWizardFinancialState,
    inventoryState: DesktopWizardInventoryState,
    licenseState: DesktopWizardLicenseState,
    onLicenseChange: (DesktopWizardLicenseState) -> Unit,
    currentSpotPrice: Long,
    onNavigateToStep: (DesktopWizardStep) -> Unit,
    isOnlineSync: Boolean = false,
    onOnlineSyncChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()

    // Calculate metrics for summary
    val showcaseWeight = PersianNumberFormatter.parseToCleanDouble(inventoryState.vitrinWeight) ?: 0.0
    val meltWeight = PersianNumberFormatter.parseToCleanDouble(inventoryState.meltWeight) ?: 0.0
    val coinsWeight = (inventoryState.coinTamam * 8.133) +
            (inventoryState.coinQadim * 8.133) +
            (inventoryState.coinNim * 4.066) +
            (inventoryState.coinRob * 2.033) +
            (inventoryState.coinGerami * 1.01)

    val totalGoldWeight = showcaseWeight + meltWeight + coinsWeight
    val cashTankhah = PersianNumberFormatter.parseToCleanLong(inventoryState.cashTankhah) ?: 0L
    val bankBalances = PersianNumberFormatter.parseToCleanLong(inventoryState.bankBalances) ?: 0L
    val totalCash = cashTankhah + bankBalances
    val estimatedValuation = if (currentSpotPrice > 0L) {
        (totalGoldWeight * currentSpotPrice).toLong() + totalCash
    } else {
        totalCash
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
            .testTag("wizard-completion-pane"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Trophy / Celebration Header Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(1.dp, colors.goldBorder)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Subtle gradient overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colors.goldPrimary.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    colors.goldSecondary.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.goldContainer)
                            .border(1.5.dp, colors.goldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "آماده افتتاح رسمی دوره و استقرار سیستم",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.profitGreen.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, colors.profitGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "تأیید نهایی اسناد",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.profitGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "کلیه مشخصات واحد صنفی، موازین مالیاتی قانون ۲۶ ارزش افزوده و موجودی‌های نقدی و وزنی با موفقیت تجمیع شدند. پس از فشردن دکمه پایان، سند افتتاحیه صادر شده و کارتابل حسابداری شروع به کار خواهد کرد.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // 2. Summary Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Summary Card
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "مشخصات واحد صنفی",
                icon = Icons.Outlined.Store,
                onEdit = { onNavigateToStep(DesktopWizardStep.PROFILE) }
            ) {
                SummaryRow(label = "نام گالری:", value = profileState.galleryName.ifBlank { "ثبت نشده" })
                SummaryRow(label = "مدیر مسئول:", value = profileState.managerName.ifBlank { "ثبت نشده" })
                SummaryRow(label = "شماره پروانه:", value = PersianNumberFormatter.toPersianDigits(profileState.unionCode.ifBlank { "-" }))
                SummaryRow(label = "شماره تماس:", value = PersianNumberFormatter.toPersianDigits(profileState.phone.ifBlank { "-" }))
                SummaryRow(
                    label = "لوگو و مهر:",
                    value = when {
                        profileState.logoUri.isNotBlank() && profileState.signatureUri.isNotBlank() -> "لوگو و امضا بارگذاری شد"
                        profileState.logoUri.isNotBlank() -> "فقط لوگو بارگذاری شد"
                        profileState.signatureUri.isNotBlank() -> "فقط امضا ثبت شد"
                        else -> "فاقد تصویر و امضا"
                    }
                )
            }

            // Financial Defaults Card
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "تنظیمات و موازین مالی",
                icon = Icons.Outlined.AccountBalance,
                onEdit = { onNavigateToStep(DesktopWizardStep.FINANCIAL) }
            ) {
                SummaryRow(label = "درصد سود فروش:", value = "${PersianNumberFormatter.toPersianDigits(financialState.profitPercent)} درصد")
                SummaryRow(
                    label = "مالیات بر ارزش افزوده:",
                    value = if (financialState.isVatEnabled) {
                        "${PersianNumberFormatter.toPersianDigits(financialState.vatRate)} درصد (معافیت اصل طلا)"
                    } else {
                        "غیرفعال"
                    }
                )
                SummaryRow(label = "عیار مبنای معاملات:", value = "${financialState.baseKarat.name} (۷۵۰)")
                SummaryRow(label = "فرمول محاسبه مالیات:", value = "فقط روی اجرت و سود")
                SummaryRow(label = "دقت وزن سیستم:", value = "۰.۰۰۱ گرم (سه رقم اعشار)")
            }

            // Inventory Summary Card
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "موجودی و سرمایه اول دوره",
                icon = Icons.Outlined.Inventory2,
                onEdit = { onNavigateToStep(DesktopWizardStep.INVENTORY) }
            ) {
                SummaryRow(
                    label = "وزن کل طلا و سکه:",
                    value = "${PersianNumberFormatter.formatWeight(totalGoldWeight)} گرم"
                )
                SummaryRow(
                    label = "موجودی طلای ویترین:",
                    value = "${PersianNumberFormatter.formatWeight(showcaseWeight)} گرم"
                )
                SummaryRow(
                    label = "موجودی آبشده:",
                    value = "${PersianNumberFormatter.formatWeight(meltWeight)} گرم"
                )
                SummaryRow(
                    label = "موجودی نقدینگی و بانک:",
                    value = "${PersianNumberFormatter.formatPrice(totalCash)} تومان"
                )
                SummaryRow(
                    label = "تخمین کل سرمایه اولیه:",
                    value = "${PersianNumberFormatter.formatPrice(estimatedValuation)} تومان",
                    highlight = true
                )
            }
        }

        // 2.5 Dual Storage & Cloud Sync Mode Selection Cards (Online vs Offline)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isOnlineSync) Icons.Outlined.CloudSync else Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "نحوه ذخیره‌سازی داده‌ها و همگام‌سازی",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "انتخاب وضعیت اتصال ابری برای ثبت اسناد، فاکتورها و دفاتر مالی",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Offline Mode Card (Right side in RTL, first child)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOnlineSyncChange(false) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (!isOnlineSync) colors.goldContainer.copy(alpha = 0.35f) else colors.surfaceElevated,
                        border = BorderStroke(
                            if (!isOnlineSync) 1.5.dp else 0.8.dp,
                            if (!isOnlineSync) colors.goldPrimary else colors.border
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
                                    .background(if (!isOnlineSync) colors.goldPrimary.copy(alpha = 0.15f) else colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = if (!isOnlineSync) colors.goldPrimary else colors.textMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ذخیره در دستگاه",
                                fontSize = 12.sp,
                                fontWeight = if (!isOnlineSync) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isOnlineSync) colors.textMain else colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "(آفلاین)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (!isOnlineSync) colors.goldPrimary else colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 2. Online Mode Card (Left side in RTL, second child)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOnlineSyncChange(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOnlineSync) colors.goldContainer.copy(alpha = 0.35f) else colors.surfaceElevated,
                        border = BorderStroke(
                            if (isOnlineSync) 1.5.dp else 0.8.dp,
                            if (isOnlineSync) colors.goldPrimary else colors.border
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
                                    .background(if (isOnlineSync) colors.goldPrimary.copy(alpha = 0.15f) else colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudSync,
                                    contentDescription = null,
                                    tint = if (isOnlineSync) colors.goldPrimary else colors.textMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "همگام‌سازی ابری",
                                fontSize = 12.sp,
                                fontWeight = if (isOnlineSync) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOnlineSync) colors.textMain else colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "(آنلاین)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isOnlineSync) colors.goldPrimary else colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 3. License Selection Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "نوع فعال‌سازی و لایسنس نرم‌افزار",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Trial
                    val isTrial = licenseState.choice == DesktopWizardLicenseChoice.TRIAL
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable {
                            onLicenseChange(licenseState.copy(choice = DesktopWizardLicenseChoice.TRIAL))
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isTrial) colors.goldContainer.copy(alpha = 0.25f) else colors.surfaceElevated,
                        border = BorderStroke(
                            if (isTrial) 1.5.dp else 0.6.dp,
                            if (isTrial) colors.goldPrimary else colors.border
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isTrial) colors.goldPrimary else colors.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "طرح آزمایشی رایگان (۷ روز)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTrial) colors.textMain else colors.textSecondary
                                    )
                                }
                                if (isTrial) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "دسترسی کامل و نامحدود به تمامی امکانات زرگری، صدور فاکتور و کارتابل حسابداری بدون نیاز به پرداخت.",
                                fontSize = 11.5.sp,
                                color = colors.textMuted,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Option 2: Code
                    val isCode = licenseState.choice == DesktopWizardLicenseChoice.CODE
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable {
                            onLicenseChange(licenseState.copy(choice = DesktopWizardLicenseChoice.CODE))
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCode) colors.goldContainer.copy(alpha = 0.25f) else colors.surfaceElevated,
                        border = BorderStroke(
                            if (isCode) 1.5.dp else 0.6.dp,
                            if (isCode) colors.goldPrimary else colors.border
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        imageVector = Icons.Outlined.Key,
                                        contentDescription = null,
                                        tint = if (isCode) colors.goldPrimary else colors.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "ثبت کد لایسنس دائمی",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCode) colors.textMain else colors.textSecondary
                                    )
                                }
                                if (isCode) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "فعال‌سازی با کد خریداری‌شده از وب‌سایت قیراط و اتصال دائمی به سرور.",
                                fontSize = 11.5.sp,
                                color = colors.textMuted,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (licenseState.choice == DesktopWizardLicenseChoice.CODE) {
                    DesktopField(
                        value = licenseState.licenseCode,
                        onChange = { onLicenseChange(licenseState.copy(licenseCode = it)) },
                        label = "کد فعال‌سازی لایسنس تجاری (۱۶ رقمی)",
                        adornment = Icons.Outlined.Key,
                        modifier = Modifier.fillMaxWidth().testTag("wizard-license-code")
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.goldPrimary.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Verified,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "شناسه پایدار دستگاه شما به صورت امن ثبت شده و دوره آزمایشی ۷ روزه بلافاصله پس از تأیید فعال خواهد شد.",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 4. Irrevocable Audit Disclaimer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(0.5.dp, colors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.goldSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "نکته امنیتی و انضباط مالی: با ثبت نهایی، رکوردهای افتتاحیه در دفتر کل و کارتابل ثبت شده و تراز افتتاح دوره قفل می‌شود. اصلاحات بعدی تنها از طریق اسناد تعدیل موجودی یا صدور سند اصلاحی ممکن خواهد بود.",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                DesktopActionButton(
                    text = "ویرایش",
                    onClick = onEdit,
                    icon = Icons.Outlined.Edit,
                    isSecondary = true,
                    height = 36.dp
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    val colors = LocalGoldExColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textMuted
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) colors.goldPrimary else colors.textMain,
            textAlign = TextAlign.End
        )
    }
}

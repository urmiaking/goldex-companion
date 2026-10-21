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
import com.goldex.companion.desktop.ui.DesktopField
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopFilterChip
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun DesktopWizardFinancialPane(
    financialState: DesktopWizardFinancialState,
    onFinancialChange: (DesktopWizardFinancialState) -> Unit,
    financialErrors: DesktopWizardFinancialErrors,
    showErrors: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("wizard-financial-pane")) {
        val isWide = maxWidth >= 960.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FinancialSettingsCard(
                    financialState = financialState,
                    onFinancialChange = onFinancialChange,
                    financialErrors = financialErrors,
                    showErrors = showErrors,
                    modifier = Modifier.weight(1.1f).fillMaxHeight()
                )

                Article26LawCard(
                    financialState = financialState,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FinancialSettingsCard(
                    financialState = financialState,
                    onFinancialChange = onFinancialChange,
                    financialErrors = financialErrors,
                    showErrors = showErrors,
                    modifier = Modifier.fillMaxWidth()
                )

                Article26LawCard(
                    financialState = financialState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FinancialSettingsCard(
    financialState: DesktopWizardFinancialState,
    onFinancialChange: (DesktopWizardFinancialState) -> Unit,
    financialErrors: DesktopWizardFinancialErrors,
    showErrors: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier.testTag("financial-settings-card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                    text = "تنظیمات سود، مالیات و عیار مبنا",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }

            Text(
                text = "این مقادیر به‌صورت پیش‌فرض در محاسبات طلا و فاکتورها اعمال می‌شوند و در هر زمان قابل ویرایش هستند.",
                fontSize = 12.5.sp,
                color = colors.textSecondary
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            // Profit Margin Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "درصد سود فروشگاه طلا و جواهر (مصوب اتحادیه: ۷٪)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )

                DesktopField(
                    value = financialState.profitPercent,
                    onChange = { onFinancialChange(financialState.copy(profitPercent = it)) },
                    label = "درصد سود پیش‌فرض",
                    unit = "٪",
                    numeric = true,
                    adornment = Icons.Outlined.Percent,
                    error = if (showErrors) financialErrors.profitError else null,
                    modifier = Modifier.fillMaxWidth().testTag("wizard-profit-percent")
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))

            // VAT Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "محاسبه مالیات بر ارزش افزوده (VAT)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                        Text(
                            text = "محاسبه صرفاً روی اجرت و سود (معافیت اصل طلا)",
                            fontSize = 11.5.sp,
                            color = colors.textMuted
                        )
                    }

                    Switch(
                        checked = financialState.isVatEnabled,
                        onCheckedChange = { onFinancialChange(financialState.copy(isVatEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.goldPrimary),
                        modifier = Modifier.testTag("wizard-vat-switch")
                    )
                }

                if (financialState.isVatEnabled) {
                    DesktopField(
                        value = financialState.vatRate,
                        onChange = { onFinancialChange(financialState.copy(vatRate = it)) },
                        label = "نرخ مالیات بر ارزش افزوده (پیش‌فرض قانونی: ۱۰٪)",
                        unit = "٪",
                        numeric = true,
                        adornment = Icons.Outlined.AccountBalance,
                        error = if (showErrors) financialErrors.vatError else null,
                        modifier = Modifier.fillMaxWidth().testTag("wizard-vat-rate")
                    )
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))

            // Base Karat Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "عیار استاندارد مبنای معاملات",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )

                LuxurySegmentedControl(
                    items = listOf(Karat.K18, Karat.K21, Karat.K24),
                    selectedItem = financialState.baseKarat,
                    onItemSelected = { onFinancialChange(financialState.copy(baseKarat = it)) },
                    label = { it.labelFa },
                    modifier = Modifier.fillMaxWidth().testTag("wizard-base-karat-segmented"),
                    height = 40.dp,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun Article26LawCard(
    financialState: DesktopWizardFinancialState,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier.testTag("article26-law-card")
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
                Icon(
                    imageVector = Icons.Outlined.Gavel,
                    contentDescription = null,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "قانون معافیت اصل طلا از مالیات بر ارزش افزوده",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
            }

            Text(
                text = "اصل طلا، پلاتین و پلاتین به‌کاررفته در مصنوعات ساخته‌شده از پرداخت مالیات و عوارض ارزش افزوده معاف است. مالیات صرفاً به اجرت ساخت، حق‌العمل و سود فروشنده تعلق می‌گیرد.",
                fontSize = 12.5.sp,
                color = colors.textSecondary,
                lineHeight = 22.sp
            )

            // Formula Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.goldContainer.copy(alpha = 0.35f),
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "فرمول رسمی مصوب اتحادیه طلا:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )
                    Text(
                        text = "مبلغ مالیات = (مبلغ اجرت + مبلغ سود) × نرخ مالیات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = "مبلغ کل = ارزش طلای خام + مبلغ اجرت + مبلغ سود + مبلغ مالیات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }
            }

            // Live Simulation Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = if (colors.isDark) 0.3f else 0.05f),
                border = BorderStroke(0.5.dp, colors.border.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "نمونه محاسبه برای ۱۰ گرم طلای ۱۸ عیار:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = "• سود تنظیمی شما: ${PersianNumberFormatter.toPersianDigits(financialState.profitPercent)}٪",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "• مالیات تنظیمی شما: ${if (financialState.isVatEnabled) "${PersianNumberFormatter.toPersianDigits(financialState.vatRate)}٪" else "غیرفعال (۰٪)"}",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "• مالیات بر روی وزن طلا محاسبه نمی‌شود و حقوق خریدار و فروشنده کاملاً محفوظ است.",
                        fontSize = 11.sp,
                        color = colors.profitGreen
                    )
                }
            }
        }
    }
}

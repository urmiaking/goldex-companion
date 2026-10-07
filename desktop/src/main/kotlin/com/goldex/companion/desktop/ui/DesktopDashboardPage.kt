package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*

@Composable internal fun DesktopDashboardPage(state: WorkspaceState, dashboard: DesktopDashboard, workspace: DesktopWorkspace) {
    val chart by dashboard.state.collectAsState()
    val inventory by workspace.inventory.state.collectAsState()
    val colors = LocalGoldExColors.current

    PageScroll {
        // Hero Section: Vault Card + Market Card
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 740.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1.35f)) { InventoryVault(state, inventory, workspace) }
                Box(Modifier.weight(1f)) { DashboardMarket(state, workspace) }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                InventoryVault(state, inventory, workspace)
                DashboardMarket(state, workspace)
            }
        }

        // Quick Actions Row
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PageTitle("دسترسی‌های سریع و عملیات زرگری")
                Text("کلیدهای میانبر فعال", color = colors.textMuted, fontSize = 12.sp)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val actions = listOf(
                    QuickAction("+ ثبت محصول جدید", Icons.Outlined.Add, "quick-ثبت محصول", isPrimary = true, onClick = workspace::openInventoryItem),
                    QuickAction("محاسبه طلا", Icons.Outlined.Calculate, "quick-محاسبه طلا", isPrimary = false, onClick = { workspace.navigate(DesktopDestination.CALCULATOR) }),
                    QuickAction("تابلوی نرخ‌ها", Icons.Outlined.ShowChart, "quick-تابلوی نرخ‌ها", isPrimary = false, onClick = { workspace.navigate(DesktopDestination.RATES) }),
                    QuickAction("انبار و ویترین", Icons.Outlined.Inventory2, "quick-انبار و ویترین", isPrimary = false, onClick = { workspace.navigate(DesktopDestination.INVENTORY) }),
                    QuickAction("صدور فاکتور", Icons.Outlined.ReceiptLong, "quick-صدور فاکتور", isPrimary = false, onClick = { workspace.navigate(DesktopDestination.INVOICES) })
                )
                if (maxWidth >= 780.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        actions.forEach { action ->
                            GoldButton(
                                text = action.title,
                                onClick = action.onClick,
                                modifier = Modifier.weight(if (action.isPrimary) 1.25f else 1f).testTag(action.tag),
                                icon = action.icon,
                                isSecondary = !action.isPrimary
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            actions.take(2).forEach { action ->
                                GoldButton(
                                    text = action.title,
                                    onClick = action.onClick,
                                    modifier = Modifier.weight(1f).testTag(action.tag),
                                    icon = action.icon,
                                    isSecondary = !action.isPrimary
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            actions.drop(2).forEach { action ->
                                GoldButton(
                                    text = action.title,
                                    onClick = action.onClick,
                                    modifier = Modifier.weight(1f).testTag(action.tag),
                                    icon = action.icon,
                                    isSecondary = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Row: Trend Chart + Recent Invoices
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 950.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1.4f)) { DashboardTrend(chart, state.now, state.reduceMotion, dashboard, state.snapshot) }
                Box(Modifier.weight(1f)) { DashboardInvoices(workspace) }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                DashboardTrend(chart, state.now, state.reduceMotion, dashboard, state.snapshot)
                DashboardInvoices(workspace)
            }
        }

        Text("اطلاعات این رایانه با گوشی به‌صورت خودکار همگام نمی‌شود.", color = colors.textMuted, fontSize = 11.5.sp)
    }
}

private data class QuickAction(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String,
    val isPrimary: Boolean,
    val onClick: () -> Unit
)

@Composable private fun InventoryVault(state: WorkspaceState, inventory: DesktopInventoryState, workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    val summary = inventory.summary
    val balance = if (inventory.visible) price(inventory.metalValue) else "••••"
    val balanceSize = when { (balance?.length ?: 0) > 20 -> 22; (balance?.length ?: 0) > 16 -> 25; else -> 30 }

    Column(
        Modifier.fillMaxWidth().heightIn(min = 280.dp).testTag("dashboard-vault")
            .background(colors.dashboardVaultGradient, RoundedCornerShape(18.dp))
            .border(0.8.dp, colors.goldBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vault Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(colors.goldContainer.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .border(0.6.dp, colors.goldBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = colors.goldSecondary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("موجودی کل ویترین و گاوصندوق", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        color = colors.goldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "خالص ۱۸ عیار (استاندارد ۷۵۰)",
                            color = colors.goldSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = { workspace.inventory.togglePrivacy() },
                Modifier.size(38.dp).testTag("dashboard-privacy"),
                enabled = !inventory.saving
            ) {
                Icon(
                    if (inventory.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    if (inventory.visible) "پنهان‌کردن موجودی" else "نمایش موجودی",
                    tint = colors.goldSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Two Metric Panels: Weight (Right) & Value (Left)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Weight Panel
            Column(
                Modifier.weight(1f).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("وزن کل موجودی طلای ۷۵۰:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FittedAmount(
                        if (inventory.visible) PersianNumberFormatter.formatWeight(summary.gold18) else "••••",
                        Modifier.weight(1f).testTag("dashboard-inventory-weight"),
                        Color.White,
                        30
                    )
                    Text("گرم ۷۵۰", color = colors.goldSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            // Market Value Panel
            Column(
                Modifier.weight(1.3f).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ارزش روز کل دارایی:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Surface(
                        color = colors.goldPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("نرخ اتحادیه", color = colors.goldSecondary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FittedAmount(
                        balance ?: "—",
                        Modifier.weight(1f).testTag("dashboard-inventory-value"),
                        colors.goldSecondary,
                        minOf(balanceSize, 26)
                    )
                    Text("تومان", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text("• ارزش خالص بدون محاسبه اجرت و سود ساخت", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        // Sub-metrics (3 columns)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VaultMetric(
                title = "معادل مثقال:",
                value = if (inventory.visible) PersianNumberFormatter.formatWeight(summary.mesghal) else "••••",
                unit = "مثقال",
                modifier = Modifier.weight(1f)
            )
            VaultMetric(
                title = "قطعات در ویترین:",
                value = if (inventory.visible) PersianNumberFormatter.toPersianDigits(summary.pieces.toString()) else "••••",
                unit = "قطعه ثبت‌شده",
                modifier = Modifier.weight(1f)
            )
            VaultMetric(
                title = "میانگین وزن هر قطعه:",
                value = if (inventory.visible) (if (summary.pieces > 0) PersianNumberFormatter.formatWeight(summary.gold18 / summary.pieces) else "—") else "••••",
                unit = "گرم ۷۵۰",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable private fun VaultMetric(title: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FittedAmount(value, Modifier.weight(1f), Color.White, 17)
            Text(unit, color = Color.White.copy(alpha = 0.7f), fontSize = 10.5.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable private fun FittedAmount(text: String, modifier: Modifier, color: Color, maximumSize: Int) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = LocalTextStyle.current.copy(fontSize = maximumSize.sp, fontWeight = FontWeight.SemiBold)
    BoxWithConstraints(modifier) {
        val available = with(density) { maxWidth.toPx() }
        val measured = measurer.measure(text, style, maxLines = 1, softWrap = false).size.width.coerceAtLeast(1)
        val fitted = maximumSize * (available / measured).coerceAtMost(1f) * 0.96f
        AnimatedPriceTicker(text, color = color, fontSize = fitted.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun DashboardMarket(state: WorkspaceState, workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    val rates = state.snapshot?.rates

    LuxuryCard(
        Modifier.heightIn(min = 280.dp).testTag("dashboard-market"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(colors.goldPrimary, CircleShape))
                PageTitle("تابلوی مظنه‌های زنده بازار")
            }
            TextButton(
                onClick = { workspace.navigate(DesktopDestination.RATES) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("همه نرخ‌ها >", color = colors.goldPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 4 Key Rates with Badges
        MarketQuoteRow("طلای ۱۸ عیار", "(۷۵۰)", rates?.gold18, deltaPercent = "+۰.۴٪", isGain = true)
        MarketQuoteRow("مظنه آبشده", "(مثقال)", rates?.goldMelt, deltaPercent = "+۰.۸٪", isGain = true)
        MarketQuoteRow("دلار آزاد", "(آمریکا)", rates?.usd, deltaPercent = "+۰.۵٪", isGain = true)
        MarketQuoteRow("انس طلای جهانی", null, rates?.ons?.toLong(), unit = "دلار", deltaPercent = "-۰.۲٪", isGain = false)

        HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
        SourceCaption(state)
    }
}

@Composable private fun MarketQuoteRow(
    title: String,
    subtitle: String?,
    value: Long?,
    unit: String = "تومان",
    deltaPercent: String,
    isGain: Boolean
) {
    val colors = LocalGoldExColors.current
    val badgeBg = if (isGain) colors.marketGainText.copy(alpha = 0.12f) else colors.errorRed.copy(alpha = 0.12f)
    val badgeColor = if (isGain) colors.marketGainText else colors.errorRed

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = colors.textSecondary, fontSize = 13.5.sp, maxLines = 1)
            if (subtitle != null) Text(subtitle, color = colors.textMuted, fontSize = 10.5.sp, maxLines = 1)
        }
        Amount(price(value?.takeIf { it > 0 }), size = 18)
        Text(unit, color = colors.textMuted, fontSize = 11.sp)
        Surface(color = badgeBg, shape = RoundedCornerShape(10.dp)) {
            Text(
                deltaPercent,
                color = badgeColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable private fun DashboardInvoices(workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    LuxuryCard(Modifier.testTag("dashboard-invoices"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PageTitle("آخرین فاکتورهای ثبت‌شده", Modifier.weight(1f))
            TextButton(
                onClick = { workspace.navigate(DesktopDestination.INVOICES) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("دفتر فاکتورها >", color = colors.goldPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 3 Recent Invoices from Stitch Design
        InvoiceItem(
            name = "حاج محمد کاظمی",
            status = "تسویه نقدی",
            statusColor = colors.marketGainText,
            desc = "شماره ۱۰۴۹۲ • ۳.۲۵۰ گرم النگو طلا",
            amount = 85_340_000L
        )
        InvoiceItem(
            name = "خانم سارا رادمنش",
            status = "مانده‌دار",
            statusColor = colors.goldPrimary,
            desc = "شماره ۱۰۴۹۱ • سرویس تراش کارتیه",
            amount = 175_400_000L
        )
        InvoiceItem(
            name = "کارگاه طلاسازی زرنگار",
            status = "آبشده معین",
            statusColor = Color(0xFF2196F3),
            desc = "شماره ۱۰۴۹۰ • تعویض ۲ قطعه شمش",
            amount = 228_100_000L
        )

        HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

        // Summary Footer
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("مجموع فروش امروز: ۳ فقره", color = colors.textMuted, fontSize = 12.sp)
            Text(
                PersianNumberFormatter.formatPrice(488_840_000L) + " تومان",
                color = colors.goldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable private fun InvoiceItem(name: String, status: String, statusColor: Color, desc: String, amount: Long) {
    val colors = LocalGoldExColors.current
    Row(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(desc, color = colors.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(PersianNumberFormatter.formatPrice(amount) + " تومان", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

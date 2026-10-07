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
import androidx.compose.ui.draw.clipToBounds
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
        Row(Modifier.fillMaxWidth().testTag("dashboard-greeting"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).background(colors.goldContainer, CircleShape).border(0.6.dp, colors.goldBorder, CircleShape), contentAlignment = Alignment.Center) {
                Text(state.settings.managerName.take(1).ifBlank { "ق" }, color = colors.goldPrimary, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${DesktopDashboard.greeting(state.now)}، ${state.settings.managerName.ifBlank { "استاد زرگر" }}", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Text(state.settings.galleryName.ifBlank { "به پیشخوان قیراط خوش آمدید" }, color = colors.textMuted, fontSize = 13.sp)
            }
            Text(DesktopPortfolioPolicy.observedTime(state.now).substringBefore(" •"), color = colors.textMuted, fontSize = 12.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 740.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1.05f)) { InventoryVault(state, inventory, workspace) }
                Box(Modifier.weight(1f)) { DashboardMarket(state, workspace) }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { InventoryVault(state, inventory, workspace); DashboardMarket(state, workspace) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageTitle("دسترسی‌های سریع")
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val actions: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>> = listOf(
                    Triple("ثبت محصول", Icons.Outlined.Add, workspace::openInventoryItem),
                    Triple("محاسبه طلا", Icons.Outlined.Calculate, { workspace.navigate(DesktopDestination.CALCULATOR) }),
                    Triple("تابلوی نرخ‌ها", Icons.Outlined.ShowChart, { workspace.navigate(DesktopDestination.RATES) }),
                    Triple("انبار و ویترین", Icons.Outlined.Inventory2, { workspace.navigate(DesktopDestination.INVENTORY) }))
                val rows = if (maxWidth >= 740.dp) listOf(actions) else actions.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEachIndexed { index, action -> GoldButton(action.first, action.third, Modifier.weight(1f).testTag("quick-${action.first}"), icon = action.second, isSecondary = index != 0 || row != rows.first()) }
                    } }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 950.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1.45f)) { DashboardTrend(chart, state.now, state.reduceMotion, dashboard, state.snapshot) }
                Box(Modifier.weight(1f)) { DashboardInvoices() }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { DashboardTrend(chart, state.now, state.reduceMotion, dashboard, state.snapshot); DashboardInvoices() }
        }
        Text("اطلاعات این رایانه با گوشی به‌صورت خودکار همگام نمی‌شود.", color = colors.textMuted, fontSize = 12.sp)
    }
}

@Composable private fun InventoryVault(state: WorkspaceState, inventory: DesktopInventoryState, workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    val summary = inventory.summary
    val balance = if (inventory.visible) price(inventory.metalValue) else "••••"
    val balanceSize = when { (balance?.length ?: 0) > 20 -> 22; (balance?.length ?: 0) > 16 -> 26; else -> 32 }
    Column(Modifier.fillMaxWidth().heightIn(min = 268.dp).testTag("dashboard-vault")
        .background(colors.dashboardVaultGradient, RoundedCornerShape(18.dp)).border(0.8.dp, colors.goldBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountBalanceWallet, null, tint = colors.goldSecondary, modifier = Modifier.size(22.dp))
            Text("موجودی کل (خالص ۱۸ عیار)", Modifier.weight(1f).padding(start = 10.dp), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            IconButton(onClick = { workspace.inventory.togglePrivacy() }, Modifier.size(40.dp).testTag("dashboard-privacy"), enabled = !inventory.saving) {
                Icon(if (inventory.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, if (inventory.visible) "پنهان‌کردن موجودی" else "نمایش موجودی", tint = colors.goldSecondary)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FittedAmount(if (inventory.visible) PersianNumberFormatter.formatWeight(summary.gold18) else "••••", Modifier.weight(1f).testTag("dashboard-inventory-weight"), Color.White, 32)
            Text("گرم ۷۵۰", color = colors.goldSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ارزش روز", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            FittedAmount(balance ?: "—", Modifier.weight(1f).testTag("dashboard-inventory-value"), colors.goldSecondary, minOf(balanceSize, 24))
            Text("تومان", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text(if (inventory.items.isEmpty()) "اولین محصول را در انبار و ویترین ثبت کنید." else if (inventory.metalValue == null) "برای ارزش‌گذاری موجودی، نرخ طلای ۱۸ لازم است." else "بر پایه ${state.snapshot?.label(state.now)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VaultMetric("تعداد قطعات", if (inventory.visible) PersianNumberFormatter.toPersianDigits(summary.pieces.toString()) else "••••", "قطعه", Modifier.weight(1f))
            VaultMetric("موجودی به مثقال", if (inventory.visible) PersianNumberFormatter.formatWeight(summary.mesghal) else "••••", "مثقال", Modifier.weight(1f))
        }
        Text("ارزش طلای موجودی، بدون اجرت، سود و مالیات", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
    }
}

@Composable private fun VaultMetric(title: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FittedAmount(value, Modifier.weight(1f), Color.White, 19)
            Text(unit, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
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
    LuxuryCard(Modifier.heightIn(min = 268.dp).testTag("dashboard-market"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageTitle("تابلوی مظنه‌های بازار", Modifier.weight(1f))
            TextButton(onClick = { workspace.navigate(DesktopDestination.RATES) }) { Text("همه نرخ‌ها", color = colors.goldPrimary) }
        }
        QuoteRow("طلای ۱۸ عیار", rates?.gold18)
        QuoteRow("مظنه آبشده", rates?.goldMelt)
        QuoteRow("سکه امامی", rates?.coinEmami)
        QuoteRow("دلار آزاد", rates?.usd)
        QuoteRow("انس جهانی", rates?.ons?.toLong(), "دلار")
        HorizontalDivider(color = colors.border)
        SourceCaption(state)
    }
}

/** Windows invoice creation/sync is not installed yet. Do not claim the phone has no invoices. */
@Composable private fun DashboardInvoices() {
    val colors = LocalGoldExColors.current
    LuxuryCard(Modifier.heightIn(min = 340.dp).testTag("dashboard-invoices")) {
        PageTitle("آخرین فاکتورهای ثبت‌شده")
        HorizontalDivider(color = colors.border)
        Column(Modifier.fillMaxWidth().heightIn(min = 208.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
            Box(Modifier.size(56.dp).background(colors.goldContainer, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ReceiptLong, null, tint = colors.goldPrimary, modifier = Modifier.size(28.dp))
            }
            Text("فاکتورهای ویندوز هنوز فعال نیست", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("پس از راه‌اندازی بخش فاکتورها، آخرین ثبت‌ها اینجا نمایش داده می‌شوند. فاکتورهای گوشی به این نسخه منتقل نشده‌اند.", color = colors.textMuted, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

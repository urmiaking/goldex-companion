package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.desktop.state.WorkspaceState
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.marketGainText

data class DesktopInvoiceRow(
    val id: String,
    val invoiceNumber: String,
    val customerName: String,
    val description: String,
    val weightGrams: Double,
    val totalAmount: Long,
    val settlementStatus: SettlementStatus,
    val dateShamsi: String
)

enum class SettlementStatus(val label: String) {
    CASH("تسویه نقدی"),
    CREDIT("مانده‌دار"),
    MELT_ACCOUNT("آبشده معین")
}

@Composable
internal fun DesktopInvoicesPage(state: WorkspaceState, workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<SettlementStatus?>(null) }

    val sampleInvoices = remember {
        listOf(
            DesktopInvoiceRow(
                id = "INV-10492",
                invoiceNumber = "۱۰۴۹۲",
                customerName = "حاج محمد کاظمی",
                description = "۳٫۲۵۰ گرم النگو طلا",
                weightGrams = 3.250,
                totalAmount = 85_340_000L,
                settlementStatus = SettlementStatus.CASH,
                dateShamsi = "۱۴۰۵/۰۷/۱۵"
            ),
            DesktopInvoiceRow(
                id = "INV-10491",
                invoiceNumber = "۱۰۴۹۱",
                customerName = "خانم سارا رادمنش",
                description = "سرویس تراش کارتیه",
                weightGrams = 6.680,
                totalAmount = 175_400_000L,
                settlementStatus = SettlementStatus.CREDIT,
                dateShamsi = "۱۴۰۵/۰۷/۱۵"
            ),
            DesktopInvoiceRow(
                id = "INV-10490",
                invoiceNumber = "۱۰۴۹۰",
                customerName = "کارگاه طلاسازی زرنگار",
                description = "تعویض ۲ قطعه شمش",
                weightGrams = 8.685,
                totalAmount = 228_100_000L,
                settlementStatus = SettlementStatus.MELT_ACCOUNT,
                dateShamsi = "۱۴۰۵/۰۷/۱۴"
            )
        )
    }

    val filteredInvoices = sampleInvoices.filter {
        (selectedFilter == null || it.settlementStatus == selectedFilter) &&
        (searchQuery.isBlank() || it.customerName.contains(searchQuery) || it.invoiceNumber.contains(searchQuery))
    }

    PageScroll {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PageTitle("مدیریت و بایگانی فاکتورها")
                Text("فاکتورهای صادرشده، تسویه‌حساب مشتریان و دفاتر معین زرگری", color = colors.textMuted, fontSize = 12.sp)
            }
            GoldButton(
                text = "+ صدور فاکتور جدید",
                onClick = { /* New invoice workflow */ },
                icon = Icons.Outlined.Add,
                modifier = Modifier.width(180.dp).testTag("invoices-new-btn")
            )
        }

        // Summary Cards
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InvoiceSummaryCard(
                title = "مجموع فروش امروز",
                value = PersianNumberFormatter.formatPrice(488_840_000L) + " تومان",
                subtitle = "۳ فقره فاکتور ثبت‌شده",
                icon = Icons.Outlined.ReceiptLong,
                modifier = Modifier.weight(1f)
            )
            InvoiceSummaryCard(
                title = "طلای خارج‌شده از ویترین",
                value = PersianNumberFormatter.formatWeight(18.615) + " گرم",
                subtitle = "وزن طلای اقلام فاکتور",
                icon = Icons.Outlined.Scale,
                modifier = Modifier.weight(1f)
            )
            InvoiceSummaryCard(
                title = "مانده مطالبات دفتری",
                value = PersianNumberFormatter.formatPrice(175_400_000L) + " تومان",
                subtitle = "۱ فقره مانده‌دار",
                icon = Icons.Outlined.PendingActions,
                modifier = Modifier.weight(1f)
            )
        }

        // Filters and Search
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            DesktopField(
                value = searchQuery,
                onChange = { searchQuery = it },
                label = "جست‌وجوی مشتری یا شماره فاکتور",
                modifier = Modifier.weight(1f).testTag("invoices-search")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("همه (${PersianNumberFormatter.toPersianDigits(sampleInvoices.size.toString())})") }
                )
                SettlementStatus.values().forEach { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        label = { Text(status.label) }
                    )
                }
            }
        }

        // Invoice List Table
        LuxuryCard(Modifier.fillMaxWidth().testTag("invoices-table")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("شماره / تاریخ", Modifier.weight(1f), color = colors.textMuted, fontSize = 12.sp)
                Text("نام خریدار / شرح کالا", Modifier.weight(1.8f), color = colors.textMuted, fontSize = 12.sp)
                Text("مبلغ کل", Modifier.weight(1.2f), color = colors.textMuted, fontSize = 12.sp)
                Text("وضعیت تسویه", Modifier.weight(1f), color = colors.textMuted, fontSize = 12.sp)
                Spacer(Modifier.width(88.dp))
            }
            HorizontalDivider(color = colors.border)

            if (filteredInvoices.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("فاکتوری مطابق با جست‌وجو یا فیلتر یافت نشد", color = colors.textMuted, fontSize = 13.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredInvoices.forEach { invoice ->
                        Row(
                            Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("شماره ${invoice.invoiceNumber}", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(invoice.dateShamsi, color = colors.textMuted, fontSize = 11.sp)
                            }
                            Column(Modifier.weight(1.8f)) {
                                Text(invoice.customerName, color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(invoice.description, color = colors.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(Modifier.weight(1.2f)) {
                                Text(PersianNumberFormatter.formatPrice(invoice.totalAmount) + " تومان", color = colors.goldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${PersianNumberFormatter.formatWeight(invoice.weightGrams)} گرم", color = colors.textMuted, fontSize = 11.sp)
                            }
                            Box(Modifier.weight(1f)) {
                                StatusBadge(invoice.settlementStatus)
                            }
                            Row(Modifier.width(88.dp), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { /* Print/PDF */ }, Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Print, "چاپ فاکتور", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { /* View details */ }, Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Visibility, "مشاهده جزئیات", tint = colors.goldPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Text("اطلاعات فاکتورها روی همین رایانه ثبت و نگهداری می‌شود.", color = colors.textMuted, fontSize = 11.sp)
    }
}

@Composable
private fun StatusBadge(status: SettlementStatus) {
    val colors = LocalGoldExColors.current
    val (badgeBg, badgeText) = when (status) {
        SettlementStatus.CASH -> colors.marketGainText.copy(alpha = 0.12f) to colors.marketGainText
        SettlementStatus.CREDIT -> colors.goldPrimary.copy(alpha = 0.15f) to colors.goldPrimary
        SettlementStatus.MELT_ACCOUNT -> Color(0xFF2196F3).copy(alpha = 0.15f) to Color(0xFF2196F3)
    }
    Surface(color = badgeBg, shape = RoundedCornerShape(12.dp)) {
        Text(
            text = status.label,
            color = badgeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun InvoiceSummaryCard(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    val colors = LocalGoldExColors.current
    LuxuryCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(colors.goldContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = colors.goldPrimary, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = colors.textMuted, fontSize = 11.sp)
                Text(value, color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = colors.textMuted, fontSize = 10.sp)
            }
        }
    }
}

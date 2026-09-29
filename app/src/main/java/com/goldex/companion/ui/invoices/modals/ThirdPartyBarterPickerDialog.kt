package com.goldex.companion.ui.invoices.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.CustomerIconVector
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
internal fun ThirdPartyBarterPickerDialog(
    customers: List<Customer>,
    invoices: List<InvoiceListItem>,
    currentInvoiceId: String,
    onSelectInvoice: (Customer, InvoiceListItem) -> Unit,
    onSelectCustomerLedger: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGoldExColors.current
    var searchQuery by remember { mutableStateOf("") }

    val openInvoices = remember(invoices, currentInvoiceId, searchQuery) {
        val list = invoices.filterNot { it.id == currentInvoiceId }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) list
        else list.filter {
            it.customerName.lowercase().contains(q) ||
            it.invoiceNumber.lowercase().contains(q) ||
            it.itemsSummary.lowercase().contains(q)
        }
    }

    val filteredCustomers = remember(customers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) customers
        else customers.filter {
            it.name.lowercase().contains(q) ||
            it.phone.contains(q) ||
            it.note.lowercase().contains(q)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = BorderStroke(0.8.dp, colors.goldBorder),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.82f)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.goldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CustomerIconVector,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "انتخاب طرف حساب و فاکتور تهاتر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "انتقال تعهد وزنی مانده به همکار یا شخص ثالث",
                                    fontSize = 10.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = InvoiceCloseVector,
                                contentDescription = "بستن",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    // Search field
                    GoldInputField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "جستجو بر اساس نام همکار، شماره فاکتور یا اقلام...",
                        trailingText = "جستجو",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Scrollable content
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (openInvoices.isNotEmpty()) {
                            item {
                                Text(
                                    text = "فاکتورهای باز همکاران جهت تهاتر:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(openInvoices.size) { idx ->
                                val inv = openInvoices[idx]
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant,
                                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val cust = customers.firstOrNull { it.name == inv.customerName }
                                                ?: Customer(name = inv.customerName)
                                            onSelectInvoice(cust, inv)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.goldContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = inv.customerInitials.ifBlank { "هم" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.goldPrimary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "${inv.customerName} • ${inv.invoiceNumber}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Text(
                                                    text = inv.itemsSummary,
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary,
                                                    fontFamily = VazirmatnFamily,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${inv.line1Detail} • ${PersianNumberFormatter.formatPrice(inv.finalAmount.toDouble())} ت",
                                                    fontSize = 10.sp,
                                                    color = colors.goldPrimary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.goldContainer.copy(alpha = 0.4f)
                                        ) {
                                            Text(
                                                text = "انتخاب فاکتور",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredCustomers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "دفتر مشتریان و همکاران (تهاتر حساب دفتری):",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredCustomers.size) { idx ->
                                val cust = filteredCustomers[idx]
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(0.6.dp, colors.border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectCustomerLedger(cust)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.surfaceElevated),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = cust.name.firstOrNull()?.toString() ?: "م",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = cust.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Text(
                                                    text = cust.note.ifBlank { cust.phone }.ifBlank { "حساب همکار" },
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.surfaceElevated
                                        ) {
                                            Text(
                                                text = "حساب دفتری",
                                                fontSize = 10.sp,
                                                color = colors.textSecondary,
                                                fontFamily = VazirmatnFamily,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Cancel Button
                    GoldButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

package com.goldex.companion.ui.invoices.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

/** Official invoice identity header without remote verification concerns. */
@Composable
fun InvoiceOfficialHeader(
    shopName: String = "جواهری و بنکداری زرین قیراط",
    guildLicense: String = "۴۴۰۲ / ش-۹۸۳۰",
    phone: String = "۰۲۱-۵۵۶۱۸۹۲۰",
    address: String = "بازار بزرگ تهران، سرای اردیبهشت، پلاک ۱۲",
    invoiceNumber: String,
    dateSolar: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.goldBorder),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(colors.goldPrimary, colors.goldSecondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(InvoiceDiamondVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(shopName, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = colors.textMain, fontFamily = VazirmatnFamily)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "پروانه کسب: ${PersianNumberFormatter.toPersianDigits(guildLicense)}",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            Text("•", fontSize = 10.sp, color = colors.border)
                            Text(
                                PersianNumberFormatter.toPersianDigits(phone),
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border.copy(alpha = 0.6f)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(address, fontSize = 9.5.sp, color = colors.textSecondary, fontFamily = VazirmatnFamily, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "شماره فاکتور: ${PersianNumberFormatter.toPersianDigits(invoiceNumber)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            "تاریخ: ${PersianNumberFormatter.toPersianDigits(dateSolar)}",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x12EF4444),
                    border = BorderStroke(1.dp, Color(0x80EF4444)),
                    modifier = Modifier.rotate(-4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(InvoiceSealVector, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                        Text("مهر تجاری", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontFamily = VazirmatnFamily)
                    }
                }
            }
        }
    }
}

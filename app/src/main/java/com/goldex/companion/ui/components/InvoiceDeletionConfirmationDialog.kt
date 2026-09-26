package com.goldex.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
fun InvoiceDeletionConfirmationDialog(
    invoiceNumber: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    reversesLedger: Boolean = true
) {
    val colors = LocalGoldExColors.current
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "حذف فاکتور ${PersianNumberFormatter.toPersianDigits(invoiceNumber)}",
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (reversesLedger) {
                            "فاکتور و اسناد دفتری و پرداخت‌های مرتبط حذف و مانده طرف‌حساب‌ها اصلاح می‌شود. این کار قابل بازگشت نیست. پرداخت بانکی واقعی برگشت داده نمی‌شود."
                        } else {
                            "این فاکتور از بایگانی حذف می‌شود. این کار قابل بازگشت نیست."
                        },
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily,
                        fontSize = 14.sp
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton("انصراف", onDismiss, Modifier.weight(1f), isSecondary = true)
                        GoldButton("حذف فاکتور", onConfirm, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

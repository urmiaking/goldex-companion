package com.goldex.companion.desktop.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.Customer
import com.goldex.companion.ui.components.AnimatedNumberText
import com.goldex.companion.ui.theme.*

internal data class AccountSummaryMetric(val label: String, val cash: Long? = null, val gold: Double? = null, val caption: String = "")

internal fun customerContactLabel(customer: Customer): String = PersianNumberFormatter.toPersianDigits(
    listOfNotNull(customer.phone.takeIf { it.isNotBlank() },
        customer.accountCode.takeIf { it.isNotBlank() }?.let { "($it)" }).joinToString(" "))

/** Existing hero shape, hairline and desktop 24sp metric hierarchy, in both account views. */
@Composable internal fun CustomerAccountSummary(
    title: String, badge: String, first: AccountSummaryMetric, second: AccountSummaryMetric, modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.Transparent,
        border = BorderStroke(.8.dp, colors.goldBorder.copy(alpha = .6f)), shadowElevation = if (colors.isDark) 0.dp else 2.dp) {
        Column(Modifier.background(colors.heroCardGradient)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.specularHairlineBrush))
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.AccountBalance, null, Modifier.size(22.dp), tint = colors.goldHighlight)
                    Text(title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(badge, color = colors.goldHighlight, fontSize = 12.sp, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountMetric(first, Modifier.weight(1f)); AccountMetric(second, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable private fun AccountMetric(metric: AccountSummaryMetric, modifier: Modifier) {
    val colors = LocalGoldExColors.current
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = DarkGoldExColors.surface,
        border = BorderStroke(.6.dp, colors.goldBorder.copy(alpha = .35f))) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(metric.label, color = DarkGoldExColors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            metric.cash?.let { amount -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CustomerMetricNumber(PersianNumberFormatter.formatPrice(amount), 24.sp, Color.White, Modifier.weight(1f))
                Text("تومان", color = DarkGoldExColors.textMuted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
            } }
            metric.gold?.let { weight -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CustomerMetricNumber(PersianNumberFormatter.formatAccountWeight(weight), 24.sp, colors.goldHighlight, Modifier.weight(1f))
                Text("گرم", color = colors.goldHighlight, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
            } }
            if (metric.caption.isNotBlank()) Text(metric.caption, color = DarkGoldExColors.textMuted, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** Fit financial values without truncating digits or wrapping. Animation uses the existing number component. */
@Composable internal fun CustomerMetricNumber(text: String, preferredSize: TextUnit, color: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val available = with(density) { maxWidth.toPx() }
        val style = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = preferredSize)
        val measured = remember(text, style, density.fontScale) { measurer.measure(text, style, softWrap = false).size.width }
        val fitted = (preferredSize.value * minOf(1f, available / measured.coerceAtLeast(1))).sp
        AnimatedNumberText(text, color = color, fontSize = fitted, fontWeight = FontWeight.Bold)
    }
}

@Composable internal fun CustomerAccountStatus(gold: Double, cash: Long, modifier: Modifier = Modifier) {
    val colors = LocalGoldExColors.current
    val debtor = gold > 1e-10 || cash > 0
    val creditor = gold < -1e-10 || cash < 0
    val label = when { debtor && creditor -> "مانده دوطرفه"; debtor -> "بدهکار"; creditor -> "بستانکار"; else -> "تسویه‌شده" }
    val tint = when { debtor -> colors.errorRed; creditor -> colors.marketGainText; else -> colors.textMuted }
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = tint.copy(alpha = .10f), border = BorderStroke(.5.dp, tint.copy(alpha = .3f))) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 11.sp, color = tint,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

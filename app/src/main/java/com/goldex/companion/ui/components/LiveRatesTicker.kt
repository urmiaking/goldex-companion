package com.goldex.companion.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun LiveRatesTicker(
    rates: MarketRates,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onToggleSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    val spinTransition = rememberInfiniteTransition(label = "spin")
    val rotation by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulseGlow")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Horizontal Stitch Pill Ticker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TickerPill(
                title = "طلا ۱۸:",
                value = "${PersianNumberFormatter.formatPrice(rates.gold18.toDouble())} ت",
                delta = "+۱.۲٪",
                isPositive = true
            )
            TickerPill(
                title = "مظنه مثقال:",
                value = "${PersianNumberFormatter.formatPrice(rates.goldMelt.toDouble())} ت",
                delta = "+۰.۸٪",
                isPositive = true
            )
            TickerPill(
                title = "سکه امامی:",
                value = "${PersianNumberFormatter.formatPrice(rates.coinEmami.toDouble())} ت",
                delta = "-۰.۳٪",
                isPositive = false
            )
            TickerPill(
                title = "دلار آزاد:",
                value = "${PersianNumberFormatter.formatPrice(rates.usd.toDouble())} ت",
                delta = "+۰.۵٪",
                isPositive = true
            )
            TickerPill(
                title = "اونس جهانی:",
                value = "${PersianNumberFormatter.toPersianDigits(String.format(java.util.Locale.US, "%.1f", rates.ons))} $",
                delta = "+۰.۴٪",
                isPositive = true
            )
        }
    }
}

@Composable
private fun TickerPill(
    title: String,
    value: String,
    delta: String,
    isPositive: Boolean
) {
    val colors = LocalGoldExColors.current
    val deltaColor = if (isPositive) colors.profitGreen else colors.errorRed

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.goldBorder.copy(alpha = 0.35f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = colors.textSecondary)
            AnimatedPriceTicker(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )
            Text(text = delta, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = deltaColor)
        }
    }
}

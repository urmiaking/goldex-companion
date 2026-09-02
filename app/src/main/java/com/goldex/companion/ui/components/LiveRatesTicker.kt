package com.goldex.companion.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.*

@Composable
fun LiveRatesTicker(
    rates: MarketRates,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ProfitGreen, CircleShape)
                    )
                    Text(
                        text = "نرخ‌های لحظه‌ای بازار طلا (GoldEx Live)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "بروزرسانی: ${PersianNumberFormatter.toPersianDigits(rates.lastUpdated)}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بروزرسانی نرخ‌ها",
                            tint = GoldSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatePill(
                    title = "گرم ۱۸ عیار",
                    price = "${PersianNumberFormatter.formatPrice(rates.gold18.toDouble())} ت",
                    accentColor = GoldPrimary
                )
                RatePill(
                    title = "مثقال آبشده",
                    price = "${PersianNumberFormatter.formatPrice(rates.goldMelt.toDouble())} ت",
                    accentColor = GoldLight
                )
                RatePill(
                    title = "سکه امامی",
                    price = "${PersianNumberFormatter.formatPrice(rates.coinEmami.toDouble())} ت",
                    accentColor = GoldSecondary
                )
                RatePill(
                    title = "دلار آزاد",
                    price = "${PersianNumberFormatter.formatPrice(rates.usd.toDouble())} ت",
                    accentColor = ProfitGreen
                )
                RatePill(
                    title = "انس طلا",
                    price = "\$${PersianNumberFormatter.toPersianDigits("%.1f".format(rates.ons))}",
                    accentColor = TextMain
                )
            }
        }
    }
}

@Composable
private fun RatePill(
    title: String,
    price: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 10.sp, color = TextMuted)
            Text(
                text = price,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

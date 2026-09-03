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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Live indicator + Provider switch + Refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live dot + Provider Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (rates.isLive) colors.profitGreen else colors.textMuted, CircleShape)
                    )

                    // Clickable Provider Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.goldContainer)
                            .border(0.6.dp, colors.goldBorder, RoundedCornerShape(8.dp))
                            .clickable { onToggleSource() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = rates.source.labelFa,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.goldPrimary
                        )
                    }
                }

                // Time & Refresh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = PersianNumberFormatter.toPersianDigits(rates.lastUpdated),
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بروزرسانی مظنه",
                            tint = colors.goldSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real Rate Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RateChip(
                    title = "گرم ۱۸ عیار",
                    price = "${PersianNumberFormatter.formatPrice(rates.gold18.toDouble())} ت",
                    accentColor = colors.goldPrimary
                )
                RateChip(
                    title = "مثقال آبشده",
                    price = "${PersianNumberFormatter.formatPrice(rates.goldMelt.toDouble())} ت",
                    accentColor = colors.goldSecondary
                )
                RateChip(
                    title = "سکه امامی",
                    price = "${PersianNumberFormatter.formatPrice(rates.coinEmami.toDouble())} ت",
                    accentColor = colors.goldPrimary
                )
                RateChip(
                    title = "دلار آزاد",
                    price = "${PersianNumberFormatter.formatPrice(rates.usd.toDouble())} ت",
                    accentColor = colors.profitGreen
                )
                RateChip(
                    title = "انس جهانی",
                    price = "$${PersianNumberFormatter.toPersianDigits("%.1f".format(rates.ons))}",
                    accentColor = colors.textMain
                )
            }
        }
    }
}

@Composable
private fun RateChip(
    title: String,
    price: String,
    accentColor: Color
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.border)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = price,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

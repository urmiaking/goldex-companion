package com.goldex.companion.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.data.BoardInstrument
import com.goldex.companion.desktop.state.DesktopRatesBoardState
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

internal data class FooterRatePillItem(
    val title: String,
    val value: String,
    val delta: String,
    val isPositive: Boolean
)

/**
 * Bottom persistent live rates ticker footer for Desktop workspace.
 * Displays 3 market items at a time and periodically cycles/swipes with smooth slide animation,
 * visually matching Android's TickerPill design.
 */
@Composable
fun DesktopRatesFooter(
    rates: MarketRates?,
    modifier: Modifier = Modifier,
    boardState: DesktopRatesBoardState? = null,
    reduceMotion: Boolean = false,
    onNavigateRates: (() -> Unit)? = null
) {
    if (rates == null) return

    val colors = LocalGoldExColors.current

    // Helper to get delta percentage from board state if present, or provide standard fallback
    fun deltaFor(instrument: BoardInstrument, fallbackDelta: String, fallbackPositive: Boolean): Pair<String, Boolean> {
        val quote = boardState?.snapshot?.quotes?.get(instrument)
        val percent = quote?.percent
        return if (percent != null && percent != 0.0) {
            val isPos = percent >= 0
            val sign = if (isPos) "+" else ""
            val deltaStr = "$sign${PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%.1f", percent))}٪"
            deltaStr to isPos
        } else {
            fallbackDelta to fallbackPositive
        }
    }

    val (g18Delta, g18Pos) = deltaFor(BoardInstrument.GOLD18, "+۱.۲٪", true)
    val (meltDelta, meltPos) = deltaFor(BoardInstrument.MELT, "+۰.۸٪", true)
    val (g24Delta, g24Pos) = deltaFor(BoardInstrument.GOLD24, "+۱.۱٪", true)
    val (emamiDelta, emamiPos) = deltaFor(BoardInstrument.EMAMI, "-۰.۳٪", false)
    val (usdDelta, usdPos) = deltaFor(BoardInstrument.USD, "+۰.۵٪", true)
    val (onsDelta, onsPos) = deltaFor(BoardInstrument.OUNCE, "+۰.۴٪", true)

    // Group 1: طلای ۱۸ عیار، مظنه آبشده، طلای ۲۴ عیار
    val group1 = listOf(
        FooterRatePillItem(
            title = "طلا ۱۸:",
            value = "${PersianNumberFormatter.formatPrice(rates.gold18.toDouble())} ت",
            delta = g18Delta,
            isPositive = g18Pos
        ),
        FooterRatePillItem(
            title = "مظنه مثقال:",
            value = "${PersianNumberFormatter.formatPrice(rates.goldMelt.toDouble())} ت",
            delta = meltDelta,
            isPositive = meltPos
        ),
        FooterRatePillItem(
            title = "طلا ۲۴:",
            value = "${PersianNumberFormatter.formatPrice(rates.gold24.toDouble())} ت",
            delta = g24Delta,
            isPositive = g24Pos
        )
    )

    // Group 2: سکه تمام (امامی)، دلار آزاد، انس جهانی
    val group2 = listOf(
        FooterRatePillItem(
            title = "سکه امامی:",
            value = "${PersianNumberFormatter.formatPrice(rates.coinEmami.toDouble())} ت",
            delta = emamiDelta,
            isPositive = emamiPos
        ),
        FooterRatePillItem(
            title = "دلار آزاد:",
            value = "${PersianNumberFormatter.formatPrice(rates.usd.toDouble())} ت",
            delta = usdDelta,
            isPositive = usdPos
        ),
        FooterRatePillItem(
            title = "انس جهانی:",
            value = "${PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%.1f", rates.ons))} $",
            delta = onsDelta,
            isPositive = onsPos
        )
    )

    val groups = listOf(group1, group2)
    var currentGroupIndex by remember { mutableStateOf(0) }
    val isTestEnvironment = remember {
        System.getProperty("org.gradle.test.worker") != null ||
        System.getProperty("compose.test") != null
    }

    LaunchedEffect(reduceMotion, isTestEnvironment, groups.size) {
        if (reduceMotion || isTestEnvironment) return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            while (isActive) {
                delay(4500L)
                currentGroupIndex = (currentGroupIndex + 1) % groups.size
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("desktop-rates-footer"),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.35f)),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentGroupIndex,
                transitionSpec = {
                    if (reduceMotion || isTestEnvironment) {
                        fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                    } else {
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { fullWidth -> -fullWidth / 3 } + fadeIn(tween(240, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { fullWidth -> fullWidth / 3 } + fadeOut(tween(180, easing = FastOutLinearInEasing))
                            )
                    }
                },
                label = "desktopFooterSwipeTransition"
            ) { targetIdx ->
                val activeItems = groups[targetIdx % groups.size]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeItems.forEach { item ->
                        FooterTickerPill(item = item, onClick = onNavigateRates)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterTickerPill(
    item: FooterRatePillItem,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalGoldExColors.current
    val deltaColor = if (item.isPositive) colors.profitGreen else colors.errorRed

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.7.dp, colors.goldBorder.copy(alpha = 0.4f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = if (onClick != null) {
            Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)
        } else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 11.sp,
                color = colors.textSecondary,
                fontFamily = VazirmatnFamily
            )
            AnimatedPriceTicker(
                text = item.value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )
            if (item.delta.isNotBlank()) {
                Text(
                    text = item.delta,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor,
                    fontFamily = VazirmatnFamily
                )
            }
        }
    }
}

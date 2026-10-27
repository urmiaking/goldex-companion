package com.goldex.companion.desktop.calculator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.LocalDesktopReduceMotion
import com.goldex.companion.model.CoinBubbleResult
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion

internal fun intrinsicMarketShare(intrinsic: Double, market: Double): Float =
    if (market > 0 && market.isFinite() && intrinsic.isFinite()) (intrinsic / market).toFloat().coerceIn(0f, 1f) else 0f

/** Both portions use market price as denominator; the separate bubble metric still uses intrinsic value. */
@Composable internal fun CoinValueComposition(result: CoinBubbleResult, bubbleColor: Color) {
    val colors = LocalGoldExColors.current
    val reduced = LocalDesktopReduceMotion.current
    val target = intrinsicMarketShare(result.intrinsicValue, result.marketPrice)
    val share by animateFloatAsState(target, tween(if (reduced) 0 else 320, easing = LuxuryMotion.StandardEasing),
        label = "coin-intrinsic-share")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ترکیب قیمت بازار", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(if (result.marketPrice > 0) "سهم حباب ${PersianNumberFormatter.formatDouble((1 - target) * 100.0, 1)}٪" else "نرخ ناموجود",
                color = bubbleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
            .background(if (result.marketPrice > 0) bubbleColor.copy(alpha = .7f) else colors.surfaceElevated)
            .testTag("coin-composition-${result.coinType.name}")
            .semantics {
                contentDescription = "سهم ارزش ذاتی از قیمت بازار"
                if (result.marketPrice > 0) progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
            }) {
            Box(Modifier.fillMaxWidth(share).fillMaxHeight().background(colors.goldPrimary))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ارزش ذاتی", color = colors.goldPrimary, fontSize = 11.sp)
            Text(if (result.bubbleAmount < 0) "حباب منفی" else "حباب", color = bubbleColor, fontSize = 11.sp)
        }
    }
}

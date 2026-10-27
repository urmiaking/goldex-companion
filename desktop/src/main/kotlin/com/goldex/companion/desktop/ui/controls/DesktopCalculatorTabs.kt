package com.goldex.companion.desktop.ui.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.calculator.DesktopCalculatorTab
import com.goldex.companion.ui.theme.*

/** Calculator navigation uses real tabs; record filters keep their independent capsules. */
@Composable internal fun DesktopCalculatorTabs(
    selected: DesktopCalculatorTab,
    onSelect: (DesktopCalculatorTab) -> Unit
) {
    val colors = LocalGoldExColors.current
    val reduced = LocalDesktopReduceMotion.current
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface, border = colors.goldHairlineBorder) {
        ScrollableTabRow(
            selectedTabIndex = selected.ordinal,
            edgePadding = 8.dp,
            containerColor = colors.surface,
            contentColor = colors.goldPrimary,
            modifier = Modifier.fillMaxWidth().testTag("calculator-tab-bar"),
            divider = { HorizontalDivider(color = colors.border) },
            indicator = { positions ->
                val position = positions[selected.ordinal]
                val spec = if (reduced) tween<androidx.compose.ui.unit.Dp>(0)
                    else spring(dampingRatio = .8f, stiffness = Spring.StiffnessMediumLow)
                val offset by animateDpAsState(position.left, spec, label = "calculator-tab-offset")
                val width by animateDpAsState(position.width, spec, label = "calculator-tab-width")
                Box(Modifier.fillMaxWidth().wrapContentSize(Alignment.BottomStart)
                    .offset(x = offset).width(width).padding(horizontal = 16.dp)
                    .height(3.dp).clip(RoundedCornerShape(3.dp)).background(colors.goldPrimary)
                    .testTag("calculator-tab-indicator"))
            }
        ) {
            DesktopCalculatorTab.values().forEach { tab ->
                Tab(
                    selected = selected == tab,
                    onClick = { onSelect(tab) },
                    selectedContentColor = colors.goldPrimary,
                    unselectedContentColor = colors.textMuted,
                    modifier = Modifier.height(60.dp).clip(RoundedCornerShape(12.dp))
                        .testTag("calculator-tab-${tab.name}")
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(tab.icon, null, Modifier.size(20.dp))
                        Text(tab.title, fontFamily = VazirmatnFamily,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = VazirmatnFeatureSettings),
                            fontSize = 14.sp, fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

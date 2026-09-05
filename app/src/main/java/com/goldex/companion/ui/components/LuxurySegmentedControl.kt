package com.goldex.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

/**
 * Sovereign Aurum Animated Segmented Control.
 *
 * Provides a fluid sliding pill indicator with spring physics across mutually exclusive options.
 * Fully supports RTL and avoids any dark or heavy borders.
 */
@Composable
fun <T> LuxurySegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    containerColor: Color? = null,
    activePillColor: Color? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    val colors = LocalGoldExColors.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor ?: colors.surfaceElevated,
        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.35f)),
        modifier = modifier.height(height)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            val totalWidth = maxWidth
            val itemCount = items.size.coerceAtLeast(1)
            val itemWidth = totalWidth / itemCount

            // Calculate target offset respecting RTL layout direction
            val targetOffset = if (isRtl) {
                itemWidth * (itemCount - 1 - selectedIndex)
            } else {
                itemWidth * selectedIndex
            }

            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "segmentedPillOffset"
            )

            // 1. Sliding Active Indicator Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = activePillColor ?: colors.surface,
                border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.5f)),
                shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
            ) {}

            // 2. Interactive Text Buttons
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) colors.goldPrimary else colors.textMuted,
                        label = "segmentedTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onItemSelected(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label(item),
                            fontFamily = VazirmatnFamily,
                            fontSize = fontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

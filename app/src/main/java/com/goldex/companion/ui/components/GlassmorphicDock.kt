package com.goldex.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.theme.LocalGoldExColors

// Lightweight custom vector icons for Coin and Portfolio tabs
internal val DockCoinVector: ImageVector = ImageVector.Builder(
    name = "DockCoin",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 19.5f)
        curveTo(7.86f, 19.5f, 4.5f, 16.14f, 4.5f, 12f)
        curveTo(4.5f, 7.86f, 7.86f, 4.5f, 12f, 4.5f)
        curveTo(16.14f, 4.5f, 19.5f, 7.86f, 19.5f, 12f)
        curveTo(19.5f, 16.14f, 16.14f, 19.5f, 12f, 19.5f)
        close()
    }
}.build()

internal val DockPortfolioVector: ImageVector = ImageVector.Builder(
    name = "DockPortfolio",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(20f, 6f)
        horizontalLineTo(16f)
        verticalLineTo(4f)
        curveTo(16f, 2.89f, 15.11f, 2f, 14f, 2f)
        horizontalLineTo(10f)
        curveTo(8.89f, 2f, 8f, 2.89f, 8f, 4f)
        verticalLineTo(6f)
        horizontalLineTo(4f)
        curveTo(2.89f, 6f, 2f, 6.89f, 2f, 8f)
        verticalLineTo(19f)
        curveTo(2f, 20.11f, 2.89f, 21f, 4f, 21f)
        horizontalLineTo(20f)
        curveTo(21.11f, 21f, 22f, 20.11f, 22f, 19f)
        verticalLineTo(8f)
        curveTo(22f, 6.89f, 21.11f, 6f, 20f, 6f)
        close()
        moveTo(10f, 4f)
        horizontalLineTo(14f)
        verticalLineTo(6f)
        horizontalLineTo(10f)
        verticalLineTo(4f)
        close()
    }
}.build()

/**
 * iOS 26 / Telegram-style Glassmorphic Floating Island Navigation Dock.
 *
 * Features:
 * - Translucent frosted glass container with dual elevation shadow
 * - Specular vertical hairline gradient border simulating overhead ambient luminance
 * - Fluid sliding pill indicator driven by spring physics across the dock track
 * - RTL geometry compensation for seamless Right-to-Left tab indicator gliding
 * - Luxury micro-interactions on active tabs: 1.14x scale bounce, -1.5dp upward lift, smooth tint transition
 */
@Composable
fun GlassmorphicDock(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val tabs = remember { AppTab.values() }
    val tabCount = tabs.size
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dual Elevation Shadow Layer 1: Ambient soft gold atmospheric halo
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.goldPrimary.copy(alpha = if (colors.isDark) 0.14f else 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Dual Elevation Shadow Layer 2 & Frosted Glass Container
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.surface.copy(alpha = if (colors.isDark) 0.78f else 0.86f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (colors.isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.32f),       // Overhead ambient specular highlight
                            colors.goldPrimary.copy(alpha = 0.45f),  // Gilded aurum transition
                            colors.goldSecondary.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.40f)        // Lower shadow falloff
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.95f),       // Overhead white specular sheen
                            colors.goldPrimary.copy(alpha = 0.35f),  // Gilded hairline
                            colors.goldSecondary.copy(alpha = 0.20f),
                            colors.border.copy(alpha = 0.30f)       // Lower perimeter contour
                        )
                    }
                )
            ),
            shadowElevation = if (colors.isDark) 10.dp else 16.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val horizontalPadding = 0.dp
                val availableWidth = maxWidth - (horizontalPadding * 2)
                val tabWidth = availableWidth / tabCount
                val selectedIndex = selectedTab.ordinal.coerceIn(0, tabCount - 1)

                // Precise RTL-aware indicator track offset calculation
                val targetOffset = if (isRtl) {
                    horizontalPadding + ((tabCount - 1 - selectedIndex) * tabWidth)
                } else {
                    horizontalPadding + (selectedIndex * tabWidth)
                }

                // Fluid sliding spring physics indicator glide
                val indicatorOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = 0.76f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "dockSlidingPillGlide"
                )

                // Sliding Glass Pill Indicator Track
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.goldContainer.copy(alpha = if (colors.isDark) 0.50f else 0.65f),
                                    colors.surfaceElevated.copy(alpha = if (colors.isDark) 0.85f else 0.92f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.goldPrimary.copy(alpha = 0.75f),
                                    colors.goldSecondary.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    // Top glowing micro-accent bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.5.dp)
                            .width(18.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.goldPrimary)
                    )
                }

                // Tab Items Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val interactionSource = remember { MutableInteractionSource() }

                        val icon = when (tab) {
                            AppTab.JEWELRY -> Icons.Default.Star
                            AppTab.MELT -> Icons.Default.Build
                            AppTab.COIN -> DockCoinVector
                            AppTab.CONVERT -> Icons.Default.Refresh
                            AppTab.PORTFOLIO -> DockPortfolioVector
                        }

                        // Micro-interaction 1: 1.14x Scale Bounce
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.14f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "dockTabScale"
                        )

                        // Micro-interaction 2: -1.5dp Upward Lift
                        val translationY by animateDpAsState(
                            targetValue = if (isSelected) (-1.5).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dockTabLift"
                        )

                        // Micro-interaction 3: Smooth color transitions
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) colors.goldPrimary else colors.textMuted,
                            animationSpec = tween(durationMillis = 200),
                            label = "dockTabIconTint"
                        )

                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) colors.goldPrimary else colors.textSecondary,
                            animationSpec = tween(durationMillis = 200),
                            label = "dockTabTextColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    onTabSelected(tab)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.offset(y = translationY)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.titleFa,
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(scale)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = tab.titleFa,
                                    fontSize = if (isSelected) 10.5.sp else 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convenient alias matching the naming convention in explorer survey reports.
 */
@Composable
fun GlassmorphicBottomDock(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) = GlassmorphicDock(
    selectedTab = selectedTab,
    onTabSelected = onTabSelected,
    modifier = modifier
)

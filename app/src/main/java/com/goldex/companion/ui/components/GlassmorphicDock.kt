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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.theme.LocalGoldExColors

// Lightweight custom vector icons for 5 Stitch Navigation Destinations
internal val DockHomeVector: ImageVector = ImageVector.Builder(
    name = "DockHome",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(2.25f, 12f)
        lineTo(11.2f, 3.05f)
        curveTo(11.64f, 2.61f, 12.36f, 2.61f, 12.8f, 3.05f)
        lineTo(21.75f, 12f)
        moveTo(4.5f, 9.75f)
        verticalLineTo(19.88f)
        curveTo(4.5f, 20.5f, 5f, 21f, 5.62f, 21f)
        horizontalLineTo(9.75f)
        verticalLineTo(16.12f)
        curveTo(9.75f, 15.5f, 10.25f, 15f, 10.88f, 15f)
        horizontalLineTo(13.12f)
        curveTo(13.75f, 15f, 14.25f, 15.5f, 14.25f, 16.12f)
        verticalLineTo(21f)
        horizontalLineTo(18.38f)
        curveTo(19f, 21f, 19.5f, 20.5f, 19.5f, 19.88f)
        verticalLineTo(9.75f)
    }
}.build()

internal val DockRatesVector: ImageVector = ImageVector.Builder(
    name = "DockRates",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(2.25f, 18f)
        lineTo(9f, 11.25f)
        lineTo(13.3f, 15.55f)
        curveTo(15.2f, 13.65f, 17.5f, 12f, 19.1f, 10f)
        lineTo(21.75f, 8.8f)
        moveTo(16f, 8.8f)
        horizontalLineTo(21.75f)
        verticalLineTo(14.5f)
    }
}.build()

internal val DockCalculatorVector: ImageVector = ImageVector.Builder(
    name = "DockCalculator",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6.75f, 3.5f)
        horizontalLineTo(17.25f)
        curveTo(18.5f, 3.5f, 19.5f, 4.5f, 19.5f, 5.75f)
        verticalLineTo(19.25f)
        curveTo(19.5f, 20.5f, 18.5f, 21.5f, 17.25f, 21.5f)
        horizontalLineTo(6.75f)
        curveTo(5.5f, 21.5f, 4.5f, 20.5f, 4.5f, 19.25f)
        verticalLineTo(5.75f)
        curveTo(4.5f, 4.5f, 5.5f, 3.5f, 6.75f, 3.5f)
        close()
        moveTo(7.5f, 7f)
        horizontalLineTo(16.5f)
        moveTo(8f, 11f); horizontalLineTo(8.01f)
        moveTo(12f, 11f); horizontalLineTo(12.01f)
        moveTo(16f, 11f); horizontalLineTo(16.01f)
        moveTo(8f, 14.5f); horizontalLineTo(8.01f)
        moveTo(12f, 14.5f); horizontalLineTo(12.01f)
        moveTo(16f, 14.5f); horizontalLineTo(16.01f)
        moveTo(8f, 18f); horizontalLineTo(8.01f)
        moveTo(12f, 18f); horizontalLineTo(12.01f)
        moveTo(16f, 18f); horizontalLineTo(16.01f)
    }
}.build()

internal val DockInvoiceVector: ImageVector = ImageVector.Builder(
    name = "DockInvoice",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19.5f, 14.25f)
        verticalLineTo(11.62f)
        curveTo(19.5f, 9.76f, 18f, 8.25f, 16.12f, 8.25f)
        horizontalLineTo(14.62f)
        curveTo(14f, 8.25f, 13.5f, 7.75f, 13.5f, 7.12f)
        verticalLineTo(5.62f)
        curveTo(13.5f, 3.76f, 12f, 2.25f, 10.12f, 2.25f)
        horizontalLineTo(8.25f)
        moveTo(8.25f, 15f)
        horizontalLineTo(15.75f)
        moveTo(8.25f, 18f)
        horizontalLineTo(12f)
        moveTo(10.5f, 2.25f)
        horizontalLineTo(5.62f)
        curveTo(4.45f, 2.25f, 3.5f, 3.2f, 3.5f, 4.38f)
        verticalLineTo(19.62f)
        curveTo(3.5f, 20.8f, 4.45f, 21.75f, 5.62f, 21.75f)
        horizontalLineTo(18.38f)
        curveTo(19.55f, 21.75f, 20.5f, 20.8f, 20.5f, 19.62f)
        verticalLineTo(12.25f)
    }
}.build()

internal val DockMoreVector: ImageVector = ImageVector.Builder(
    name = "DockMore",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3.75f, 6.75f)
        horizontalLineTo(20.25f)
        moveTo(3.75f, 12f)
        horizontalLineTo(20.25f)
        moveTo(3.75f, 17.25f)
        horizontalLineTo(20.25f)
    }
}.build()

/**
 * iOS 26 / Telegram-style Glassmorphic Floating Island Navigation Dock.
 *
 * Features:
 * - 5 Sovereign Destinations: [خانه (Home), تابلوی مظنه (Rates), ماشین‌حساب (Calculator), فاکتورها (Invoices), بیشتر (More)]
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

    // Ordered from Left to Right in LTR container so Persian RTL user sees:
    // [Far Left: بیشتر (More)] [فاکتورها (Invoices)] [ماشین‌حساب (Calculator)] [تابلوی مظنه (Rates)] [Far Right: خانه (Home)]
    val displayTabs = remember {
        listOf(
            AppTab.MORE,
            AppTab.INVOICES,
            AppTab.CALCULATOR,
            AppTab.RATES,
            AppTab.HOME
        )
    }
    val tabCount = displayTabs.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 2.dp),
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
                            colors.goldPrimary.copy(alpha = if (colors.isDark) 0.16f else 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Frosted Glassmorphism Container
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (colors.isDark) {
                Color(0xCC111624) // 80% opacity dark frosted glass
            } else {
                Color(0xDDF4F5F9) // 86% opacity light frosted pearl glass
            },
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    if (colors.isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.45f),        // Crisp overhead specular reflection
                            colors.goldPrimary.copy(alpha = 0.40f), // Gilded hairline
                            Color.White.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.35f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.98f),        // Frosted specular sheen
                            colors.goldPrimary.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.06f)
                        )
                    }
                )
            ),
            shadowElevation = if (colors.isDark) 14.dp else 18.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Force LTR coordinate system for 100% deterministic sliding indicator offset
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    val horizontalPadding = 0.dp
                    val availableWidth = maxWidth - (horizontalPadding * 2)
                    val tabWidth = availableWidth / tabCount
                    val selectedSlotIndex = displayTabs.indexOf(selectedTab).coerceIn(0, tabCount - 1)
                    val targetOffset = horizontalPadding + (tabWidth * selectedSlotIndex)

                    // Fluid sliding spring physics indicator glide
                    val indicatorOffset by animateDpAsState(
                        targetValue = targetOffset,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
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
                                    if (colors.isDark) {
                                        listOf(
                                            colors.goldContainer.copy(alpha = 0.45f),
                                            colors.surfaceElevated.copy(alpha = 0.70f)
                                        )
                                    } else {
                                        listOf(
                                            colors.goldContainer.copy(alpha = 0.55f),
                                            Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        colors.goldPrimary.copy(alpha = 0.70f),
                                        colors.goldSecondary.copy(alpha = 0.25f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        // Bottom glowing indicator bar (خط هاور زیرین بر اساس درخواست کاربر)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.5.dp)
                                .width(22.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            colors.goldSecondary,
                                            colors.goldPrimary,
                                            colors.goldSecondary
                                        )
                                    )
                                )
                        )
                    }

                    // Tab Items Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displayTabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val interactionSource = remember { MutableInteractionSource() }

                            val icon = when (tab) {
                                AppTab.HOME -> DockHomeVector
                                AppTab.RATES -> DockRatesVector
                                AppTab.CALCULATOR -> DockCalculatorVector
                                AppTab.INVOICES -> DockInvoiceVector
                                AppTab.MORE -> DockMoreVector
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

package com.goldex.companion.ui.calculator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.ui.calculator.tabs.*
import com.goldex.companion.ui.components.LiveRatesTicker
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient

private val CoinVector: ImageVector = ImageVector.Builder(
    name = "Coin",
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

private val PortfolioVector: ImageVector = ImageVector.Builder(
    name = "Portfolio",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(
    viewModel: GoldCalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.goldBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = "گلدکس پرو",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = colors.textMain
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colors.goldContainer)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colors.goldPrimary
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(colors.profitGreen)
                                        )
                                        Text(
                                            text = "نرخ لحظه‌ای و دستیار معاملات",
                                            fontSize = 10.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Dark / Light Theme Toggle Button
                            IconButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceElevated)
                            ) {
                                Text(
                                    text = if (colors.isDark) "☀️" else "🌙",
                                    fontSize = 16.sp
                                )
                            }

                            // Refresh Rates Button
                            IconButton(onClick = { viewModel.refreshRates() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "بروزرسانی مظنه",
                                    tint = colors.goldSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .then(if (uiState.isRefreshingRates) Modifier.rotate(rotation) else Modifier)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface,
                            titleContentColor = colors.goldPrimary
                        )
                    )

                    // Subtle hairline gold bottom border for TopBar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.goldGradient)
                    )
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp, top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
                        shadowElevation = if (colors.isDark) 4.dp else 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            modifier = Modifier.height(64.dp)
                        ) {
                            AppTab.values().forEach { tab ->
                                val isSelected = uiState.selectedTab == tab
                                val icon = when (tab) {
                                    AppTab.JEWELRY -> Icons.Default.Star
                                    AppTab.MELT -> Icons.Default.Build
                                    AppTab.COIN -> CoinVector
                                    AppTab.CONVERT -> Icons.Default.Refresh
                                    AppTab.PORTFOLIO -> PortfolioVector
                                }

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = tab.titleFa,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.titleFa,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.goldPrimary,
                                        selectedTextColor = colors.goldPrimary,
                                        unselectedIconColor = colors.textMuted,
                                        unselectedTextColor = colors.textSecondary,
                                        indicatorColor = colors.goldContainer
                                    )
                                )
                            }
                        }
                    }
                }
            },
            containerColor = colors.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveRatesTicker(
                    rates = uiState.rates,
                    isRefreshing = uiState.isRefreshingRates,
                    onRefresh = { viewModel.refreshRates() },
                    onToggleSource = { viewModel.togglePriceSource() }
                )

                // Tab Contents with smooth AnimatedContent slide & fade transitions
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        val isForward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { fullWidth -> if (isForward) -fullWidth / 4 else fullWidth / 4 } + fadeIn(
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        )).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { fullWidth -> if (isForward) fullWidth / 4 else -fullWidth / 4 } + fadeOut(
                                animationSpec = tween(180, easing = FastOutLinearInEasing)
                            )
                        )
                    },
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                        AppTab.JEWELRY -> JewelryTab(viewModel, uiState)
                        AppTab.MELT -> MeltTab(viewModel, uiState)
                        AppTab.COIN -> CoinBubbleTab(viewModel, uiState)
                        AppTab.CONVERT -> KaratConvertTab(viewModel, uiState)
                        AppTab.PORTFOLIO -> PortfolioTab(uiState)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

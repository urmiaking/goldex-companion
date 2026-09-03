package com.goldex.companion.ui.calculator

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.ui.calculator.tabs.*
import com.goldex.companion.ui.components.LiveRatesTicker
import com.goldex.companion.ui.theme.LocalGoldExColors

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
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.goldContainer)
                                    .border(0.8.dp, colors.goldBorder, CircleShape),
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
                                Text(
                                    text = "همراه گلدکس",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = colors.goldPrimary
                                )
                                Text(
                                    text = "دستیار تخصصی طلا، مسکوکات و ارز",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
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
            },
            bottomBar = {
                Surface(
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.border),
                    shadowElevation = if (colors.isDark) 0.dp else 4.dp
                ) {
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(70.dp)
                    ) {
                        AppTab.values().forEach { tab ->
                            val isSelected = uiState.selectedTab == tab
                            val icon = when (tab) {
                                AppTab.JEWELRY -> Icons.Default.Star
                                AppTab.MELT -> Icons.Default.Build
                                AppTab.COIN -> Icons.Default.AccountBalance
                                AppTab.CONVERT -> Icons.Default.SwapHoriz
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tab.titleFa,
                                        modifier = Modifier.size(21.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.titleFa,
                                        fontSize = 11.sp,
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

                // Tab Contents
                when (uiState.selectedTab) {
                    AppTab.JEWELRY -> JewelryTab(viewModel, uiState)
                    AppTab.MELT -> MeltTab(viewModel, uiState)
                    AppTab.COIN -> CoinBubbleTab(viewModel, uiState)
                    AppTab.CONVERT -> KaratConvertTab(viewModel, uiState)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

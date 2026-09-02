package com.goldex.companion.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.ui.calculator.tabs.*
import com.goldex.companion.ui.components.LiveRatesTicker
import com.goldex.companion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(
    viewModel: GoldCalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer)
                                    .border(1.dp, GoldBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "گلدکس پرو",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "دستیار محاسبات طلا و مظنه زنده",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBg,
                        titleContentColor = GoldPrimary
                    )
                )
            },
            containerColor = DarkBg
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

                // Minimalist Capsule Segmented Tab Bar
                MinimalSegmentedTabBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
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

@Composable
fun MinimalSegmentedTabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val background = if (isSelected) GoldPrimary.copy(alpha = 0.16f) else Color.Transparent
                val borderColor = if (isSelected) GoldPrimary.copy(alpha = 0.6f) else Color.Transparent
                val contentColor = if (isSelected) GoldPrimary else TextSecondary
                val weight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(background)
                        .border(if (isSelected) 0.8.dp else 0.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.titleFa,
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = weight,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

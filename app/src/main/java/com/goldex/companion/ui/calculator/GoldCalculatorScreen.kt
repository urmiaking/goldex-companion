package com.goldex.companion.ui.calculator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.ui.calculator.tabs.*
import com.goldex.companion.ui.components.AddCustomerDialog
import com.goldex.companion.ui.components.CustomerIconVector
import com.goldex.companion.ui.components.CustomerPickerDialog
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
    val context = LocalContext.current

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

    // Customer Management Modal from Top Bar
    if (uiState.isCustomerManagerVisible) {
        CustomerPickerDialog(
            customers = uiState.customerList,
            selectedCustomer = uiState.selectedCustomer,
            onSelectCustomer = {
                viewModel.selectCustomer(it)
                viewModel.setCustomerManagerVisible(false)
            },
            onAddNewCustomerClick = { viewModel.setAddCustomerDialogVisible(true) },
            onDeleteCustomer = { viewModel.deleteCustomer(context, it) },
            onUpdateCustomer = { viewModel.updateCustomer(context, it) },
            onDismiss = { viewModel.setCustomerManagerVisible(false) }
        )
    }

    if (uiState.isAddCustomerDialogVisible) {
        AddCustomerDialog(
            onDismiss = { viewModel.setAddCustomerDialogVisible(false) },
            onSaveCustomer = { viewModel.addCustomer(context, it, autoSelect = true) }
        )
    }

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
                                            text = "نرخ لحظه‌ای و دستیار فاکتور طلا",
                                            fontSize = 10.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Customer Management Button in TopBar
                            IconButton(
                                onClick = {
                                    viewModel.loadCustomers(context)
                                    viewModel.setCustomerManagerVisible(true)
                                },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceElevated)
                            ) {
                                Icon(
                                    imageVector = CustomerIconVector,
                                    contentDescription = "مدیریت مشتریان",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

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
                // Enhanced Luxury Floating Island Dock Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = colors.surface.copy(alpha = if (colors.isDark) 0.96f else 0.98f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    colors.goldSecondary.copy(alpha = 0.35f),
                                    colors.goldPrimary.copy(alpha = 0.75f),
                                    colors.goldSecondary.copy(alpha = 0.35f)
                                )
                            )
                        ),
                        shadowElevation = if (colors.isDark) 8.dp else 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
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

                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.12f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "tabScale"
                                )

                                val indicatorWidth by animateDpAsState(
                                    targetValue = if (isSelected) 16.dp else 0.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "indicatorWidth"
                                )

                                val interactionSource = remember { MutableInteractionSource() }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.verticalGradient(
                                                    listOf(
                                                        colors.goldContainer.copy(alpha = if (colors.isDark) 0.40f else 0.50f),
                                                        colors.surfaceElevated
                                                    )
                                                )
                                            } else {
                                                SolidColor(Color.Transparent)
                                            }
                                        )
                                        .border(
                                            if (isSelected) 0.8.dp else 0.dp,
                                            if (isSelected) colors.goldPrimary.copy(alpha = 0.6f) else Color.Transparent,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            viewModel.selectTab(tab)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        // Active top glowing micro-indicator dot
                                        Box(
                                            modifier = Modifier
                                                .width(indicatorWidth)
                                                .height(2.5.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (isSelected) colors.goldPrimary else Color.Transparent)
                                        )

                                        Icon(
                                            imageVector = icon,
                                            contentDescription = tab.titleFa,
                                            tint = if (isSelected) colors.goldPrimary else colors.textMuted,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .scale(scale)
                                        )

                                        Text(
                                            text = tab.titleFa,
                                            fontSize = if (isSelected) 10.5.sp else 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = colors.background
        ) { paddingValues ->
            val scrollState = rememberScrollState()
            LaunchedEffect(uiState.selectedTab) {
                scrollState.scrollTo(0)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
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
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { fullWidth -> if (isForward) -fullWidth / 4 else fullWidth / 4 } + fadeIn(
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
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
                    label = "tabTransition",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
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

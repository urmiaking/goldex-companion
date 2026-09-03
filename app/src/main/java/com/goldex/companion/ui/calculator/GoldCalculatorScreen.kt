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
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient
import kotlinx.coroutines.launch

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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // In-App Auto-Update Dialog Prompt
    uiState.updateInfo?.let { info ->
        if (info.isAvailable && !uiState.isUpdateDialogDismissed) {
            UpdateDialog(
                updateInfo = info,
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }
    }

    // Customer Management Modal from Top Bar / Drawer
    if (uiState.isCustomerManagerVisible) {
        CustomerPickerDialog(
            customers = uiState.customerList,
            selectedCustomer = uiState.selectedCustomer,
            onSelectCustomer = {
                viewModel.selectCustomer(it)
                viewModel.setCustomerManagerVisible(false)
            },
            onAddNewCustomerClick = { viewModel.setAddCustomerDialogVisible(true) },
            onDeleteCustomer = { viewModel.deleteCustomer(it) },
            onUpdateCustomer = { viewModel.updateCustomer(it) },
            onDismiss = { viewModel.setCustomerManagerVisible(false) }
        )
    }

    if (uiState.isAddCustomerDialogVisible) {
        AddCustomerDialog(
            onDismiss = { viewModel.setAddCustomerDialogVisible(false) },
            onSaveCustomer = { viewModel.addCustomer(it, autoSelect = true) }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LuxuryDrawer(
            drawerState = drawerState,
            rates = uiState.rates,
            customerCount = uiState.customerList.size,
            onNavigateCustomers = {
                viewModel.loadCustomers()
                viewModel.setCustomerManagerVisible(true)
            },
            onNavigateSettings = {
                viewModel.toggleTheme()
            },
            onCheckForUpdates = {
                viewModel.checkForUpdates(manual = true)
            }
        ) {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "منوی اصلی",
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(colors.profitGreen)
                                    )
                                }
                            },
                            actions = {
                                // Dark / Light Theme Toggle Button
                                IconButton(
                                    onClick = { viewModel.toggleTheme() },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated)
                                ) {
                                    Text(
                                        text = if (colors.isDark) "☀️" else "🌙",
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Refresh Rates Button
                                IconButton(
                                    onClick = { viewModel.refreshRates() },
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "بروزرسانی مظنه",
                                        tint = colors.goldSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
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
                containerColor = colors.background
            ) { paddingValues ->
                val scrollState = rememberScrollState()
                LaunchedEffect(uiState.selectedTab) {
                    scrollState.scrollTo(0)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
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
                                AppTab.PORTFOLIO -> PortfolioTab(viewModel, uiState)
                            }
                        }

                        // Clearance spacer so content scrolls completely above the floating dock
                        Spacer(modifier = Modifier.height(105.dp))
                    }

                    // Floating Glassmorphic Dock pinned to bottom center
                    GlassmorphicDock(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
    }
}
}

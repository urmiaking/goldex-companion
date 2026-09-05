package com.goldex.companion.ui.calculator

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.R
import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.ui.calculator.tabs.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.dashboard.DashboardScreen
import com.goldex.companion.ui.hub.JewelerProfileModal
import com.goldex.companion.ui.hub.MoreHubScreen
import com.goldex.companion.ui.hub.PriceSourceModal
import com.goldex.companion.ui.hub.StandardFormulasScreen
import com.goldex.companion.ui.hub.TaxProfitModal
import com.goldex.companion.ui.rates.LiveRatesScreen
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.goldGradient

enum class CalculatorSubTab(val titleFa: String) {
    JEWELRY("طلا و جواهر"),
    MELT("مظنه آبشده"),
    COIN("حباب سکه"),
    CONVERT("تبدیل عیار")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(
    viewModel: GoldCalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalGoldExColors.current
    val context = LocalContext.current

    // Sub-tab selection for Calculator destination
    var calcSubTab by remember { mutableStateOf(CalculatorSubTab.JEWELRY) }

    // In-App Auto-Update Dialog Prompt
    uiState.updateInfo?.let { info ->
        if (info.isAvailable && !uiState.isUpdateDialogDismissed) {
            UpdateDialog(
                updateInfo = info,
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }
    }

    // Customer Management Modal from Top Bar / Hub
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

    // Tax & Profit Configuration Bottom Sheet Modal
    if (uiState.isTaxProfitModalVisible) {
        TaxProfitModal(
            settings = uiState.appSettings,
            onDismiss = { viewModel.setTaxProfitModalVisible(false) },
            onSave = { profit, tax, wageType ->
                viewModel.updateTaxAndProfit(profit, tax, wageType)
                Toast.makeText(context, "سود مصوب و مالیات با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Price Source & Live Rates Bottom Sheet Modal
    if (uiState.isPriceSourceModalVisible) {
        PriceSourceModal(
            settings = uiState.appSettings,
            onDismiss = { viewModel.setPriceSourceModalVisible(false) },
            onSave = { source, autoSync ->
                viewModel.updatePriceSource(source, autoSync)
                Toast.makeText(context, "مرجع قیمت‌ها با موفقیت همگام‌سازی شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Invoice Manager & Archive Modal
    if (uiState.isInvoiceManagerVisible) {
        InvoiceManagerDialog(
            invoices = uiState.savedInvoices,
            settings = uiState.appSettings,
            onDismiss = { viewModel.setInvoiceManagerVisible(false) },
            onDeleteInvoice = { viewModel.deleteInvoice(it) }
        )
    }

    // Jeweler Profile Modal (Stitch ID: 4457d74b46974ee99ffc049b24feb860)
    if (uiState.isJewelerProfileModalVisible) {
        JewelerProfileModal(
            settings = uiState.appSettings,
            onDismiss = { viewModel.setJewelerProfileModalVisible(false) },
            onSaveProfile = { galleryName, managerName, unionCode, phone, address ->
                viewModel.updateJewelerProfile(galleryName, managerName, unionCode, phone, address)
                Toast.makeText(context, "اطلاعات بنکداری و پروانه زرگری ذخیره شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Qirat Official App Emblem
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F141C))
                                        .border(
                                            width = 0.8.dp,
                                            color = colors.goldBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = "آیکن برنامه قیراط",
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.padding(top = 3.dp),
                                    verticalArrangement = Arrangement.spacedBy((-2).dp)
                                ) {
                                    Text(
                                        text = "قیراط",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.5.sp,
                                        lineHeight = 22.sp,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = "دستیار جامع محاسبات و فاکتور طلا",
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        },
                        actions = {
                            // Notification Bell with Golden Live Dot
                            val statusColor = when (uiState.connectionStatus) {
                                ConnectionStatus.ONLINE -> colors.profitGreen
                                ConnectionStatus.CONNECTING -> Color(0xFFF59E0B)
                                ConnectionStatus.OFFLINE -> colors.errorRed
                            }

                            Surface(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surface,
                                border = BorderStroke(0.6.dp, colors.goldBorder),
                                shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "نگارش ۲.۴.۰ پرو • اتصال به شبکه طلا و جواهر برقرار است",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "اعلان‌ها",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = 8.dp, y = 8.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.surface)
                                            .padding(1.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface,
                            titleContentColor = colors.textMain
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
                    // Live Rates Ticker (Visible across app for live market intelligence)
                    LiveRatesTicker(
                        rates = uiState.rates,
                        isRefreshing = uiState.isRefreshingRates,
                        onRefresh = { viewModel.refreshRates() },
                        onToggleSource = { viewModel.togglePriceSource() }
                    )

                    // 5 Main System Destinations via AnimatedContent
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
                        label = "mainDestinationTransition",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                    ) { destination ->
                        when (destination) {
                            AppTab.HOME -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    onNavigateCalculator = {
                                        calcSubTab = CalculatorSubTab.JEWELRY
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    },
                                    onNavigateInvoices = {
                                        viewModel.loadInvoices()
                                        viewModel.setInvoiceManagerVisible(true)
                                    },
                                    onNavigateConvert = {
                                        calcSubTab = CalculatorSubTab.CONVERT
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    },
                                    onNavigateCoinBubble = {
                                        calcSubTab = CalculatorSubTab.COIN
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    },
                                    onNavigateMelt = {
                                        calcSubTab = CalculatorSubTab.MELT
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    },
                                    onNavigateLedger = {
                                        viewModel.loadCustomers()
                                        viewModel.setCustomerManagerVisible(true)
                                    }
                                )
                            }

                            AppTab.RATES -> {
                                LiveRatesScreen(
                                    uiState = uiState,
                                    onRefresh = { viewModel.refreshRates() },
                                    onNavigateCalculator = {
                                        calcSubTab = CalculatorSubTab.JEWELRY
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    }
                                )
                            }

                            AppTab.CALCULATOR -> {
                                // Calculator Destination with Specialized Sub-tabs Segmented Control
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Segmented Control Pill Row
                                    val subTabScrollState = rememberScrollState()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(subTabScrollState),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CalculatorSubTab.values().forEach { subTab ->
                                            val isSelected = calcSubTab == subTab
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surface,
                                                border = BorderStroke(
                                                    width = 0.8.dp,
                                                    color = if (isSelected) colors.goldPrimary else colors.border
                                                ),
                                                modifier = Modifier.clickable { calcSubTab = subTab }
                                            ) {
                                                Text(
                                                    text = subTab.titleFa,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Render selected calculator engine
                                    when (calcSubTab) {
                                        CalculatorSubTab.JEWELRY -> JewelryTab(viewModel, uiState)
                                        CalculatorSubTab.MELT -> MeltTab(viewModel, uiState)
                                        CalculatorSubTab.COIN -> CoinBubbleTab(viewModel, uiState)
                                        CalculatorSubTab.CONVERT -> KaratConvertTab(viewModel, uiState)
                                    }
                                }
                            }

                            AppTab.INVOICES -> {
                                // Phase 3 Preview: سامانه فاکتورها (Invoices)
                                InvoicesTabPreviewCard(
                                    uiState = uiState,
                                    onOpenInvoiceManager = {
                                        viewModel.loadInvoices()
                                        viewModel.setInvoiceManagerVisible(true)
                                    }
                                )
                            }

                            AppTab.MORE -> {
                                // Phase 1 Hub Screen (Stitch ID: a4fb5e02179f4ce0b4ac213ee29bca16)
                                MoreHubScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    onNavigateLedger = {
                                        viewModel.loadCustomers()
                                        viewModel.setCustomerManagerVisible(true)
                                    },
                                    onNavigateMelt = {
                                        calcSubTab = CalculatorSubTab.MELT
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    },
                                    onNavigateWorkshop = {
                                        Toast.makeText(context, "سامانه سفارشات و کارگاه در فاز ۴ فعال خواهد شد", Toast.LENGTH_SHORT).show()
                                    },
                                    onNavigateInventory = {
                                        Toast.makeText(context, "سامانه انبارداری و موجودی در فاز ۴ فعال خواهد شد", Toast.LENGTH_SHORT).show()
                                    },
                                    onNavigateConvert = {
                                        calcSubTab = CalculatorSubTab.CONVERT
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    },
                                    onNavigateCoinBubble = {
                                        calcSubTab = CalculatorSubTab.COIN
                                        viewModel.selectTab(AppTab.CALCULATOR)
                                    },
                                    onNavigateInvoices = {
                                        viewModel.loadInvoices()
                                        viewModel.setInvoiceManagerVisible(true)
                                    },
                                    onOpenTaxProfitModal = {
                                        viewModel.setTaxProfitModalVisible(true)
                                    },
                                    onOpenPriceSourceModal = {
                                        viewModel.setPriceSourceModalVisible(true)
                                    },
                                    onOpenJewelerProfile = {
                                        viewModel.setJewelerProfileModalVisible(true)
                                    },
                                    onNavigateStandardFormulas = {
                                        viewModel.setStandardFormulasVisible(true)
                                    }
                                )
                            }
                        }
                    }

                    // Clearance spacer so content scrolls cleanly above the floating dock
                    Spacer(modifier = Modifier.height(78.dp))
                }

                // Floating Glassmorphic Dock pinned to bottom center
                GlassmorphicDock(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Gold Union Standard Formulas Guide (Stitch Screen ID: 1e8173ae11924cad8cabf7f74a1c042b)
        AnimatedVisibility(
            visible = uiState.isStandardFormulasVisible,
            enter = LuxuryMotion.ScreenPushEnter,
            exit = LuxuryMotion.ScreenPopExit,
            modifier = Modifier.fillMaxSize()
        ) {
            StandardFormulasScreen(
                onBack = { viewModel.setStandardFormulasVisible(false) },
                onNavigateCalculator = {
                    viewModel.setStandardFormulasVisible(false)
                    viewModel.selectTab(AppTab.CALCULATOR)
                }
            )
        }
    }
}
}


@Composable
private fun InvoicesTabPreviewCard(
    uiState: CalculatorUiState,
    onOpenInvoiceManager: () -> Unit
) {
    val colors = LocalGoldExColors.current

    LuxuryCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدیریت و بایگانی فاکتورها",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.goldContainer.copy(alpha = 0.4f),
                    border = BorderStroke(0.6.dp, colors.goldBorder)
                ) {
                    Text(
                        text = "پیش‌نمایش فاز ۳",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "تعداد فاکتورهای ذخیره‌شده: ${uiState.savedInvoices.size} فقره",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Button(
                onClick = onOpenInvoiceManager,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.goldPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = DockInvoiceVector, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "مشاهده بایگانی فاکتورها و چاپ PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

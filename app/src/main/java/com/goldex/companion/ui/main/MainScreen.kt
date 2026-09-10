package com.goldex.companion.ui.main

import android.app.Application
import android.widget.Toast
import com.goldex.companion.ui.components.QiratoToast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
import com.goldex.companion.model.*
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.calculator.KaratConvertViewModel
import com.goldex.companion.ui.calculator.screens.CoinBubbleScreen
import com.goldex.companion.ui.calculator.screens.KaratConvertScreen
import com.goldex.companion.ui.calculator.screens.MeltCalcScreen
import com.goldex.companion.ui.calculator.tabs.JewelryTab
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.dashboard.DashboardScreen
import com.goldex.companion.ui.dashboard.DashboardUiState
import com.goldex.companion.ui.hub.JewelerProfileModal
import com.goldex.companion.ui.hub.MoreHubScreen
import com.goldex.companion.ui.hub.PriceSourceModal
import com.goldex.companion.ui.hub.StandardFormulasScreen
import com.goldex.companion.ui.hub.TaxProfitModal
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.ui.invoices.BarterInvoiceScreen
import com.goldex.companion.ui.invoices.BarterInvoiceViewModel
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import com.goldex.companion.ui.invoices.CustomerManagerViewModelFactory
import com.goldex.companion.ui.invoices.FloatingNewInvoiceButton
import com.goldex.companion.ui.invoices.InvoicesManagementScreen
import com.goldex.companion.ui.invoices.InvoicesSubScreen
import com.goldex.companion.ui.invoices.InvoiceManagerViewModel
import com.goldex.companion.ui.invoices.InvoiceManagerViewModelFactory
import com.goldex.companion.ui.portfolio.PortfolioManagerViewModel
import com.goldex.companion.ui.portfolio.PortfolioManagerViewModelFactory
import com.goldex.companion.ui.rates.LiveRatesScreen
import com.goldex.companion.ui.rates.MarketRatesUiState
import com.goldex.companion.ui.settings.SettingsViewModel
import com.goldex.companion.ui.settings.SettingsViewModelFactory
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.update.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val customerViewModel: CustomerManagerViewModel = viewModel(factory = CustomerManagerViewModelFactory(app))
    val invoiceViewModel: InvoiceManagerViewModel = viewModel(factory = InvoiceManagerViewModelFactory(app))
    val portfolioViewModel: PortfolioManagerViewModel = viewModel(factory = PortfolioManagerViewModelFactory(app))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(app))
    val updateViewModel: UpdateViewModel = viewModel()
    val karatConvertViewModel: KaratConvertViewModel = viewModel()
    val barterInvoiceViewModel: BarterInvoiceViewModel = viewModel()

    val mainUiState by mainViewModel.uiState.collectAsState()
    val customerState by customerViewModel.uiState.collectAsState()
    val invoiceState by invoiceViewModel.uiState.collectAsState()
    @Suppress("UNUSED_VARIABLE")
    val portfolioState by portfolioViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()
    val karatConvertUiState by karatConvertViewModel.uiState.collectAsState()
    val barterUiState by barterInvoiceViewModel.uiState.collectAsState()

    val colors = LocalGoldExColors.current

    // In-App Auto-Update Check & Dialog Prompt
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates(manual = false)
    }

    // Sync live market rate or input spot price with barter invoice spot price
    LaunchedEffect(mainUiState.rates.gold18, mainUiState.spotPriceInput, mainUiState.priceBasisTab) {
        val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
            PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
            mainUiState.priceBasisTab
        ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L
        barterInvoiceViewModel.setLiveRate(currentSpot)
    }

    updateState.updateInfo?.let { info ->
        if (info.isAvailable && !updateState.isUpdateDialogDismissed) {
            UpdateDialog(
                updateInfo = info,
                onDismiss = { updateViewModel.dismissUpdateDialog() }
            )
        }
    }

    // Customer Management Modal from Top Bar / Hub
    if (customerState.isCustomerManagerVisible) {
        CustomerPickerDialog(
            customers = customerState.customerList,
            selectedCustomer = customerState.selectedCustomer,
            onSelectCustomer = {
                customerViewModel.selectCustomer(it)
                mainViewModel.setSelectedCustomer(it)
                barterInvoiceViewModel.setCustomer(it)
                customerViewModel.setCustomerManagerVisible(false)
            },
            onAddNewCustomerClick = { customerViewModel.setAddCustomerDialogVisible(true) },
            onDeleteCustomer = { customerViewModel.deleteCustomer(it) },
            onUpdateCustomer = { customerViewModel.updateCustomer(it) },
            onDismiss = { customerViewModel.setCustomerManagerVisible(false) }
        )
    }

    if (customerState.isAddCustomerDialogVisible) {
        AddCustomerDialog(
            onDismiss = { customerViewModel.setAddCustomerDialogVisible(false) },
            onSaveCustomer = {
                customerViewModel.addCustomer(it, autoSelect = true)
                mainViewModel.setSelectedCustomer(it)
                barterInvoiceViewModel.setCustomer(it)
            }
        )
    }

    // Tax & Profit Configuration Bottom Sheet Modal
    if (settingsState.isTaxProfitModalVisible) {
        TaxProfitModal(
            settings = settingsState.appSettings,
            onDismiss = { settingsViewModel.setTaxProfitModalVisible(false) },
            onSave = { profit, tax, wageType ->
                settingsViewModel.updateTaxAndProfit(profit, tax, wageType)
                mainViewModel.applySettingsDefaults(profit, tax, wageType)
                QiratoToast.show(context, "سود مصوب و مالیات با موفقیت ذخیره شد")
            }
        )
    }

    // Price Source & Live Rates Bottom Sheet Modal
    if (settingsState.isPriceSourceModalVisible) {
        PriceSourceModal(
            settings = settingsState.appSettings,
            onDismiss = { settingsViewModel.setPriceSourceModalVisible(false) },
            onSave = { source, autoSync ->
                settingsViewModel.updatePriceSource(source, autoSync)
                mainViewModel.updatePriceSource(source, autoSync)
                QiratoToast.show(context, "مرجع قیمت‌ها با موفقیت همگام‌سازی شد")
            }
        )
    }

    // Invoice Manager & Archive Modal
    if (invoiceState.isInvoiceManagerVisible) {
        InvoiceManagerDialog(
            invoices = invoiceState.savedInvoices,
            settings = settingsState.appSettings,
            onDismiss = { invoiceViewModel.setInvoiceManagerVisible(false) },
            onDeleteInvoice = { invoiceViewModel.deleteInvoice(it) }
        )
    }

    // Jeweler Profile Modal
    if (settingsState.isJewelerProfileModalVisible) {
        JewelerProfileModal(
            settings = settingsState.appSettings,
            onDismiss = { settingsViewModel.setJewelerProfileModalVisible(false) },
            onSaveProfile = { galleryName, managerName, unionCode, phone, address ->
                settingsViewModel.updateJewelerProfile(galleryName, managerName, unionCode, phone, address)
                QiratoToast.show(context, "اطلاعات بنکداری و پروانه زرگری ذخیره شد")
            }
        )
    }

    if (mainUiState.selectedTab == AppTab.INVOICES && barterUiState.subScreen == InvoicesSubScreen.EDITOR) {
        BackHandler {
            barterInvoiceViewModel.navigateBackToList()
        }
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
                                            painter = painterResource(id = R.drawable.ic_logo_raw),
                                            contentDescription = "آیکن برنامه قیراط",
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.padding(top = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.text_persian),
                                            contentDescription = "قیراط",
                                            modifier = Modifier.height(23.dp)
                                        )
                                        Text(
                                            text = "دستیار جامع محاسبات و فاکتور طلا",
                                            fontSize = 9.5.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Notification Bell with Golden Live Dot
                                val statusColor = when (mainUiState.connectionStatus) {
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
                                        QiratoToast.show(
                                            context,
                                            "نگارش ۲.۴.۰ پرو • اتصال به شبکه طلا و جواهر برقرار است"
                                        )
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
                LaunchedEffect(mainUiState.selectedTab) {
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
                        // Live Rates Ticker
                        LiveRatesTicker(
                            rates = mainUiState.rates
                        )

                        // 5 Main System Destinations via AnimatedContent
                        AnimatedContent(
                            targetState = mainUiState.selectedTab,
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
                                        uiState = DashboardUiState(
                                            appSettings = settingsState.appSettings,
                                            rates = mainUiState.rates,
                                            savedInvoiceCount = invoiceState.savedInvoices.size
                                        ),
                                        onNavigateCalculator = {
                                            mainViewModel.selectTab(AppTab.CALCULATOR)
                                        },
                                        onNavigateInvoices = {
                                            barterInvoiceViewModel.navigateBackToList()
                                            mainViewModel.selectTab(AppTab.INVOICES)
                                        },
                                        onNavigateConvert = {
                                            mainViewModel.setKaratConvertVisible(true)
                                        },
                                        onNavigateCoinBubble = {
                                            mainViewModel.setCoinBubbleVisible(true)
                                        },
                                        onNavigateMelt = {
                                            mainViewModel.setMeltVisible(true)
                                        },
                                        onNavigateLedger = {
                                            customerViewModel.loadCustomers()
                                            customerViewModel.setCustomerManagerVisible(true)
                                        }
                                    )
                                }

                                AppTab.RATES -> {
                                    LiveRatesScreen(
                                        uiState = MarketRatesUiState(
                                            rates = mainUiState.rates,
                                            isRefreshing = mainUiState.isRefreshingRates
                                        ),
                                        onRefresh = { mainViewModel.refreshRates() },
                                        onNavigateCalculator = {
                                            mainViewModel.selectTab(AppTab.CALCULATOR)
                                        }
                                    )
                                }

                                AppTab.CALCULATOR -> {
                                    JewelryTab(
                                        viewModel = mainViewModel,
                                        uiState = mainUiState.toJewelryUiState()
                                    )
                                }

                                AppTab.INVOICES -> {
                                    AnimatedContent(
                                        targetState = barterUiState.subScreen,
                                        transitionSpec = {
                                            if (targetState == InvoicesSubScreen.EDITOR) {
                                                (slideInHorizontally(
                                                    animationSpec = LuxuryMotion.IntOffsetSpring
                                                ) { fullWidth -> -fullWidth / 4 } + fadeIn(tween(240, easing = LuxuryMotion.StandardEasing))).togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = tween(200, easing = LuxuryMotion.AccelerationEasing)
                                                    ) { fullWidth -> fullWidth / 4 } + fadeOut(tween(160))
                                                )
                                            } else {
                                                (slideInHorizontally(
                                                    animationSpec = LuxuryMotion.IntOffsetSpring
                                                ) { fullWidth -> fullWidth / 4 } + fadeIn(tween(240, easing = LuxuryMotion.StandardEasing))).togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = tween(200, easing = LuxuryMotion.AccelerationEasing)
                                                    ) { fullWidth -> -fullWidth / 4 } + fadeOut(tween(160))
                                                )
                                            }
                                        },
                                        label = "invoicesSubScreenTransition"
                                    ) { subScreen ->
                                        if (subScreen == InvoicesSubScreen.LIST) {
                                            InvoicesManagementScreen(
                                                uiState = barterUiState,
                                                onSearchQueryChange = barterInvoiceViewModel::setSearchQuery,
                                                onFilterSelect = barterInvoiceViewModel::setSelectedFilter,
                                                onNewInvoiceClick = {
                                                    val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
                                                        PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
                                                        mainUiState.priceBasisTab
                                                    ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L
                                                    barterInvoiceViewModel.openNewInvoice(currentSpot)
                                                },
                                                onInvoiceItemClick = barterInvoiceViewModel::openInvoiceDetails,
                                                onExportPdfClick = { item ->
                                                    QiratoToast.show(context, "در حال صدور فایل PDF فاکتور ${item.invoiceNumber}...")
                                                },
                                                onScanQrClick = {
                                                    QiratoToast.show(context, "قابلیت اسکن بارکد و QR فاکتور فعال شد")
                                                }
                                            )
                                        } else {
                                            BarterInvoiceScreen(
                                                uiState = barterUiState,
                                                customerList = customerState.customerList,
                                                onSetCustomerRole = barterInvoiceViewModel::setCustomerRole,
                                                onSetSettlementMethod = barterInvoiceViewModel::setSettlementMethod,
                                                onSetCashPosAmount = barterInvoiceViewModel::setCashPosAmount,
                                                onSetLedgerAmount = barterInvoiceViewModel::setLedgerAmount,
                                                onSetPosTrackingCode = barterInvoiceViewModel::setPosTrackingCode,
                                                onSetLedgerDueDate = barterInvoiceViewModel::setLedgerDueDate,
                                                onSetBullionSettlement = barterInvoiceViewModel::setBullionSettlement,
                                                onSetThirdPartyTransfer = barterInvoiceViewModel::setThirdPartyTransfer,
                                                onSetNote = barterInvoiceViewModel::setNote,
                                                onOpenAddItemModal = barterInvoiceViewModel::openAddItemModal,
                                                onOpenEditItemModal = barterInvoiceViewModel::openEditItemModal,
                                                onCloseItemModal = barterInvoiceViewModel::closeItemModal,
                                                onSaveItem = barterInvoiceViewModel::saveItem,
                                                onDeleteSalesItem = barterInvoiceViewModel::deleteSalesItem,
                                                onDeleteReceivedItem = barterInvoiceViewModel::deleteReceivedItem,
                                                onSetRateEditDialogVisible = barterInvoiceViewModel::setRateEditDialogVisible,
                                                onUpdateSpotPrice = barterInvoiceViewModel::updateSpotPrice,
                                                onOpenCustomerPicker = {
                                                    customerViewModel.loadCustomers()
                                                    customerViewModel.setCustomerManagerVisible(true)
                                                },
                                                onOpenInvoiceManager = {
                                                    barterInvoiceViewModel.navigateBackToList()
                                                },
                                                onPreviewPdf = {
                                                    QiratoToast.show(context, "در حال تولید سند رسمی PDF فاکتور تهاتر...")
                                                },
                                                onSendSms = {
                                                    QiratoToast.show(context, "ارسال پیامک فاکتور به شماره طرف حساب...")
                                                },
                                                onFinalSubmit = {
                                                    barterInvoiceViewModel.submitAndSaveCurrentInvoice()
                                                    QiratoToast.show(context, "فاکتور تهاتر با موفقیت در سیستم ثبت شد.")
                                                },
                                                onNavigateBack = {
                                                    barterInvoiceViewModel.navigateBackToList()
                                                }
                                            )
                                        }
                                    }
                                }

                                AppTab.MORE -> {
                                    MoreHubScreen(
                                        settings = settingsState.appSettings,
                                        customerCount = customerState.customerList.size,
                                        onToggleBiometricLock = { settingsViewModel.toggleBiometricLock(it) },
                                        onCheckForUpdates = { updateViewModel.checkForUpdates(manual = true) },
                                        onNavigateLedger = {
                                            customerViewModel.loadCustomers()
                                            customerViewModel.setCustomerManagerVisible(true)
                                        },
                                        onNavigateMelt = {
                                            mainViewModel.setMeltVisible(true)
                                        },
                                        onNavigateWorkshop = {
                                            QiratoToast.show(context, "سامانه سفارشات و کارگاه در فاز ۴ فعال خواهد شد")
                                        },
                                        onNavigateInventory = {
                                            QiratoToast.show(context, "سامانه انبارداری و موجودی در فاز ۴ فعال خواهد شد")
                                        },
                                        onNavigateConvert = {
                                            mainViewModel.setKaratConvertVisible(true)
                                        },
                                        onNavigateCoinBubble = {
                                            mainViewModel.setCoinBubbleVisible(true)
                                        },
                                        onNavigateInvoices = {
                                            invoiceViewModel.loadInvoices()
                                            invoiceViewModel.setInvoiceManagerVisible(true)
                                        },
                                        onOpenTaxProfitModal = {
                                            settingsViewModel.setTaxProfitModalVisible(true)
                                        },
                                        onOpenPriceSourceModal = {
                                            settingsViewModel.setPriceSourceModalVisible(true)
                                        },
                                        onOpenJewelerProfile = {
                                            settingsViewModel.setJewelerProfileModalVisible(true)
                                        },
                                        onNavigateStandardFormulas = {
                                            mainViewModel.setStandardFormulasVisible(true)
                                        }
                                    )
                                }
                            }
                        }

                        // Clearance spacer so content scrolls cleanly above the floating dock
                        Spacer(modifier = Modifier.height(78.dp))
                    }

                    // Floating New Invoice Action Button (Sticky above GlassmorphicDock)
                    AnimatedVisibility(
                        visible = mainUiState.selectedTab == AppTab.INVOICES && barterUiState.subScreen == InvoicesSubScreen.LIST,
                        enter = fadeIn(tween(220)) + slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it },
                        exit = fadeOut(tween(160)) + slideOutVertically { it },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 86.dp)
                    ) {
                        FloatingNewInvoiceButton(
                            onNewInvoiceClick = {
                                val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
                                    PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
                                    mainUiState.priceBasisTab
                                ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L
                                barterInvoiceViewModel.openNewInvoice(currentSpot)
                            }
                        )
                    }

                    // Floating Glassmorphic Dock pinned to bottom center
                    GlassmorphicDock(
                        selectedTab = mainUiState.selectedTab,
                        onTabSelected = { mainViewModel.selectTab(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // BackHandler for all sub-screens
            BackHandler(
                enabled = mainUiState.isStandardFormulasVisible ||
                          mainUiState.isKaratConvertVisible ||
                          mainUiState.isCoinBubbleVisible ||
                          mainUiState.isMeltVisible
            ) {
                if (mainUiState.isStandardFormulasVisible) mainViewModel.setStandardFormulasVisible(false)
                if (mainUiState.isKaratConvertVisible) mainViewModel.setKaratConvertVisible(false)
                if (mainUiState.isCoinBubbleVisible) mainViewModel.setCoinBubbleVisible(false)
                if (mainUiState.isMeltVisible) mainViewModel.setMeltVisible(false)
            }

            // Gold Union Standard Formulas Guide
            AnimatedVisibility(
                visible = mainUiState.isStandardFormulasVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                StandardFormulasScreen(
                    onBack = { mainViewModel.setStandardFormulasVisible(false) },
                    onNavigateCalculator = {
                        mainViewModel.setStandardFormulasVisible(false)
                        mainViewModel.selectTab(AppTab.CALCULATOR)
                    }
                )
            }

            // Karat Converter & Lab Settlement Screen
            AnimatedVisibility(
                visible = mainUiState.isKaratConvertVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                KaratConvertScreen(
                    uiState = karatConvertUiState,
                    rates = mainUiState.rates,
                    onBack = { mainViewModel.setKaratConvertVisible(false) },
                    onConvertWeightChanged = karatConvertViewModel::onConvertWeightChanged,
                    onConvertFromKarat = karatConvertViewModel::onConvertFromKarat,
                    onConvertToKarat = karatConvertViewModel::onConvertToKarat,
                    onSwapConvertKarats = karatConvertViewModel::swapConvertKarats,
                    onSetConvertMode = karatConvertViewModel::setConvertMode,
                    onAssayKaratChanged = karatConvertViewModel::onAssayKaratChanged,
                    onAgreedKaratChanged = karatConvertViewModel::onAgreedKaratChanged,
                    onTransferToInvoice = {
                        mainViewModel.addItemToInvoice()
                        mainViewModel.setKaratConvertVisible(false)
                        mainViewModel.selectTab(AppTab.INVOICES)
                    }
                )
            }

            // Coin Bubble Analyzer Screen
            AnimatedVisibility(
                visible = mainUiState.isCoinBubbleVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                CoinBubbleScreen(
                    rates = mainUiState.rates,
                    onRefreshRates = { mainViewModel.refreshRates() },
                    onBack = { mainViewModel.setCoinBubbleVisible(false) }
                )
            }

            // Melt Gold Calculator Screen
            AnimatedVisibility(
                visible = mainUiState.isMeltVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                MeltCalcScreen(
                    uiState = mainUiState.toMeltUiState(),
                    onMeltWeightChanged = mainViewModel::onMeltWeightChanged,
                    onMesghalPriceChanged = mainViewModel::onMesghalPriceChanged,
                    onBack = { mainViewModel.setMeltVisible(false) }
                )
            }
        }
    }
}



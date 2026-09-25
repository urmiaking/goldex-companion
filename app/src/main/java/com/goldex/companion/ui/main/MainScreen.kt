package com.goldex.companion.ui.main

import android.app.Application
import android.content.Intent
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
import com.goldex.companion.ui.theme.ButtonShape
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.goldex.companion.ui.customers.CustomerLedgerScreen
import com.goldex.companion.ui.customers.CustomerStatementScreen
import com.goldex.companion.ui.customers.modals.AddLedgerEntryModal
import com.goldex.companion.ui.inventory.InventoryScreen
import com.goldex.companion.ui.inventory.InventoryViewModel
import com.goldex.companion.ui.inventory.InventoryViewModelFactory
import com.goldex.companion.ui.invoices.BarterInvoiceScreen
import com.goldex.companion.ui.invoices.BarterInvoiceViewModel
import com.goldex.companion.ui.invoices.BarterInvoiceViewModelFactory
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import com.goldex.companion.ui.invoices.CustomerManagerViewModelFactory
import com.goldex.companion.ui.invoices.FloatingNewInvoiceButton
import com.goldex.companion.ui.invoices.InvoicesManagementScreen
import com.goldex.companion.ui.invoices.InvoicesSubScreen
import com.goldex.companion.ui.invoices.InvoicePdfPreviewModal
import com.goldex.companion.ui.invoices.InvoiceManagerViewModel
import com.goldex.companion.ui.invoices.InvoiceManagerViewModelFactory
import com.goldex.companion.ui.util.OfficialInvoicePdfGenerator
import com.goldex.companion.ui.portfolio.PortfolioManagerViewModel
import com.goldex.companion.ui.portfolio.PortfolioManagerViewModelFactory
import com.goldex.companion.ui.rates.LiveRatesScreen
import com.goldex.companion.ui.rates.MarketRatesUiState
import com.goldex.companion.ui.rates.MarketRateDetailScreen
import com.goldex.companion.model.MarketRateDetailState
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.ui.settings.SettingsViewModel
import com.goldex.companion.ui.settings.SettingsViewModelFactory
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.update.UpdateViewModel
import com.goldex.companion.ui.wizard.OnboardingWizardScreen
import com.goldex.companion.ui.wizard.WizardLicenseChoice
import com.goldex.companion.ui.license.LicenseViewModel
import com.goldex.companion.ui.license.LicenseViewModelFactory
import com.goldex.companion.ui.security.AppLockViewModel
import com.goldex.companion.ui.license.LicenseActivationModal
import com.goldex.companion.ui.reporting.ReportingScreen
import com.goldex.companion.ui.reporting.ReportingDetailScreen
import com.goldex.companion.ui.reporting.ShamsiDateRangePickerDialog
import com.goldex.companion.ui.reporting.reportingShareText
import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.ui.reporting.ReportingViewModel
import com.goldex.companion.ui.reporting.ReportingViewModelFactory
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.model.Karat
import com.goldex.companion.model.CoinType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel(),
    appLockViewModel: AppLockViewModel? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val customerViewModel: CustomerManagerViewModel = viewModel(factory = CustomerManagerViewModelFactory(app))
    val invoiceViewModel: InvoiceManagerViewModel = viewModel(factory = InvoiceManagerViewModelFactory(app))
    val portfolioViewModel: PortfolioManagerViewModel = viewModel(factory = PortfolioManagerViewModelFactory(app))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(app))
    val updateViewModel: UpdateViewModel = viewModel()
    val karatConvertViewModel: KaratConvertViewModel = viewModel()
    val barterInvoiceViewModel: BarterInvoiceViewModel = viewModel(factory = BarterInvoiceViewModelFactory(app))
    val licenseViewModel: LicenseViewModel = viewModel(factory = LicenseViewModelFactory(app))
    val inventoryViewModel: InventoryViewModel = viewModel(factory = InventoryViewModelFactory(app))
    val reportingViewModel: ReportingViewModel = viewModel(factory = ReportingViewModelFactory(app))

    val mainUiState by mainViewModel.uiState.collectAsState()
    val customerState by customerViewModel.uiState.collectAsState()
    val invoiceState by invoiceViewModel.uiState.collectAsState()
    @Suppress("UNUSED_VARIABLE")
    val portfolioState by portfolioViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()
    val karatConvertUiState by karatConvertViewModel.uiState.collectAsState()
    val barterUiState by barterInvoiceViewModel.uiState.collectAsState()
    val licenseUiState by licenseViewModel.uiState.collectAsState()
    val inventoryState by inventoryViewModel.uiState.collectAsState()
    val reportingUiState by reportingViewModel.uiState.collectAsState()
    val licenseInfo = licenseUiState.licenseInfo

    val colors = LocalGoldExColors.current
    var pdfPreview by remember { mutableStateOf<Pair<File, String>?>(null) }

    fun openPdfPreview(invoice: BarterInvoice) {
        val file = OfficialInvoicePdfGenerator.create(context, invoice, settingsState.appSettings)
        if (file == null) {
            QiratoToast.show(context, "ساخت فایل PDF ناموفق بود؛ دوباره تلاش کنید")
        } else {
            pdfPreview = file to invoice.invoiceNumber
        }
    }

    pdfPreview?.let { (file, invoiceNumber) ->
        InvoicePdfPreviewModal(
            file = file,
            invoiceNumber = invoiceNumber,
            onDismiss = { pdfPreview = null },
            onShare = {
                if (!OfficialInvoicePdfGenerator.share(context, file, invoiceNumber)) {
                    QiratoToast.show(context, "اشتراک‌گذاری فایل PDF ناموفق بود")
                }
            }
        )
    }

    // In-App Auto-Update Check & Dialog Prompt (Triggers on initial launch and whenever user re-enters the app)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateViewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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

    // Ledger Entry Registration Modal (Screens 1 & 2)
    if (customerState.isAddLedgerEntryModalVisible && customerState.ledgerEntryTargetCustomer != null) {
        AddLedgerEntryModal(
            customer = customerState.ledgerEntryTargetCustomer!!,
            editingTransaction = customerState.editingLedgerTransaction,
            onDismiss = { customerViewModel.closeAddLedgerEntry() },
            onSaveEntry = { tx ->
                if (!licenseInfo.isLicensed) {
                    licenseViewModel.setActivationDialogVisible(true)
                    QiratoToast.show(context, "ثبت سند در دفتر معین نیازمند اشتراک معتبر است.")
                } else {
                    val isEditing = customerState.editingLedgerTransaction != null
                    customerViewModel.saveLedgerEntry(tx)
                    val msg = if (isEditing) {
                        "سند شماره ${PersianNumberFormatter.toPersianDigits(tx.documentNumber)} با موفقیت ویرایش شد"
                    } else {
                        "سند شماره ${PersianNumberFormatter.toPersianDigits(tx.documentNumber)} در دفتر معین ثبت شد"
                    }
                    QiratoToast.show(context, msg)
                }
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
            onSaveProfile = { galleryName, managerName, unionCode, phone, address, logoUri, stampUri ->
                settingsViewModel.updateJewelerProfile(
                    galleryName = galleryName,
                    managerName = managerName,
                    unionCode = unionCode,
                    phone = phone,
                    address = address,
                    logoUri = logoUri,
                    stampUri = stampUri
                )
                QiratoToast.show(context, "اطلاعات بنکداری و پروانه زرگری ذخیره شد")
            }
        )
    }

    // License & Subscription Activation Modal
    if (licenseUiState.isActivationDialogVisible) {
        LicenseActivationModal(
            licenseInfo = licenseInfo,
            isLoading = licenseUiState.isLoading,
            errorMessage = licenseUiState.errorMessage,
            successMessage = licenseUiState.successMessage,
            onDismiss = { licenseViewModel.setActivationDialogVisible(false) },
            onActivateCode = { code -> licenseViewModel.activateCode(code) },
            onActivateTrial = { licenseViewModel.activateTrial() }
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
                                    shape = ButtonShape,
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
                                    val inventoryWeight = inventoryState.totalGoldWeight18k
                                    val inventoryValuation = if (mainUiState.rates.gold18 > 0) {
                                        (inventoryWeight * mainUiState.rates.gold18).toLong()
                                    } else 0L

                                    DashboardScreen(
                                        uiState = DashboardUiState(
                                            appSettings = settingsState.appSettings,
                                            rates = mainUiState.rates,
                                            savedInvoiceCount = barterUiState.invoicesList.size,
                                            gold18Charts = mainUiState.dashboardGold18Charts,
                                            licenseInfo = licenseInfo,
                                            totalInventoryWeight18k = inventoryWeight,
                                            totalInventoryValuationTomans = inventoryValuation,
                                            recentInvoices = barterUiState.invoicesList.take(5)
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
                                            customerViewModel.openCustomerLedger()
                                        },
                                        onNavigateInventory = {
                                            inventoryViewModel.setInventoryVisible(true)
                                        },
                                        onOpenLicenseActivation = {
                                            licenseViewModel.setActivationDialogVisible(true)
                                        },
                                        onEnableBiometricLock = {
                                            if (appLockViewModel != null) {
                                                appLockViewModel.toggleBiometricLock(true) { success, message ->
                                                    if (success) {
                                                        settingsViewModel.loadSettings()
                                                    }
                                                    if (!message.isNullOrBlank()) {
                                                        QiratoToast.show(context, message)
                                                    }
                                                }
                                            } else {
                                                settingsViewModel.toggleBiometricLock(true)
                                            }
                                        },
                                        onDismissBiometricTip = {
                                            settingsViewModel.dismissBiometricTip()
                                        }
                                    )
                                }

                                AppTab.RATES -> {
                                    LiveRatesScreen(
                                        uiState = MarketRatesUiState(
                                            rates = mainUiState.rates,
                                            isRefreshing = mainUiState.isRefreshingRates,
                                            todayCandlesByType = mainUiState.todayCandlesByType
                                        ),
                                        onRefresh = { mainViewModel.refreshRates() },
                                        onNavigateCalculator = {
                                            mainViewModel.selectTab(AppTab.CALCULATOR)
                                        },
                                        onNavigateRateDetail = { rateType ->
                                            mainViewModel.openRateDetail(rateType)
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
                                    InvoicesManagementScreen(
                                        uiState = barterUiState,
                                        onSearchQueryChange = barterInvoiceViewModel::setSearchQuery,
                                        onFilterSelect = barterInvoiceViewModel::setSelectedFilter,
                                        onNewInvoiceClick = {
                                            if (!licenseInfo.isLicensed) {
                                                licenseViewModel.setActivationDialogVisible(true)
                                                QiratoToast.show(context, "جهت صدور فاکتور نیاز به فعال‌سازی اشتراک دارید.")
                                            } else {
                                                val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
                                                    PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
                                                    mainUiState.priceBasisTab
                                                ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L
                                                barterInvoiceViewModel.openNewInvoice(currentSpot)
                                            }
                                        },
                                        onInvoiceItemClick = barterInvoiceViewModel::openInvoiceDetails,
                                        onExportPdfClick = { item ->
                                            if (!licenseInfo.isLicensed) {
                                                licenseViewModel.setActivationDialogVisible(true)
                                                QiratoToast.show(context, "صدور فایل PDF فاکتور نیازمند اشتراک معتبر است.")
                                            } else {
                                                val invoice = item.barterInvoice
                                                if (invoice == null) {
                                                    QiratoToast.show(context, "اطلاعات کامل فاکتور برای صدور PDF موجود نیست")
                                                } else openPdfPreview(invoice)
                                            }
                                        }
                                    )
                                }

                                AppTab.MORE -> {
                                    MoreHubScreen(
                                        settings = settingsState.appSettings,
                                        customerCount = customerState.customerList.size,
                                        inventoryWeight = inventoryState.totalGoldWeight18k,
                                        isDarkTheme = mainUiState.isDarkTheme,
                                        licenseInfo = licenseInfo,
                                        onOpenLicenseActivation = {
                                            licenseViewModel.setActivationDialogVisible(true)
                                        },
                                        onToggleTheme = mainViewModel::toggleTheme,
                                        onToggleBiometricLock = { enabled ->
                                            if (appLockViewModel != null) {
                                                appLockViewModel.toggleBiometricLock(enabled) { success, message ->
                                                    if (success) {
                                                        settingsViewModel.loadSettings()
                                                    }
                                                    if (!message.isNullOrBlank()) {
                                                        QiratoToast.show(context, message)
                                                    }
                                                }
                                            } else {
                                                settingsViewModel.toggleBiometricLock(enabled)
                                            }
                                        },
                                        onCheckForUpdates = { updateViewModel.checkForUpdates(manual = true) },
                                        onNavigateLedger = {
                                            customerViewModel.openCustomerLedger()
                                        },
                                        onNavigateMelt = {
                                            mainViewModel.setMeltVisible(true)
                                        },
                                        onNavigateInventory = {
                                            inventoryViewModel.setInventoryVisible(true)
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
                                        onNavigateReporting = {
                                            reportingViewModel.setReportingVisible(true)
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
                                        },
                                        onOpenOnboardingWizard = {
                                            mainViewModel.setWizardVisible(true)
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
                                if (!licenseInfo.isLicensed) {
                                    licenseViewModel.setActivationDialogVisible(true)
                                    QiratoToast.show(context, "جهت صدور فاکتور نیاز به فعال‌سازی اشتراک دارید.")
                                } else {
                                    val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
                                        PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
                                        mainUiState.priceBasisTab
                                    ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L
                                    barterInvoiceViewModel.openNewInvoice(currentSpot)
                                }
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
                enabled = reportingUiState.isReportingVisible ||
                          customerState.selectedCustomerForStatement != null ||
                          customerState.isCustomerLedgerVisible ||
                          barterUiState.subScreen != InvoicesSubScreen.LIST ||
                          inventoryState.isInventoryVisible ||
                          mainUiState.isStandardFormulasVisible ||
                          mainUiState.isKaratConvertVisible ||
                          mainUiState.isCoinBubbleVisible ||
                          mainUiState.isMeltVisible ||
                          mainUiState.isRateDetailVisible
            ) {
                if (reportingUiState.isReportingVisible) {
                    if (reportingUiState.activeBreakdown != null) {
                        reportingViewModel.closeBreakdown()
                    } else {
                        reportingViewModel.setReportingVisible(false)
                    }
                } else if (customerState.selectedCustomerForStatement != null) {
                    customerViewModel.closeCustomerStatement()
                } else if (customerState.isCustomerLedgerVisible) {
                    customerViewModel.closeCustomerLedger()
                } else if (barterUiState.subScreen != InvoicesSubScreen.LIST) {
                    barterInvoiceViewModel.navigateBackToList()
                } else if (inventoryState.isAddModalOpen) {
                    inventoryViewModel.closeAddModal()
                } else if (inventoryState.isAdjustModalOpen) {
                    inventoryViewModel.closeAdjustModal()
                } else if (inventoryState.isInventoryVisible) {
                    inventoryViewModel.setInventoryVisible(false)
                } else if (mainUiState.isRateDetailVisible) mainViewModel.setRateDetailVisible(false)
                else if (mainUiState.isStandardFormulasVisible) mainViewModel.setStandardFormulasVisible(false)
                else if (mainUiState.isKaratConvertVisible) mainViewModel.setKaratConvertVisible(false)
                else if (mainUiState.isCoinBubbleVisible) mainViewModel.setCoinBubbleVisible(false)
                else if (mainUiState.isMeltVisible) mainViewModel.setMeltVisible(false)
            }

            // Gold Union Standard Formulas Guide
            AnimatedVisibility(
                visible = mainUiState.isStandardFormulasVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                StandardFormulasScreen(
                    defaultProfitPercent = settingsState.appSettings.defaultProfitPercent,
                    defaultTaxPercent = settingsState.appSettings.defaultTaxPercent,
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

            // Gold Inventory & Showcase Screen
            AnimatedVisibility(
                visible = inventoryState.isInventoryVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                InventoryScreen(
                    uiState = inventoryState,
                    rates = mainUiState.rates,
                    onBack = { inventoryViewModel.setInventoryVisible(false) },
                    onSelectCategory = inventoryViewModel::selectCategory,
                    onSearchQueryChanged = inventoryViewModel::setSearchQuery,
                    onOpenAddModal = inventoryViewModel::openAddModal,
                    onCloseAddModal = inventoryViewModel::closeAddModal,
                    onOpenAdjustModal = inventoryViewModel::openAdjustModal,
                    onCloseAdjustModal = inventoryViewModel::closeAdjustModal,
                    onSaveNewItem = inventoryViewModel::addItem,
                    onConfirmAdjustment = inventoryViewModel::adjustStock,
                    onDeleteItem = inventoryViewModel::deleteItem,
                    onTransferToInvoice = { item ->
                        val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
                            PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
                            mainUiState.priceBasisTab
                        ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L

                        barterInvoiceViewModel.openNewInvoice(currentSpot)

                        val rawGoldValue = item.netGoldWeightGrams * currentSpot * (item.karat.value.toDouble() / 750.0)
                        val wageAmount = rawGoldValue * (item.wagePercent / 100.0)
                        val profitAmount = (rawGoldValue + wageAmount) * (item.profitPercent / 100.0)
                        val taxAmount = (wageAmount + profitAmount) * (item.taxPercent / 100.0)
                        val totalPayable = rawGoldValue + wageAmount + profitAmount + taxAmount

                        val barterItem = CraftedGoldItem(
                            title = item.title,
                            karat = item.karat,
                            grossWeight = item.grossWeightGrams,
                            stoneWeight = item.stoneWeightGrams,
                            netWeight = item.netGoldWeightGrams,
                            spotPrice = currentSpot,
                            wageType = WageType.PERCENTAGE,
                            wageInput = item.wagePercent,
                            wageAmount = wageAmount,
                            profitPercent = item.profitPercent,
                            profitAmount = profitAmount,
                            taxPercent = item.taxPercent,
                            taxAmount = taxAmount,
                            rawGoldValue = rawGoldValue,
                            totalPayable = totalPayable,
                            equivalent18kWeight = item.weightIn18kGrams
                        )
                        barterInvoiceViewModel.addSalesItem(barterItem)
                        inventoryViewModel.setInventoryVisible(false)
                        mainViewModel.selectTab(AppTab.INVOICES)
                        QiratoToast.show(context, "کالای ${item.code} به فاکتور منتقل شد")
                    }
                )
            }

            // Comprehensive Barter Invoice Screen (Side Drawer / Push Screen)
            AnimatedVisibility(
                visible = barterUiState.subScreen != InvoicesSubScreen.LIST,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                BarterInvoiceScreen(
                    uiState = barterUiState,
                    customerList = customerState.customerList,
                    onSetSettlementMethod = barterInvoiceViewModel::setSettlementMethod,
                    onSetCashPosAmount = barterInvoiceViewModel::setCashPosAmount,
                    onSetLedgerAmount = barterInvoiceViewModel::setLedgerAmount,
                    onSetPosTrackingCode = barterInvoiceViewModel::setPosTrackingCode,
                    onSetLedgerDueDate = barterInvoiceViewModel::setLedgerDueDate,
                    onSetBullionSettlement = barterInvoiceViewModel::setBullionSettlement,
                    onSetThirdPartyTransfer = barterInvoiceViewModel::setThirdPartyTransfer,
                    onSetSettlementPayments = barterInvoiceViewModel::setSettlementPayments,
                    onSetSyncWithLedger = barterInvoiceViewModel::setSyncWithLedger,
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
                        openPdfPreview(barterUiState.invoice)
                    },
                    onSendSms = {
                        QiratoToast.show(context, "ارسال پیامک فاکتور به شماره طرف حساب...")
                    },
                    onFinalSubmit = {
                        barterInvoiceViewModel.submitAndSaveCurrentInvoice()
                        customerViewModel.loadCustomers()
                        QiratoToast.show(context, "فاکتور تهاتر با موفقیت در سیستم ثبت شد.")
                    },
                    onNavigateBack = {
                        barterInvoiceViewModel.navigateBackToList()
                    },
                    defaultProfitPercent = settingsState.appSettings.defaultProfitPercent,
                    defaultTaxPercent = settingsState.appSettings.defaultTaxPercent
                )
            }

            // Customer Ledger Management Screen (Screen 4)
            AnimatedVisibility(
                visible = customerState.isCustomerLedgerVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                CustomerLedgerScreen(
                    uiState = customerState,
                    onSearchQueryChange = { customerViewModel.setSearchQuery(it) },
                    onFilterSelect = { customerViewModel.setLedgerFilter(it) },
                    onOpenStatement = { customerViewModel.openCustomerStatement(it) },
                    onOpenAddEntry = { customerViewModel.openAddLedgerEntry(it) },
                    onAddNewCustomer = { customerViewModel.setAddCustomerDialogVisible(true) },
                    onBack = { customerViewModel.closeCustomerLedger() }
                )
            }

            // Customer Statement Screen (Screen 3)
            AnimatedVisibility(
                visible = customerState.selectedCustomerForStatement != null,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                val statementCustomer = customerState.selectedCustomerForStatement
                if (statementCustomer != null) {
                    CustomerStatementScreen(
                        customer = statementCustomer,
                        transactions = customerState.filteredStatementTransactions,
                        allTransactions = customerState.activeCustomerTransactions,
                        selectedFilter = customerState.selectedStatementFilter,
                        onFilterSelect = { customerViewModel.setStatementFilter(it) },
                        onOpenAddEntry = { customerViewModel.openAddLedgerEntry(statementCustomer) },
                        onEditTransaction = { customerViewModel.openEditLedgerEntry(it) },
                        onDeleteTransaction = { customerViewModel.deleteLedgerEntry(it) },
                        onBack = { customerViewModel.closeCustomerStatement() }
                    )
                }
            }

            // Reporting Center & Balance Sheets Screen
            AnimatedVisibility(
                visible = reportingUiState.isReportingVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                ReportingScreen(
                    uiState = reportingUiState,
                    onBack = { reportingViewModel.setReportingVisible(false) },
                    onSelectPeriod = reportingViewModel::selectPeriod,
                    onOpenBreakdown = reportingViewModel::openBreakdown,
                    onOpenCustomDateDialog = { reportingViewModel.setCustomDateDialogVisible(true) },
                    onCloseCustomDateDialog = { reportingViewModel.setCustomDateDialogVisible(false) },
                    onSubmitCustomRange = reportingViewModel::setCustomDateRange
                )
            }

            // Keep the last page composed while its matching screen-pop exit runs.
            var lastReportType by remember { mutableStateOf<ReportingBreakdownType?>(null) }
            LaunchedEffect(reportingUiState.activeBreakdown) {
                reportingUiState.activeBreakdown?.let { lastReportType = it }
            }
            AnimatedVisibility(
                visible = reportingUiState.isReportingVisible && reportingUiState.activeBreakdown != null,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                (reportingUiState.activeBreakdown ?: lastReportType)?.let { reportType ->
                    ReportingDetailScreen(
                        type = reportType,
                        uiState = reportingUiState,
                        onBack = reportingViewModel::closeBreakdown,
                        onSelectPeriod = reportingViewModel::selectPeriod,
                        onOpenCustomDateDialog = { reportingViewModel.setCustomDateDialogVisible(true) },
                        onNavigateInventory = {
                            reportingViewModel.setReportingVisible(false)
                            inventoryViewModel.setInventoryVisible(true)
                        },
                        onNavigateCustomerLedger = {
                            reportingViewModel.setReportingVisible(false)
                            customerViewModel.openCustomerLedger()
                        },
                        onShareReport = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reportingShareText(reportType, reportingUiState))
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "اشتراک خلاصه گزارش"))
                        }
                    )
                    if (reportingUiState.isCustomDateDialogVisible) {
                        ShamsiDateRangePickerDialog(
                            currentStartShamsi = reportingUiState.customStartDateShamsi,
                            currentEndShamsi = reportingUiState.customEndDateShamsi,
                            onDismiss = { reportingViewModel.setCustomDateDialogVisible(false) },
                            onConfirmRange = reportingViewModel::setCustomDateRange
                        )
                    }
                }
            }

            // Market Rate Detail & Trend Chart Screen (Stitch Screen 57d121df61a34215ba36be0989160e62)
            AnimatedVisibility(
                visible = mainUiState.isRateDetailVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                val detailState = remember(
                    mainUiState.selectedRateDetailType,
                    mainUiState.rates,
                    mainUiState.rateDetailHistory,
                    mainUiState.isHistoryLoading
                ) {
                    MarketRateDetailState.create(
                        type = mainUiState.selectedRateDetailType,
                        rates = mainUiState.rates,
                        historyByHorizon = mainUiState.rateDetailHistory,
                        isLoading = mainUiState.isHistoryLoading
                    )
                }
                MarketRateDetailScreen(
                    state = detailState,
                    onBack = { mainViewModel.setRateDetailVisible(false) },
                    onAddToInvoice = { price, karat ->
                        mainViewModel.setRateDetailVisible(false)
                        val basisTab = when (karat) {
                            Karat.K24 -> PriceBasisTab.K24
                            else -> PriceBasisTab.K18
                        }
                        mainViewModel.setPriceBasisTab(basisTab)
                        mainViewModel.onSpotPriceChanged(price.toString())
                        mainViewModel.selectTab(AppTab.CALCULATOR)
                    },
                    onSetPriceAlert = { _, _ ->
                        // Handled via in-app toast & state
                    },
                    onViewAllTransactions = {
                        mainViewModel.setRateDetailVisible(false)
                        customerViewModel.openCustomerLedger()
                    }
                )
            }

            // Onboarding & Setup Wizard (First-launch or manually triggered from Hub)
            AnimatedVisibility(
                visible = !settingsState.appSettings.hasCompletedOnboarding || mainUiState.isWizardVisible,
                enter = LuxuryMotion.ScreenPushEnter,
                exit = LuxuryMotion.ScreenPopExit,
                modifier = Modifier.fillMaxSize()
            ) {
                OnboardingWizardScreen(
                    currentSettings = settingsState.appSettings,
                    liveGold18Price = mainUiState.rates.gold18,
                    onValidateLicense = { choice, code, onSuccess, onError ->
                        when (choice) {
                            WizardLicenseChoice.TRIAL -> {
                                licenseViewModel.activateTrial(
                                    onSuccess = onSuccess,
                                    onError = onError
                                )
                            }
                            WizardLicenseChoice.CODE -> {
                                licenseViewModel.activateCode(
                                    code = code,
                                    onSuccess = onSuccess,
                                    onError = onError
                                )
                            }
                        }
                    },
                    onFinish = { targetTab, updatedSettings, initialInventory, licenseState ->
                        settingsViewModel.updateSettings(updatedSettings)
                        mainViewModel.applySettingsDefaults(
                            updatedSettings.defaultProfitPercent,
                            updatedSettings.defaultTaxPercent,
                            updatedSettings.defaultWageType
                        )

                        // Save initial inventory items into Inventory and Portfolio if entered
                        val vitrinWeight = PersianNumberFormatter.parseToCleanDouble(initialInventory.vitrinWeight) ?: 0.0
                        val vitrinOjrat = PersianNumberFormatter.parseToCleanDouble(initialInventory.vitrinOjrat) ?: 0.0
                        if (vitrinWeight > 0.0) {
                            val galleryTitle = updatedSettings.galleryName.ifBlank { "گالری" }
                            inventoryViewModel.addItem(
                                InventoryItem(
                                    code = "VITRIN-01",
                                    title = "مصنوعات ویترین ($galleryTitle)",
                                    category = InventoryCategory.SETS,
                                    location = "سینی شماره ۱ ویترین اصلی",
                                    grossWeightGrams = vitrinWeight,
                                    karat = Karat.K18,
                                    customKaratValue = 750,
                                    workshop = galleryTitle,
                                    wageType = WageType.PERCENTAGE,
                                    wagePercent = vitrinOjrat,
                                    wageValue = vitrinOjrat,
                                    profitPercent = updatedSettings.defaultProfitPercent.toDoubleOrNull() ?: 7.0,
                                    taxPercent = updatedSettings.defaultTaxPercent.toDoubleOrNull() ?: 9.0,
                                    quantity = 1
                                )
                            )
                            portfolioViewModel.addPortfolioItem(
                                PortfolioItem(
                                    title = "مصنوعات ویترین ($galleryTitle)",
                                    category = PortfolioCategory.GOLD,
                                    weightGrams = vitrinWeight,
                                    karat = Karat.K18,
                                    purchasePriceTotal = 0L,
                                    purchaseDate = "موجودی اول دوره"
                                )
                            )
                        }

                        val meltWeight = PersianNumberFormatter.parseToCleanDouble(initialInventory.meltWeight) ?: 0.0
                        val meltAyarInt = PersianNumberFormatter.parseToCleanLong(initialInventory.meltAyar)?.toInt() ?: 750
                        if (meltWeight > 0.0) {
                            val meltKarat = if (meltAyarInt >= 900) Karat.K21 else Karat.K18
                            inventoryViewModel.addItem(
                                InventoryItem(
                                    code = "MELT-01",
                                    title = "طلای آبشده گاوصندوق",
                                    category = InventoryCategory.MISC,
                                    location = "گاوصندوق اصلی",
                                    grossWeightGrams = meltWeight,
                                    karat = meltKarat,
                                    customKaratValue = meltAyarInt,
                                    workshop = "ری‌گیری و ذوب",
                                    wageType = WageType.PERCENTAGE,
                                    wagePercent = 0.0,
                                    profitPercent = 0.0,
                                    taxPercent = 0.0,
                                    quantity = 1
                                )
                            )
                            portfolioViewModel.addPortfolioItem(
                                PortfolioItem(
                                    title = "طلای آبشده گاوصندوق",
                                    category = PortfolioCategory.GOLD,
                                    weightGrams = meltWeight,
                                    karat = meltKarat,
                                    purchasePriceTotal = 0L,
                                    purchaseDate = "موجودی اول دوره"
                                )
                            )
                        }

                        if (initialInventory.coinTamam > 0) {
                            inventoryViewModel.addItem(
                                InventoryItem(
                                    code = "COIN-EMAMI",
                                    title = "تمام بهار آزادی (طرح جدید)",
                                    category = InventoryCategory.COINS,
                                    location = "گاوصندوق اصلی",
                                    grossWeightGrams = 8.133,
                                    karat = Karat.K21,
                                    customKaratValue = 900,
                                    quantity = initialInventory.coinTamam,
                                    profitPercent = 0.0,
                                    taxPercent = 0.0
                                )
                            )
                            portfolioViewModel.addPortfolioItem(
                                PortfolioItem(
                                    title = "تمام بهار آزادی (طرح جدید)",
                                    category = PortfolioCategory.COIN,
                                    quantity = initialInventory.coinTamam,
                                    coinType = CoinType.EMAMI,
                                    purchaseDate = "موجودی اول دوره"
                                )
                            )
                        }

                        if (initialInventory.coinNim > 0) {
                            inventoryViewModel.addItem(
                                InventoryItem(
                                    code = "COIN-NIM",
                                    title = "نیم سکه بهار آزادی",
                                    category = InventoryCategory.COINS,
                                    location = "گاوصندوق اصلی",
                                    grossWeightGrams = 4.066,
                                    karat = Karat.K21,
                                    customKaratValue = 900,
                                    quantity = initialInventory.coinNim,
                                    profitPercent = 0.0,
                                    taxPercent = 0.0
                                )
                            )
                            portfolioViewModel.addPortfolioItem(
                                PortfolioItem(
                                    title = "نیم سکه بهار آزادی",
                                    category = PortfolioCategory.COIN,
                                    quantity = initialInventory.coinNim,
                                    coinType = CoinType.HALF,
                                    purchaseDate = "موجودی اول دوره"
                                )
                            )
                        }

                        if (initialInventory.coinRob > 0) {
                            inventoryViewModel.addItem(
                                InventoryItem(
                                    code = "COIN-ROB",
                                    title = "ربع سکه بهار آزادی",
                                    category = InventoryCategory.COINS,
                                    location = "گاوصندوق اصلی",
                                    grossWeightGrams = 2.033,
                                    karat = Karat.K21,
                                    customKaratValue = 900,
                                    quantity = initialInventory.coinRob,
                                    profitPercent = 0.0,
                                    taxPercent = 0.0
                                )
                            )
                            portfolioViewModel.addPortfolioItem(
                                PortfolioItem(
                                    title = "ربع سکه بهار آزادی",
                                    category = PortfolioCategory.COIN,
                                    quantity = initialInventory.coinRob,
                                    coinType = CoinType.QUARTER,
                                    purchaseDate = "موجودی اول دوره"
                                )
                            )
                        }

                        if (initialInventory.coinQadim > 0) {
                            inventoryViewModel.addItem(
                                InventoryItem(
                                    code = "COIN-QADIM",
                                    title = "تمام بهار آزادی (طرح قدیم)",
                                    category = InventoryCategory.COINS,
                                    location = "گاوصندوق اصلی",
                                    grossWeightGrams = 8.133,
                                    karat = Karat.K21,
                                    customKaratValue = 900,
                                    quantity = initialInventory.coinQadim,
                                    profitPercent = 0.0,
                                    taxPercent = 0.0
                                )
                            )
                            portfolioViewModel.addPortfolioItem(
                                PortfolioItem(
                                    title = "تمام بهار آزادی (طرح قدیم)",
                                    category = PortfolioCategory.COIN,
                                    quantity = initialInventory.coinQadim,
                                    coinType = CoinType.BAHAR,
                                    purchaseDate = "موجودی اول دوره"
                                )
                            )
                        }

                        if (initialInventory.coinGerami > 0) {
                            inventoryViewModel.addItem(
                                InventoryItem(
                                    code = "COIN-GERAMI",
                                    title = "سکه یک گرمی بانکی",
                                    category = InventoryCategory.COINS,
                                    location = "گاوصندوق اصلی",
                                    grossWeightGrams = 1.01,
                                    karat = Karat.K21,
                                    customKaratValue = 900,
                                    quantity = initialInventory.coinGerami,
                                    profitPercent = 0.0,
                                    taxPercent = 0.0
                                )
                            )
                            portfolioViewModel.addPortfolioItem(
                                PortfolioItem(
                                    title = "سکه یک گرمی بانکی",
                                    category = PortfolioCategory.COIN,
                                    quantity = initialInventory.coinGerami,
                                    coinType = CoinType.GERAMI,
                                    purchaseDate = "موجودی اول دوره"
                                )
                            )
                        }

                        mainViewModel.setWizardVisible(false)
                        mainViewModel.selectTab(targetTab)
                        QiratoToast.show(context, "پیکربندی اولیه با موفقیت انجام شد")
                    },
                    onSkip = {
                        settingsViewModel.completeOnboarding()
                        mainViewModel.setWizardVisible(false)
                        QiratoToast.show(context, "ورود به عنوان مهمان")
                    }
                )
            }

        }
    }
}


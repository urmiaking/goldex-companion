package com.goldex.companion.ui.main

import androidx.lifecycle.ViewModelProvider
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
import com.goldex.companion.ui.sync.*
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
import com.goldex.companion.domain.invoice.BarterCalculationUseCases
import com.goldex.companion.ui.customers.CustomerLedgerScreen
import com.goldex.companion.ui.customers.CustomerStatementScreen
import com.goldex.companion.ui.customers.modals.AddLedgerEntryModal
import com.goldex.companion.ui.inventory.InventoryScreen
import com.goldex.companion.ui.inventory.InventoryViewModel
import com.goldex.companion.ui.invoices.BarterInvoiceScreen
import com.goldex.companion.ui.invoices.BarterInvoiceViewModel
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import com.goldex.companion.ui.invoices.FloatingNewInvoiceButton
import com.goldex.companion.ui.invoices.InvoicesManagementScreen
import com.goldex.companion.ui.invoices.InvoicesSubScreen
import com.goldex.companion.ui.invoices.InvoicePdfPreviewModal
import com.goldex.companion.ui.invoices.InvoiceManagerViewModel
import com.goldex.companion.ui.util.OfficialInvoicePdfGenerator
import com.goldex.companion.ui.portfolio.PortfolioManagerViewModel
import com.goldex.companion.ui.rates.LiveRatesScreen
import com.goldex.companion.ui.rates.MarketRatesUiState
import com.goldex.companion.ui.rates.MarketRateDetailScreen
import com.goldex.companion.model.MarketRateDetailState
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.ui.settings.SettingsViewModel
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.update.UpdateViewModel
import com.goldex.companion.ui.wizard.OnboardingWizardScreen
import com.goldex.companion.ui.wizard.OnboardingViewModel
import com.goldex.companion.ui.wizard.WizardLicenseChoice
import com.goldex.companion.ui.license.LicenseViewModel
import com.goldex.companion.ui.security.AppLockViewModel
import com.goldex.companion.ui.license.LicenseActivationModal
import com.goldex.companion.ui.reporting.ReportingScreen
import com.goldex.companion.ui.reporting.ReportingDetailScreen
import com.goldex.companion.ui.reporting.ShamsiDateRangePickerDialog
import com.goldex.companion.ui.reporting.reportingShareText
import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.ui.reporting.ReportingViewModel
import com.goldex.companion.model.Karat
import com.goldex.companion.model.CoinType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    viewModelFactory: ViewModelProvider.Factory,
    appLockViewModel: AppLockViewModel? = null
) {
    val context = LocalContext.current

    val customerViewModel: CustomerManagerViewModel = viewModel(factory = viewModelFactory)
    val invoiceViewModel: InvoiceManagerViewModel = viewModel(factory = viewModelFactory)
    val portfolioViewModel: PortfolioManagerViewModel = viewModel(factory = viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val updateViewModel: UpdateViewModel = viewModel(factory = viewModelFactory)
    val karatConvertViewModel: KaratConvertViewModel = viewModel(factory = viewModelFactory)
    val barterInvoiceViewModel: BarterInvoiceViewModel = viewModel(factory = viewModelFactory)
    val licenseViewModel: LicenseViewModel = viewModel(factory = viewModelFactory)
    val inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory)
    val reportingViewModel: ReportingViewModel = viewModel(factory = viewModelFactory)

    val onboardingViewModel: OnboardingViewModel = viewModel(factory = viewModelFactory)
    val cloudViewModel: CloudSyncViewModel = viewModel(factory = viewModelFactory)
    val cloudState by cloudViewModel.state.collectAsState()
    var showCloudSettings by remember { mutableStateOf(false) }
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

    if (showCloudSettings) CloudSettingsDialog(cloudViewModel, onDismiss = { showCloudSettings = false })
    LaunchedEffect(mainUiState.isWizardVisible) { cloudViewModel.deferOnboarding(mainUiState.isWizardVisible) }
    LaunchedEffect(cloudState.restoredGeneration) {
        if (cloudState.restoredGeneration > 0) {
            customerViewModel.loadCustomers(); invoiceViewModel.loadInvoices(); portfolioViewModel.loadPortfolio()
            inventoryViewModel.loadItems(); settingsViewModel.loadSettings(); barterInvoiceViewModel.reloadInvoices()
        }
    }
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
            },
            onPrint = {
                if (!OfficialInvoicePdfGenerator.print(context, file, invoiceNumber)) {
                    QiratoToast.show(context, "ارسال به چاپگر با خطا مواجه شد")
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
                cloudViewModel.sync()
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
            onSaveProfile = { galleryName, managerName, unionCode, phone, address, logoUri, signatureUri ->
                settingsViewModel.updateJewelerProfile(
                    galleryName = galleryName,
                    managerName = managerName,
                    unionCode = unionCode,
                    phone = phone,
                    address = address,
                    logoUri = logoUri,
                    signatureUri = signatureUri
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
                                CloudSyncButton(cloudState, onClick = { showCloudSettings = true })
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
                                        uiState = mainUiState.toJewelryUiState(),
                                        onAddToInvoice = {
                                            if (!licenseInfo.isLicensed) {
                                                licenseViewModel.setActivationDialogVisible(true)
                                                QiratoToast.show(context, "جهت صدور فاکتور نیاز به فعال‌سازی اشتراک دارید.")
                                            } else {
                                                val res = mainUiState.jewelryResult
                                                val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
                                                    PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
                                                    mainUiState.priceBasisTab
                                                ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L

                                                if (barterUiState.subScreen == InvoicesSubScreen.LIST) {
                                                    barterInvoiceViewModel.openNewInvoice(currentSpot)
                                                }

                                                if (res != null) {
                                                    val customKarat = PersianNumberFormatter.parseToCleanLong(mainUiState.karatInput)?.toInt() ?: 750
                                                    val wageInputVal = PersianNumberFormatter.parsePersianOrEnglish(mainUiState.wageInput) ?: 0.0
                                                    val profitVal = PersianNumberFormatter.parsePersianOrEnglish(mainUiState.profitPercentInput) ?: 0.0
                                                    val taxVal = PersianNumberFormatter.parsePersianOrEnglish(mainUiState.taxPercentInput) ?: 0.0

                                                    val barterItem = BarterCalculationUseCases.calculateCraftedItem(
                                                        title = mainUiState.itemTitleInput.ifBlank { "دستبند و زیورآلات ساخته شده" },
                                                        karat = mainUiState.selectedKarat,
                                                        customKaratValue = customKarat,
                                                        grossWeight = res.grossWeight,
                                                        stoneWeight = res.stoneWeight,
                                                        spotPrice18k = currentSpot,
                                                        wageType = mainUiState.wageType,
                                                        wageInput = wageInputVal,
                                                        profitPercent = profitVal,
                                                        taxPercent = taxVal
                                                    )
                                                    barterInvoiceViewModel.addSalesItem(barterItem)
                                                    mainViewModel.addItemToInvoice()
                                                    mainViewModel.selectTab(AppTab.INVOICES)
                                                    QiratoToast.show(context, "قطعه به سبد فاکتور افزوده شد ✓")
                                                } else {
                                                    mainViewModel.addItemToInvoice()
                                                    mainViewModel.selectTab(AppTab.INVOICES)
                                                }
                                            }
                                        }
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
                                        onDeleteInvoice = { invoiceId ->
                                            if (barterInvoiceViewModel.deleteInvoice(invoiceId)) {
                                                customerViewModel.loadCustomers()
                                            }
                                            QiratoToast.show(context, barterInvoiceViewModel.uiState.value.statusMessage)
                                        },
                                        onPrintClick = { item ->
                                            if (!licenseInfo.isLicensed) {
                                                licenseViewModel.setActivationDialogVisible(true)
                                                QiratoToast.show(context, "چاپ فاکتور نیازمند اشتراک معتبر است.")
                                            } else {
                                                val invoice = item.barterInvoice
                                                if (invoice == null) {
                                                    QiratoToast.show(context, "اطلاعات کامل فاکتور برای چاپ موجود نیست")
                                                } else {
                                                    openPdfPreview(invoice)
                                                }
                                            }
                                        }
                                    )
                                }

                                AppTab.MORE -> {
                                    MoreHubScreen(
                                        onOpenCloudSettings = { showCloudSettings = true },
                                        cloudEnabled = cloudState.enabled,
                                        cloudStatus = cloudState.status.title(),
                                        onToggleCloud = { enabled ->
                                            if (enabled) {
                                                if (cloudState.phone.isBlank() || cloudState.status == com.goldex.companion.data.sync.SyncStatus.AUTH_REQUIRED) {
                                                    showCloudSettings = true
                                                } else {
                                                    cloudViewModel.setEnabled(true)
                                                }
                                            } else {
                                                cloudViewModel.setEnabled(false)
                                            }
                                        },
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
                        val currentSpot = GoldCalculationUseCases.toSpotPrice18k(
                            PersianNumberFormatter.parseToCleanLong(mainUiState.spotPriceInput) ?: 0L,
                            mainUiState.priceBasisTab
                        ).takeIf { it > 0 } ?: mainUiState.rates.gold18.takeIf { it > 0 } ?: 23_360_000L

                        if (barterUiState.subScreen == InvoicesSubScreen.LIST) {
                            barterInvoiceViewModel.openNewInvoice(currentSpot)
                        }

                        val eq18k = karatConvertUiState.convertedWeight
                        val barterItem = BarterCalculationUseCases.calculateCraftedItem(
                            title = "طلای تبدیل عیار (${karatConvertUiState.convertFromKarat.karatNumber} به ${karatConvertUiState.convertToKarat.karatNumber})",
                            karat = Karat.K18,
                            customKaratValue = 750,
                            grossWeight = eq18k,
                            stoneWeight = 0.0,
                            spotPrice18k = currentSpot,
                            wageType = WageType.PERCENTAGE,
                            wageInput = 0.0,
                            profitPercent = 0.0,
                            taxPercent = 0.0
                        )
                        barterInvoiceViewModel.addSalesItem(barterItem)
                        mainViewModel.addItemToInvoice()
                        mainViewModel.setKaratConvertVisible(false)
                        mainViewModel.selectTab(AppTab.INVOICES)
                        QiratoToast.show(context, "قطعه به سبد فاکتور افزوده شد ✓")
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

                        val barterItem = BarterCalculationUseCases.calculateCraftedItem(
                            title = item.title,
                            karat = item.karat,
                            customKaratValue = item.customKaratValue,
                            grossWeight = item.grossWeightGrams,
                            stoneWeight = item.stoneWeightGrams,
                            spotPrice18k = currentSpot,
                            wageType = WageType.PERCENTAGE,
                            wageInput = item.wagePercent,
                            profitPercent = item.profitPercent,
                            taxPercent = item.taxPercent
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
                        QiratoToast.show(context, barterInvoiceViewModel.uiState.value.statusMessage)
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
                    cloudContent = { WizardCloudSyncSection(cloudViewModel) },
                    cloudReady = { !cloudState.enabled || (cloudState.phone.isNotBlank() && !cloudState.busy && cloudState.status in listOf(com.goldex.companion.data.sync.SyncStatus.PENDING, com.goldex.companion.data.sync.SyncStatus.SYNCED)) },
                    cloudRestored = { cloudState.restoredGeneration > 0 },
                    onDeferCloud = { cloudViewModel.setEnabled(false) },
                    onOpenCloudRestore = { showCloudSettings = true },
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
                    onFinish = { targetTab, updatedSettings, initialInventory, _ ->
                        val completed = onboardingViewModel.complete(
                            updatedSettings, initialInventory,
                            restoredFromCloud = cloudState.restoredGeneration > 0L
                        )
                        settingsViewModel.loadSettings()
                        if (completed) {
                            inventoryViewModel.loadItems()
                            portfolioViewModel.loadPortfolio()
                            val applied = settingsViewModel.uiState.value.appSettings
                            mainViewModel.applySettingsDefaults(
                                applied.defaultProfitPercent, applied.defaultTaxPercent, applied.defaultWageType
                            )
                            mainViewModel.setWizardVisible(false)
                            mainViewModel.selectTab(targetTab)
                            QiratoToast.show(context, "پیکربندی اولیه با موفقیت انجام شد")
                        } else {
                            QiratoToast.show(context, onboardingViewModel.state.value.errorMessage.orEmpty())
                        }
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


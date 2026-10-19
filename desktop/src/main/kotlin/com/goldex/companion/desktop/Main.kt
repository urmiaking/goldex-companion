package com.goldex.companion.desktop
import com.goldex.companion.desktop.ui.controls.DesktopAlertDialog as AlertDialog
import com.goldex.companion.desktop.ui.controls.DesktopDialogHost
import com.goldex.companion.desktop.ui.controls.LocalDesktopReduceMotion

import com.goldex.companion.desktop.ui.controls.DesktopActionButton as GoldButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.update.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.presentation.calculator.*
import com.goldex.companion.ui.theme.*
import org.jetbrains.skia.Data
import org.jetbrains.skia.Typeface
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FileDialog
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

fun desktopVersion(): String = Properties().apply {
    checkNotNull(ManualGoldCalculator::class.java.getResourceAsStream("/version.properties")).use { load(it) }
}.getProperty("version")

fun main(args: Array<String>) {
    if ("--verify-runtime" in args) {
        try {
            verifyRuntime()
            kotlin.system.exitProcess(0)
        } catch (t: Throwable) {
            java.nio.file.Files.writeString(java.nio.file.Path.of("runtime-error.txt"), t.stackTraceToString())
            kotlin.system.exitProcess(1)
        }
    }
    launchWorkspace()
}

private fun verifyRuntime() {
    val font = checkNotNull(ManualGoldCalculator::class.java.getResourceAsStream("/font/vazirmatn_regular.ttf"))
    font.use { input -> Data.makeFromBytes(input.readBytes()).use { data ->
        checkNotNull(Typeface.makeFromData(data)).use { check(it.familyName.isNotBlank()) }
    } }
    checkNotNull(ManualGoldCalculator::class.java.getResourceAsStream("/mipmap-xxxhdpi/ic_launcher.png")).use { input ->
        org.jetbrains.skia.Image.makeFromEncoded(input.readBytes()).use { check(it.width > 0 && it.height > 0) }
    }
    val calculator = ManualGoldCalculator().apply {
        setInput(CalculatorField.SPOT, "6000000"); setInput(CalculatorField.GROSS_WEIGHT, "2"); setInput(CalculatorField.WAGE, "10")
    }
    check(calculator.state.value.result?.totalPayable?.toLong() == 14_315_160L)
    check(PersistenceJsonCodecs.decodeSettings("{}").defaultProfitPercent == "7")
    check(desktopVersion().matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")))
    checkNotNull(ManualGoldCalculator::class.java.getResourceAsStream("/update/install-msi.ps1")).use { check(it.read() >= 0) }
    val inventory = com.goldex.companion.presentation.inventory.InventoryForm.toItem(com.goldex.companion.presentation.inventory.InventoryDraft(code = "RNG-123", title = "آزمون", gross = "2"))
    check(com.goldex.companion.presentation.inventory.InventoryForm.price(inventory, 6_000_000)!!.total == 14_315_160L)
    InventoryLabelPrinter.render(inventory).let { check(it.width == 600); it.flush() }
    val customerDirectory = java.nio.file.Files.createTempDirectory("qirato-customer-runtime-")
    try {
        DesktopDataStore(customerDirectory).use { store ->
            val customers = DesktopCustomerStore(store)
            val contact = com.goldex.companion.model.Customer(id = "runtime-customer", name = "آزمون اجرا", goldDebtGrams = 2.0)
            customers.addCustomer(contact)
            val invoices = DesktopCustomerInvoiceStore(store, customers)
            val settlement = com.goldex.companion.domain.customers.RecordCustomerSettlementUseCase(customers, invoices, customers)
            val tx = settlement.record(com.goldex.companion.domain.customers.SettlementRequest(id = "runtime-settlement",
                customerId = contact.id, targetType = com.goldex.companion.model.LedgerEntryType.GOLD_WEIGHT,
                paymentType = com.goldex.companion.model.LedgerEntryType.CASH_RIAL, amountTomans = 12_000_000, rateTomans = 6_000_000),
                contact, com.goldex.companion.domain.customers.OutstandingBalance(2.0, 0))
            check(customers.getCustomers().single().goldDebtGrams == 0.0)
            check(customers.getTransactions(contact.id).single().settlement == tx.settlement)
            val sale = com.goldex.companion.model.BarterInvoice(id = "runtime-invoice", customer = customers.getCustomers().single(),
                debtBasis = com.goldex.companion.model.InvoiceDebtBasis.CASH, spotPrice18k = 6_000_000,
                salesItems = listOf(com.goldex.companion.model.BankCoinItem(unitPrice = 75_000_000,
                    totalPayable = 75_000_000.0, equivalent18kWeight = 9.759)))
            com.goldex.companion.domain.invoice.SaveBarterInvoiceUseCase(invoices, customers, customers).save(sale)
            check(customers.getCustomers().single().cashDebtTomans == 75_000_000L)
            val pdfPath = customerDirectory.resolve("invoice-runtime.pdf")
            DesktopInvoiceDocuments.save(sale, store.loadSettings(), pdfPath)
            org.apache.pdfbox.Loader.loadPDF(pdfPath.toFile()).use { check(it.numberOfPages == 1) }
            check(com.goldex.companion.domain.invoice.InvoiceDeletionUseCase(invoices, customers, customers).delete(sale.id)
                is com.goldex.companion.domain.invoice.InvoiceDeletionResult.Deleted)
            check(customers.getCustomers().single().cashDebtTomans == 0L)
        }
        DesktopDataStore(customerDirectory).use { check(DesktopCustomerStore(it).getTransactions("runtime-customer").size == 1) }
    } finally {
        customerDirectory.toFile().deleteRecursively()
    }
    println("Qirato runtime version=${desktopVersion()}")
    println("Qirato font, icon, shared codecs, customer/invoice storage, financial and official PDF runtime verification passed")
}

private fun launchWorkspace() {
    val directory = DesktopDataStore.defaultDirectory()
    val initialized = runCatching {
        val storage = DesktopDataStore(directory)
        try { storage to DesktopWorkspace(storage, DesktopMarketRepository(storage)) }
        catch (failure: Exception) { storage.close(); throw failure }
    }
    val closed = AtomicBoolean(false)
    val updater = WindowsUpdater(WindowsUpdateInstaller(checkNotNull(WindowsVersion.parse(desktopVersion())), directory))
    fun closeData() { if (closed.compareAndSet(false, true)) { updater.close(); initialized.getOrNull()?.let { (store, workspace) -> workspace.close(); store.close() } } }
    application {
        DisposableEffect(Unit) { onDispose { closeData() } }
        val workspace = initialized.getOrNull()?.second
        val state = workspace?.state?.collectAsState()?.value
        val inventoryState = workspace?.inventory?.state?.collectAsState()?.value
        val customerState = workspace?.customers?.manager?.uiState?.collectAsState()?.value
        val settlementState = workspace?.customers?.settlement?.state?.collectAsState()?.value
        val invoiceState = workspace?.invoices?.manager?.uiState?.collectAsState()?.value
        val invoiceHost = workspace?.invoices?.host?.collectAsState()?.value
        var confirmClose by remember { mutableStateOf(false) }
        LaunchedEffect(workspace) {
            workspace?.start()
            if (workspace != null && !workspace.state.value.settings.hasCompletedOnboarding) {
                workspace.openWizard()
            }
        }
        Window(
            onCloseRequest = {
                if (state?.saving == true || inventoryState?.saving == true || state?.draft != null || state?.settingsDraft != null || inventoryState?.draft != null || inventoryState?.movement != null || workspace?.customers?.saving == true || workspace?.customers?.hasDialog == true || workspace?.invoices?.saving == true || workspace?.invoices?.hasDraft == true || workspace?.invoices?.hasDialog == true) confirmClose = true
                else { closeData(); exitApplication() }
            }, title = "قیراط | ${state?.destination?.title ?: "اطلاعات"}",
            icon = painterResource("mipmap-xxxhdpi/ic_launcher.png"), state = rememberWindowState(width = 1400.dp, height = 900.dp),
            onPreviewKeyEvent = { event ->
                if (workspace != null && event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    when (event.key) {
                        Key.One -> workspace.navigate(DesktopDestination.DASHBOARD)
                        Key.Two -> workspace.navigate(DesktopDestination.CALCULATOR)
                        Key.Three -> workspace.navigate(DesktopDestination.RATES)
                        Key.Four -> workspace.navigate(DesktopDestination.INVENTORY)
                        Key.Five -> workspace.navigate(DesktopDestination.SETTINGS)
                        Key.Six -> workspace.navigate(DesktopDestination.INVENTORY)
                        Key.Seven -> workspace.navigate(DesktopDestination.CUSTOMERS)
                        Key.Eight -> workspace.navigate(DesktopDestination.INVOICES)
                        Key.R -> { workspace.refreshRates() }
                        Key.N -> if (workspace.state.value.draft == null && workspace.state.value.pendingDelete == null && !workspace.inventory.state.value.hasDialog && !workspace.customers.hasDialog && !workspace.customers.saving) {
                            workspace.openInventoryItem()
                        }
                        Key.S -> if (workspace.inventory.state.value.draft != null) workspace.inventory.save()
                            else if (workspace.inventory.state.value.movement != null) workspace.inventory.saveMovement()
                            else return@Window false
                        else -> return@Window false
                    }
                    true
                } else false
            }
        ) {
            window.minimumSize = Dimension(940, 700)
            GoldExCompanionTheme(isDarkTheme = state?.dark ?: false) {
                CompositionLocalProvider(LocalDesktopReduceMotion provides (state?.reduceMotion ?: false)) {
                DesktopDialogHost(confirmClose) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val busy = state?.saving == true || inventoryState?.saving == true || customerState?.isSaving == true || settlementState?.isSaving == true || invoiceState?.isSaving == true || invoiceHost?.documentBusy == true
                    AlertDialog(onDismissRequest = { confirmClose = false }, title = { Text(if (busy) "ذخیره اطلاعات در حال انجام است" else "بستن برنامه بدون ذخیره؟") },
                        text = { Text(if (busy) "پس از پایان عملیات می‌توانید برنامه را ببندید." else "فرم باز یا تنظیمات ویرایش‌شده ذخیره نشده‌اند. پیش از خروج، آن‌ها را ذخیره کنید.") },
                        confirmButton = { Row(Modifier.width(380.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GoldButton("بازگشت به برنامه", { confirmClose = false }, Modifier.weight(1f), isSecondary = true, icon = Icons.Outlined.ArrowForward)
                            GoldButton("خروج", { closeData(); exitApplication() }, Modifier.weight(1f), enabled = !busy, icon = Icons.Outlined.ExitToApp)
                        } })
                } }
                }
                if (workspace != null) DesktopWorkspaceScreen(workspace, version = desktopVersion(), updater = updater,
                    onRestart = { if (!workspace.state.value.saving && workspace.state.value.draft == null && workspace.state.value.pendingDelete == null && workspace.state.value.settingsDraft == null && !workspace.inventory.state.value.saving && !workspace.inventory.state.value.hasDialog && !workspace.customers.saving && !workspace.customers.hasDialog && !workspace.invoices.saving && !workspace.invoices.hasDialog && !workspace.invoices.hasDraft) updater.restart { javax.swing.SwingUtilities.invokeLater { closeData(); exitApplication() } } }, onBackup = {
                    FileDialog(window, "ذخیره فایل پشتیبان", FileDialog.SAVE).apply {
                        file = "Qirato-backup-${java.time.LocalDate.now()}.json"
                        isVisible = true
                        if (file != null && this.directory != null) workspace.exportBackup(Path.of(this.directory, file))
                        dispose()
                    }
                }) else CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(color = LocalGoldExColors.current.background) {
                        Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("اطلاعات قابل بازکردن نیست", style = MaterialTheme.typography.headlineMedium)
                            Text("اگر برنامه در پنجره دیگری باز است، آن را ببندید. فایل اصلی و پشتیبان شما حفظ شده‌اند؛ برای بررسی یا بازیابی، پوشه اطلاعات را باز کنید.")
                            GoldButton("بازکردن پوشه اطلاعات", { Desktop.getDesktop().open(directory.toFile()) }, isSecondary = true, icon = Icons.Outlined.FolderOpen)
                            GoldButton("بستن", { exitApplication() }, icon = Icons.Outlined.Close)
                        }
                    }
                }
            }
        }
    }
}

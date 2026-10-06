package com.goldex.companion.desktop

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
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.presentation.calculator.*
import com.goldex.companion.ui.components.GoldButton
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
    if ("--verify-runtime" in args) { verifyRuntime(); return }
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
    println("Qirato font, icon, shared codecs and financial runtime verification passed")
}

private fun launchWorkspace() {
    val directory = DesktopDataStore.defaultDirectory()
    val initialized = runCatching {
        val storage = DesktopDataStore(directory)
        try { storage to DesktopWorkspace(storage, DesktopMarketRepository(storage)) }
        catch (failure: Exception) { storage.close(); throw failure }
    }
    val closed = AtomicBoolean(false)
    fun closeData() { if (closed.compareAndSet(false, true)) initialized.getOrNull()?.let { (store, workspace) -> workspace.close(); store.close() } }
    application {
        DisposableEffect(Unit) { onDispose { closeData() } }
        val workspace = initialized.getOrNull()?.second
        val state = workspace?.state?.collectAsState()?.value
        LaunchedEffect(workspace) { workspace?.start() }
        Window(
            onCloseRequest = { closeData(); exitApplication() }, title = "قیراط | ${state?.destination?.title ?: "اطلاعات"}",
            icon = painterResource("mipmap-xxxhdpi/ic_launcher.png"), state = rememberWindowState(width = 1400.dp, height = 900.dp),
            onPreviewKeyEvent = { event ->
                if (workspace != null && event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    when (event.key) {
                        Key.One -> workspace.navigate(DesktopDestination.DASHBOARD)
                        Key.Two -> workspace.navigate(DesktopDestination.CALCULATOR)
                        Key.Three -> workspace.navigate(DesktopDestination.RATES)
                        Key.Four -> workspace.navigate(DesktopDestination.PORTFOLIO)
                        Key.Five -> workspace.navigate(DesktopDestination.SETTINGS)
                        Key.R -> { workspace.refreshRates() }
                        Key.N -> if (workspace.state.value.draft == null && workspace.state.value.pendingDelete == null) { workspace.navigate(DesktopDestination.PORTFOLIO); workspace.openAsset() }
                        else -> return@Window false
                    }
                    true
                } else false
            }
        ) {
            window.minimumSize = Dimension(940, 700)
            GoldExCompanionTheme(isDarkTheme = state?.dark ?: false) {
                if (workspace != null) DesktopWorkspaceScreen(workspace, version = desktopVersion(), onBackup = {
                    FileDialog(window, "ذخیرهٔ فایل پشتیبان", FileDialog.SAVE).apply {
                        file = "Qirato-backup-${java.time.LocalDate.now()}.json"
                        isVisible = true
                        if (file != null && this.directory != null) workspace.exportBackup(Path.of(this.directory, file))
                        dispose()
                    }
                }) else CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(color = LocalGoldExColors.current.background) {
                        Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("اطلاعات قابل بازکردن نیست", style = MaterialTheme.typography.headlineMedium)
                            Text("اگر برنامه در پنجرهٔ دیگری باز است، آن را ببندید. فایل اصلی و پشتیبان شما حفظ شده‌اند؛ برای بررسی یا بازیابی، پوشهٔ اطلاعات را باز کنید.")
                            GoldButton("بازکردن پوشهٔ اطلاعات", { Desktop.getDesktop().open(directory.toFile()) }, isSecondary = true)
                            GoldButton("بستن", { exitApplication() })
                        }
                    }
                }
            }
        }
    }
}

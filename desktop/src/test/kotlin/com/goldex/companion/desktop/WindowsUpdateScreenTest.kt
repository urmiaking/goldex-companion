package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.goldex.companion.desktop.ui.WindowsUpdatePrompt
import com.goldex.companion.desktop.update.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WindowsUpdateScreenTest {
    @get:Rule val compose = createComposeRule()
    private val release = WindowsRelease(WindowsVersion(0, 56, 38), "windows-v0.56.38", "✨ تغییرات نسخه\n• دریافت خودکار به‌روزرسانی‌ها\n• نصب آسان پس از تأیید شما", URI("https://github.com"), 100, "a".repeat(64))
    private lateinit var updater: WindowsUpdater
    @Before fun prepare() = runBlocking {
        updater = WindowsUpdater(object : WindowsUpdateGateway {
            override suspend fun check() = release
            override suspend fun prepare(release: WindowsRelease, progress: (Long,Long)->Unit, verifying:()->Unit) =
                PreparedWindowsUpdate(release, Path.of("Qirato"), Path.of("staging"), Path.of("bundle"))
            override suspend fun launch(update: PreparedWindowsUpdate) = error("UI must emit restart intent only")
        })
        updater.check()?.join()
        Unit
    }
    @After fun close() { updater.close() }

    @Test fun readyPromptOffersPostponeWithoutRestartingAndRendersBothThemes() {
        var restart = 0
        val dark = mutableStateOf(false)
        compose.setContent {
            GoldExCompanionTheme(isDarkTheme = dark.value) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    WindowsUpdatePrompt(updater, true) { restart++ }
                }
            }
        }
        compose.onNodeWithTag("restart-update").assertIsEnabled()
        screenshot("update-light.png")
        compose.runOnIdle { dark.value = true }
        screenshot("update-dark.png")
        compose.onNodeWithTag("postpone-update").performClick()
        assertFalse(updater.state.value.dialog); assertEquals(0, restart)
        compose.onNodeWithTag("update-dialog").assertDoesNotExist()
    }

    @Test fun restartIsDisabledWhileFinancialSaveIsInProgress() {
        var restart = 0
        compose.setContent { GoldExCompanionTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { WindowsUpdatePrompt(updater, false) { restart++ } }
        } }
        compose.onNodeWithTag("restart-update").assertIsNotEnabled()
        compose.onNodeWithTag("postpone-update").assertIsEnabled()
        assertEquals(0, restart)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("update-dialog").captureToImage().asSkiaBitmap()
        val output = File(System.getProperty("qirato.screenshotDir"), name).apply { parentFile.mkdirs() }
        org.jetbrains.skia.Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()?.use { output.writeBytes(it.bytes) } }
    }
}

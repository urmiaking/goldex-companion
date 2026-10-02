package com.goldex.companion.ui.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.goldex.companion.data.sync.SyncStatus
import com.goldex.companion.data.sync.SyncUiState
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp")
class CloudAccountActionsTest {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(SyncUiState(enabled = true, phone = "09123456789", status = SyncStatus.SYNCED))
    private val busy = mutableStateOf(false)
    private val dark = mutableStateOf(false)
    private val events = mutableListOf<String>()

    @Test fun maintenanceRequiresExpansionAndRoutesActionsInBothThemes() {
        render()
        for (theme in listOf(false, true)) {
            compose.runOnIdle { dark.value = theme }
            compose.onNodeWithText("خروج از حساب").assertDoesNotExist()
            compose.onNodeWithText("همگام‌سازی اکنون").performClick()
            compose.onNodeWithText("گزینه‌های بیشتر").performClick()
            assertEquals(listOf("sync"), events)
            for ((label, event) in listOf(
                "انتقال به دستگاه دیگر" to "devices",
                "ذخیرهٔ پشتیبان محلی و اختلاف‌ها" to "backup",
                "جداسازی داده برای اتصال به حساب دیگر" to "detach",
                "خروج از حساب" to "logout"
            )) {
                compose.onNodeWithText(label).performScrollTo().assertIsEnabled().performClick()
                assertEquals(event, events.last())
            }
            compose.onNodeWithText("گزینه‌های بیشتر").performScrollTo().performClick()
            compose.onNodeWithText("خروج از حساب").assertDoesNotExist()
            compose.runOnIdle { events.clear() }
        }
    }

    @Test fun recoveryRemainsVisibleAndWriterTransferUsesSeparateIntents() {
        compose.runOnIdle { state.value = state.value.copy(status = SyncStatus.CONFLICT) }
        render()
        compose.onNodeWithText("مقایسهٔ نسخه‌های کل گروه").performClick()
        compose.onNodeWithText("بررسی و بازیابی نسخهٔ ابری").performClick()
        assertEquals(listOf("review", "restore"), events)
        compose.onNodeWithText("خروج از حساب").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(status = SyncStatus.WRITER_CHANGED) }
        compose.onNodeWithText("انتقال نویسندگی به این دستگاه").performScrollTo().performClick()
        compose.onNodeWithText("تأیید تازهٔ شماره برای انتقال").performScrollTo().performClick()
        assertEquals(listOf("review", "restore", "takeover", "reauthenticate"), events)
    }

    @Test fun busyStateBlocksMaintenanceAndLoggedOutReadOnlyDataStillAllowsBackup() {
        render()
        compose.onNodeWithText("گزینه‌های بیشتر").performClick()
        compose.runOnIdle { busy.value = true }
        for (label in listOf("انتقال به دستگاه دیگر", "ذخیرهٔ پشتیبان محلی و اختلاف‌ها", "خروج از حساب")) {
            compose.onNodeWithText(label).performScrollTo().assertIsNotEnabled().performClick()
        }
        assertEquals(emptyList<String>(), events)
        compose.runOnIdle {
            busy.value = false
            state.value = SyncUiState(enabled = false, readOnly = true, status = SyncStatus.AUTH_REQUIRED)
        }
        compose.onNodeWithText("همگام‌سازی اکنون").assertDoesNotExist()
        compose.onNodeWithText("گزینه‌های بیشتر").performScrollTo().performClick()
        compose.onNodeWithText("خروج از حساب").assertDoesNotExist()
        compose.onNodeWithText("ذخیرهٔ پشتیبان محلی و اختلاف‌ها").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf("backup"), events)
    }

    @Test @Config(qualifiers = "w800dp-h360dp-land")
    fun expandedActionsRemainReachableInLandscapeWithLargeText() {
        render(fontScale = 1.5f)
        compose.onNodeWithText("گزینه‌های بیشتر").performScrollTo().performClick()
        compose.onNodeWithText("جداسازی داده برای اتصال به حساب دیگر").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("خروج از حساب").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(listOf("detach", "logout"), events)
    }

    private fun render(fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, fontScale)
            ) {
                GoldExCompanionTheme(isDarkTheme = dark.value) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        CloudAccountActions(
                            state.value, busy.value,
                            onSync = { events += "sync" },
                            onReviewConflict = { events += "review" },
                            onRestore = { events += "restore" },
                            onTakeover = { events += "takeover" },
                            onReauthenticate = { events += "reauthenticate" },
                            onListDevices = { events += "devices" },
                            onExportBackup = { events += "backup" },
                            onLogout = { events += "logout" },
                            onDetach = { events += "detach" }
                        )
                    }
                }
            }
        }
    }
}

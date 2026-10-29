package com.goldex.companion.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.sync.*
import com.goldex.companion.desktop.ui.*
import com.goldex.companion.desktop.ui.controls.*
import com.goldex.companion.ui.theme.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class WindowsHoverCloudTest {
    @Test fun roundedHeaderAndBodyFeedbackLeavesCornersUntouchedInBothThemes() = runDesktopComposeUiTest(width = 940, height = 440) {
        var dark by mutableStateOf(false)
        setContent {
            GoldExCompanionTheme(dark) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.fillMaxSize().background(LocalGoldExColors.current.background).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        WorkspaceHeader(WorkspaceState(), cloudState = DesktopSyncUiState(), onCloudClick = {})
                        DesktopActionButton("همگام‌سازی", {}, Modifier.width(180.dp).testTag("body-hover"), Icons.Outlined.Sync, isSecondary = true)
                        DesktopQuickAccessTile("همگام‌سازی", Icons.Outlined.Sync, {}, Modifier.width(180.dp).testTag("tile-hover"))
                        DesktopInlineChipField("", {}, false, {}, Modifier.width(180.dp).testTag("inline-hover"))
                    }
                }
            }
        }
        for (theme in listOf(false, true)) {
            runOnIdle { dark = theme }
            for (tag in listOf("cloud-sync-chip", "body-hover", "tile-hover", "inline-hover")) {
                val node = onNodeWithTag(tag)
                node.performMouseInput { moveTo(Offset(-20f, -20f)) }
                val before = node.captureToImage().toPixelMap()
                node.performMouseInput { moveTo(center) }
                val after = node.captureToImage().toPixelMap()
                var changed = 0
                for (y in 0 until after.height) for (x in 0 until after.width) {
                    if (before[x,y] != after[x,y]) changed++
                }
                assertTrue(changed > 0, "$tag must retain visible hover feedback")
                for (y in 0..2) for (x in 0..2) {
                    for (px in listOf(x, after.width - 1 - x)) for (py in listOf(y, after.height - 1 - y))
                        assertEquals(before[px,py], after[px,py], "$tag paints a rectangular hover corner ($px,$py)")
                }
            }
        }
    }

    @Test fun cloudModalShowsRealStatesAndGuardsBusyActionsInRtl() = runDesktopComposeUiTest(width = 940, height = 700) {
        var state by mutableStateOf(DesktopSyncUiState(enabled = true, status = DesktopSyncStatus.PENDING,
            phone = "09123456789", pendingCount = 3, lastSuccessAt = 1792798200000))
        var dark by mutableStateOf(false)
        var syncs = 0; var logouts = 0; var logins = 0; var closes = 0
        setContent {
            GoldExCompanionTheme(dark) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDesktopReduceMotion provides true) {
                    DesktopDialogHost(true) {
                        CloudAccountDialog(state, { closes++ }, { logins++ }, { state = state.copy(enabled = it) }, { syncs++ }, { logouts++ })
                    }
                }
            }
        }
        for (theme in listOf(false, true)) {
            runOnIdle { dark = theme }
            onNodeWithTag("cloud-dialog-pending").assertTextContains("۳", substring = true)
        }
        onNodeWithTag("cloud-dialog-sync").performSemanticsAction(SemanticsActions.RequestFocus)
        onNodeWithTag("cloud-dialog-sync").performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals(1, syncs); state = state.copy(busy = true, status = DesktopSyncStatus.SYNCING) }
        onNodeWithTag("cloud-dialog-sync").assertIsNotEnabled()
        onNodeWithTag("cloud-dialog-logout").assertIsNotEnabled()
        onNodeWithTag("cloud-dialog-enabled").assertIsNotEnabled()
        onNodeWithTag("cloud-dialog-close").assertIsNotEnabled()
        runOnIdle { state = state.copy(busy = false, status = DesktopSyncStatus.OFFLINE) }
        onNodeWithTag("cloud-dialog-enabled").performClick()
        onNodeWithTag("cloud-dialog-sync").assertIsNotEnabled()
        onNodeWithTag("cloud-dialog-logout").performClick()
        runOnIdle { assertEquals(1, logouts); state = DesktopSyncUiState(status = DesktopSyncStatus.AUTH_REQUIRED) }
        onNodeWithTag("cloud-dialog-login").performClick()
        runOnIdle { assertEquals(1, logins) }
        onNodeWithTag("cloud-dialog-close").performClick()
        runOnIdle { assertEquals(1, closes); assertEquals(1, syncs) }
    }

    @Test fun headerAndSettingsOpenSameModalAndLoginReturnsWithoutChangingPage() {
        val directory = Files.createTempDirectory("qirato-cloud-ui")
        DesktopDataStore(directory).use { store ->
            store.saveReduceMotion(true)
            val workspace = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline fixture") }))
            try {
                workspace.navigate(DesktopDestination.CUSTOMERS)
                runDesktopComposeUiTest(width = 940, height = 700) {
                    setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "test") } }
                    onNodeWithTag("cloud-sync-chip").performClick()
                    onNodeWithTag("cloud-account-dialog").assertIsDisplayed()
                    onNodeWithTag("cloud-dialog-login").performClick()
                    onNodeWithTag("cloud-otp-dialog").assertIsDisplayed()
                    onNodeWithText("انصراف").performClick()
                    onNodeWithTag("cloud-account-dialog").assertIsDisplayed()
                    onNodeWithTag("cloud-dialog-close").performClick()
                    onNodeWithTag("cloud-account-dialog").assertDoesNotExist()
                    runOnIdle { assertEquals(DesktopDestination.CUSTOMERS, workspace.state.value.destination) }
                    onNodeWithTag("nav-SETTINGS").performClick()
                    onNodeWithTag("open-cloud-dialog").performScrollTo().performClick()
                    onNodeWithTag("cloud-account-dialog").assertIsDisplayed()
                    onNodeWithTag("cloud-dialog-close").performClick()
                    onNodeWithTag("cloud-sync-chip").performClick()
                    onNodeWithTag("cloud-account-dialog").assertIsDisplayed()
                    onNodeWithTag("cloud-otp-dialog").assertDoesNotExist()
                    runOnIdle { assertFalse(workspace.cloud.state.value.busy); assertEquals(DesktopDestination.SETTINGS, workspace.state.value.destination) }
                }
            } finally { workspace.close() }
        }
    }
}

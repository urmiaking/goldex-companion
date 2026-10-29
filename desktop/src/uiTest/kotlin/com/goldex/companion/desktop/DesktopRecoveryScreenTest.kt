package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.DesktopMarketRepository
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.desktop.ui.CloudAccountDialog
import com.goldex.companion.desktop.ui.controls.LocalDesktopReduceMotion
import com.goldex.companion.desktop.sync.DesktopSyncStatus
import com.goldex.companion.desktop.sync.DesktopSyncUiState
import com.goldex.companion.desktop.wizard.DesktopOnboardingWizardScreen
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DesktopRecoveryScreenTest {
    @Test fun wizardExistingAccountActionFitsMinimumWindowInBothThemes() = wizard(940, 700)
    @Test fun wizardExistingAccountActionFitsWideWindowInBothThemes() = wizard(1400, 980)

    private fun wizard(width: Int, height: Int) {
        DesktopDataStore(Files.createTempDirectory("qirato-recovery-ui")).use { store ->
            val workspace = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline fixture") }))
            try {
                runDesktopComposeUiTest(width = width, height = height) {
                    var dark by mutableStateOf(false)
                    setContent {
                        GoldExCompanionTheme(dark) {
                            CompositionLocalProvider(LocalDesktopReduceMotion provides true) {
                                DesktopOnboardingWizardScreen(workspace, workspace::closeWizard)
                            }
                        }
                    }
                    for (theme in listOf(false, true)) {
                        runOnIdle { dark = theme }
                        onNodeWithTag("wizard-cloud-restore").assertIsDisplayed()
                        onNodeWithTag("wizard-next-btn").assertIsDisplayed()
                    }
                }
            } finally { workspace.close() }
        }
    }

    @Test fun existingAccountRequiresExplicitRecoveryAndBusyDisablesIt() = runDesktopComposeUiTest(width = 940, height = 700) {
        var state by mutableStateOf(DesktopSyncUiState(enabled = true, phone = "09120000000", status = DesktopSyncStatus.RESTORE_REQUIRED))
        var restores = 0
        setContent {
            GoldExCompanionTheme(false) {
                CloudAccountDialog(state, {}, {}, {}, {}, {}, onRestore = { restores++ })
            }
        }
        onNodeWithTag("cloud-dialog-restore").assertIsDisplayed().performClick()
        runOnIdle { assertEquals(1, restores); state = state.copy(busy = true) }
        onNodeWithTag("cloud-dialog-restore").assertIsNotEnabled()
    }
}

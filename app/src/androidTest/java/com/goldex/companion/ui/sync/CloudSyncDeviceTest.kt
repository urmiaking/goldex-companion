package com.goldex.companion.ui.sync

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.goldex.companion.data.sync.SyncStatus
import com.goldex.companion.data.sync.SyncUiState
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class CloudSyncDeviceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun everyCloudStateIsAccessibleClickableInRtlAndBothThemes() {
        val status=mutableStateOf(SyncStatus.PENDING)
        val dark=mutableStateOf(false)
        val reduced=mutableStateOf(true)
        var clicks=0
        compose.setContent {
            GoldExCompanionTheme(isDarkTheme=dark.value) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    CloudSyncButton(SyncUiState(enabled=true,status=status.value),reducedMotion=reduced.value,onClick={ clicks++ })
                }
            }
        }
        for(theme in listOf(false,true)) for(state in SyncStatus.entries.filter { it!=SyncStatus.DISABLED }) {
            compose.runOnIdle { dark.value=theme; status.value=state }
            compose.onNodeWithContentDescription(state.title(),useUnmergedTree=false).assertExists().performClick()
        }
        assertEquals((SyncStatus.entries.size-1)*2,clicks)
        compose.runOnIdle { reduced.value=false; status.value=SyncStatus.SYNCING }
        compose.mainClock.autoAdvance=false
        compose.mainClock.advanceTimeBy(1200)
        compose.runOnIdle { status.value=SyncStatus.SYNCED }
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithContentDescription(SyncStatus.SYNCED.title()).assertExists()
    }
}

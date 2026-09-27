package com.goldex.companion.ui.sync

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goldex.companion.data.sync.SyncStatus
import com.goldex.companion.data.sync.SyncUiState
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class CloudSyncDeviceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun modalLoginHasSeparatePhoneAndCodeStepsAndDismissesInBothThemes() {
        var requests=0; var verifications=0; var dismissals=0
        val phone=mutableStateOf(""); val code=mutableStateOf("")
        val form=mutableStateOf(CloudFormState()); val dark=mutableStateOf(false)
        val show=mutableStateOf(true)
        compose.setContent {
            GoldExCompanionTheme(isDarkTheme=dark.value) {
                if(show.value) CloudSettingsModal(onDismiss={ dismissals++; show.value=false }) {
                    Column(Modifier.padding(20.dp)) {
                        CloudSignInForm(phone.value,code.value,form.value,
                            onPhoneChange={ phone.value=it },onCodeChange={ code.value=it },
                            onRequest={ requests++; form.value=CloudFormState(phone=phone.value,requestedAtMillis=System.currentTimeMillis(),
                                challenge=JSONObject().put("challengeId","test").put("otpMode","temporary").put("temporaryCode","123456").put("retryAfter",60)) },
                            onVerify={ verifications++ },onChangePhone={ form.value=CloudFormState() },onResend={ requests++ })
                    }
                }
            }
        }
        compose.onNodeWithText("شماره موبایل").performTextInput("09123456789")
        compose.onNodeWithText("دریافت کد ورود").performClick()
        compose.onNodeWithText("کد ورود موقت: ۱۲۳۴۵۶").assertExists()
        compose.onNodeWithText("کد ورود شش‌رقمی").performTextInput("123456")
        compose.onNodeWithText("تأیید و اتصال").performClick()
        assertEquals(1,requests); assertEquals(1,verifications)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        for(theme in listOf(false,true)) {
            compose.runOnIdle { dark.value=theme }
            compose.waitForIdle()
            val directory=File(instrumentation.targetContext.getExternalFilesDir(null),"cloud-ui").apply { mkdirs() }
            val bitmap=instrumentation.uiAutomation.takeScreenshot()
            File(directory,if(theme) "cloud-modal-dark.png" else "cloud-modal-light.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
            }; bitmap.recycle()
        }
        compose.onNodeWithContentDescription("بستن همگام‌سازی ابری").performClick()
        compose.waitUntil(timeoutMillis=3000) { dismissals==1 }
    }
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

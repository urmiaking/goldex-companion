package com.goldex.companion.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.goldex.companion.desktop.ui.controls.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Test
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class DesktopDialogMotionTest {
    @Test fun closingParentAlsoClosesAndDisablesNestedConfirmation() = runDesktopComposeUiTest(width=940,height=700) {
        var parent by mutableStateOf(true)
        var saves=0
        setContent {
            GoldExCompanionTheme {
                DesktopDialogHost(parent) {
                    DesktopDialog({parent=false}) {
                        DesktopDialogHost(true) {
                            DesktopAlertDialog({}, {
                                DesktopActionButton("ثبت",{saves++},Modifier.testTag("nested-save"),Icons.Outlined.Save)
                            },modifier=Modifier.testTag("nested-confirm"),text={Text("تأیید تو‌در‌تو")})
                        }
                    }
                }
            }
        }
        onNodeWithTag("nested-save").assertIsEnabled()
        mainClock.autoAdvance=false
        runOnIdle { parent=false }
        mainClock.advanceTimeByFrame()
        onNodeWithTag("nested-save").assertIsNotEnabled()
        mainClock.advanceTimeBy(240)
        onNodeWithTag("nested-confirm").assertDoesNotExist()
        runOnIdle { assertEquals(0,saves) }
    }
    @Test fun dialogAndConfirmationRetainSnapshotUntilExitAndBlockSecondSave() = runDesktopComposeUiTest(width=940,height=700) {
        var current by mutableStateOf<String?>(null)
        var alert by mutableStateOf(false)
        var saves = 0
        mainClock.autoAdvance = false
        setContent {
            GoldExCompanionTheme {
                DesktopDialogHost(current) { snapshot ->
                    val save: @Composable () -> Unit = {
                        DesktopActionButton("ثبت", { saves++; current = null }, Modifier.testTag("motion-save"), Icons.Outlined.Save)
                    }
                    if (alert) DesktopAlertDialog({ current=null }, save,
                        modifier=Modifier.testTag("motion-body"), text={ Text(snapshot) })
                    else DesktopDialog({ current=null }) {
                        Surface(Modifier.width(300.dp).testTag("motion-body")) {
                            Column(Modifier.padding(24.dp)) { Text(snapshot); save() }
                        }
                    }
                }
            }
        }
        for (confirmation in listOf(false,true)) {
            runOnIdle { alert=confirmation; current="snapshot-${if(confirmation) "confirmation" else "dialog"}" }
            mainClock.advanceTimeByFrame()
            runOnIdle { }
            mainClock.advanceTimeBy(80)
            capture(onNodeWithTag("motion-body"),"${if(confirmation) "confirmation" else "dialog"}-enter.png")
            mainClock.advanceTimeBy(300)
            onNodeWithTag("motion-save").assertIsEnabled().performClick()
            mainClock.advanceTimeByFrame()
            onNodeWithTag("motion-save").assertIsNotEnabled()
            onNodeWithTag("motion-save").performMouseInput { click() }
            mainClock.advanceTimeBy(64)
            onNodeWithText("snapshot-${if(confirmation) "confirmation" else "dialog"}").assertExists()
            capture(onNodeWithTag("motion-body"),"${if(confirmation) "confirmation" else "dialog"}-exit.png")
            mainClock.advanceTimeBy(220)
            onNodeWithTag("motion-body").assertDoesNotExist()
            runOnIdle { assertEquals(if(confirmation) 2 else 1,saves) }
        }
    }

    @Test fun modalRetainsCustomerSnapshotThroughExit() = runDesktopComposeUiTest(width=940,height=700) {
        var current by mutableStateOf<String?>("modal-customer")
        setContent {
            GoldExCompanionTheme {
                DesktopDialogHost(current,modal=true) { snapshot ->
                    DesktopModalBottomSheet({current=null},rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
                        Column(Modifier.fillMaxWidth().height(200.dp).testTag("motion-modal")) {
                            Text(snapshot)
                            DesktopActionButton("ثبت",{current=null},Modifier.testTag("modal-save"),Icons.Outlined.Save)
                        }
                    }
                }
            }
        }
        onNodeWithTag("modal-save").assertIsDisplayed()
        mainClock.autoAdvance=false
        onNodeWithTag("modal-save").performClick()
        mainClock.advanceTimeBy(96)
        onNodeWithText("modal-customer").assertExists()
        onNodeWithTag("modal-save").assertIsNotEnabled()
        mainClock.advanceTimeBy(300)
        onNodeWithTag("motion-modal").assertDoesNotExist()
    }

    @Test fun reducedMotionRemovesWindowWithinTwoFramesAndReopeningUsesNewIdentity() = runDesktopComposeUiTest(width=940,height=700) {
        var current by mutableStateOf<String?>("first")
        mainClock.autoAdvance=false
        setContent {
            GoldExCompanionTheme {
                CompositionLocalProvider(LocalDesktopReduceMotion provides true) {
                    DesktopDialogHost(current,contentKey={it}) { snapshot ->
                        DesktopDialog({current=null}) { Text(snapshot,Modifier.testTag("reduced-body")) }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(32)
        onNodeWithText("first").assertExists()
        runOnIdle { current=null }
        mainClock.advanceTimeBy(32)
        onNodeWithTag("reduced-body").assertDoesNotExist()
        runOnIdle { current="second" }
        mainClock.advanceTimeBy(32)
        onNodeWithText("second").assertExists()
        onNodeWithText("first").assertDoesNotExist()
    }

    private fun capture(node: SemanticsNodeInteraction,name: String) {
        val bitmap=node.captureToImage().asSkiaBitmap()
        val image=org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data=image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir")).apply{mkdirs()}.resolve(name).writeBytes(data.bytes)
        data.close();image.close();bitmap.close()
    }
}

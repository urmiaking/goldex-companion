package com.goldex.companion.ui.components

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.goldex.companion.BuildConfig
import com.goldex.companion.data.UpdateInfo
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w375dp-h800dp")
class UpdateDialogTest {
    @get:Rule val compose = createComposeRule()
    private var dismissCount = 0
    private val releasePage = "https://example.com/releases/v9.1.0"
    private val apk = "https://example.com/app.apk"
    private val notes = (1..40).joinToString("\n") { "تغییر شماره $it: بهبود تجربه کاربری و نمایش اطلاعات برنامه" }

    @Test fun longNotesKeepActionsVisibleAndOrderedInLightTheme() = verifyReadingAndActions()

    @Test @Config(qualifiers = "w800dp-h375dp-land")
    fun landscapeKeepsActionsAccessibleInDarkTheme() = verifyReadingAndActions(dark = true)

    @Test @Config(qualifiers = "w375dp-h640dp")
    fun largeTextKeepsActionsAccessibleOnSmallPhone() = verifyReadingAndActions(fontScale = 2f, dark = true)

    @Test fun downloadOpensApkAndDismissesOnce() {
        show()
        compose.onNodeWithText("دانلود و نصب").assertIsDisplayed().performClick()
        compose.runOnIdle {
            val intent = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(apk, intent.data.toString())
            assertEquals(1, dismissCount)
        }
    }

    @Test fun releasePageIsUsedWhenApkIsMissingAndNotesAreEmpty() {
        show(download = "", releaseNotes = "")
        compose.onNodeWithText("جزئیات این نسخه در صفحه انتشار در دسترس است.").assertIsDisplayed()
        compose.onNodeWithText("دانلود و نصب").performClick()
        compose.runOnIdle {
            val intent = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
            assertEquals(releasePage, intent.data.toString())
            assertEquals(1, dismissCount)
        }
    }

    private fun verifyReadingAndActions(fontScale: Float = 1f, dark: Boolean = false) {
        show(fontScale = fontScale, dark = dark)
        val later = compose.onNodeWithText("بعداً").assertIsDisplayed()
        val download = compose.onNodeWithText("دانلود و نصب").assertIsDisplayed()
        assertTrue("Cancel must be to the right of download in RTL",
            later.fetchSemanticsNode().boundsInRoot.left > download.fetchSemanticsNode().boundsInRoot.left)
        compose.onNodeWithText("نسخه جدید: ۹.۱.۰").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("نسخه نصب‌شده: ${PersianNumberFormatter.toPersianDigits(BuildConfig.VERSION_NAME)}")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(notes).performTouchInput { swipeUp() }
        later.assertIsDisplayed()
        download.assertIsDisplayed()
        later.performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, dismissCount) }
    }

    private fun show(download: String = apk, releaseNotes: String = notes, fontScale: Float = 1f, dark: Boolean = false) {
        org.robolectric.RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                GoldExCompanionTheme(isDarkTheme = dark) {
                    UpdateDialog(
                        UpdateInfo(isAvailable = true, latestVersion = "v9.1.0", releaseNotes = releaseNotes,
                            downloadUrl = download, releasePageUrl = releasePage),
                        onDismiss = { dismissCount++ }
                    )
                }
            }
        }
        compose.waitForIdle()
    }
}


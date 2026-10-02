package com.goldex.companion.ui.hub

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.goldex.companion.BuildConfig
import com.goldex.companion.R
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppVersionFooterTest {
    @get:Rule val compose = createComposeRule()

    @Test fun footerDisplaysOfficialNameAndInstalledVersionInPersian() {
        val name = ApplicationProvider.getApplicationContext<Context>().getString(R.string.app_name)
        compose.setContent { GoldExCompanionTheme { AppVersionFooter() } }
        compose.onNodeWithText("$name • نسخه ${PersianNumberFormatter.toPersianDigits(BuildConfig.VERSION_NAME)}")
            .assertIsDisplayed()
        compose.onNodeWithText("۲.۴.۰", substring = true).assertDoesNotExist()
    }
}

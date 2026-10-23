package com.goldex.companion.ui.customers

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.model.Customer
import com.goldex.companion.ui.components.AddCustomerDialog
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NewCustomerFormTest {
    @get:Rule val compose = createComposeRule()
    @Test fun customerCanBeCreatedWithoutRoleChoiceAndExistingDefaultIsRetained() {
        var saved: Customer? = null
        compose.setContent { GoldExCompanionTheme { AddCustomerDialog({}, { saved = it }) } }
        compose.onNodeWithText("نوع طرف حساب:").assertDoesNotExist()
        compose.onNodeWithText("همکار و بنکدار").assertDoesNotExist()
        compose.onNodeWithText("نام و نام‌خانوادگی خریدار *").performTextInput("مشتری آزمایشی")
        compose.onNodeWithText("مشتری آزمایشی").assertExists()
        compose.onNodeWithText("ثبت و انتخاب").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("مشتری آزمایشی", saved?.name)
            assertEquals("مشتری عادی (مصرف‌کننده)", saved?.role)
        }
    }
}

package com.goldex.companion.ui.wizard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WizardValidationTest {

    @Test
    fun validateWizardProfile_whenAllFieldsBlank_returnsErrorsForAllFields() {
        val emptyProfile = WizardProfileState(
            galleryName = "",
            managerName = "",
            unionCode = "",
            phone = "",
            address = ""
        )

        val errors = validateWizardProfile(emptyProfile)

        assertTrue(errors.hasErrors)
        assertNotNull(errors.galleryNameError)
        assertNotNull(errors.managerNameError)
        assertNotNull(errors.unionCodeError)
        assertNotNull(errors.phoneError)
        assertNotNull(errors.addressError)
        assertEquals("وارد کردن نام تجاری طلافروشی / بنکداری اجباری است.", errors.firstErrorMessage)
    }

    @Test
    fun validateWizardProfile_whenWhitespaceOnly_returnsErrors() {
        val whitespaceProfile = WizardProfileState(
            galleryName = "   ",
            managerName = " \t ",
            unionCode = "  ",
            phone = " ",
            address = "\n"
        )

        val errors = validateWizardProfile(whitespaceProfile)

        assertTrue(errors.hasErrors)
        assertNotNull(errors.galleryNameError)
        assertNotNull(errors.managerNameError)
        assertNotNull(errors.unionCodeError)
        assertNotNull(errors.phoneError)
        assertNotNull(errors.addressError)
    }

    @Test
    fun validateWizardProfile_whenAllFieldsValid_returnsNoErrors() {
        val validProfile = WizardProfileState(
            galleryName = "گالری طلای پرسپولیس",
            managerName = "محمد حسینی",
            unionCode = "۹۸۷۶۵",
            phone = "۰۲۱۲۲۳۳۴۴۵۵",
            address = "تهران، بازار بزرگ، بازار زرگرها"
        )

        val errors = validateWizardProfile(validProfile)

        assertFalse(errors.hasErrors)
        assertNull(errors.galleryNameError)
        assertNull(errors.managerNameError)
        assertNull(errors.unionCodeError)
        assertNull(errors.phoneError)
        assertNull(errors.addressError)
        assertNull(errors.firstErrorMessage)
    }

    @Test
    fun validateWizardFinancial_withValidDefaults_returnsNoErrors() {
        val defaultFinancial = WizardFinancialState(
            profitPercent = "7",
            isVatEnabled = true,
            vatRate = "9"
        )

        val errors = validateWizardFinancial(defaultFinancial)

        assertFalse(errors.hasErrors)
        assertNull(errors.profitError)
        assertNull(errors.vatError)
    }

    @Test
    fun validateWizardFinancial_withPersianDigits_returnsNoErrors() {
        val persianFinancial = WizardFinancialState(
            profitPercent = "۷.۵",
            isVatEnabled = true,
            vatRate = "۹"
        )

        val errors = validateWizardFinancial(persianFinancial)

        assertFalse(errors.hasErrors)
        assertNull(errors.profitError)
        assertNull(errors.vatError)
    }

    @Test
    fun validateWizardFinancial_whenProfitBlankOrInvalid_returnsProfitError() {
        val invalidFinancial = WizardFinancialState(
            profitPercent = "",
            isVatEnabled = false,
            vatRate = "0"
        )

        val errors = validateWizardFinancial(invalidFinancial)

        assertTrue(errors.hasErrors)
        assertNotNull(errors.profitError)
    }

    @Test
    fun validateWizardFinancial_whenVatDisabled_ignoresInvalidVatRate() {
        val disabledVatFinancial = WizardFinancialState(
            profitPercent = "7",
            isVatEnabled = false,
            vatRate = "invalid"
        )

        val errors = validateWizardFinancial(disabledVatFinancial)

        assertFalse(errors.hasErrors)
        assertNull(errors.vatError)
    }
}

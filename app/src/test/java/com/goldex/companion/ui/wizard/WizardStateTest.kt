package com.goldex.companion.ui.wizard

import com.goldex.companion.data.AppSettings
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WizardStateTest {

    @Test
    fun `intro slider advances on right swipe in rtl`() {
        assertEquals(1, introSlideIndexAfterSwipe(0, 3, horizontalDrag = 80f))
        assertEquals(0, introSlideIndexAfterSwipe(1, 3, horizontalDrag = -80f))
        assertEquals(2, introSlideIndexAfterSwipe(0, 3, horizontalDrag = -80f))
        assertEquals(1, introSlideIndexAfterSwipe(1, 3, horizontalDrag = 10f))
    }

    @Test
    fun testWizardStepsSequence() {
        val steps = WizardStep.values()
        assertEquals(5, steps.size)
        assertEquals(WizardStep.INTRO, steps[0])
        assertEquals(WizardStep.PROFILE, steps[1])
        assertEquals(WizardStep.FINANCIAL_DEFAULTS, steps[2])
        assertEquals(WizardStep.INVENTORY, steps[3])
        assertEquals(WizardStep.COMPLETION, steps[4])

        assertEquals(0, WizardStep.INTRO.stepNumber)
        assertEquals(1, WizardStep.PROFILE.stepNumber)
        assertEquals(2, WizardStep.FINANCIAL_DEFAULTS.stepNumber)
        assertEquals(3, WizardStep.INVENTORY.stepNumber)
        assertEquals(4, WizardStep.COMPLETION.stepNumber)
    }

    @Test
    fun testWizardProfileStateDefaults() {
        val state = WizardProfileState()
        assertEquals("", state.galleryName)
        assertEquals("", state.managerName)
        assertEquals("", state.unionCode)
        assertEquals("", state.phone)
        assertEquals("", state.address)
        assertEquals("", state.logoUri)
        assertEquals("", state.stampUri)
    }

    @Test
    fun testWizardProfileStateWithBrandAssets() {
        val state = WizardProfileState(
            galleryName = "گالری زرین",
            managerName = "علی راد",
            unionCode = "12345",
            phone = "09123456789",
            address = "بازار طلا",
            logoUri = "content://media/external/images/media/1",
            stampUri = "content://media/external/images/media/2"
        )
        assertEquals("content://media/external/images/media/1", state.logoUri)
        assertEquals("content://media/external/images/media/2", state.stampUri)
    }

    @Test
    fun testWizardFinancialStateDefaults() {
        val state = WizardFinancialState()
        assertEquals("7", state.profitPercent)
        assertTrue(state.isVatEnabled)
        assertEquals("9", state.vatRate)
        assertEquals(Karat.K18, state.baseKarat)
    }

    @Test
    fun testWizardInventoryCalculations() {
        val inv = WizardInventoryState(
            vitrinWeight = "100.0",
            meltWeight = "50.0",
            coinTamam = 2,
            coinNim = 1,
            coinRob = 0,
            coinQadim = 0,
            coinGerami = 0,
            cashTankhah = "50000000",
            bankBalances = "150000000"
        )

        val vitrin = PersianNumberFormatter.parseToCleanDouble(inv.vitrinWeight) ?: 0.0
        val melt = PersianNumberFormatter.parseToCleanDouble(inv.meltWeight) ?: 0.0
        val coinsWeight = (inv.coinTamam * 8.133) + (inv.coinNim * 4.066)
        val totalGoldWeight = vitrin + melt + coinsWeight

        assertEquals(150.0 + (2 * 8.133) + 4.066, totalGoldWeight, 0.001)

        val cash = PersianNumberFormatter.parseToCleanLong(inv.cashTankhah) ?: 0L
        val bank = PersianNumberFormatter.parseToCleanLong(inv.bankBalances) ?: 0L
        val totalCash = cash + bank
        assertEquals(200_000_000L, totalCash)
    }

    @Test
    fun testAppSettingsOnboardingFlag() {
        val settingsDefault = AppSettings()
        assertFalse(settingsDefault.hasCompletedOnboarding)

        val settingsCompleted = settingsDefault.copy(hasCompletedOnboarding = true)
        assertTrue(settingsCompleted.hasCompletedOnboarding)
    }
}

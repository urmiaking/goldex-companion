package com.goldex.companion.presentation.calculator

import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.model.WageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManualGoldCalculatorTest {
    private fun calculator() = ManualGoldCalculator().apply {
        setInput(CalculatorField.SPOT, "۶٬۰۰۰٬۰۰۰")
        setInput(CalculatorField.GROSS_WEIGHT, "۲٫۵۰۰")
        setInput(CalculatorField.STONE_WEIGHT, "۰٫۵۰۰")
        setInput(CalculatorField.WAGE, "۱۰")
        setInput(CalculatorField.PROFIT, "۷")
        setInput(CalculatorField.TAX, "۹")
    }

    @Test fun persianInputUsesExistingFinancialPolicyAndTaxExcludesRawGold() {
        val result = assertNotNull(calculator().state.value.result)
        assertEquals(2.0, result.netWeight)
        assertEquals(12_000_000.0, result.rawGoldValue)
        assertEquals(1_200_000.0, result.wageAmount)
        assertEquals(924_000.0, result.profitAmount, 0.000001)
        assertEquals(191_160.0, result.taxAmount, 0.000001)
        assertEquals(14_315_160.0, result.totalPayable, 0.000001)
    }

    @Test fun changingBasisRetainsTheQuoteValue() {
        val calculator = calculator()
        val before = assertNotNull(calculator.state.value.result).totalPayable
        calculator.setPriceBasis(PriceBasisTab.K24)
        assertEquals("8000000", calculator.state.value.input(CalculatorField.SPOT))
        assertEquals(before, assertNotNull(calculator.state.value.result).totalPayable)
    }

    @Test fun changingWageUnitRequiresANewValue() {
        val calculator = calculator()
        calculator.setWageType(WageType.TOMAN_PER_GRAM)
        assertNull(calculator.state.value.result)
        assertEquals("", calculator.state.value.input(CalculatorField.WAGE))
        calculator.setInput(CalculatorField.WAGE, "۵۰۰۰۰")
        assertEquals(100_000.0, assertNotNull(calculator.state.value.result).wageAmount)
    }

    @Test fun invalidInputsRemoveStaleFinancialResults() {
        val calculator = calculator()
        for (invalid in listOf("-1", "NaN", "Infinity", "1e30", "۲٫۵۰۰۱", "1..2")) {
            calculator.setInput(CalculatorField.GROSS_WEIGHT, invalid)
            assertNull(calculator.state.value.result, invalid)
            assertTrue(CalculatorField.GROSS_WEIGHT in calculator.state.value.errors, invalid)
        }
    }

    @Test fun invalidDeductionAndOverflowCannotProduceAnAmount() {
        val calculator = calculator()
        calculator.setInput(CalculatorField.STONE_WEIGHT, "3")
        assertNull(calculator.state.value.result)
        assertTrue(CalculatorField.STONE_WEIGHT in calculator.state.value.errors)
        calculator.setInput(CalculatorField.STONE_WEIGHT, "0")
        calculator.setInput(CalculatorField.SPOT, Long.MAX_VALUE.toString())
        assertNull(calculator.state.value.result)
        assertTrue(CalculatorField.SPOT in calculator.state.value.errors)
    }

    @Test fun resetClearsQuoteAndWeightAndRestoresConfiguredDefaults() {
        val calculator = calculator()
        assertNotNull(calculator.summary())
        calculator.reset()
        assertNull(calculator.state.value.result)
        assertNull(calculator.summary())
        assertEquals("", calculator.state.value.input(CalculatorField.SPOT))
        assertEquals("9", calculator.state.value.input(CalculatorField.TAX))
    }

    @Test fun milligramInputAndCustomFinenessAreRetained() {
        val calculator = calculator()
        calculator.setInput(CalculatorField.GROSS_WEIGHT, "0.001")
        calculator.setInput(CalculatorField.STONE_WEIGHT, "0")
        calculator.setInput(CalculatorField.KARAT, "875")
        assertEquals(0.001, assertNotNull(calculator.state.value.result).netWeight)
        assertEquals(7_000.0, assertNotNull(calculator.state.value.result).rawGoldValue, 0.000001)
    }
}

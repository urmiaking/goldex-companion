package com.goldex.companion.domain.calculator

import kotlin.test.*

class KaratConversionPolicyTest {
    @Test fun customFinenessPreservesPureGoldAndInvoiceWeight() {
        val result = KaratConversionPolicy.convert(10.0, 742, 999)!!
        assertEquals(7.42, result.pureGoldWeight, 1e-10)
        assertEquals(7.4274274274, result.targetWeight, 1e-10)
        assertEquals(9.8933333333, result.gold750Weight, 1e-10)
        val reverse = KaratConversionPolicy.convert(result.targetWeight, 999, 742)!!
        assertEquals(10.0, reverse.targetWeight, 1e-10)
    }
    @Test fun conventionalKaratsMatchCalculatorAndThousandthsAreExplicit() {
        assertEquals(750.0, KaratConversionPolicy.fineness(18))
        assertEquals(875.0, KaratConversionPolicy.fineness(21))
        assertEquals(917.0, KaratConversionPolicy.fineness(22))
        assertEquals(1000.0, KaratConversionPolicy.fineness(24))
        assertEquals(999.0, KaratConversionPolicy.fineness(999))
        assertEquals(7.5, KaratConversionPolicy.convert(10.0, 18, 24)!!.targetWeight, 1e-10)
    }
    @Test fun non750AssayIsValuedOn750DifferenceWithoutChangingGoldContent() {
        val result = KaratConversionPolicy.assay(100.0, 995, 990)!!
        assertEquals(99.4974874372, result.adjustedWeight, 1e-10)
        assertEquals(-2.0 / 3.0, result.gold750Difference, 1e-10)
        assertEquals(-10_000_000L, result.valueAt(15_000_000L))
        assertEquals(132.0, result.gold750Weight, 1e-10)
        assertEquals(10_000_000L, KaratConversionPolicy.assay(100.0, 990, 995)!!.valueAt(15_000_000L))
    }
    @Test fun equalAssayHasNoDebtAndMissingRateNeverMakesUpCash() {
        val result = KaratConversionPolicy.assay(10.0, 750, 750)!!
        assertEquals(0L, result.valueAt(15_000_000L))
        assertNull(result.valueAt(0))
        assertNull(result.valueAt(-1))
        assertEquals(10.0, result.adjustedWeight)
    }
    @Test fun invalidInputsAndOverflowCannotProduceUsableResults() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertNull(KaratConversionPolicy.convert(it, 750, 999))
            assertNull(KaratConversionPolicy.assay(it, 750, 742))
        }
        listOf(0, -1, 1001, Int.MAX_VALUE).forEach {
            assertNull(KaratConversionPolicy.convert(10.0, it, 750))
            assertNull(KaratConversionPolicy.convert(10.0, 750, it))
            assertNull(KaratConversionPolicy.assay(10.0, it, 742))
        }
        assertNull(KaratConversionPolicy.assay(1e20, 750, 742)!!.valueAt(Long.MAX_VALUE))
    }
    @Test fun cashRoundsHalfUpInBothDirections() {
        assertEquals(1L, KaratConversionPolicy.assay(0.5, 750, 751)!!.valueAt(750L))
        assertEquals(-1L, KaratConversionPolicy.assay(0.5, 750, 749)!!.valueAt(750L))
    }
}

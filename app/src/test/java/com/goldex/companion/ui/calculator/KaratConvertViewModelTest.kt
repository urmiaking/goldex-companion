package com.goldex.companion.ui.calculator

import com.goldex.companion.model.Karat
import org.junit.Assert.*
import org.junit.Test

class KaratConvertViewModelTest {
    @Test fun customPersianInputsRecalculateSwapAndTransferThe750Equivalent() {
        val vm = KaratConvertViewModel()
        vm.onConvertWeightChanged("۱۰٫۰۰۱")
        vm.onCustomFromKaratChanged("۷۴۲")
        vm.onCustomToKaratChanged("۹۹۹")
        val initial = vm.uiState.value
        assertEquals("10.001", initial.convertWeightInput)
        assertEquals(10.001 * 742 / 999, initial.convertedWeight, 1e-10)
        assertEquals(10.001 * 742 / 750, initial.invoiceGold750!!, 1e-10)
        vm.swapConvertKarats()
        assertEquals("999", vm.uiState.value.fromInput)
        assertEquals("742", vm.uiState.value.toInput)
        vm.onConvertToKarat(Karat.K18)
        assertNull(vm.uiState.value.customToKarat)
        assertEquals(10.001 * 999 / 750, vm.uiState.value.convertedWeight, 1e-10)
    }
    @Test fun incompleteOrOutOfRangeCustomValueNeverRetainsAnOldResult() {
        val vm = KaratConvertViewModel()
        vm.onConvertWeightChanged("10")
        assertNotNull(vm.uiState.value.invoiceGold750)
        vm.onCustomToKaratChanged("")
        assertNull(vm.uiState.value.invoiceGold750)
        vm.onCustomToKaratChanged("1001")
        assertNull(vm.uiState.value.conversion)
        vm.onCustomToKaratChanged("999")
        assertNotNull(vm.uiState.value.conversion)
        vm.onConvertWeightChanged("-10")
        assertNull(vm.uiState.value.invoiceGold750)
        vm.onConvertWeightChanged("")
        assertNull(vm.uiState.value.invoiceGold750)
    }
    @Test fun conditionalModeRequiresActualAssayAndTransfersItsGoldContent() {
        val vm = KaratConvertViewModel()
        vm.onConvertWeightChanged("100")
        vm.setConvertMode(KaratConvertMode.SETTLEMENT)
        assertNull(vm.uiState.value.invoiceGold750)
        vm.onSpotPriceChanged(15_000_000L)
        vm.onAgreedKaratChanged("۹۹۵")
        vm.onAssayKaratChanged("۹۹۰")
        assertEquals(132.0, vm.uiState.value.invoiceGold750!!, 1e-10)
        assertEquals(-10_000_000L, vm.uiState.value.assayCashTomans)
        vm.onSpotPriceChanged(0L)
        assertNull(vm.uiState.value.assayCashTomans)
        assertNotNull(vm.uiState.value.invoiceGold750)
        vm.onAssayKaratChanged("")
        assertNull(vm.uiState.value.assay)
    }
    @Test fun invoiceHandoffPreservesGoldValueWithoutFeesOrInventedRates() {
        val vm = KaratConvertViewModel()
        vm.onConvertWeightChanged("10")
        vm.onCustomFromKaratChanged("742")
        vm.onCustomToKaratChanged("999")
        assertNull(vm.createInvoiceItem(0L))
        val item = vm.createInvoiceItem(15_000_000L)!!
        assertEquals(10.0 * 742 / 750, item.equivalent18kWeight, 1e-10)
        assertEquals(148_400_000.0, item.totalPayable, 1e-6)
        vm.onCustomToKaratChanged("")
        assertNull(vm.createInvoiceItem(15_000_000L))
        vm.onCustomToKaratChanged("750")
        vm.onConvertWeightChanged("100000000000000000000")
        assertNull(vm.createInvoiceItem(Long.MAX_VALUE))
    }

}

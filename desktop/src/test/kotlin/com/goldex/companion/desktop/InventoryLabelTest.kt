package com.goldex.companion.desktop

import com.goldex.companion.desktop.data.InventoryLabelPrinter
import com.goldex.companion.model.InventoryItem
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Test
import kotlin.test.*

class InventoryLabelTest {
    @Test fun printedBarcodeActuallyDecodesToTheOriginalCodeIncludingPersian() {
        for (code in listOf("RNG-12345", "۱۲۳-کالا", "LONG-" + "1".repeat(60))) {
            val item = InventoryItem(code = code, title = "کالای آزمایشی", grossWeightGrams = 2.0)
            val image = InventoryLabelPrinter.barcode(item)
            val source = RGBLuminanceSource(image.width, image.height, image.getRGB(0, 0, image.width, image.height, null, 0, image.width))
            assertEquals(code, MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text)
            image.flush()
            val label = InventoryLabelPrinter.render(item); assertEquals(600, label.width); assertEquals(420, label.height); label.flush()
        }
    }
}

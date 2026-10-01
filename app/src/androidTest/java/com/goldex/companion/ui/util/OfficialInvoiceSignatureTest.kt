package com.goldex.companion.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goldex.companion.data.AppSettings
import com.goldex.companion.model.BarterInvoice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfficialInvoiceSignatureTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun savedSignatureAddsVisibleInkToTheSellerAreaOfTheExportedPdf() {
        val signature = Bitmap.createBitmap(180, 60, Bitmap.Config.ARGB_8888)
        Canvas(signature).drawLine(10f, 45f, 170f, 15f, Paint().apply {
            color = Color.BLACK
            strokeWidth = 8f
        })
        val image = File(context.filesDir, "existing-seller-signature.png")
        image.outputStream().use { assertTrue(signature.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        signature.recycle()

        val invoice = BarterInvoice(id = "signature-regression", invoiceNumber = "1001")
        val settings = AppSettings(galleryName = "گالری آزمایشی", invoiceSignatureUri = image.toURI().toString())
        val signedFile = OfficialInvoicePdfGenerator.create(context, invoice, settings)
        assertNotNull(signedFile)
        val signed = render(signedFile!!)
        val unsignedFile = OfficialInvoicePdfGenerator.create(context, invoice, settings.copy(invoiceSignatureUri = ""))
        assertNotNull(unsignedFile)
        val unsigned = render(unsignedFile!!)

        var addedInkPixels = 0
        for (y in 350 until 374) {
            for (x in 55 until 135) {
                val pixel = signed.getPixel(x, y)
                if (pixel != unsigned.getPixel(x, y) && Color.red(pixel) < 100) addedInkPixels++
            }
        }
        assertTrue("Seller signature must be visible in the generated PDF", addedInkPixels > 20)
        signed.recycle()
        unsigned.recycle()
    }

    private fun render(file: File): Bitmap {
        val bitmap = Bitmap.createBitmap(595, 420, Bitmap.Config.ARGB_8888)
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
            assertEquals(1, pdf.pageCount)
            pdf.openPage(0).use { it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
        }
        return bitmap
    }
}

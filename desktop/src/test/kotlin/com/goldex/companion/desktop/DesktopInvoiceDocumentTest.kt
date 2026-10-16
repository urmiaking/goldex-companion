package com.goldex.companion.desktop

import com.goldex.companion.data.AppSettings
import com.goldex.companion.desktop.data.DesktopInvoiceDocuments
import com.goldex.companion.model.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

class DesktopInvoiceDocumentTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun invoice(count: Int) = BarterInvoice(id = "synthetic", invoiceNumber = "14051234", createdAt = 1,
        spotPrice18k = 6_000_000, customer = Customer(name = "مشتری آزمون", nationalId = "1234567890"),
        salesItems = (1..count).map { BankCoinItem(id = "coin-$it", title = "سکه آزمون $it", unitPrice = 75_000_000,
            totalPayable = 75_000_000.0, equivalent18kWeight = 9.759) })

    @Test fun officialPdfContainsEveryPageAndBundledPersianFontsRenderInPackagedLayout() {
        val path = temporary.newFolder().toPath().resolve("official.pdf")
        val settings = AppSettings(galleryName = "گالری طلای آزمون", managerName = "مدیر آزمون", galleryPhone = "09123456789",
            galleryLicense = "1234", galleryAddress = "نشانی آزمایشی")
        DesktopInvoiceDocuments.save(invoice(9), settings, path)
        Loader.loadPDF(path.toFile()).use { pdf ->
            assertEquals(3, pdf.numberOfPages)
            assertEquals(595f, pdf.getPage(0).mediaBox.width)
            assertEquals(420f, pdf.getPage(0).mediaBox.height)
            val image = PDFRenderer(pdf).renderImageWithDPI(0, 144f)
            try {
                assertEquals(1190, image.width)
                assertTrue((44 until 1146).any { image.getRGB(it, 65) != java.awt.Color.WHITE.rgb })
                val screenshots = java.io.File(System.getProperty("qirato.screenshotDir"), "invoice-official-document.png")
                screenshots.parentFile.mkdirs(); ImageIO.write(image, "png", screenshots)
            } finally { image.flush() }
        }
        assertTrue(DesktopInvoiceDocuments.message(invoice(1)).contains("۱۴۰۵۱۲۳۴"))
    }

    @Test fun invalidBrandAssetKeepsPreviousExportBytesAndDoesNotSilentlyDropIdentity() {
        val path = temporary.newFolder().toPath().resolve("official.pdf")
        val original = "previous export".toByteArray(); Files.write(path, original)
        assertFails { DesktopInvoiceDocuments.save(invoice(1), AppSettings(invoiceLogoUri = path.resolveSibling("missing.png").toString()), path) }
        assertContentEquals(original, Files.readAllBytes(path))
    }
}

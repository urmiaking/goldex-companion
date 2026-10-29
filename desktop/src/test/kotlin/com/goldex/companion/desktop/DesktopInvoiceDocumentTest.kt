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

    @Test fun summaryAndSellerSignatureAppearOnlyOnTheFinalPage() {
        val signature = java.awt.image.BufferedImage(180, 60, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        signature.createGraphics().let { g ->
            try { g.color = java.awt.Color.BLACK; g.stroke = java.awt.BasicStroke(8f); g.drawLine(10, 45, 170, 15) }
            finally { g.dispose() }
        }
        val asset = temporary.newFile("signature.png")
        ImageIO.write(signature, "png", asset); signature.flush()
        val settings = AppSettings(galleryName = "گالری طلای آزمون", managerName = "مدیر آزمون",
            unionCode = "۱۲۳۴", invoiceSignatureUri = asset.toURI().toString())
        val pages = DesktopInvoiceDocuments.render(invoice(9), settings)
        val unsigned = DesktopInvoiceDocuments.render(invoice(9), settings.copy(invoiceSignatureUri = ""))
        try {
            assertEquals(3, pages.size)
            assertEquals(0xFFFFFEFA.toInt(), pages.first().getRGB(1120, 740), "Continuation pages have no payable summary")
            assertEquals(0xFFFFF0B8.toInt(), pages.last().getRGB(1120, 740), "Last page contains the Android golden total")
            pages.indices.forEach { index ->
                var changed = 0
                for (y in 700 until 748) for (x in 110 until 270) {
                    if (pages[index].getRGB(x, y) != unsigned[index].getRGB(x, y)) changed++
                }
                if (index < pages.lastIndex) assertEquals(0, changed)
                else assertTrue(changed > 20, "Signature adds visible ink in the seller approval area")
            }
        } finally { (pages + unsigned).forEach { it.flush() } }
    }

    @Test fun stoneDeductionAndReceivedItemAreVisibleInTheOfficialTable() {
        val crafted = CraftedGoldItem(id = "crafted", title = "دستبند طلای نگین‌دار", grossWeight = 3.456,
            stoneWeight = .123, netWeight = 3.333, spotPrice = 6_000_000, wageInput = 10.0, wageAmount = 1_999_800.0,
            profitPercent = 7.0, profitAmount = 1_539_846.0, taxPercent = 10.0, taxAmount = 353_964.6,
            rawGoldValue = 19_998_000.0, totalPayable = 23_891_610.6, equivalent18kWeight = 3.333)
        val received = ScrapGoldItem(id = "received", title = "طلای مستعمل مشتری", grossWeight = 1.125, stoneWeight = .125,
            netWeight = 1.0, spotPrice = 6_000_000, effectiveGramPrice = 5_880_000,
            totalPayable = 5_880_000.0, equivalent18kWeight = .98)
        val sample = invoice(0).copy(salesItems = listOf(crafted), receivedItems = listOf(received),
            note = "توضیحات آزمایشی خرید و تهاتر", createdAt = 1_792_576_800_000)
        val settings = AppSettings(galleryName = "طلا و جواهر قیراط", managerName = "مدیر گالری", unionCode = "123456",
            galleryPhone = "02112345678", galleryAddress = "تهران، خیابان نمونه، پلاک ۱۲")
        val page = DesktopInvoiceDocuments.render(sample, settings).single()
        val changed = DesktopInvoiceDocuments.render(sample.copy(salesItems = listOf(crafted.copy(stoneWeight = .987))), settings).single()
        try {
            val stoneCellChanged = (366 until 402).any { y -> (450 until 536).any { x -> page.getRGB(x, y) != changed.getRGB(x, y) } }
            assertTrue(stoneCellChanged, "Stone deduction must be printed, not silently omitted")
            assertEquals(0xFFFFF1F0.toInt(), page.getRGB(80, 447), "Received rows use the Android red-tinted background")
            assertEquals(0xFFFFFFFF.toInt(), page.getRGB(80, 397), "Sale rows remain distinct from receipts")
        } finally { page.flush(); changed.flush() }
    }

    @Test fun logoAndStampKeepAspectRatioAndDoNotReplaceTheSellerSignature() {
        fun asset(name: String, width: Int, height: Int, color: java.awt.Color): String {
            val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            try { g.color = color; g.fillRect(0, 0, width, height) } finally { g.dispose() }
            val file = temporary.newFile(name)
            ImageIO.write(image, "png", file); image.flush()
            return file.toURI().toString()
        }
        val page = DesktopInvoiceDocuments.render(invoice(1), AppSettings(
            invoiceLogoUri = asset("logo.png", 300, 100, java.awt.Color.BLUE),
            invoiceStampUri = asset("stamp.png", 100, 100, java.awt.Color.RED),
            invoiceSignatureUri = asset("seller.png", 300, 100, java.awt.Color.GREEN))).single()
        try {
            assertEquals(java.awt.Color.BLUE.rgb, page.getRGB(1085, 89))
            assertEquals(java.awt.Color.WHITE.rgb, page.getRGB(1085, 60), "Wide logo is centered without stretching")
            assertEquals(java.awt.Color.RED.rgb, page.getRGB(117, 722))
            assertEquals(java.awt.Color.GREEN.rgb, page.getRGB(232, 718))
        } finally { page.flush() }
    }

    @Test fun emptyAndLongMixedDirectionFieldsStayInsideThePaper() {
        val settings = AppSettings(galleryName = "گالری آزمایشی ABC 123 ".repeat(30), managerName = "نام مدیر ".repeat(40),
            galleryAddress = "نشانی واحد صنفی ".repeat(100), unionCode = "1234567890")
        val sample = invoice(1).copy(customer = Customer(name = "مشتری نمونه ".repeat(40), phone = "09123456789"),
            salesItems = listOf((invoice(1).salesItems.single() as BankCoinItem).copy(title = "سکه امامی 1405 ABC ".repeat(40))),
            note = "توضیحات آزمایشی ".repeat(60))
        for ((name, fixture) in listOf("empty" to invoice(0), "long-fields" to sample)) {
            val page = DesktopInvoiceDocuments.render(fixture, settings).single()
            try {
                assertEquals(1190, page.width); assertEquals(840, page.height)
                for (y in 0 until page.height) for (x in listOf(0, 20, 1189)) {
                    assertEquals(0xFFFFFEFA.toInt(), page.getRGB(x, y), "Text must remain inside document margins")
                }
            } finally { page.flush() }
        }
    }


    private fun inspectVectors(resources: org.apache.pdfbox.pdmodel.PDResources): Pair<Int, Int> {
        var images = 0
        var curves = 0
        for (name in resources.xObjectNames) {
            when (val objectValue = resources.getXObject(name)) {
                is org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject -> images++
                is org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject -> {
                    curves += org.apache.pdfbox.pdfparser.PDFStreamParser(objectValue).parse()
                        .filterIsInstance<org.apache.pdfbox.contentstream.operator.Operator>().count { it.name == "c" }
                    val nested = inspectVectors(objectValue.resources)
                    images += nested.first; curves += nested.second
                }
            }
        }
        return images to curves
    }

    @Test fun exportedInvoiceContainsVectorArtworkInsteadOfPageSizedBitmapsAtEveryZoom() {
        val path = temporary.newFolder().toPath().resolve("vector.pdf")
        DesktopInvoiceDocuments.save(invoice(9), AppSettings(galleryName = "طلا و جواهر قیراط", managerName = "مدیر آزمون", unionCode = "1234"), path)
        Loader.loadPDF(path.toFile()).use { pdf ->
            assertEquals(3, pdf.numberOfPages)
            for (page in pdf.pages) {
                val stats = inspectVectors(page.resources)
                assertEquals(0, stats.first, "An invoice without brand images must contain no raster XObjects, including nested forms")
                assertTrue(stats.second > 100, "Persian glyphs and artwork must be vector curves")
            }
            val enlarged = PDFRenderer(pdf).renderImageWithDPI(0, 576f)
            try {
                assertEquals(4760, enlarged.width)
                assertEquals(3360, enlarged.height)
            } finally { enlarged.flush() }
        }
    }

    @Test fun onlyOriginalBrandAssetRemainsRasterInAnOtherwiseVectorPdf() {
        val logo = temporary.newFile("original-logo.png")
        val bitmap = java.awt.image.BufferedImage(300, 100, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = bitmap.createGraphics()
        try { g.color = java.awt.Color.BLUE; g.fillRect(0, 0, 300, 100) } finally { g.dispose() }
        ImageIO.write(bitmap, "png", logo); bitmap.flush()
        val output = temporary.newFile("branded-vector.pdf")
        DesktopInvoiceDocuments.save(invoice(1), AppSettings(invoiceLogoUri = logo.toURI().toString()), output.toPath())
        Loader.loadPDF(output).use { pdf ->
            val stats = inspectVectors(pdf.getPage(0).resources)
            assertEquals(1, stats.first, "Only the original logo may be an image; the whole page must stay vector")
            assertTrue(stats.second > 100)
        }
    }

}
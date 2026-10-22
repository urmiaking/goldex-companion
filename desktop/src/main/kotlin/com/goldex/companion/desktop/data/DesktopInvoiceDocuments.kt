package com.goldex.companion.desktop.data

import com.goldex.companion.data.AppSettings
import com.goldex.companion.domain.invoice.OfficialInvoiceDocumentFactory
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.PersianNumberFormatter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.printing.PDFPageable
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.awt.print.PrinterJob
import java.nio.file.*
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/** A5 landscape official document; AWT shapes Persian with the bundled Vazirmatn font. */
object DesktopInvoiceDocuments {
    const val ROWS_PER_PAGE = 4
    private fun font(bold: Boolean): Font = checkNotNull(javaClass.getResourceAsStream(
        if (bold) "/font/vazirmatn_bold.ttf" else "/font/vazirmatn_regular.ttf"))
        .use { Font.createFont(Font.TRUETYPE_FONT, it) }

    fun render(invoice: BarterInvoice, settings: AppSettings): List<BufferedImage> {
        val model = OfficialInvoiceDocumentFactory.create(invoice, settings)
        val regular = font(false); val bold = font(true)
        val assets = mutableListOf<BufferedImage>()
        val pages = mutableListOf<BufferedImage>()
        fun asset(uri: String) = loadBrand(uri)?.also { assets.add(it) }
        try {
            val logo = asset(settings.invoiceLogoUri)
            val signature = asset(settings.invoiceSignatureUri)
            val stamp = asset(settings.invoiceStampUri)
            val chunks = model.rows.chunked(ROWS_PER_PAGE).ifEmpty { listOf(emptyList()) }
            chunks.forEachIndexed { index, rows ->
                val image = BufferedImage(1190, 840, BufferedImage.TYPE_INT_RGB)
                pages.add(image)
                val g = image.createGraphics()
                try { DesktopInvoicePageRenderer(g, regular, bold).draw(model, settings, rows, index, chunks.size, logo, signature, stamp) }
                finally { g.dispose() }
            }
            return pages
        } catch (error: Exception) {
            pages.forEach { it.flush() }; throw error
        } finally { assets.forEach { it.flush() } }
    }

    private fun loadBrand(uri: String): BufferedImage? {
        if (uri.isBlank()) return null
        val path = if (uri.startsWith("file:")) Path.of(java.net.URI(uri)) else Path.of(uri)
        require(Files.isRegularFile(path) && Files.size(path) <= 12 * 1024 * 1024) { "تصویر مشخصات واحد صنفی قابل خواندن نیست" }
        return ImageIO.createImageInputStream(path.toFile()).use { input ->
            val readers = ImageIO.getImageReaders(input)
            require(readers.hasNext()) { "قالب تصویر مشخصات واحد صنفی معتبر نیست" }
            val reader = readers.next()
            try {
                reader.input = input
                require(reader.getWidth(0).toLong() * reader.getHeight(0) <= 20_000_000) { "ابعاد تصویر بیش از حد بزرگ است" }
                reader.read(0)
            } finally { reader.dispose() }
        }
    }

    private fun document(invoice: BarterInvoice, settings: AppSettings): PDDocument {
        val result = PDDocument()
        try {
            render(invoice, settings).forEach { image ->
                try {
                    val page = PDPage(PDRectangle(595f, 420f)); result.addPage(page)
                    PDPageContentStream(result, page).use { it.drawImage(LosslessFactory.createFromImage(result, image), 0f, 0f, 595f, 420f) }
                } finally { image.flush() }
            }
            return result
        } catch (error: Exception) { result.close(); throw error }
    }

    /** Write to a sibling temporary file first; failure never replaces an existing exported invoice. */
    fun save(invoice: BarterInvoice, settings: AppSettings, path: Path) {
        val target = path.toAbsolutePath()
        val temp = Files.createTempFile(target.parent, "qirato-invoice-", ".tmp")
        try {
            document(invoice, settings).use { it.save(temp.toFile()) }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temp) }
    }

    fun saveWithDialog(invoice: BarterInvoice, settings: AppSettings): Boolean {
        var target: Path? = null
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = "ذخیره PDF فاکتور"
                selectedFile = java.io.File("Qirato-${invoice.cleanInvoiceNumber.filter { it.isLetterOrDigit() || it == '-' }}.pdf")
            }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                val selected = chooser.selectedFile.toPath()
                val candidate = if (selected.fileName.toString().endsWith(".pdf", true)) selected else selected.resolveSibling("${selected.fileName}.pdf")
                if (!Files.exists(candidate) || JOptionPane.showConfirmDialog(null, "فایل قبلی جایگزین شود؟", "ذخیره فاکتور", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) target = candidate
            }
        }
        val path = target ?: return false
        save(invoice, settings, path); return true
    }

    fun print(invoice: BarterInvoice, settings: AppSettings): Boolean = document(invoice, settings).use { pdf ->
        val job = PrinterJob.getPrinterJob()
        job.jobName = "Qirato-${invoice.cleanInvoiceNumber}"
        job.setPageable(PDFPageable(pdf))
        var accepted = false
        SwingUtilities.invokeAndWait { accepted = job.printDialog() }
        if (accepted) job.print()
        accepted
    }

    fun message(invoice: BarterInvoice): String = buildString {
        appendLine("فاکتور ${PersianNumberFormatter.toPersianDigits(invoice.cleanInvoiceNumber)}")
        appendLine(invoice.customer?.name ?: "مشتری عمومی")
        appendLine("صافی: ${PersianNumberFormatter.formatPrice(invoice.balance.netPayableAmount)} تومان")
        appendLine("وزن فروش: ${PersianNumberFormatter.formatWeight(invoice.balance.totalSales18kWeight)} گرم")
    }
    fun copyMessage(invoice: BarterInvoice) { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(message(invoice)), null) }
}

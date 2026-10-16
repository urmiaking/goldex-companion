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
    private val ink = Color(0x0F172A)
    private val gold = Color(0x9A7416)
    private val line = Color(0xE2E8F0)
    private fun font(bold: Boolean): Font = checkNotNull(javaClass.getResourceAsStream(
        if (bold) "/font/vazirmatn_bold.ttf" else "/font/vazirmatn_regular.ttf"))
        .use { Font.createFont(Font.TRUETYPE_FONT, it) }

    fun render(invoice: BarterInvoice, settings: AppSettings): List<BufferedImage> {
        val model = OfficialInvoiceDocumentFactory.create(invoice, settings)
        val regular = font(false); val bold = font(true)
        val chunks = model.rows.chunked(ROWS_PER_PAGE).ifEmpty { listOf(emptyList()) }
        return chunks.mapIndexed { index, rows ->
            BufferedImage(1190, 840, BufferedImage.TYPE_INT_RGB).apply {
                val g = createGraphics()
                try {
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g.color = Color(0xFFFFFA); g.fillRect(0, 0, width, height)
                    fun text(value: String, right: Int, baseline: Int, size: Float = 19f, strong: Boolean = false, color: Color = ink, maxWidth: Int = 1090) {
                        g.color = color; g.font = (if (strong) bold else regular).deriveFont(size)
                        // Do not truncate legal/customer text; fit a bounded line to the document cell.
                        val w = g.fontMetrics.stringWidth(value).coerceAtLeast(1)
                        if (w > maxWidth) g.font = g.font.deriveFont(size * maxWidth / w)
                        val localized = PersianNumberFormatter.toPersianDigits(value)
                        g.drawString(localized, right - g.fontMetrics.stringWidth(localized), baseline)
                    }
                    fun rule(y: Int) { g.color = line; g.drawLine(44, y, 1146, y) }
                    g.color = Color(0xF5D777); g.fillRoundRect(44, 30, 1102, 96, 18, 18)
                    text(model.sellerName.ifBlank { "واحد صنفی" }, 1094, 66, 28f, true, maxWidth = 870)
                    text("فاکتور رسمی زرگری و تهاتر • پروانه ${model.sellerLicense}", 1094, 94, 18f)
                    text("شماره ${PersianNumberFormatter.toPersianDigits(model.invoiceNumber)} • ${model.issuedDate} • ${model.issuedTime}", 1146, 158, 20f, true, gold)
                    text("فروشنده: ${model.sellerManager} • تلفن: ${model.sellerPhone} • ${model.sellerAddress}", 1146, 189, 17f)
                    text("خریدار: ${model.buyerName} • تلفن: ${model.buyerPhone} • کد ملی: ${model.buyerNationalId}", 1146, 223, 20f, true)
                    text("${model.buyerRole} • نرخ صدور ۱۸ عیار: ${PersianNumberFormatter.formatPrice(model.spotPrice18k)} تومان", 1146, 254, 18f)
                    rule(272)
                    val rights = listOf(1140, 840, 720, 590, 460, 290)
                    val widths = listOf(280, 110, 120, 120, 155, 235)
                    listOf("شرح کالا / نوع معامله", "عیار", "وزن کل", "وزن خالص", "اجرت / سود", "مبلغ (تومان)").forEachIndexed { col, label ->
                        text(label, rights[col], 305, 17f, true, gold, widths[col])
                    }
                    rows.forEachIndexed { rowIndex, row ->
                        val y = 348 + rowIndex * 64
                        text(row.title, rights[0], y, 19f, true, maxWidth = widths[0])
                        text((if (row.isReceived) "دریافتی • " else "فروش • ") + row.subtitle, rights[0], y + 22, 14f, maxWidth = widths[0])
                        text(row.karat?.let { PersianNumberFormatter.toPersianDigits(it.toString()) } ?: "—", rights[1], y, maxWidth = widths[1])
                        text(row.grossWeight?.let { PersianNumberFormatter.formatWeight(it) } ?: "—", rights[2], y, maxWidth = widths[2])
                        text(row.netWeight?.let { PersianNumberFormatter.formatWeight(it) } ?: "—", rights[3], y, maxWidth = widths[3])
                        text("${row.wageLabel} / ${row.profitLabel}", rights[4], y, 17f, maxWidth = widths[4])
                        text(PersianNumberFormatter.formatPrice(row.amountTomans), rights[5], y, 20f, true, maxWidth = widths[5])
                        rule(y + 32)
                    }
                    text("واحد وزن: گرم • وزن کل: ${PersianNumberFormatter.formatWeight(model.totalGrossWeight)} • معادل ۱۸ عیار: ${PersianNumberFormatter.formatWeight(model.totalNetWeight)}", 1146, 618, 17f)
                    text("ارزش طلا: ${PersianNumberFormatter.formatPrice(model.totalRawGoldValue)} • اجرت و سود: ${PersianNumberFormatter.formatPrice(model.totalWageAndProfit)} • مالیات: ${PersianNumberFormatter.formatPrice(model.totalTax)} تومان", 1146, 647, 17f)
                    text("دریافتی تهاتر: ${PersianNumberFormatter.formatPrice(model.totalReceived)} • صافی: ${PersianNumberFormatter.formatPrice(model.payableAmount)} تومان", 1146, 679, 22f, true, gold)
                    text("روش تسویه: ${model.settlementLabel} • پیگیری: ${PersianNumberFormatter.toPersianDigits(model.trackingCode)}", 1146, 708, 17f)
                    text(model.note, 1146, 735, 16f)
                    text("مهر و امضای فروشنده", 390, 775, 17f)
                    text("امضای خریدار", 1000, 775, 17f)
                    text("صفحه ${PersianNumberFormatter.toPersianDigits((index + 1).toString())} از ${PersianNumberFormatter.toPersianDigits(chunks.size.toString())}", 660, 817, 15f, color = gold)
                    drawBrand(g, settings.invoiceLogoUri, 56, 34, 90, 86)
                    drawBrand(g, settings.invoiceStampUri, 45, 741, 115, 62)
                    drawBrand(g, settings.invoiceSignatureUri, 172, 782, 218, 40)
                } finally { g.dispose() }
            }
        }
    }

    private fun drawBrand(g: Graphics2D, uri: String, x: Int, y: Int, width: Int, height: Int) {
        if (uri.isBlank()) return
        val path = if (uri.startsWith("file:")) Path.of(java.net.URI(uri)) else Path.of(uri)
        require(Files.isRegularFile(path) && Files.size(path) <= 12 * 1024 * 1024) { "تصویر مشخصات واحد صنفی قابل خواندن نیست" }
        ImageIO.createImageInputStream(path.toFile()).use { input ->
            val readers = ImageIO.getImageReaders(input)
            require(readers.hasNext()) { "قالب تصویر مشخصات واحد صنفی معتبر نیست" }
            val reader = readers.next()
            try {
                reader.input = input
                require(reader.getWidth(0).toLong() * reader.getHeight(0) <= 20_000_000) { "ابعاد تصویر بیش از حد بزرگ است" }
                val image = reader.read(0)
                try {
                    val scale = minOf(width.toDouble() / image.width, height.toDouble() / image.height)
                    g.drawImage(image, x, y, (image.width * scale).toInt(), (image.height * scale).toInt(), null)
                } finally { image.flush() }
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

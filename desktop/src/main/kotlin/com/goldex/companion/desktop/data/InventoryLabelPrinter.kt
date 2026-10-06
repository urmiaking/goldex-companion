package com.goldex.companion.desktop.data

import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.PersianNumberFormatter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.print.Paper
import java.awt.print.Printable
import java.awt.print.PrinterJob
import javax.swing.SwingUtilities

/** Actual scannable label: Code 128 for ASCII codes, UTF-8 QR for localized identifiers. */
object InventoryLabelPrinter {
    fun barcode(item: InventoryItem): BufferedImage {
        val linear = item.code.all { it.code in 32..126 } && item.code.length <= 40
        val size = if (linear) 560 to 110 else 180 to 180
        val matrix = MultiFormatWriter().encode(item.code, if (linear) BarcodeFormat.CODE_128 else BarcodeFormat.QR_CODE,
            size.first, size.second, mapOf(EncodeHintType.MARGIN to 12, EncodeHintType.CHARACTER_SET to "UTF-8"))
        return BufferedImage(matrix.width, matrix.height, BufferedImage.TYPE_INT_RGB).apply {
            for (y in 0 until height) for (x in 0 until width) setRGB(x, y, if (matrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
        }
    }
    fun render(item: InventoryItem): BufferedImage {
        val image = BufferedImage(600, 420, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color.WHITE; graphics.fillRect(0, 0, image.width, image.height); graphics.color = Color.BLACK
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val font = checkNotNull(javaClass.getResourceAsStream("/font/vazirmatn_regular.ttf")).use { Font.createFont(Font.TRUETYPE_FONT, it) }
            fun line(text: String, y: Int, size: Float) {
                graphics.font = font.deriveFont(size)
                val initialWidth = graphics.fontMetrics.stringWidth(text).coerceAtLeast(1)
                if (initialWidth > image.width - 24) graphics.font = font.deriveFont(size * (image.width - 24) / initialWidth)
                val metrics = graphics.fontMetrics
                graphics.drawString(text, (image.width - metrics.stringWidth(text)).coerceAtLeast(12) / 2, y)
            }
            line(item.title.take(55), 40, 26f)
            line("${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams)} گرم • عیار ${PersianNumberFormatter.toPersianDigits(item.customKaratValue.toString())}", 78, 22f)
            val barcode = barcode(item)
            graphics.drawImage(barcode, (image.width - barcode.width) / 2, 100, null); barcode.flush()
            line(item.code.take(80), 330, 20f)
            line("قیراط • ${item.category.titleFa}", 370, 19f)
        } finally { graphics.dispose() }
        return image
    }
    /** Called from IO; the dialog is on EDT, printer I/O never blocks the Compose UI. */
    fun print(item: InventoryItem): Boolean {
        val image = render(item)
        try {
            val job = PrinterJob.getPrinterJob()
            job.jobName = "Qirato inventory label"
            val format = job.defaultPage().apply { paper = Paper().apply {
                val width = 60.0 / 25.4 * 72; val height = 42.0 / 25.4 * 72
                setSize(width, height); setImageableArea(3.0, 3.0, width - 6, height - 6)
            } }
            job.setPrintable({ graphics, page, index ->
                if (index != 0) Printable.NO_SUCH_PAGE else {
                    val g = graphics as Graphics2D
                    g.translate(page.imageableX, page.imageableY)
                    val scale = minOf(page.imageableWidth / image.width, page.imageableHeight / image.height)
                    g.scale(scale, scale); g.drawImage(image, 0, 0, null); Printable.PAGE_EXISTS
                }
            }, format)
            var accepted = false
            SwingUtilities.invokeAndWait { accepted = job.printDialog() }
            if (accepted) job.print()
            return accepted
        } finally { image.flush() }
    }
}

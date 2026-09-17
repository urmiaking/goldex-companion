package com.goldex.companion.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.goldex.companion.R
import com.goldex.companion.data.AppSettings
import com.goldex.companion.domain.invoice.OfficialInvoiceDocument
import com.goldex.companion.domain.invoice.OfficialInvoiceDocumentFactory
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.PersianNumberFormatter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object OfficialInvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 420

    fun share(context: Context, invoice: BarterInvoice, settings: AppSettings): Boolean {
        val file = create(context, invoice, settings) ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "فاکتور رسمی ${invoice.invoiceNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "دریافت یا اشتراک نسخه PDF"))
        return true
    }

    fun print(context: Context, invoice: BarterInvoice, settings: AppSettings): Boolean {
        val file = create(context, invoice, settings) ?: return false
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            "Qirat-${invoice.invoiceNumber}",
            CachedPdfPrintAdapter(file),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A5.asLandscape())
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
        return true
    }

    fun create(context: Context, invoice: BarterInvoice, settings: AppSettings): File? {
        val model = OfficialInvoiceDocumentFactory.create(invoice, settings)
        val directory = File(context.cacheDir, "invoices").apply { mkdirs() }
        val output = File(directory, model.fileName)
        val pdf = PdfDocument()
        return try {
            val rowsPerPage = 5
            val chunks = model.rows.ifEmpty { listOf() }.chunked(rowsPerPage).ifEmpty { listOf(emptyList()) }
            chunks.forEachIndexed { pageIndex, rows ->
                val page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                )
                drawPage(
                    canvas = page.canvas,
                    model = model,
                    rowStartIndex = pageIndex * rowsPerPage,
                    rows = rows,
                    pageNumber = pageIndex + 1,
                    pageCount = chunks.size,
                    isLastPage = pageIndex == chunks.lastIndex,
                    regularTypeface = ResourcesCompat.getFont(context, R.font.vazirmatn_regular)
                        ?: Typeface.DEFAULT,
                    boldTypeface = ResourcesCompat.getFont(context, R.font.vazirmatn_bold)
                        ?: Typeface.DEFAULT_BOLD
                )
                pdf.finishPage(page)
            }
            FileOutputStream(output).use { pdf.writeTo(it) }
            output
        } catch (_: Exception) {
            output.delete()
            null
        } finally {
            pdf.close()
        }
    }

    private fun drawPage(
        canvas: Canvas,
        model: OfficialInvoiceDocument,
        rowStartIndex: Int,
        rows: List<com.goldex.companion.domain.invoice.OfficialInvoiceRow>,
        pageNumber: Int,
        pageCount: Int,
        isLastPage: Boolean,
        regularTypeface: Typeface,
        boldTypeface: Typeface
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regularTypeface }
        fun fill(color: Int, left: Float, top: Float, right: Float, bottom: Float, radius: Float = 0f) {
            paint.style = Paint.Style.FILL
            paint.color = color
            if (radius > 0f) canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
            else canvas.drawRect(left, top, right, bottom, paint)
        }
        fun stroke(color: Int, left: Float, top: Float, right: Float, bottom: Float, radius: Float = 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.65f
            paint.color = color
            if (radius > 0f) canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
            else canvas.drawRect(left, top, right, bottom, paint)
            paint.style = Paint.Style.FILL
        }
        fun text(
            value: String,
            x: Float,
            y: Float,
            size: Float = 7f,
            color: Int = Color.rgb(28, 28, 28),
            align: Paint.Align = Paint.Align.RIGHT,
            bold: Boolean = false
        ) {
            paint.color = color
            paint.textSize = size
            paint.textAlign = align
            paint.typeface = if (bold) boldTypeface else regularTypeface
            canvas.drawText(value, x, y, paint)
        }

        fill(Color.WHITE, 0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat())
        fill(Color.rgb(151, 115, 25), 22f, 13f, 573f, 16f, 2f)
        fill(Color.rgb(240, 208, 105), 170f, 13f, 425f, 16f)

        // Official three-column header.
        text(model.sellerName, 558f, 31f, 10.5f, bold = true)
        text(model.sellerLicense, 558f, 43f, 6.7f, Color.DKGRAY)
        fill(Color.rgb(250, 250, 249), 207f, 21f, 388f, 51f, 4f)
        stroke(Color.rgb(214, 211, 209), 207f, 21f, 388f, 51f, 4f)
        text("فاکتور رسمی الکترونیکی فروش طلا و جواهر", 297.5f, 34f, 8.2f, align = Paint.Align.CENTER, bold = true)
        text("${model.invoiceNumber}  •  ${model.issuedDate}  •  ${model.issuedTime}", 297.5f, 45f, 6.4f, Color.DKGRAY, Paint.Align.CENTER)
        text("کد رهگیری سامانه", 191f, 31f, 6.7f, bold = true)
        text(model.trackingCode, 191f, 43f, 6.5f, Color.rgb(134, 102, 13), bold = true)
        drawQrMark(canvas, paint, 28f, 22f)
        text("صفحه ${PersianNumberFormatter.toPersianDigits(pageNumber.toString())} از ${PersianNumberFormatter.toPersianDigits(pageCount.toString())}", 50f, 48f, 5.8f, Color.GRAY, Paint.Align.LEFT)
        paint.color = Color.rgb(41, 37, 36)
        paint.strokeWidth = 0.8f
        canvas.drawLine(22f, 57f, 573f, 57f, paint)

        // Seller and buyer cards.
        fill(Color.rgb(250, 250, 249), 303f, 65f, 573f, 111f, 4f)
        stroke(Color.rgb(231, 229, 228), 303f, 65f, 573f, 111f, 4f)
        text("مشخصات فروشنده (واحد صنفی)", 561f, 78f, 7f, Color.rgb(138, 104, 14), bold = true)
        text("متصدی: ${model.sellerManager}", 561f, 91f, 6.4f)
        text("تلفن: ${model.sellerPhone}", 430f, 91f, 6.4f)
        text("نشانی: ${model.sellerAddress.take(54)}", 561f, 104f, 6.1f, Color.DKGRAY)

        fill(Color.rgb(250, 250, 249), 22f, 65f, 292f, 111f, 4f)
        stroke(Color.rgb(231, 229, 228), 22f, 65f, 292f, 111f, 4f)
        text("مشخصات خریدار", 280f, 78f, 7f, bold = true)
        text("نام: ${model.buyerName}", 280f, 91f, 6.4f)
        text("کد ملی: ${model.buyerNationalId}", 150f, 91f, 6.4f)
        text("همراه: ${model.buyerPhone}", 280f, 104f, 6.4f)
        text(model.buyerRole, 150f, 104f, 6.1f, Color.rgb(134, 102, 13), bold = true)

        fill(Color.rgb(255, 251, 235), 22f, 118f, 573f, 137f, 3f)
        stroke(Color.rgb(253, 230, 138), 22f, 118f, 573f, 137f, 3f)
        text("مظنه مبنا در زمان صدور", 560f, 131f, 6.5f, Color.rgb(131, 99, 15), bold = true)
        text("گرم ۱۸ عیار (۷۵۰): ${PersianNumberFormatter.formatPrice(model.spotPrice18k.toDouble())} تومان", 295f, 131f, 7f, bold = true)
        text("روش تسویه: ${model.settlementLabel}", 35f, 131f, 6.5f, Color.DKGRAY, Paint.Align.LEFT)

        // Table.
        val top = 145f
        val rowHeight = 25f
        val columns = floatArrayOf(22f, 48f, 221f, 258f, 316f, 369f, 426f, 475f, 573f)
        fill(Color.rgb(245, 245, 244), 22f, top, 573f, top + 20f)
        stroke(Color.rgb(214, 211, 209), 22f, top, 573f, top + 20f, 3f)
        val headers = listOf("ردیف", "شرح و مشخصات", "عیار", "ناخالص", "کسر نگین", "خالص", "اجرت/سود", "ارزش کل")
        headers.forEachIndexed { index, header ->
            val center = (columns[index] + columns[index + 1]) / 2f
            text(header, center, top + 13f, 6.1f, align = Paint.Align.CENTER, bold = true)
        }
        rows.forEachIndexed { index, row ->
            val yTop = top + 20f + index * rowHeight
            if (row.isReceived) fill(Color.rgb(255, 247, 237), 22f, yTop, 573f, yTop + rowHeight)
            stroke(Color.rgb(231, 229, 228), 22f, yTop, 573f, yTop + rowHeight)
            val centerY = yTop + 15f
            text(PersianNumberFormatter.toPersianDigits((rowStartIndex + index + 1).toString()), 35f, centerY, 6.6f, Color.GRAY, Paint.Align.CENTER, true)
            text(row.title.take(31), 211f, yTop + 10f, 6.6f, bold = true)
            text(row.subtitle.take(38), 211f, yTop + 20f, 5.4f, Color.GRAY)
            text(row.karat?.let { PersianNumberFormatter.toPersianDigits(it.toString()) } ?: "—", 239f, centerY, 6.3f, align = Paint.Align.CENTER, bold = true)
            text(row.grossWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 287f, centerY, 6.1f, align = Paint.Align.CENTER)
            text(row.stoneWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 342f, centerY, 6.1f, align = Paint.Align.CENTER)
            text(row.netWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 397f, centerY, 6.1f, align = Paint.Align.CENTER, bold = true)
            text("${row.wageLabel} / ${row.profitLabel}", 450f, centerY, 5.6f, align = Paint.Align.CENTER)
            val sign = if (row.isReceived) "−" else ""
            text("$sign${PersianNumberFormatter.formatPrice(row.amountTomans.toDouble())}", 564f, centerY, 6.4f, align = Paint.Align.RIGHT, bold = true)
        }

        val tableBottom = top + 20f + rows.size.coerceAtLeast(1) * rowHeight
        if (rows.isEmpty()) text("این فاکتور فاقد قلم ثبت‌شده است", 297.5f, tableBottom - 9f, 7f, Color.GRAY, Paint.Align.CENTER)

        if (isLastPage) {
            val summaryTop = (tableBottom + 8f).coerceAtMost(294f)
            fill(Color.rgb(250, 250, 249), 303f, summaryTop, 573f, summaryTop + 72f, 4f)
            stroke(Color.rgb(231, 229, 228), 303f, summaryTop, 573f, summaryTop + 72f, 4f)
            val summary = listOf(
                "وزن ناخالص / معادل خالص" to "${PersianNumberFormatter.formatWeight(model.totalGrossWeight)} / ${PersianNumberFormatter.formatWeight(model.totalNetWeight)} گرم",
                "ارزش طلای خام" to "${PersianNumberFormatter.formatPrice(model.totalRawGoldValue.toDouble())} تومان",
                "اجرت ساخت و سود" to "${PersianNumberFormatter.formatPrice(model.totalWageAndProfit.toDouble())} تومان",
                "مالیات ارزش افزوده اجرت و سود" to "${PersianNumberFormatter.formatPrice(model.totalTax.toDouble())} تومان",
                "جمع دریافتی/تهاتر" to "${PersianNumberFormatter.formatPrice(model.totalReceived.toDouble())} تومان"
            )
            summary.forEachIndexed { i, entry ->
                text(entry.first, 560f, summaryTop + 13f + i * 10.5f, 5.9f, Color.DKGRAY)
                text(entry.second, 315f, summaryTop + 13f + i * 10.5f, 6.1f, align = Paint.Align.LEFT, bold = true)
            }
            fill(Color.rgb(255, 251, 235), 303f, summaryTop + 76f, 573f, summaryTop + 103f, 3f)
            stroke(Color.rgb(217, 174, 53), 303f, summaryTop + 76f, 573f, summaryTop + 103f, 3f)
            text("مبلغ کل قابل پرداخت", 560f, summaryTop + 94f, 7.3f, bold = true)
            text("${PersianNumberFormatter.formatPrice(model.payableAmount.toDouble())} تومان", 315f, summaryTop + 95f, 10.5f, Color.rgb(134, 102, 13), Paint.Align.LEFT, true)

            fill(Color.rgb(250, 250, 249), 22f, summaryTop, 292f, summaryTop + 52f, 4f)
            stroke(Color.rgb(231, 229, 228), 22f, summaryTop, 292f, summaryTop + 52f, 4f)
            text("بر اساس ماده ۲۶ قانون مالیات بر ارزش افزوده، اصل طلا از مالیات", 280f, summaryTop + 15f, 5.6f, Color.DKGRAY)
            text("معاف است و مالیات صرفاً بر اجرت و سود اعمال شده است.", 280f, summaryTop + 27f, 5.6f, Color.DKGRAY)
            text("یادداشت: ${model.note.ifBlank { "بدون توضیحات" }.take(52)}", 280f, summaryTop + 43f, 5.7f, Color.rgb(134, 102, 13), bold = true)
            stroke(Color.rgb(220, 38, 38), 43f, summaryTop + 61f, 93f, summaryTop + 101f, 20f)
            text("مهر واحد صنفی", 68f, summaryTop + 84f, 5.7f, Color.rgb(185, 28, 28), Paint.Align.CENTER, true)
            text("امضاء فروشنده", 180f, summaryTop + 84f, 6.2f, Color.DKGRAY, Paint.Align.CENTER, true)
            text("امضاء خریدار", 260f, summaryTop + 84f, 6.2f, Color.DKGRAY, Paint.Align.CENTER, true)
        }

        paint.color = Color.rgb(214, 211, 209)
        canvas.drawLine(22f, 404f, 573f, 404f, paint)
        text("سند تولیدشده در سامانه هوشمند قیراط • ${model.trackingCode}", 22f, 414f, 5.4f, Color.GRAY, Paint.Align.LEFT)
    }

    private fun drawQrMark(canvas: Canvas, paint: Paint, left: Float, top: Float) {
        paint.color = Color.rgb(28, 25, 23)
        val pattern = arrayOf(
            "111010111", "101010101", "111110111", "000101000", "111011101",
            "101110001", "111011111", "001101001", "111001111"
        )
        pattern.forEachIndexed { y, line ->
            line.forEachIndexed { x, char ->
                if (char == '1') canvas.drawRect(left + x * 3f, top + y * 3f, left + (x + 1) * 3f, top + (y + 1) * 3f, paint)
            }
        }
    }

    private class CachedPdfPrintAdapter(private val file: File) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                }
                if (cancellationSignal.isCanceled) callback.onWriteCancelled()
                else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (error: Exception) {
                callback.onWriteFailed(error.message)
            }
        }
    }
}

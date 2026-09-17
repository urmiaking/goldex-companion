package com.goldex.companion.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
import com.goldex.companion.domain.invoice.OfficialInvoiceRow
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.PersianNumberFormatter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object OfficialInvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 420
    private const val MARGIN = 22f
    private const val GOLD = 0xFF9A7416.toInt()
    private const val GOLD_LIGHT = 0xFFF5D777.toInt()
    private const val INK = 0xFF1C2535.toInt()
    private const val MUTED = 0xFF667085.toInt()
    private const val LINE = 0xFFE4E0D7.toInt()
    private const val PAPER = 0xFFFFFEFA.toInt()

    fun share(context: Context, invoice: BarterInvoice, settings: AppSettings): Boolean {
        val file = create(context, invoice, settings) ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "فاکتور رسمی ${invoice.invoiceNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "ذخیره یا اشتراک نسخه PDF"))
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
        val regular = ResourcesCompat.getFont(context, R.font.vazirmatn_regular) ?: Typeface.DEFAULT
        val bold = ResourcesCompat.getFont(context, R.font.vazirmatn_bold) ?: Typeface.DEFAULT_BOLD
        val logo = loadPersistedBitmap(context, settings.invoiceLogoUri)
        val stamp = loadPersistedBitmap(context, settings.invoiceStampUri)

        return try {
            val rowsPerPage = 5
            val chunks = model.rows.chunked(rowsPerPage).ifEmpty { listOf(emptyList()) }
            chunks.forEachIndexed { pageIndex, rows ->
                val page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                )
                drawPage(
                    canvas = page.canvas,
                    model = model,
                    settings = settings,
                    logo = logo,
                    stamp = stamp,
                    rowStartIndex = pageIndex * rowsPerPage,
                    rows = rows,
                    pageNumber = pageIndex + 1,
                    pageCount = chunks.size,
                    isLastPage = pageIndex == chunks.lastIndex,
                    regularTypeface = regular,
                    boldTypeface = bold
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
        settings: AppSettings,
        logo: Bitmap?,
        stamp: Bitmap?,
        rowStartIndex: Int,
        rows: List<OfficialInvoiceRow>,
        pageNumber: Int,
        pageCount: Int,
        isLastPage: Boolean,
        regularTypeface: Typeface,
        boldTypeface: Typeface
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regularTypeface }
        val box: (Int, RectF, Float) -> Unit = { color, rect, radius ->
            paint.style = Paint.Style.FILL
            paint.color = color
            if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, paint) else canvas.drawRect(rect, paint)
        }
        val outline: (Int, RectF, Float, Float) -> Unit = { color, rect, radius, width ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = width
            paint.color = color
            if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, paint) else canvas.drawRect(rect, paint)
            paint.style = Paint.Style.FILL
        }
        val label: (String, Float, Float, Float, Int, Paint.Align, Boolean, Int) -> Unit =
            { value, x, y, size, color, align, bold, alpha ->
                paint.color = color
                paint.alpha = alpha
                paint.textSize = size
                paint.textAlign = align
                paint.typeface = if (bold) boldTypeface else regularTypeface
                canvas.drawText(value, x, y, paint)
                paint.alpha = 255
            }

        box(PAPER, RectF(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat()), 0f)
        if (settings.invoiceWatermarkEnabled) {
            canvas.save()
            canvas.rotate(-12f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
            label(settings.galleryName.ifBlank { "قیراط" }, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f + 14f, 38f, GOLD, Paint.Align.CENTER, true, 16)
            label("سند رسمی طلا و جواهر", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f + 32f, 11f, GOLD, Paint.Align.CENTER, true, 14)
            canvas.restore()
        }

        drawTopRibbon(canvas, paint)
        drawBrandHeader(canvas, paint, model, settings, logo, label, box, outline)
        drawPartyCards(model, settings, label, box, outline)
        drawRateStrip(model, settings, label, box, outline)
        drawItemsTable(model, rows, rowStartIndex, label, box, outline)
        if (isLastPage) {
            drawSummaryAndSignatures(canvas, model, settings, stamp, label, box, outline)
        } else {
            label("ادامه اقلام و جمع‌بندی مالی در صفحه بعد", PAGE_WIDTH / 2f, 338f, 7f, MUTED, Paint.Align.CENTER, true, 255)
        }

        paint.color = LINE
        paint.strokeWidth = 0.7f
        canvas.drawLine(MARGIN, 402f, PAGE_WIDTH - MARGIN, 402f, paint)
        label("سند امن قیراط • ${model.trackingCode}", PAGE_WIDTH - MARGIN, 413f, 5.5f, MUTED, Paint.Align.RIGHT, false, 255)
        label("صفحه ${PersianNumberFormatter.toPersianDigits(pageNumber.toString())} از ${PersianNumberFormatter.toPersianDigits(pageCount.toString())}", MARGIN, 413f, 5.5f, MUTED, Paint.Align.LEFT, false, 255)
    }

    private fun drawTopRibbon(canvas: Canvas, paint: Paint) {
        paint.shader = android.graphics.LinearGradient(
            MARGIN, 0f, PAGE_WIDTH - MARGIN, 0f,
            intArrayOf(GOLD, GOLD_LIGHT, GOLD), null, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(MARGIN, 11f, PAGE_WIDTH - MARGIN, 15f, 2f, 2f, paint)
        paint.shader = null
    }

    private fun drawBrandHeader(
        canvas: Canvas,
        paint: Paint,
        model: OfficialInvoiceDocument,
        settings: AppSettings,
        logo: Bitmap?,
        label: (String, Float, Float, Float, Int, Paint.Align, Boolean, Int) -> Unit,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val header = RectF(MARGIN, 22f, PAGE_WIDTH - MARGIN, 65f)
        box(Color.WHITE, header, 7f)
        outline(LINE, header, 7f, 0.7f)
        val logoRect = RectF(526f, 29f, 555f, 58f)
        if (logo != null) canvas.drawBitmap(logo, null, logoRect, paint) else {
            box(0xFFFFF8E1.toInt(), logoRect, 7f)
            outline(0x66D4AF37, logoRect, 7f, 0.8f)
            label(settings.galleryName.take(2), 540.5f, 48f, 8f, GOLD, Paint.Align.CENTER, true, 255)
        }
        label(settings.galleryName, 516f, 39f, 9.5f, INK, Paint.Align.RIGHT, true, 255)
        label("مدیر: ${settings.managerName}", 516f, 51f, 5.8f, MUTED, Paint.Align.RIGHT, false, 255)
        label("پروانه: ${settings.unionCode}", 516f, 60f, 5.8f, GOLD, Paint.Align.RIGHT, true, 255)

        box(0xFFFAF8F1.toInt(), RectF(205f, 28f, 390f, 59f), 6f)
        label("فاکتور رسمی فروش و مبادله طلا", 297.5f, 41f, 8f, INK, Paint.Align.CENTER, true, 255)
        label("شماره ${model.invoiceNumber}  •  ${model.issuedDate}  •  ${model.issuedTime}", 297.5f, 53f, 5.9f, MUTED, Paint.Align.CENTER, false, 255)

        val qrEnabled = settings.invoiceQrVerificationEnabled || settings.invoiceQrGemCertificateEnabled || settings.invoiceQrCatalogEnabled
        if (qrEnabled) drawQrMark(canvas, paint, 31f, 29f, 3f)
        label("QR اصالت", 70f, 38f, 6.4f, INK, Paint.Align.LEFT, true, 255)
        label(model.trackingCode.take(22), 70f, 49f, 5.5f, GOLD, Paint.Align.LEFT, true, 255)
        val features = buildList {
            if (settings.invoiceQrVerificationEnabled) add("استعلام")
            if (settings.invoiceQrGemCertificateEnabled) add("شناسنامه")
            if (settings.invoiceQrCatalogEnabled) add("کاتالوگ")
        }.joinToString(" • ").ifBlank { "غیرفعال" }
        label(features, 70f, 59f, 5f, MUTED, Paint.Align.LEFT, false, 255)
    }

    private fun drawPartyCards(
        model: OfficialInvoiceDocument,
        settings: AppSettings,
        label: (String, Float, Float, Float, Int, Paint.Align, Boolean, Int) -> Unit,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val seller = RectF(303f, 72f, 573f, 117f)
        val buyer = RectF(22f, 72f, 292f, 117f)
        listOf(seller, buyer).forEach { box(Color.WHITE, it, 6f); outline(LINE, it, 6f, 0.65f) }
        box(0xFFFFF7DE.toInt(), RectF(492f, 77f, 563f, 89f), 6f)
        label("فروشنده رسمی", 527.5f, 85.5f, 5.7f, GOLD, Paint.Align.CENTER, true, 255)
        label(settings.galleryName, 563f, 99f, 7f, INK, Paint.Align.RIGHT, true, 255)
        label(settings.galleryPhone, 315f, 99f, 6f, MUTED, Paint.Align.LEFT, false, 255)
        label(settings.galleryAddress.take(55), 563f, 111f, 5.5f, MUTED, Paint.Align.RIGHT, false, 255)

        box(0xFFF2F4F7.toInt(), RectF(211f, 77f, 282f, 89f), 6f)
        label("مشخصات خریدار", 246.5f, 85.5f, 5.7f, INK, Paint.Align.CENTER, true, 255)
        label(model.buyerName, 282f, 99f, 7f, INK, Paint.Align.RIGHT, true, 255)
        label(model.buyerPhone, 34f, 99f, 6f, MUTED, Paint.Align.LEFT, false, 255)
        label("شناسه: ${model.buyerNationalId}", 282f, 111f, 5.5f, MUTED, Paint.Align.RIGHT, false, 255)
        label(model.buyerRole, 34f, 111f, 5.5f, GOLD, Paint.Align.LEFT, true, 255)
    }

    private fun drawRateStrip(
        model: OfficialInvoiceDocument,
        settings: AppSettings,
        label: (String, Float, Float, Float, Int, Paint.Align, Boolean, Int) -> Unit,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val strip = RectF(MARGIN, 123f, PAGE_WIDTH - MARGIN, 143f)
        box(0xFFFFFAE8.toInt(), strip, 5f)
        outline(0x80E8C96A.toInt(), strip, 5f, 0.65f)
        label("مبنای محاسبه", 558f, 136f, 6f, GOLD, Paint.Align.RIGHT, true, 255)
        label("هر گرم طلای ۱۸ عیار: ${PersianNumberFormatter.formatPrice(model.spotPrice18k.toDouble())} تومان", 402f, 136f, 6.5f, INK, Paint.Align.RIGHT, true, 255)
        label("روش تسویه: ${model.settlementLabel}", 35f, 136f, 6f, MUTED, Paint.Align.LEFT, false, 255)
        if (settings.invoiceQrVerificationEnabled) {
            box(0xFFDDF7EC.toInt(), RectF(213f, 127f, 270f, 139f), 6f)
            label("اصالت فعال", 241.5f, 135.5f, 5.3f, 0xFF08704F.toInt(), Paint.Align.CENTER, true, 255)
        }
    }

    private fun drawItemsTable(
        model: OfficialInvoiceDocument,
        rows: List<OfficialInvoiceRow>,
        rowStartIndex: Int,
        label: (String, Float, Float, Float, Int, Paint.Align, Boolean, Int) -> Unit,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val top = 150f
        val headerHeight = 21f
        val rowHeight = 25f
        val columns = floatArrayOf(22f, 45f, 218f, 260f, 310f, 360f, 412f, 476f, 573f)
        box(0xFFF0EEE8.toInt(), RectF(22f, top, 573f, top + headerHeight), 5f)
        outline(LINE, RectF(22f, top, 573f, top + headerHeight), 5f, 0.7f)
        val headers = listOf("ردیف", "شرح قلم", "عیار", "ناخالص", "کسر", "خالص", "اجرت / سود", "مبلغ (تومان)")
        headers.forEachIndexed { index, header ->
            label(header, (columns[index] + columns[index + 1]) / 2f, top + 14f, 5.8f, INK, Paint.Align.CENTER, true, 255)
        }
        if (rows.isEmpty()) {
            outline(LINE, RectF(22f, top + headerHeight, 573f, top + headerHeight + rowHeight), 0f, 0.6f)
            label("هنوز قلمی در این فاکتور ثبت نشده است", 297.5f, top + 38f, 6.5f, MUTED, Paint.Align.CENTER, false, 255)
            return
        }
        rows.forEachIndexed { index, row ->
            val yTop = top + headerHeight + index * rowHeight
            val background = when { row.isReceived -> 0xFFFFF5F3.toInt(); index % 2 == 0 -> Color.WHITE; else -> 0xFFFBFAF7.toInt() }
            box(background, RectF(22f, yTop, 573f, yTop + rowHeight), 0f)
            outline(LINE, RectF(22f, yTop, 573f, yTop + rowHeight), 0f, 0.45f)
            val centerY = yTop + 15f
            label(PersianNumberFormatter.toPersianDigits((rowStartIndex + index + 1).toString()), 33.5f, centerY, 6.3f, MUTED, Paint.Align.CENTER, true, 255)
            val title = (if (row.isReceived) "دریافت • " else "") + row.title
            label(title.take(35), 209f, yTop + 10f, 6.4f, if (row.isReceived) 0xFF9F3A2C.toInt() else INK, Paint.Align.RIGHT, true, 255)
            label(row.subtitle.take(42), 209f, yTop + 20f, 5f, MUTED, Paint.Align.RIGHT, false, 255)
            label(row.karat?.let { PersianNumberFormatter.toPersianDigits(it.toString()) } ?: "—", 239f, centerY, 6f, GOLD, Paint.Align.CENTER, true, 255)
            label(row.grossWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 285f, centerY, 5.9f, INK, Paint.Align.CENTER, false, 255)
            label(row.stoneWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 335f, centerY, 5.9f, MUTED, Paint.Align.CENTER, false, 255)
            label(row.netWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 386f, centerY, 6f, INK, Paint.Align.CENTER, true, 255)
            label("${row.wageLabel} / ${row.profitLabel}", 444f, centerY, 5.2f, MUTED, Paint.Align.CENTER, false, 255)
            val prefix = if (row.isReceived) "−" else ""
            label("$prefix${PersianNumberFormatter.formatPrice(row.amountTomans.toDouble())}", 564f, centerY, 6.4f, INK, Paint.Align.RIGHT, true, 255)
        }
        if (model.rows.size > rows.size) label("اقلام این صفحه", 565f, 298f, 5f, MUTED, Paint.Align.RIGHT, false, 255)
    }

    private fun drawSummaryAndSignatures(
        canvas: Canvas,
        model: OfficialInvoiceDocument,
        settings: AppSettings,
        stamp: Bitmap?,
        label: (String, Float, Float, Float, Int, Paint.Align, Boolean, Int) -> Unit,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val top = 304f
        val finance = RectF(303f, top, 573f, 392f)
        val legal = RectF(22f, top, 292f, 392f)
        box(Color.WHITE, finance, 7f); outline(LINE, finance, 7f, 0.7f)
        box(0xFFFBFAF7.toInt(), legal, 7f); outline(LINE, legal, 7f, 0.7f)
        label("جمع‌بندی مالی", 560f, top + 14f, 7f, INK, Paint.Align.RIGHT, true, 255)
        val summaryRows = listOf(
            "وزن ناخالص / معادل خالص" to "${PersianNumberFormatter.formatWeight(model.totalGrossWeight)} / ${PersianNumberFormatter.formatWeight(model.totalNetWeight)} گرم",
            "ارزش طلای خام" to "${PersianNumberFormatter.formatPrice(model.totalRawGoldValue.toDouble())} تومان",
            "اجرت و سود" to "${PersianNumberFormatter.formatPrice(model.totalWageAndProfit.toDouble())} تومان",
            "مالیات اجرت و سود" to "${PersianNumberFormatter.formatPrice(model.totalTax.toDouble())} تومان",
            "اقلام دریافتی / تهاتر" to "${PersianNumberFormatter.formatPrice(model.totalReceived.toDouble())} تومان"
        )
        summaryRows.forEachIndexed { index, item ->
            val y = top + 26f + index * 8.5f
            label(item.first, 560f, y, 5.4f, MUTED, Paint.Align.RIGHT, false, 255)
            label(item.second, 315f, y, 5.7f, INK, Paint.Align.LEFT, true, 255)
        }
        box(0xFFFFF4CC.toInt(), RectF(309f, top + 66f, 567f, top + 84f), 5f)
        outline(0x99D4AF37.toInt(), RectF(309f, top + 66f, 567f, top + 84f), 5f, 0.7f)
        label("مبلغ نهایی قابل پرداخت", 554f, top + 78f, 6.2f, INK, Paint.Align.RIGHT, true, 255)
        label("${PersianNumberFormatter.formatPrice(model.payableAmount.toDouble())} تومان", 320f, top + 79f, 8.5f, GOLD, Paint.Align.LEFT, true, 255)

        label("اصالت و تایید سند", 280f, top + 14f, 7f, INK, Paint.Align.RIGHT, true, 255)
        label("اصل طلا مطابق ماده ۲۶ از مالیات معاف است؛ مالیات فقط", 280f, top + 27f, 5.2f, MUTED, Paint.Align.RIGHT, false, 255)
        label("بر اجرت و سود محاسبه شده و عیار اقلام تضمین می‌شود.", 280f, top + 37f, 5.2f, MUTED, Paint.Align.RIGHT, false, 255)
        if (model.note.isNotBlank()) label("توضیحات: ${model.note.take(55)}", 280f, top + 49f, 5.1f, GOLD, Paint.Align.RIGHT, true, 255)
        if (settings.invoiceStampEnabled) {
            val stampRect = RectF(35f, top + 48f, 78f, top + 87f)
            if (stamp != null) {
                canvas.drawBitmap(stamp, null, stampRect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = (settings.invoiceStampOpacity.coerceIn(30, 100) * 2.55f).toInt()
                })
            } else {
                outline(0xFFB42318.toInt(), stampRect, 22f, 1.1f)
                val alpha = settings.invoiceStampOpacity * 255 / 100
                label("مهر رسمی", 56.5f, top + 68f, 5.5f, 0xFFB42318.toInt(), Paint.Align.CENTER, true, alpha)
                label(settings.unionCode, 56.5f, top + 77f, 4.7f, 0xFFB42318.toInt(), Paint.Align.CENTER, true, alpha)
            }
        }
        label("امضاء فروشنده", 142f, top + 72f, 5.6f, MUTED, Paint.Align.CENTER, true, 255)
        label("امضاء خریدار", 238f, top + 72f, 5.6f, MUTED, Paint.Align.CENTER, true, 255)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LINE; strokeWidth = 0.6f }
        canvas.drawLine(105f, top + 78f, 178f, top + 78f, linePaint)
        canvas.drawLine(201f, top + 78f, 274f, top + 78f, linePaint)
    }

    private fun drawQrMark(canvas: Canvas, paint: Paint, left: Float, top: Float, cell: Float) {
        paint.color = 0xFF1C1917.toInt()
        val pattern = arrayOf(
            "111010111", "101010101", "111110111", "000101000", "111011101",
            "101110001", "111011111", "001101001", "111001111"
        )
        pattern.forEachIndexed { y, line -> line.forEachIndexed { x, char ->
            if (char == '1') canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell, top + (y + 1) * cell, paint)
        } }
    }

    private fun loadPersistedBitmap(context: Context, uriValue: String): Bitmap? {
        if (uriValue.isBlank()) return null
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriValue))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    private class CachedPdfPrintAdapter(private val file: File) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?
        ) {
            if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build(), true
            )
        }

        override fun onWrite(
            pages: Array<out PageRange>, destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal, callback: WriteResultCallback
        ) {
            try {
                FileInputStream(file).use { input -> FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) } }
                if (cancellationSignal.isCanceled) callback.onWriteCancelled() else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (error: Exception) {
                callback.onWriteFailed(error.message)
            }
        }
    }
}

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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
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
    private const val INK = 0xFF172033.toInt()
    private const val MUTED = 0xFF667085.toInt()
    private const val LINE = 0xFFE6E1D7.toInt()
    private const val PAPER = 0xFFFFFEFA.toInt()
    private const val SOFT_GOLD = 0xFFFFF8E6.toInt()

    fun share(context: Context, invoice: BarterInvoice, settings: AppSettings): Boolean {
        val file = create(context, invoice, settings) ?: return false
        return share(context, file, invoice.invoiceNumber)
    }

    fun share(context: Context, file: File, invoiceNumber: String): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "فاکتور رسمی $invoiceNumber")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری فاکتور PDF"))
        true
    }.getOrDefault(false)

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
            val rowsPerPage = 4
            val chunks = model.rows.chunked(rowsPerPage).ifEmpty { listOf(emptyList()) }
            chunks.forEachIndexed { pageIndex, rows ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create())
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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val typography = PdfTypography(canvas, regularTypeface, boldTypeface)
        fun box(color: Int, rect: RectF, radius: Float = 0f) {
            paint.style = Paint.Style.FILL
            paint.color = color
            if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, paint) else canvas.drawRect(rect, paint)
        }
        fun outline(color: Int, rect: RectF, radius: Float = 0f, width: Float = 0.7f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = width
            paint.color = color
            if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, paint) else canvas.drawRect(rect, paint)
            paint.style = Paint.Style.FILL
        }

        box(PAPER, RectF(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat()))
        drawTopRibbon(canvas, paint)
        drawBrandHeader(canvas, paint, typography, model, settings, logo, ::box, ::outline)
        drawPartyCards(typography, model, settings, ::box, ::outline)
        drawRateStrip(typography, model, settings, ::box, ::outline)
        drawItemsTable(typography, model, rows, rowStartIndex, ::box, ::outline)
        if (isLastPage) {
            drawSummaryAndSignatures(canvas, typography, model, stamp, ::box, ::outline)
        } else {
            typography.draw("ادامه اقلام و جمع‌بندی مالی در صفحه بعد", PAGE_WIDTH / 2f, 298f, 6.2f, MUTED, Paint.Align.CENTER, true, 260f)
        }

        paint.color = LINE
        paint.strokeWidth = 0.7f
        canvas.drawLine(MARGIN, 402f, PAGE_WIDTH - MARGIN, 402f, paint)
        typography.draw("سند قیراط • ${model.trackingCode}", PAGE_WIDTH - MARGIN, 408f, 5.2f, MUTED, Paint.Align.RIGHT, maxWidth = 330f)
        typography.draw(
            "صفحه ${PersianNumberFormatter.toPersianDigits(pageNumber.toString())} از ${PersianNumberFormatter.toPersianDigits(pageCount.toString())}",
            MARGIN, 408f, 5.2f, MUTED, Paint.Align.LEFT, maxWidth = 100f
        )
    }

    private fun drawTopRibbon(canvas: Canvas, paint: Paint) {
        paint.shader = android.graphics.LinearGradient(
            MARGIN, 0f, PAGE_WIDTH - MARGIN, 0f,
            intArrayOf(GOLD, GOLD_LIGHT, GOLD), null, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(MARGIN, 10f, PAGE_WIDTH - MARGIN, 14f, 2f, 2f, paint)
        paint.shader = null
    }

    private fun drawBrandHeader(
        canvas: Canvas,
        paint: Paint,
        text: PdfTypography,
        model: OfficialInvoiceDocument,
        settings: AppSettings,
        logo: Bitmap?,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val header = RectF(MARGIN, 21f, PAGE_WIDTH - MARGIN, 69f)
        box(Color.WHITE, header, 8f)
        outline(LINE, header, 8f, 0.7f)
        val logoRect = RectF(527f, 29f, 558f, 60f)
        if (logo != null) {
            canvas.drawBitmap(logo, null, logoRect, paint)
        } else {
            box(SOFT_GOLD, logoRect, 8f)
            outline(0x66D4AF37, logoRect, 8f, 0.8f)
            text.draw(settings.galleryName.take(2), 542.5f, 40f, 8f, GOLD, Paint.Align.CENTER, true, 24f)
        }
        text.draw(settings.galleryName, 517f, 28f, 9.2f, INK, Paint.Align.RIGHT, true, 122f)
        text.draw("مدیر: ${settings.managerName}", 517f, 43f, 5.7f, MUTED, Paint.Align.RIGHT, maxWidth = 122f)
        text.draw("پروانه کسب: ${settings.unionCode}", 517f, 54f, 5.5f, GOLD, Paint.Align.RIGHT, true, 122f)

        box(0xFFFAF8F2.toInt(), RectF(207f, 28f, 388f, 62f), 7f)
        text.draw("فاکتور رسمی فروش طلا و جواهر", 297.5f, 31f, 8.2f, INK, Paint.Align.CENTER, true, 165f)
        text.draw("شماره ${model.invoiceNumber}", 297.5f, 46f, 6f, GOLD, Paint.Align.CENTER, true, 165f)
        text.draw("${model.issuedDate}  •  ${model.issuedTime}", 297.5f, 56f, 5.4f, MUTED, Paint.Align.CENTER, maxWidth = 165f)

        text.draw("شناسه پیگیری سند", 34f, 28f, 6f, INK, Paint.Align.LEFT, true, 148f)
        text.draw(model.trackingCode, 34f, 42f, 5.4f, GOLD, Paint.Align.LEFT, true, 148f)
        text.draw(settings.galleryPhone, 34f, 55f, 5.2f, MUTED, Paint.Align.LEFT, maxWidth = 148f)
    }

    private fun drawPartyCards(
        text: PdfTypography,
        model: OfficialInvoiceDocument,
        settings: AppSettings,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val seller = RectF(303f, 76f, 573f, 122f)
        val buyer = RectF(22f, 76f, 292f, 122f)
        listOf(seller, buyer).forEach { box(Color.WHITE, it, 7f); outline(LINE, it, 7f, 0.65f) }
        box(SOFT_GOLD, RectF(490f, 81f, 563f, 94f), 6f)
        text.draw("فروشنده", 526.5f, 83f, 5.6f, GOLD, Paint.Align.CENTER, true, 65f)
        text.draw(settings.galleryName, 563f, 98f, 6.8f, INK, Paint.Align.RIGHT, true, 165f)
        text.draw(settings.galleryPhone, 315f, 98f, 5.7f, MUTED, Paint.Align.LEFT, maxWidth = 90f)
        text.draw(settings.galleryAddress, 563f, 110f, 5.2f, MUTED, Paint.Align.RIGHT, maxWidth = 248f)

        box(0xFFF3F4F6.toInt(), RectF(209f, 81f, 282f, 94f), 6f)
        text.draw("خریدار", 245.5f, 83f, 5.6f, INK, Paint.Align.CENTER, true, 65f)
        text.draw(model.buyerName, 282f, 98f, 6.8f, INK, Paint.Align.RIGHT, true, 165f)
        text.draw(model.buyerPhone, 34f, 98f, 5.7f, MUTED, Paint.Align.LEFT, maxWidth = 95f)
        text.draw("شناسه: ${model.buyerNationalId}", 282f, 110f, 5.2f, MUTED, Paint.Align.RIGHT, maxWidth = 170f)
        text.draw(model.buyerRole, 34f, 110f, 5.2f, GOLD, Paint.Align.LEFT, true, 82f)
    }

    private fun drawRateStrip(
        text: PdfTypography,
        model: OfficialInvoiceDocument,
        settings: AppSettings,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val strip = RectF(MARGIN, 129f, PAGE_WIDTH - MARGIN, 151f)
        box(SOFT_GOLD, strip, 6f)
        outline(0x80E8C96A.toInt(), strip, 6f, 0.65f)
        text.draw("مبنای محاسبه", 558f, 134f, 5.6f, GOLD, Paint.Align.RIGHT, true, 80f)
        text.draw(
            "هر گرم طلای ۱۸ عیار: ${PersianNumberFormatter.formatPrice(model.spotPrice18k.toDouble())} تومان",
            468f, 134f, 6.2f, INK, Paint.Align.RIGHT, true, 205f
        )
        text.draw("روش تسویه: ${model.settlementLabel}", 35f, 134f, 5.7f, MUTED, Paint.Align.LEFT, maxWidth = 170f)
        box(0xFFFFF0B8.toInt(), RectF(207f, 134f, 270f, 146f), 6f)
        text.draw("پروانه ${settings.unionCode}", 238.5f, 135f, 4.9f, GOLD, Paint.Align.CENTER, true, 57f)
    }

    private fun drawItemsTable(
        text: PdfTypography,
        model: OfficialInvoiceDocument,
        rows: List<OfficialInvoiceRow>,
        rowStartIndex: Int,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val top = 158f
        val headerHeight = 22f
        val rowHeight = 25f
        // Physical left-to-right edges; logical columns are rendered right-to-left.
        val columns = floatArrayOf(22f, 111f, 170f, 223f, 270f, 319f, 366f, 525f, 573f)
        val headers = listOf("مبلغ (تومان)", "اجرت / سود", "خالص", "کسر", "ناخالص", "عیار", "شرح قلم", "ردیف")
        box(0xFFF0EEE8.toInt(), RectF(22f, top, 573f, top + headerHeight), 6f)
        outline(LINE, RectF(22f, top, 573f, top + headerHeight), 6f, 0.7f)
        headers.forEachIndexed { index, header ->
            text.draw(header, (columns[index] + columns[index + 1]) / 2f, top + 6f, 5.6f, INK, Paint.Align.CENTER, true, columns[index + 1] - columns[index] - 5f)
        }
        if (rows.isEmpty()) {
            outline(LINE, RectF(22f, top + headerHeight, 573f, top + headerHeight + rowHeight), 0f, 0.6f)
            text.draw("هنوز قلمی در این فاکتور ثبت نشده است", 297.5f, top + 31f, 6.2f, MUTED, Paint.Align.CENTER, maxWidth = 300f)
            return
        }

        rows.forEachIndexed { index, row ->
            val yTop = top + headerHeight + index * rowHeight
            val background = when {
                row.isReceived -> 0xFFFFF4F1.toInt()
                index % 2 == 0 -> Color.WHITE
                else -> 0xFFFBFAF7.toInt()
            }
            box(background, RectF(22f, yTop, 573f, yTop + rowHeight), 0f)
            outline(LINE, RectF(22f, yTop, 573f, yTop + rowHeight), 0f, 0.45f)
            columns.drop(1).dropLast(1).forEach { x -> outline(LINE, RectF(x, yTop, x + 0.35f, yTop + rowHeight), 0f, 0.35f) }

            val centerTop = yTop + 7f
            val prefix = if (row.isReceived) "−" else ""
            text.draw("$prefix${PersianNumberFormatter.formatPrice(row.amountTomans.toDouble())}", 105f, centerTop, 6f, INK, Paint.Align.RIGHT, true, 78f)
            text.draw("${row.wageLabel} / ${row.profitLabel}", 140.5f, centerTop, 4.9f, MUTED, Paint.Align.CENTER, maxWidth = 53f)
            text.draw(row.netWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 196.5f, centerTop, 5.8f, INK, Paint.Align.CENTER, true, 47f)
            text.draw(row.stoneWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 246.5f, centerTop, 5.6f, MUTED, Paint.Align.CENTER, maxWidth = 41f)
            text.draw(row.grossWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 294.5f, centerTop, 5.7f, INK, Paint.Align.CENTER, maxWidth = 43f)
            text.draw(row.karat?.let { PersianNumberFormatter.toPersianDigits(it.toString()) } ?: "—", 342.5f, centerTop, 5.8f, GOLD, Paint.Align.CENTER, true, 40f)
            val titleColor = if (row.isReceived) 0xFF9F3A2C.toInt() else INK
            val titlePrefix = if (row.isReceived) "دریافتی • " else ""
            text.draw(titlePrefix + row.title, 518f, yTop + 3f, 6.1f, titleColor, Paint.Align.RIGHT, true, 145f)
            text.draw(row.subtitle, 518f, yTop + 13f, 4.8f, MUTED, Paint.Align.RIGHT, maxWidth = 145f)
            text.draw(PersianNumberFormatter.toPersianDigits((rowStartIndex + index + 1).toString()), 549f, centerTop, 6f, MUTED, Paint.Align.CENTER, true, 40f)
        }
        if (model.rows.size > rows.size) text.draw("اقلام این صفحه", 565f, 285f, 4.8f, MUTED, Paint.Align.RIGHT, maxWidth = 80f)
    }

    private fun drawSummaryAndSignatures(
        canvas: Canvas,
        text: PdfTypography,
        model: OfficialInvoiceDocument,
        stamp: Bitmap?,
        box: (Int, RectF, Float) -> Unit,
        outline: (Int, RectF, Float, Float) -> Unit
    ) {
        val top = 289f
        val finance = RectF(303f, top, 573f, 394f)
        val approval = RectF(22f, top, 292f, 394f)
        box(Color.WHITE, finance, 8f); outline(LINE, finance, 8f, 0.7f)
        box(0xFFFBFAF7.toInt(), approval, 8f); outline(LINE, approval, 8f, 0.7f)
        text.draw("جمع‌بندی مالی", 560f, top + 7f, 6.8f, INK, Paint.Align.RIGHT, true, 120f)
        val summaryRows = listOf(
            "وزن ناخالص / خالص" to "${PersianNumberFormatter.formatWeight(model.totalGrossWeight)} / ${PersianNumberFormatter.formatWeight(model.totalNetWeight)} گرم",
            "ارزش طلای خام" to "${PersianNumberFormatter.formatPrice(model.totalRawGoldValue.toDouble())} تومان",
            "اجرت و سود" to "${PersianNumberFormatter.formatPrice(model.totalWageAndProfit.toDouble())} تومان",
            "مالیات اجرت و سود" to "${PersianNumberFormatter.formatPrice(model.totalTax.toDouble())} تومان",
            "اقلام دریافتی / تهاتر" to "${PersianNumberFormatter.formatPrice(model.totalReceived.toDouble())} تومان"
        )
        summaryRows.forEachIndexed { index, item ->
            val y = top + 22f + index * 9f
            text.draw(item.first, 560f, y, 5.2f, MUTED, Paint.Align.RIGHT, maxWidth = 105f)
            text.draw(item.second, 315f, y, 5.5f, INK, Paint.Align.LEFT, true, 135f)
        }
        box(0xFFFFF0B8.toInt(), RectF(309f, top + 72f, 567f, top + 97f), 6f)
        outline(0x99D4AF37.toInt(), RectF(309f, top + 72f, 567f, top + 97f), 6f, 0.7f)
        text.draw("مبلغ نهایی قابل پرداخت", 554f, top + 79f, 5.8f, INK, Paint.Align.RIGHT, true, 115f)
        text.draw("${PersianNumberFormatter.formatPrice(model.payableAmount.toDouble())} تومان", 320f, top + 78f, 8.2f, GOLD, Paint.Align.LEFT, true, 125f)

        text.draw("تأیید و امضا", 280f, top + 7f, 6.8f, INK, Paint.Align.RIGHT, true, 115f)
        text.draw("مالیات صرفاً بر اجرت ساخت و سود فروشنده محاسبه شده است.", 280f, top + 22f, 5.1f, MUTED, Paint.Align.RIGHT, maxWidth = 245f)
        if (model.note.isNotBlank()) text.draw("توضیحات: ${model.note}", 280f, top + 35f, 5f, GOLD, Paint.Align.RIGHT, true, 245f)
        val stampRect = RectF(34f, top + 49f, 78f, top + 91f)
        if (stamp != null) {
            canvas.drawBitmap(stamp, null, stampRect, Paint(Paint.ANTI_ALIAS_FLAG))
        } else {
            outline(LINE, stampRect, 22f, 0.8f)
            text.draw("مهر تجاری", 56f, top + 64f, 5f, MUTED, Paint.Align.CENTER, true, 34f)
        }
        text.draw("امضای فروشنده", 142f, top + 67f, 5.3f, MUTED, Paint.Align.CENTER, true, 70f)
        text.draw("امضای خریدار", 238f, top + 67f, 5.3f, MUTED, Paint.Align.CENTER, true, 70f)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LINE; strokeWidth = 0.6f }
        canvas.drawLine(105f, top + 82f, 178f, top + 82f, linePaint)
        canvas.drawLine(201f, top + 82f, 274f, top + 82f, linePaint)
    }

    private fun loadPersistedBitmap(context: Context, uriValue: String): Bitmap? {
        if (uriValue.isBlank()) return null
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriValue))?.use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

    /** A single width-bounded typography path for Persian shaping and bidi ordering. */
    private class PdfTypography(
        private val canvas: Canvas,
        private val regularTypeface: Typeface,
        private val boldTypeface: Typeface
    ) {
        private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)

        fun draw(
            value: String,
            x: Float,
            top: Float,
            size: Float,
            color: Int,
            align: Paint.Align,
            bold: Boolean = false,
            maxWidth: Float
        ) {
            val width = maxWidth.coerceAtLeast(1f).toInt()
            paint.apply {
                textSize = size
                this.color = color
                typeface = if (bold) boldTypeface else regularTypeface
            }
            val isRtlText = value.any { it in '\u0600'..'\u06FF' }
            val layoutAlignment = when (align) {
                Paint.Align.CENTER -> Layout.Alignment.ALIGN_CENTER
                Paint.Align.RIGHT -> if (isRtlText) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
                Paint.Align.LEFT -> if (isRtlText) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            }
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
                .setAlignment(layoutAlignment)
                .setTextDirection(if (isRtlText) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .setIncludePad(false)
                .setMaxLines(1)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(width)
                .build()
            val left = when (align) {
                Paint.Align.RIGHT -> x - width
                Paint.Align.CENTER -> x - width / 2f
                Paint.Align.LEFT -> x
            }
            canvas.save()
            canvas.translate(left, top)
            layout.draw(canvas)
            canvas.restore()
        }
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

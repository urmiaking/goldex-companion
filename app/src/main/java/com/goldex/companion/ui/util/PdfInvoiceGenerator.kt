package com.goldex.companion.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.goldex.companion.model.DetailedJewelryResult
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.InvoiceItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.data.AppSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfInvoiceGenerator {

    fun generateAndShareInvoice(
        context: Context,
        invoice: Invoice,
        sourceName: String = "اتحادیه طلا",
        settings: AppSettings = AppSettings()
    ) {
        val file = createInvoicePdf(context, invoice, sourceName, settings)
        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "فاکتور رسمی خرید طلا - قیراط")
                putExtra(Intent.EXTRA_TEXT, "فاکتور رسمی صادر شده توسط ${settings.galleryName} (سامانه هوشمند زرگری قیراط).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری یا چاپ فاکتور PDF"))
        }
    }

    fun generateAndShareInvoice(
        context: Context,
        uiState: CalculatorUiState,
        result: DetailedJewelryResult,
        settings: AppSettings = AppSettings()
    ) {
        val singleItem = InvoiceItem(
            title = uiState.itemTitleInput.ifBlank { "قطعه طلا ۱" },
            karat = uiState.selectedKarat,
            grossWeight = result.grossWeight,
            stoneWeight = result.stoneWeight,
            netWeight = result.netWeight,
            spotPrice = PersianNumberFormatter.parseToCleanLong(uiState.spotPriceInput) ?: 0L,
            wageType = uiState.wageType,
            wageInput = PersianNumberFormatter.parsePersianOrEnglish(uiState.wageInput) ?: 0.0,
            wageAmount = result.wageAmount,
            profitPercent = PersianNumberFormatter.parsePersianOrEnglish(uiState.profitPercentInput) ?: 0.0,
            profitAmount = result.profitAmount,
            taxPercent = PersianNumberFormatter.parsePersianOrEnglish(uiState.taxPercentInput) ?: 0.0,
            taxAmount = result.taxAmount,
            rawGoldValue = result.rawGoldValue,
            totalPayable = result.totalPayable,
            effectiveGramPrice = result.effectiveGramPrice
        )
        val invoice = Invoice(
            customer = uiState.selectedCustomer,
            items = listOf(singleItem)
        )
        generateAndShareInvoice(context, invoice, uiState.rates.source.labelFa, settings)
    }

    private fun createInvoicePdf(
        context: Context,
        invoice: Invoice,
        sourceName: String,
        settings: AppSettings = AppSettings()
    ): File? {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
        var page = doc.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun drawHeaderRibbon(c: Canvas, isFirstPage: Boolean, pageNum: Int) {
            paint.color = Color.WHITE
            c.drawRect(0f, 0f, 595f, 842f, paint)

            paint.color = Color.rgb(212, 175, 55) // Gold #D4AF37
            if (isFirstPage) {
                c.drawRect(0f, 0f, 595f, 68f, paint)

                paint.color = Color.rgb(17, 24, 39)
                paint.textSize = 16.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.CENTER
                c.drawText(settings.galleryName, 595f / 2f, 32f, paint)

                paint.color = Color.rgb(71, 85, 105)
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("فاکتور رسمی برآورد و معامله طلا و جواهر • ${settings.galleryLicense}", 595f / 2f, 47f, paint)
                c.drawText("تلفن: ${PersianNumberFormatter.toPersianDigits(settings.galleryPhone)} • نشانی: ${settings.galleryAddress}", 595f / 2f, 60f, paint)
            } else {
                c.drawRect(0f, 0f, 595f, 32f, paint)

                paint.color = Color.rgb(17, 24, 39)
                paint.textSize = 11.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.CENTER
                c.drawText("ادامه اقلام فاکتور رسمی زرگری #${PersianNumberFormatter.toPersianDigits(invoice.invoiceNumber)} (صفحه ${PersianNumberFormatter.toPersianDigits(pageNum.toString())})", 595f / 2f, 21f, paint)
            }
        }

        fun drawTableHeader(c: Canvas, yTop: Float) {
            paint.color = Color.rgb(248, 250, 252)
            c.drawRoundRect(40f, yTop, 555f, yTop + 26f, 6f, 6f, paint)

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.6f
            c.drawRoundRect(40f, yTop, 555f, yTop + 26f, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT

            c.drawText("ردیف", 545f, yTop + 17f, paint)
            c.drawText("شرح قطعه طلا", 515f, yTop + 17f, paint)
            c.drawText("عیار", 390f, yTop + 17f, paint)
            c.drawText("وزن کل", 345f, yTop + 17f, paint)
            c.drawText("وزن خالص", 295f, yTop + 17f, paint)
            c.drawText("اجرت و سود", 235f, yTop + 17f, paint)
            c.drawText("مبلغ ردیف (تومان)", 150f, yTop + 17f, paint)
        }

        var currentPage = 1
        drawHeaderRibbon(canvas, true, currentPage)

        // Metadata & Customer Info Cards (Side by Side in Distinct Rounded Panels)
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(invoice.createdAt))

        // Right Box: Invoice Meta Card
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(305f, 75f, 555f, 136f, 8f, 8f, paint)
        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.6f
        canvas.drawRoundRect(305f, 75f, 555f, 136f, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(184, 134, 11)
        canvas.drawText("اطلاعات رسمی فاکتور", 545f, 91f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawText("شماره فاکتور: #${PersianNumberFormatter.toPersianDigits(invoice.invoiceNumber)}", 545f, 106f, paint)
        canvas.drawText("تاریخ صدور: ${PersianNumberFormatter.toPersianDigits(dateStr)}", 545f, 120f, paint)
        canvas.drawText("مرجع مظنه: $sourceName", 545f, 133f, paint)

        // Left Box: Customer Info Card
        val custName = invoice.customer?.name ?: "مشتری عمومی (نقدی)"
        val custPhone = invoice.customer?.phone?.takeIf { it.isNotBlank() } ?: "ثبت نشده"
        val custId = invoice.customer?.nationalId?.takeIf { it.isNotBlank() } ?: "---"

        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(40f, 75f, 290f, 136f, 8f, 8f, paint)
        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.6f
        canvas.drawRoundRect(40f, 75f, 290f, 136f, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(184, 134, 11)
        canvas.drawText("مشخصات خریدار", 280f, 91f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawText("نام خریدار: $custName", 280f, 106f, paint)
        canvas.drawText("شماره تماس: ${PersianNumberFormatter.toPersianDigits(custPhone)}", 280f, 120f, paint)
        canvas.drawText("کد ملی / شناسه: ${PersianNumberFormatter.toPersianDigits(custId)}", 280f, 133f, paint)

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 144f, 555f, 144f, paint)

        // Table Header
        drawTableHeader(canvas, 152f)

        // Item Rows
        var y = 198f
        val lineSpacing = 22f

        invoice.items.forEachIndexed { index, item ->
            // If approaching bottom of page, start a new page
            if (y > 770f) {
                doc.finishPage(page)
                currentPage++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, currentPage).create())
                canvas = page.canvas
                drawHeaderRibbon(canvas, false, currentPage)
                drawTableHeader(canvas, 42f)
                y = 86f
            }

            paint.color = Color.rgb(30, 41, 59)
            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f

            // Row #
            canvas.drawText(PersianNumberFormatter.toPersianDigits((index + 1).toString()), 545f, y, paint)

            // Title with safe width truncation
            val maxTitleWidth = 115f
            var displayTitle = item.title
            if (paint.measureText(displayTitle) > maxTitleWidth) {
                while (displayTitle.length > 3 && paint.measureText("$displayTitle…") > maxTitleWidth) {
                    displayTitle = displayTitle.dropLast(1)
                }
                displayTitle = "$displayTitle…"
            }
            canvas.drawText(displayTitle, 515f, y, paint)

            // Karat
            canvas.drawText(item.karat.labelFa.split(" ").firstOrNull() ?: "۱۸ عیار", 390f, y, paint)
            // Gross weight
            canvas.drawText("${PersianNumberFormatter.formatWeight(item.grossWeight)} گ", 345f, y, paint)
            // Net weight
            canvas.drawText("${PersianNumberFormatter.formatWeight(item.netWeight)} گ", 295f, y, paint)

            // Wage & Profit
            val wageText = if (item.wageType == WageType.PERCENTAGE) {
                "${PersianNumberFormatter.formatPercent(item.wageInput)}٪"
            } else {
                "${PersianNumberFormatter.formatPrice(item.wageInput)} ت"
            }
            val profitText = "${PersianNumberFormatter.formatPercent(item.profitPercent)}٪"
            canvas.drawText("$wageText | $profitText", 235f, y, paint)

            // Row Total
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(184, 134, 11)
            canvas.drawText("${PersianNumberFormatter.formatPrice(item.totalPayable)} ت", 150f, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, y + 5f, 555f, y + 5f, paint)

            y += lineSpacing
        }

        // Check if summary fits on this page, else new page
        if (y + 220f > 820f) {
            doc.finishPage(page)
            currentPage++
            page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, currentPage).create())
            canvas = page.canvas
            drawHeaderRibbon(canvas, false, currentPage)
            y = 55f
        }

        // Summary Breakdown Box
        y += 8f
        fun drawSummaryRow(label: String, value: String, isBold: Boolean = false) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.color = if (isBold) Color.rgb(184, 134, 11) else Color.rgb(51, 65, 85)
            paint.textSize = 9f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, 535f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(value, 60f, y, paint)

            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(50f, y + 4f, 545f, y + 4f, paint)
            y += 18f
        }

        drawSummaryRow("مجموع وزن خالص طلای اقلام فاکتور:", "${PersianNumberFormatter.formatWeight(invoice.totalNetWeight)} گرم", isBold = true)
        drawSummaryRow("مجموع ارزش خام طلا:", "${PersianNumberFormatter.formatPrice(invoice.totalRawGoldValue)} تومان")
        drawSummaryRow("مجموع کل اجرت ساخت کارگاهی:", "${PersianNumberFormatter.formatPrice(invoice.totalWageAmount)} تومان")
        drawSummaryRow("مجموع کل سود مصوب فروشنده:", "${PersianNumberFormatter.formatPrice(invoice.totalProfitAmount)} تومان")
        drawSummaryRow("مجموع مالیات بر ارزش افزوده (قانونی):", "${PersianNumberFormatter.formatPrice(invoice.totalTaxAmount)} تومان")

        // Prominent Grand Total Box
        y += 8f
        paint.color = Color.rgb(254, 243, 199) // Light Amber Gold
        canvas.drawRoundRect(40f, y, 555f, y + 62f, 10f, 10f, paint)

        paint.color = Color.rgb(184, 134, 11)
        paint.strokeWidth = 1.2f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(40f, y, 555f, y + 62f, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 10.5f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("مبلغ کل نهایی قابل پرداخت فاکتور", 595f / 2f, y + 20f, paint)

        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(184, 134, 11)
        canvas.drawText("${PersianNumberFormatter.formatPrice(invoice.totalPayable)} تومان", 595f / 2f, y + 39f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("(${PersianWordsFormatter.toWords(invoice.totalPayable.toLong())})", 595f / 2f, y + 54f, paint)

        // Legal footnote
        y += 76f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 8f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(
            "طبق ماده ۲۶ قانون دائمی مالیات بر ارزش افزوده، اصل طلا از مالیات معاف بوده و ۹٪ صرفاً بر سود و اجرت ساخت محاسبه گردیده است.",
            595f / 2f,
            y,
            paint
        )

        // Signatures
        y += 38f
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9.5f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("مهر و امضاء واحد صنفی / فروشنده:", 520f, y, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("امضاء و اثر انگشت خریدار:", 70f, y, paint)

        doc.finishPage(page)

        // Save to cache directory
        return try {
            val invoiceDir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val invoiceFile = File(invoiceDir, "Qirat_Invoice_${invoice.invoiceNumber}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(invoiceFile)
            doc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            doc.close()
            invoiceFile
        } catch (e: Exception) {
            e.printStackTrace()
            doc.close()
            null
        }
    }
}

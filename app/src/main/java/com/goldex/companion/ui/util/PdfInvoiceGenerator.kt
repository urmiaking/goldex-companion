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
import com.goldex.companion.ui.calculator.CalculatorUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfInvoiceGenerator {

    fun generateAndShareInvoice(
        context: Context,
        invoice: Invoice,
        sourceName: String = "اتحادیه طلا"
    ) {
        val file = createInvoicePdf(context, invoice, sourceName)
        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "فاکتور رسمی خرید طلا - گلدکس پرو")
                putExtra(Intent.EXTRA_TEXT, "فاکتور رسمی صادر شده توسط سامانه معامله‌گران طلا (GoldEx Pro).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری یا چاپ فاکتور PDF"))
        }
    }

    fun generateAndShareInvoice(
        context: Context,
        uiState: CalculatorUiState,
        result: DetailedJewelryResult
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
        generateAndShareInvoice(context, invoice, uiState.rates.source.labelFa)
    }

    private fun createInvoicePdf(
        context: Context,
        invoice: Invoice,
        sourceName: String
    ): File? {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Gold Header Band
        paint.color = Color.rgb(212, 175, 55) // Gold #D4AF37
        canvas.drawRect(0f, 0f, 595f, 65f, paint)

        // Header Title
        paint.color = Color.rgb(17, 24, 39)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("فاکتور رسمی برآورد و معامله طلا و جواهر", 595f / 2f, 38f, paint)

        // Subtitle
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("سامانه هوشمند زرگری و معاملات طلا (GoldEx Pro)", 595f / 2f, 54f, paint)

        // Metadata & Customer Info Cards (Side by Side)
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(invoice.createdAt))

        // Right Box: Invoice Meta
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9.5f
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawText("شماره فاکتور: #${PersianNumberFormatter.toPersianDigits(invoice.invoiceNumber)}", 555f, 90f, paint)
        canvas.drawText("تاریخ و ساعت صدور: ${PersianNumberFormatter.toPersianDigits(dateStr)}", 555f, 106f, paint)
        canvas.drawText("مرجع مظنه اتحادیه: $sourceName", 555f, 122f, paint)

        // Left Box: Customer Info
        val custName = invoice.customer?.name ?: "مشتری عمومی (نقدی)"
        val custPhone = invoice.customer?.phone?.takeIf { it.isNotBlank() } ?: "ثبت نشده"
        val custId = invoice.customer?.nationalId?.takeIf { it.isNotBlank() } ?: "---"

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(17, 24, 39)
        canvas.drawText("مشخصات خریدار: $custName", 40f, 90f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawText("شماره تماس: ${PersianNumberFormatter.toPersianDigits(custPhone)}", 40f, 106f, paint)
        canvas.drawText("کد ملی / شناسه: ${PersianNumberFormatter.toPersianDigits(custId)}", 40f, 122f, paint)

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 138f, 555f, 138f, paint)

        // Table Header
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(40f, 150f, 555f, 178f, 6f, 6f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT

        canvas.drawText("ردیف", 545f, 168f, paint)
        canvas.drawText("شرح قطعه طلا", 515f, 168f, paint)
        canvas.drawText("عیار", 390f, 168f, paint)
        canvas.drawText("وزن کل", 345f, 168f, paint)
        canvas.drawText("وزن خالص", 295f, 168f, paint)
        canvas.drawText("اجرت و سود", 235f, 168f, paint)
        canvas.drawText("مبلغ ردیف (تومان)", 150f, 168f, paint)

        // Item Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        var y = 200f
        val lineSpacing = 24f

        invoice.items.forEachIndexed { index, item ->
            paint.color = Color.rgb(30, 41, 59)
            paint.textAlign = Paint.Align.RIGHT

            // Row #
            canvas.drawText(PersianNumberFormatter.toPersianDigits((index + 1).toString()), 545f, y, paint)
            // Title
            canvas.drawText(item.title, 515f, y, paint)
            // Karat
            canvas.drawText(item.karat.labelFa.split(" ").firstOrNull() ?: "۱۸ عیار", 390f, y, paint)
            // Gross weight
            canvas.drawText("${PersianNumberFormatter.formatWeight(item.grossWeight)} گ", 345f, y, paint)
            // Net weight
            canvas.drawText("${PersianNumberFormatter.formatWeight(item.netWeight)} گ", 295f, y, paint)
            // Wage & Profit
            val wageText = if (item.wageType.name == "PERCENTAGE") {
                "${PersianNumberFormatter.toPersianDigits(item.wageInput.toString())}٪"
            } else {
                "${PersianNumberFormatter.formatPrice(item.wageInput)} ت"
            }
            canvas.drawText("$wageText | ${PersianNumberFormatter.toPersianDigits(item.profitPercent.toString())}٪", 235f, y, paint)
            // Row Total
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(184, 134, 11)
            canvas.drawText("${PersianNumberFormatter.formatPrice(item.totalPayable)} ت", 150f, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, y + 6f, 555f, y + 6f, paint)

            y += lineSpacing
        }

        // Summary Breakdown Box
        y += 10f
        fun drawSummaryRow(label: String, value: String, isBold: Boolean = false) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.color = if (isBold) Color.rgb(184, 134, 11) else Color.rgb(51, 65, 85)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, 535f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(value, 60f, y, paint)

            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(50f, y + 5f, 545f, y + 5f, paint)
            y += 20f
        }

        drawSummaryRow("مجموع وزن خالص طلای اقلام فاکتور:", "${PersianNumberFormatter.formatWeight(invoice.totalNetWeight)} گرم", isBold = true)
        drawSummaryRow("مجموع ارزش خام طلا:", "${PersianNumberFormatter.formatPrice(invoice.totalRawGoldValue)} تومان")
        drawSummaryRow("مجموع کل اجرت ساخت کارگاهی:", "${PersianNumberFormatter.formatPrice(invoice.totalWageAmount)} تومان")
        drawSummaryRow("مجموع کل سود مصوب فروشنده:", "${PersianNumberFormatter.formatPrice(invoice.totalProfitAmount)} تومان")
        drawSummaryRow("مجموع مالیات بر ارزش افزوده (قانونی):", "${PersianNumberFormatter.formatPrice(invoice.totalTaxAmount)} تومان")

        // Prominent Grand Total Box
        y += 12f
        paint.color = Color.rgb(254, 243, 199) // Light Amber Gold
        canvas.drawRoundRect(40f, y, 555f, y + 66f, 10f, 10f, paint)

        paint.color = Color.rgb(184, 134, 11)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(40f, y, 555f, y + 66f, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 11.5f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("مبلغ کل نهایی قابل پرداخت فاکتور", 595f / 2f, y + 23f, paint)

        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(184, 134, 11)
        canvas.drawText("${PersianNumberFormatter.formatPrice(invoice.totalPayable)} تومان", 595f / 2f, y + 43f, paint)

        paint.textSize = 9.5f
        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("(${PersianWordsFormatter.toWords(invoice.totalPayable.toLong())} تومان)", 595f / 2f, y + 58f, paint)

        // Legal footnote
        y += 82f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 8.5f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(
            "طبق ماده ۲۶ قانون دائمی مالیات بر ارزش افزوده، اصل طلا از مالیات معاف بوده و ۹٪ صرفاً بر سود و اجرت ساخت محاسبه گردیده است.",
            595f / 2f,
            y,
            paint
        )

        // Signatures
        y += 45f
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
            val invoiceFile = File(invoiceDir, "GoldEx_Invoice_${System.currentTimeMillis()}.pdf")
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

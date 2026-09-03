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
        uiState: CalculatorUiState,
        result: DetailedJewelryResult
    ) {
        val file = createInvoicePdf(context, uiState, result)
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
                putExtra(Intent.EXTRA_TEXT, "فاکتور رسمی برآورد و خرید طلا صادر شده توسط اپلیکیشن همراه گلدکس.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری یا چاپ فاکتور PDF"))
        }
    }

    private fun createInvoicePdf(
        context: Context,
        uiState: CalculatorUiState,
        result: DetailedJewelryResult
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
        canvas.drawText("فاکتور رسمی برآورد و معامله طلا و جواهر", 595f / 2f, 40f, paint)

        // Subtitle
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("سامانه هوشمند تحلیل و محاسبات بازار طلا (GoldEx Pro)", 595f / 2f, 56f, paint)

        // Metadata box
        val now = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date())
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 10f
        paint.color = Color.rgb(55, 65, 81)
        canvas.drawText("تاریخ و ساعت صدور: ${PersianNumberFormatter.toPersianDigits(now)}", 555f, 95f, paint)
        canvas.drawText("شماره فاکتور: ${PersianNumberFormatter.toPersianDigits((100000..999999).random().toString())}", 555f, 112f, paint)
        canvas.drawText("منبع استعلام نرخ: ${uiState.rates.source.labelFa}", 555f, 129f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("مظنه خام هر گرم ۱۸: ${PersianNumberFormatter.formatPrice(uiState.spotPriceInput.toDoubleOrNull() ?: 0.0)} تومان", 40f, 95f, paint)
        canvas.drawText("عیار استاندارد قطعه: ${uiState.selectedKarat.labelFa}", 40f, 112f, paint)

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 145f, 555f, 145f, paint)

        // Table Header
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(40f, 160f, 555f, 190f, 8f, 8f, paint)

        paint.color = Color.rgb(17, 24, 39)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT

        canvas.drawText("شرح آیتم", 535f, 179f, paint)
        canvas.drawText("مقدار / نرخ", 340f, 179f, paint)
        canvas.drawText("مبلغ نهایی (تومان)", 160f, 179f, paint)

        // Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        var y = 220f
        val lineSpacing = 28f

        fun drawRow(label: String, detail: String, amount: String, isBold: Boolean = false) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.color = if (isBold) Color.rgb(184, 134, 11) else Color.rgb(30, 41, 59)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, 535f, y, paint)
            canvas.drawText(detail, 340f, y, paint)
            canvas.drawText(amount, 160f, y, paint)

            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)
            y += lineSpacing
        }

        drawRow("وزن کل ناخالص", "${PersianNumberFormatter.formatWeight(result.grossWeight)} گرم", "---")
        drawRow("کسر وزن نگین و سنگ", "${PersianNumberFormatter.formatWeight(result.stoneWeight)} گرم", "منفی")
        drawRow("وزن خالص طلای معامله‌شده", "${PersianNumberFormatter.formatWeight(result.netWeight)} گرم", "---", isBold = true)
        drawRow("ارزش خام طلا", "پایه روز", "${PersianNumberFormatter.formatPrice(result.rawGoldValue)} تومان")
        drawRow("اجرت ساخت کارگاهی", "${uiState.wageInput} ${if (uiState.wageType.name == "PERCENTAGE") "٪" else "تومان"}", "${PersianNumberFormatter.formatPrice(result.wageAmount)} تومان")
        drawRow("سود فروشنده", "${uiState.profitPercentInput} ٪", "${PersianNumberFormatter.formatPrice(result.profitAmount)} تومان")
        drawRow("مالیات ارزش‌افزوده قانونی", "${uiState.taxPercentInput} ٪ (بر سود و اجرت)", "${PersianNumberFormatter.formatPrice(result.taxAmount)} تومان")
        drawRow("قیمت تمام‌شده هر گرم", "فی میانگین", "${PersianNumberFormatter.formatPrice(result.effectiveGramPrice)} تومان", isBold = true)

        // Big Total Box
        y += 15f
        paint.color = Color.rgb(254, 243, 199) // Light Amber Gold
        canvas.drawRoundRect(40f, y, 555f, y + 70f, 12f, 12f, paint)

        paint.color = Color.rgb(184, 134, 11)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(40f, y, 555f, y + 70f, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("مبلغ کل قابل پرداخت فاکتور", 595f / 2f, y + 25f, paint)

        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(184, 134, 11)
        canvas.drawText("${PersianNumberFormatter.formatPrice(result.totalPayable)} تومان", 595f / 2f, y + 46f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("(${PersianWordsFormatter.toWords(result.totalPayable.toLong())} تومان)", 595f / 2f, y + 62f, paint)

        // Signatures
        y += 100f
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 10f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("امضاء و مهر فروشنده / واحد صنفی:", 520f, y, paint)

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

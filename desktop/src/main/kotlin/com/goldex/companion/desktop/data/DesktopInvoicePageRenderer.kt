package com.goldex.companion.desktop.data

import com.goldex.companion.data.AppSettings
import com.goldex.companion.domain.invoice.OfficialInvoiceDocument
import com.goldex.companion.domain.invoice.OfficialInvoiceRow
import com.goldex.companion.model.PersianNumberFormatter
import java.awt.*
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.geom.Line2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.text.AttributedString

/** AWT port of OfficialInvoicePdfGenerator's A5 artwork, in the same 595 x 420 coordinates.
 * Preview, export and the Windows print spooler all consume these exact pages.
 */
internal class DesktopInvoicePageRenderer(
    private val g: Graphics2D,
    private val regular: Font,
    private val bold: Font
) {
    private companion object {
        const val BASE_PAGE_WIDTH = 595f
        const val MARGIN = 22f
        const val GOLD = 0xFF9A7416.toInt()
        const val INK = 0xFF0F172A.toInt()
        const val MUTED = 0xFF64748B.toInt()
        const val LINE = 0xFFE2E8F0.toInt()
        const val SOFT_GOLD = 0xFFFFFBEB.toInt()
    }
    private data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float)
    private enum class Align { LEFT, CENTER, RIGHT }

    fun draw(model: OfficialInvoiceDocument, settings: AppSettings, rows: List<OfficialInvoiceRow>,
        pageIndex: Int, pageCount: Int, logo: BufferedImage?, signature: BufferedImage?, stamp: BufferedImage?) {
        g.scale(2.0, 2.0)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        box(0xFFFFFEFA.toInt(), Bounds(0f, 0f, 595f, 420f))
        g.paint = LinearGradientPaint(22f, 0f, 573f, 0f, floatArrayOf(0f, .5f, 1f),
            arrayOf(Color(GOLD), Color(0xF5D777), Color(GOLD)))
        g.fill(RoundRectangle2D.Float(22f, 10f, 551f, 4f, 4f, 4f))
        drawBrandHeader(model, settings, logo)
        drawPartyCards(model, settings)
        drawRateStrip(model, settings)
        drawItemsTable(model, rows, pageIndex * DesktopInvoiceDocuments.ROWS_PER_PAGE)
        if (pageIndex == pageCount - 1) drawSummaryAndSignatures(model, settings, signature, stamp)
        else text("ادامه اقلام و جمع‌بندی مالی در صفحه بعد", 297.5f, 298f, 6.2f, MUTED, Align.CENTER, true, 260f)
        line(22f, 402f, 573f, 402f)
        text("سند رسمی قیراط • \u200E${model.trackingCode}", 573f, 408f, 5.2f, MUTED, Align.RIGHT, maxWidth = 330f)
        text("صفحه ${pageIndex + 1} از $pageCount", 22f, 408f, 5.2f, MUTED, Align.LEFT, maxWidth = 100f)
    }

    private fun shape(rect: Bounds, radius: Float) = RoundRectangle2D.Float(
        rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top, radius * 2, radius * 2)
    private fun box(color: Int, rect: Bounds, radius: Float = 0f) {
        g.color = Color(color, true); g.fill(shape(rect, radius))
    }
    private fun outline(color: Int, rect: Bounds, radius: Float = 0f, width: Float = .7f) {
        g.color = Color(color, true); g.stroke = BasicStroke(width); g.draw(shape(rect, radius))
    }
    private fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        g.color = Color(LINE, true); g.stroke = BasicStroke(.7f); g.draw(Line2D.Float(x1, y1, x2, y2))
    }
    private fun image(bitmap: BufferedImage, rect: Bounds) {
        val scale = minOf((rect.right - rect.left) / bitmap.width, (rect.bottom - rect.top) / bitmap.height)
        val width = bitmap.width * scale; val height = bitmap.height * scale
        val transform = java.awt.geom.AffineTransform.getTranslateInstance(
            (rect.left + (rect.right - rect.left - width) / 2).toDouble(),
            (rect.top + (rect.bottom - rect.top - height) / 2).toDouble())
        transform.scale(scale.toDouble(), scale.toDouble())
        g.drawImage(bitmap, transform, null)
    }

    /** One bounded shaping path: preserves Vazirmatn size, bidi ordering and Android's ellipsis. */
    private fun text(value: String, x: Float, top: Float, size: Float, color: Int,
        align: Align, bold: Boolean = false, maxWidth: Float) {
        if (value.isBlank()) return
        val font = (if (bold) this.bold else regular).deriveFont(size)
        val localized = PersianNumberFormatter.toPersianDigits(value).replace(Regex("[\\r\\n]+"), " ")
        fun layout(content: String): TextLayout {
            val attributed = AttributedString(content)
            attributed.addAttribute(TextAttribute.FONT, font)
            attributed.addAttribute(TextAttribute.RUN_DIRECTION,
                if (localized.any { Character.getDirectionality(it) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC })
                    TextAttribute.RUN_DIRECTION_RTL else TextAttribute.RUN_DIRECTION_LTR)
            return TextLayout(attributed.iterator, g.fontRenderContext)
        }
        var shaped = layout(localized)
        if (shaped.advance > maxWidth) {
            val boundaries = java.text.BreakIterator.getCharacterInstance(java.util.Locale.ROOT).apply { setText(localized) }
            val ends = mutableListOf<Int>()
            var end = boundaries.first()
            while (end != java.text.BreakIterator.DONE) { ends.add(end); end = boundaries.next() }
            var low = 0; var high = ends.lastIndex
            while (low < high) {
                val middle = (low + high + 1) / 2
                if (layout(localized.substring(0, ends[middle]) + "…").advance <= maxWidth) low = middle else high = middle - 1
            }
            shaped = layout(localized.substring(0, ends[low]) + "…")
        }
        val left = when (align) { Align.RIGHT -> x - shaped.advance; Align.CENTER -> x - shaped.advance / 2; Align.LEFT -> x }
        g.color = Color(color, true)
        shaped.draw(g, left, top + shaped.ascent)
    }

    private fun drawBrandHeader(model: OfficialInvoiceDocument, settings: AppSettings, logo: BufferedImage?) {
        val header = Bounds(MARGIN, 21f, BASE_PAGE_WIDTH - MARGIN, 69f)
        box(0xFFFFFFFF.toInt(), header, 8f)
        outline(LINE, header, 8f, 0.7f)
        val logoRect = Bounds(527f, 29f, 558f, 60f)
        if (logo != null) {
            image(logo, logoRect)
        } else {
            box(SOFT_GOLD, logoRect, 8f)
            outline(0x66D4AF37, logoRect, 8f, 0.8f)
            text(settings.galleryName.take(2).ifBlank { "قی" }, 542.5f, 40f, 8f, GOLD, Align.CENTER, true, 24f)
        }
        text(settings.galleryName.ifBlank { "طلافروشی و جواهری" }, 517f, 28f, 9.2f, INK, Align.RIGHT, true, 122f)
        text("مدیر:\u200F ${settings.managerName.ifBlank { "متصدی واحد" }}", 517f, 43f, 5.7f, MUTED, Align.RIGHT, maxWidth = 122f)
        text("پروانه کسب:\u200F ${PersianNumberFormatter.toPersianDigits(settings.unionCode)}", 517f, 54f, 5.5f, GOLD, Align.RIGHT, true, 122f)

        box(0xFFFAF8F2.toInt(), Bounds(207f, 28f, 388f, 62f), 7f)
        outline(0x40E2E8F0.toInt(), Bounds(207f, 28f, 388f, 62f), 7f, 0.6f)
        text("فاکتور رسمی فروش طلا و جواهر", 297.5f, 31f, 8.2f, INK, Align.CENTER, true, 165f)
        text("شماره فاکتور:\u200F ${PersianNumberFormatter.toPersianDigits(model.invoiceNumber)}", 297.5f, 46f, 6f, GOLD, Align.CENTER, true, 165f)
        text("تاریخ:\u200F ${model.issuedDate}  •  ساعت\u200F ${model.issuedTime}", 297.5f, 56f, 5.2f, MUTED, Align.CENTER, maxWidth = 165f)

        text("شناسه پیگیری سند", 34f, 28f, 6f, INK, Align.LEFT, true, 148f)
        text("\u200E${model.trackingCode}", 34f, 42f, 5.4f, GOLD, Align.LEFT, true, 148f)
        text("تلفن:\u200F ${PersianNumberFormatter.toPersianDigits(settings.galleryPhone)}", 34f, 55f, 5.2f, MUTED, Align.LEFT, maxWidth = 148f)
    }

    private fun drawPartyCards(model: OfficialInvoiceDocument, settings: AppSettings) {
        val seller = Bounds(303f, 76f, 573f, 122f)
        val buyer = Bounds(22f, 76f, 292f, 122f)
        listOf(seller, buyer).forEach { box(0xFFFFFFFF.toInt(), it, 7f); outline(LINE, it, 7f, 0.65f) }
        box(SOFT_GOLD, Bounds(490f, 81f, 563f, 94f), 6f)
        text("فروشنده", 526.5f, 83f, 5.6f, GOLD, Align.CENTER, true, 65f)
        text(settings.galleryName.ifBlank { "واحد تجاری" }, 563f, 98f, 6.8f, INK, Align.RIGHT, true, 165f)
        text("تلفن:\u200F ${PersianNumberFormatter.toPersianDigits(settings.galleryPhone)}", 315f, 98f, 5.7f, MUTED, Align.LEFT, maxWidth = 110f)
        text("نشانی:\u200F ${settings.galleryAddress.ifBlank { "ثبت نشده" }}", 563f, 110f, 5.2f, MUTED, Align.RIGHT, maxWidth = 248f)

        box(0xFFF1F5F9.toInt(), Bounds(209f, 81f, 282f, 94f), 6f)
        text("خریدار", 245.5f, 83f, 5.6f, INK, Align.CENTER, true, 65f)
        text(model.buyerName, 282f, 98f, 6.8f, INK, Align.RIGHT, true, 165f)
        text("تلفن:\u200F ${model.buyerPhone}", 34f, 98f, 5.7f, MUTED, Align.LEFT, maxWidth = 110f)
        text("کد ملی:\u200F ${model.buyerNationalId}", 282f, 110f, 5.2f, MUTED, Align.RIGHT, maxWidth = 170f)
        text(model.buyerRole, 34f, 110f, 5.2f, GOLD, Align.LEFT, true, 82f)
    }

    private fun drawRateStrip(model: OfficialInvoiceDocument, settings: AppSettings) {
        val strip = Bounds(MARGIN, 129f, BASE_PAGE_WIDTH - MARGIN, 151f)
        box(SOFT_GOLD, strip, 6f)
        outline(0x80E8C96A.toInt(), strip, 6f, 0.65f)
        text("مبنای محاسبه", 558f, 134f, 5.6f, GOLD, Align.RIGHT, true, 80f)
        text(
            "نرخ مبنای هر گرم طلای ۱۸ عیار:\u200F ${PersianNumberFormatter.formatPrice(model.spotPrice18k)} تومان",
            468f, 134f, 6.2f, INK, Align.RIGHT, true, 220f
        )
        text("روش تسویه:\u200F ${model.settlementLabel}", 35f, 134f, 5.7f, MUTED, Align.LEFT, maxWidth = 170f)
        box(0xFFFFF0B8.toInt(), Bounds(207f, 134f, 270f, 146f), 6f)
        text(
            if (settings.unionCode.isNotBlank()) "پروانه\u200F ${PersianNumberFormatter.toPersianDigits(settings.unionCode)}" else "اتحادیه طلا",
            238.5f, 135f, 4.9f, GOLD, Align.CENTER, true, 57f
        )
    }

    private fun drawItemsTable(model: OfficialInvoiceDocument, rows: List<OfficialInvoiceRow>, rowStartIndex: Int) {
        val top = 158f
        val headerHeight = 22f
        val rowHeight = 25f
        // Physical left-to-right edges; logical columns are rendered right-to-left.
        val columns = floatArrayOf(22f, 111f, 170f, 223f, 270f, 319f, 366f, 525f, 573f)
        val headers = listOf("مبلغ (تومان)", "اجرت / سود", "خالص (گرم)", "کسر (گرم)", "ناخالص (گرم)", "عیار", "شرح قلم و مشخصات", "ردیف")
        box(0xFFF1F5F9.toInt(), Bounds(22f, top, 573f, top + headerHeight), 6f)
        outline(LINE, Bounds(22f, top, 573f, top + headerHeight), 6f, 0.7f)
        headers.forEachIndexed { index, header ->
            text(header, (columns[index] + columns[index + 1]) / 2f, top + 6f, 5.4f, INK, Align.CENTER, true, columns[index + 1] - columns[index] - 4f)
        }
        if (rows.isEmpty()) {
            outline(LINE, Bounds(22f, top + headerHeight, 573f, top + headerHeight + rowHeight), 0f, 0.6f)
            text("هنوز قلمی در این فاکتور ثبت نشده است", 297.5f, top + 31f, 6.2f, MUTED, Align.CENTER, maxWidth = 300f)
            return
        }

        rows.forEachIndexed { index, row ->
            val yTop = top + headerHeight + index * rowHeight
            val background = when {
                row.isReceived -> 0xFFFFF1F0.toInt()
                index % 2 == 0 -> 0xFFFFFFFF.toInt()
                else -> 0xFFFAFAFA.toInt()
            }
            box(background, Bounds(22f, yTop, 573f, yTop + rowHeight), 0f)
            outline(LINE, Bounds(22f, yTop, 573f, yTop + rowHeight), 0f, 0.45f)
            columns.drop(1).dropLast(1).forEach { x -> outline(LINE, Bounds(x, yTop, x + 0.35f, yTop + rowHeight), 0f, 0.35f) }

            val centerTop = yTop + 7f
            val formattedAmount = PersianNumberFormatter.formatPrice(row.amountTomans)
            val amountDisplay = if (row.isReceived) "($formattedAmount)" else formattedAmount
            val amountColor = if (row.isReceived) 0xFF9F3A2C.toInt() else INK
            text(amountDisplay, 105f, centerTop, 6f, amountColor, Align.RIGHT, true, 78f)

            val wageProfitText = if (row.wageLabel == "—" && row.profitLabel == "—") "—" else "${row.wageLabel} + ${row.profitLabel}"
            text(wageProfitText, 140.5f, centerTop, 4.9f, MUTED, Align.CENTER, maxWidth = 53f)

            text(row.netWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 196.5f, centerTop, 5.8f, INK, Align.CENTER, true, 47f)
            text(row.stoneWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 246.5f, centerTop, 5.6f, MUTED, Align.CENTER, maxWidth = 41f)
            text(row.grossWeight?.let(PersianNumberFormatter::formatWeight) ?: "—", 294.5f, centerTop, 5.7f, INK, Align.CENTER, maxWidth = 43f)
            text(row.karat?.let { PersianNumberFormatter.toPersianDigits(it.toString()) } ?: "—", 342.5f, centerTop, 5.8f, GOLD, Align.CENTER, true, 40f)

            val titleColor = if (row.isReceived) 0xFF9F3A2C.toInt() else INK
            val titlePrefix = if (row.isReceived) "دریافتی • " else ""
            text(titlePrefix + row.title, 518f, yTop + 3f, 6.1f, titleColor, Align.RIGHT, true, 145f)
            text(row.subtitle, 518f, yTop + 13f, 4.8f, MUTED, Align.RIGHT, maxWidth = 145f)
            text(PersianNumberFormatter.toPersianDigits((rowStartIndex + index + 1).toString()), 549f, centerTop, 6f, MUTED, Align.CENTER, true, 40f)
        }
        if (model.rows.size > rows.size) text("اقلام این صفحه", 565f, 282f, 4.8f, MUTED, Align.RIGHT, maxWidth = 80f)
    }

    private fun drawSummaryAndSignatures(model: OfficialInvoiceDocument, settings: AppSettings, signature: BufferedImage?, stamp: BufferedImage?) {
        val top = 289f
        val finance = Bounds(303f, top, 573f, 394f)
        val approval = Bounds(22f, top, 292f, 394f)
        box(0xFFFFFFFF.toInt(), finance, 8f); outline(LINE, finance, 8f, 0.7f)
        box(0xFFFAF9F6.toInt(), approval, 8f); outline(LINE, approval, 8f, 0.7f)

        // Financial summary on the visual right.
        text("صورت‌حساب و جمع‌بندی مالی", 560f, top + 7f, 6.8f, INK, Align.RIGHT, true, 150f)
        val summaryRows = listOf(
            "وزن ناخالص / خالص" to "${PersianNumberFormatter.formatWeight(model.totalGrossWeight)} / ${PersianNumberFormatter.formatWeight(model.totalNetWeight)} گرم",
            "ارزش طلای خام" to "${PersianNumberFormatter.formatPrice(model.totalRawGoldValue)} تومان",
            "مجموع اجرت و سود" to "${PersianNumberFormatter.formatPrice(model.totalWageAndProfit)} تومان",
            "مالیات ارزش افزوده (قانونی)" to "${PersianNumberFormatter.formatPrice(model.totalTax)} تومان"
        ) + (if (model.totalReceived > 0L) {
            listOf("اقلام تهاتری و دریافتی" to "${PersianNumberFormatter.formatPrice(model.totalReceived)}− تومان")
        } else emptyList())

        summaryRows.forEachIndexed { index, item ->
            val y = top + 20f + index * 9.5f
            text(item.first, 560f, y, 5.2f, MUTED, Align.RIGHT, maxWidth = 115f)
            text(item.second, 315f, y, 5.5f, INK, Align.LEFT, true, 135f)
        }

        // Final payable amount, matching the Android document.
        box(0xFFFFF0B8.toInt(), Bounds(309f, top + 72f, 567f, top + 97f), 6f)
        outline(0x99D4AF37.toInt(), Bounds(309f, top + 72f, 567f, top + 97f), 6f, 0.7f)
        text("مبلغ نهایی قابل پرداخت", 554f, top + 79f, 5.8f, INK, Align.RIGHT, true, 115f)
        text("${PersianNumberFormatter.formatPrice(model.payableAmount)} تومان", 320f, top + 78f, 8.2f, GOLD, Align.LEFT, true, 125f)

        // Approval and signatures on the visual left.
        text("تأیید اصالت و امضای طرفین", 280f, top + 7f, 6.8f, INK, Align.RIGHT, true, 120f)
        text(
            "این سند به منزله فاکتور رسمی معتبر بوده و مالیات صرفاً بر اجرت و سود محاسبه شده است.",
            280f, top + 21f, 4.9f, MUTED, Align.RIGHT, maxWidth = 250f
        )
        if (model.note.isNotBlank()) {
            text("توضیحات:\u200F ${model.note}", 280f, top + 32f, 5f, GOLD, Align.RIGHT, true, 250f)
        }

        // Buyer Signature Area (Right side of Left Box in RTL: 165f to 280f)
        text("امضای خریدار", 222f, top + 45f, 5.4f, INK, Align.CENTER, true, 90f)
        text(model.buyerName, 222f, top + 56f, 4.8f, MUTED, Align.CENTER, maxWidth = 90f)
        line(170f, top + 85f, 275f, top + 85f)

        // Seller Signature Area (Left side of Left Box in RTL: 35f to 155f)
        text("امضای متصدی / فروشنده", 95f, top + 45f, 5.4f, INK, Align.CENTER, true, 100f)
        text(settings.managerName.ifBlank { settings.galleryName }, 95f, top + 56f, 4.8f, MUTED, Align.CENTER, maxWidth = 100f)

        if (signature != null && signature.width > 0 && signature.height > 0) {
            val maxSigW = if (stamp == null) 80f else 50f
            val maxSigH = 30f
            val sigAspect = signature.width.toFloat() / signature.height.toFloat()
            val targetAspect = maxSigW / maxSigH
            val (sigW, sigH) = if (sigAspect > targetAspect) {
                maxSigW to (maxSigW / sigAspect)
            } else {
                (maxSigH * sigAspect) to maxSigH
            }
            val centerX = if (stamp == null) 95f else 116f
            val centerY = top + 70f
            val sigRect = Bounds(
                centerX - sigW / 2f,
                centerY - sigH / 2f,
                centerX + sigW / 2f,
                centerY + sigH / 2f
            )
            image(signature, sigRect)
        }
        if (stamp != null) image(stamp, Bounds(40f, top + 60f, 77f, top + 84f))
        line(40f, top + 85f, 150f, top + 85f)
    }

}

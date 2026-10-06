package com.goldex.companion.desktop.data

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Bounded, embedded JPEG makes backup independent of the selected source file. No network images. */
object InventoryPhoto {
    private const val PREFIX = "data:image/jpeg;base64,"
    fun read(path: Path): String {
        require(Files.size(path) in 1..20_000_000) { "تصویر باید کمتر از ۲۰ مگابایت باشد" }
        return encode(Files.readAllBytes(path))
    }
    fun encode(bytes: ByteArray): String {
        require(bytes.size in 1..20_000_000)
        val source = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            require(readers.hasNext()) { "تصویر قابل خواندن نیست" }
            val reader = readers.next()
            try {
                reader.input = stream
                require(reader.formatName.lowercase() in listOf("jpeg", "jpg", "png")) { "تصویر PNG یا JPEG انتخاب کنید" }
                require(reader.getWidth(0).toLong() * reader.getHeight(0) in 1..16_000_000) { "ابعاد تصویر بیش از حد بزرگ است" }
                reader.read(0)
            } finally { reader.dispose() }
        }
        val scale = minOf(1.0, 640.0 / maxOf(source.width, source.height))
        val image = BufferedImage((source.width * scale).roundToInt().coerceAtLeast(1), (source.height * scale).roundToInt().coerceAtLeast(1), BufferedImage.TYPE_INT_RGB)
        image.createGraphics().let { graphics ->
            try { graphics.color = Color.WHITE; graphics.fillRect(0, 0, image.width, image.height); graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); graphics.drawImage(source, 0, 0, image.width, image.height, null) }
            finally { graphics.dispose(); source.flush() }
        }
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "jpeg", output); image.flush()
        require(output.size() <= 220_000) { "تصویر کوچک‌تری انتخاب کنید" }
        return PREFIX + Base64.getEncoder().encodeToString(output.toByteArray())
    }
    fun bytes(value: String): ByteArray = Base64.getDecoder().decode(value.removePrefix(PREFIX))
    fun isValid(value: String): Boolean = runCatching {
        require(value.startsWith(PREFIX) && value.length <= 300_000)
        val bytes = bytes(value)
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val reader = ImageIO.getImageReaders(stream).asSequence().first()
            try { reader.input = stream; require(reader.formatName.equals("JPEG", true)); require(reader.getWidth(0) in 1..640 && reader.getHeight(0) in 1..640) }
            finally { reader.dispose() }
        }
    }.isSuccess
}

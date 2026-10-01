package com.goldex.companion.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BrandAssetBitmapTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun savedSignaturePngLoadsWithItsInkAndTransparency() {
        val file = writePng("existing signature.png", 180, 60)
        val legacyUri = file.toURI().toString()
        for (uri in listOf(legacyUri, legacyUri.replace("file:/", "file:///"))) {
            val loaded = loadSafeProfileBitmap(context, uri)
            assertNotNull("Existing signature must reach the invoice renderer", loaded)
            loaded!!
            assertEquals(180, loaded.width)
            assertEquals(60, loaded.height)
            assertEquals(Color.TRANSPARENT, loaded.getPixel(0, 0))
            assertEquals(Color.BLACK, loaded.getPixel(90, 30))
            loaded.recycle()
        }
    }

    @Test
    fun largeLogoIsBoundedAndCanBeLoadedFromALegacyAbsolutePath() {
        val file = writePng("large-logo.png", 1600, 800)
        val loaded = loadSafeProfileBitmap(context, file.absolutePath, maxDimension = 512)
        assertNotNull(loaded)
        loaded!!
        assertEquals(512, loaded.width)
        assertEquals(256, loaded.height)
        loaded.recycle()
    }

    @Test
    fun pickedLogoIsCopiedAndSurvivesRemovalOfTheSource() {
        val file = writePng("picked-logo.png", 180, 60)
        val pickedUri = Uri.parse("content://test.brand/picked-logo")
        shadowOf(context.contentResolver).registerInputStreamSupplier(pickedUri) { file.inputStream() }
        val storedUri = persistBrandAssetLocally(context, pickedUri, "test-logo")
        assertNotNull("A valid picked logo must be persisted", storedUri)
        assertTrue(file.delete())
        // Robolectric's Android file URI contains a Windows host path; emulate Android's file resolver.
        val storedFile = File(context.filesDir, "brand_assets/test-logo.png")
        shadowOf(context.contentResolver).registerInputStreamSupplier(Uri.parse(storedUri)) { storedFile.inputStream() }
        val loaded = loadSafeProfileBitmap(context, storedUri!!)
        assertNotNull("Logo must survive loss of the picker URI", loaded)
        loaded?.recycle()
    }

    @Test
    fun malformedOrMissingImagesRemainUnavailable() {
        val invalid = File(context.cacheDir, "invalid-image.png").apply { writeText("not an image") }
        assertNull(loadSafeProfileBitmap(context, Uri.fromFile(invalid).toString()))
        assertNull(loadSafeProfileBitmap(context, ""))
        assertNull(loadSafeProfileBitmap(context, "/missing-brand-image.png"))
    }

    @Test
    fun invalidLogoImportPreservesThePreviousStoredImage() {
        val original = writePng("original-logo.png", 180, 60)
        val stored = File(context.filesDir, "brand_assets/retained-logo.png")
        stored.parentFile!!.mkdirs()
        original.copyTo(stored, overwrite = true)
        val before = stored.readBytes()
        val invalid = File(context.cacheDir, "bad-logo.png").apply { writeText("not an image") }
        assertNull(persistBrandAssetLocally(context, Uri.parse(invalid.toURI().toString()), "retained-logo"))
        assertArrayEquals(before, stored.readBytes())
    }

    private fun writePng(name: String, width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(width / 2, height / 2, Color.BLACK)
        val file = File(context.cacheDir, name)
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return file
    }
}

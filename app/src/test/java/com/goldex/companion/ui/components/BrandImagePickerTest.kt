package com.goldex.companion.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34])
class BrandImagePickerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun standardDocumentPickerOpensWithoutTryingOtherProviders() {
        val opened = mutableListOf<Intent>()
        launchBrandImagePicker(context) { opened.add(it) }
        assertEquals(1, opened.size)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, opened.single().action)
        assertEquals("image/*", opened.single().type)
        assertTrue(opened.single().hasCategory(Intent.CATEGORY_OPENABLE))
        assertNull(opened.single().getStringArrayExtra(Intent.EXTRA_MIME_TYPES))
    }

    @Test
    fun unavailableDocumentPickerFallsBackToContentPicker() {
        val attempts = mutableListOf<String?>()
        launchBrandImagePicker(context) {
            attempts.add(it.action)
            if (it.action == Intent.ACTION_OPEN_DOCUMENT) throw ActivityNotFoundException()
        }
        assertEquals(listOf(Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_GET_CONTENT), attempts)
    }

    @Test
    fun blockedPickersFallBackToMimeOnlyOemGallery() {
        val opened = mutableListOf<Intent>()
        launchBrandImagePicker(context) {
            if (it.action != Intent.ACTION_PICK || it.data != null) throw SecurityException()
            opened.add(it)
        }
        assertEquals(Intent.ACTION_PICK, opened.single().action)
        assertNull(opened.single().data)
        assertEquals("image/*", opened.single().type)
    }

    @Test
    fun contentProviderWithoutOpenableCategoryCanStillOpen() {
        var opened: Intent? = null
        launchBrandImagePicker(context) {
            if (it.action != Intent.ACTION_GET_CONTENT || it.hasCategory(Intent.CATEGORY_OPENABLE)) {
                throw ActivityNotFoundException()
            }
            opened = it
        }
        assertEquals(Intent.ACTION_GET_CONTENT, opened!!.action)
        assertFalse(opened!!.hasCategory(Intent.CATEGORY_OPENABLE))
    }

    @Test(expected = ActivityNotFoundException::class)
    fun missingProvidersReportUnavailableOnlyAfterFallbacks() {
        launchBrandImagePicker(context) { throw ActivityNotFoundException() }
    }

    @Test
    fun registrationFailureIsNotMisreportedAsAMissingProvider() {
        var attempts = 0
        val failure = IllegalStateException("Launcher has not been initialized")
        try {
            launchBrandImagePicker(context) { attempts++; throw failure }
            fail("Registration failure must be preserved")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
            assertEquals(1, attempts)
        }
    }
}

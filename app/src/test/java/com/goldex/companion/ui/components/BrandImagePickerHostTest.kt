package com.goldex.companion.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34])
class BrandImagePickerHostTest {
    @Test
    fun fragmentHostLaunchesRegistryRequestAndReturnsTheSelectedLogo() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = controller.get()
        val picked = Uri.parse("content://test.brand/selected-logo")
        var received: Uri? = null
        val launcher = activity.activityResultRegistry.register(
            "brand-logo",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) received = result.data?.data
        }
        try {
            // Use the real registry and FragmentActivity, as the production Compose launcher does.
            launchBrandImagePicker(activity) { launcher.launch(it) }
            val request = shadowOf(activity).nextStartedActivityForResult
            assertNotNull("Logo picker must actually leave the activity", request)
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.intent.action)
            assertEquals("image/*", request.intent.type)
            assertTrue("Registry request codes must be accepted without 16-bit truncation", request.requestCode > 0xffff)
            assertTrue(activity.activityResultRegistry.dispatchResult(
                request.requestCode,
                Activity.RESULT_OK,
                Intent().setData(picked)
            ))
            assertEquals(picked, received)
        } finally {
            launcher.unregister()
            controller.pause().stop().destroy()
        }
    }
}

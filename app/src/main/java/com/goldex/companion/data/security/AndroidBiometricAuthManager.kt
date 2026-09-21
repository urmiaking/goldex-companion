package com.goldex.companion.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.goldex.companion.domain.security.BiometricAuthManager
import com.goldex.companion.domain.security.BiometricAuthResult
import com.goldex.companion.domain.security.BiometricStatus

/**
 * Android implementation of BiometricAuthManager using AndroidX BiometricPrompt.
 * Encapsulates all Android-specific biometric hardware APIs behind the domain interface.
 */
class AndroidBiometricAuthManager(
    private val context: Context,
    private val activityProvider: () -> FragmentActivity?
) : BiometricAuthManager {

    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    override fun checkBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    override fun authenticate(
        title: String,
        subtitle: String,
        cancelButtonText: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val activity = activityProvider()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onResult(BiometricAuthResult.Error(-1, "Activity is not available"))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(BiometricAuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onResult(BiometricAuthResult.Canceled)
                    } else {
                        onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(BiometricAuthResult.Failed)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(cancelButtonText)
            .setAllowedAuthenticators(authenticators)
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onResult(BiometricAuthResult.Error(-2, e.message ?: "Authentication failed to start"))
        }
    }
}

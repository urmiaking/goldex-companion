package com.goldex.companion.domain.security

/**
 * Platform-independent contract for biometric authentication.
 * Implementations are provided per platform (e.g., Android BiometricPrompt, iOS LocalAuthentication).
 */
interface BiometricAuthManager {
    fun checkBiometricStatus(): BiometricStatus

    val isBiometricAvailable: Boolean
        get() = checkBiometricStatus() == BiometricStatus.AVAILABLE

    fun authenticate(
        title: String,
        subtitle: String,
        cancelButtonText: String,
        onResult: (BiometricAuthResult) -> Unit
    )
}

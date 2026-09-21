package com.goldex.companion.domain.security

/**
 * Platform-independent outcome of a biometric authentication attempt.
 * Clean domain model for Kotlin Multiplatform (KMP).
 */
sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data class Error(val code: Int, val message: String) : BiometricAuthResult
    data object Failed : BiometricAuthResult
    data object Canceled : BiometricAuthResult
}

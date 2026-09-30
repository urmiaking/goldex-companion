package com.goldex.companion.domain.security

/**
 * Platform-independent representation of biometric hardware & enrollment status.
 * Clean domain model for Kotlin Multiplatform (KMP).
 */
enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

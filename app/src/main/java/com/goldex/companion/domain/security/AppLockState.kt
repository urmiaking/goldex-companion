package com.goldex.companion.domain.security

/**
 * State representing app lock status and any active authentication error message.
 */
data class AppLockState(
    val isLocked: Boolean = false,
    val authErrorMessage: String? = null
)

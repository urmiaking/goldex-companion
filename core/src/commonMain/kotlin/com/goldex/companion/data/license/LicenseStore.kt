package com.goldex.companion.data.license

import kotlinx.coroutines.flow.StateFlow

interface LicenseStore {
    val licenseInfo: StateFlow<LicenseInfo>
    fun getCachedInfo(): LicenseInfo
    suspend fun activateTrial(): LicenseResult
    suspend fun activateCode(code: String): LicenseResult
    suspend fun syncStatus(): LicenseResult
}

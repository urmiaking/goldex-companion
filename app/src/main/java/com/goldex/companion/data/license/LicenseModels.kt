package com.goldex.companion.data.license

enum class LicenseStatus(val titleFa: String) {
    NONE("غیرفعال"),
    TRIAL_ACTIVE("مهلت تست فعال"),
    TRIAL_EXPIRED("پایان مهلت تست"),
    LIFETIME("اشتراک دائمی"),
    REVOKED("مسدود شده")
}

data class LicenseInfo(
    val status: LicenseStatus = LicenseStatus.NONE,
    val licenseCode: String? = null,
    val token: String? = null,
    val expiresAt: String? = null,
    val remainingDays: Int = 0,
    val lastVerifiedAt: Long = 0L,
    val lastMessage: String? = null
) {
    /**
     * آیا برنامه مجاز به استفاده از امکانات حسابداری، فاکتور و دفتر معین است؟
     */
    val isLicensed: Boolean
        get() = status == LicenseStatus.LIFETIME || status == LicenseStatus.TRIAL_ACTIVE

    val isLifetime: Boolean
        get() = status == LicenseStatus.LIFETIME

    val isTrial: Boolean
        get() = status == LicenseStatus.TRIAL_ACTIVE

    val isExpired: Boolean
        get() = status == LicenseStatus.TRIAL_EXPIRED
}

sealed class LicenseResult {
    data class Success(val info: LicenseInfo, val message: String) : LicenseResult()
    data class Error(val message: String, val statusCode: Int? = null) : LicenseResult()
}

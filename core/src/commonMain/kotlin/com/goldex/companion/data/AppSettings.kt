package com.goldex.companion.data

import com.goldex.companion.model.WageType

data class AppSettings(
    val priceSource: PriceSource = PriceSource.TGJU,
    val defaultProfitPercent: String = "7",
    val defaultTaxPercent: String = "9",
    val defaultWageType: WageType = WageType.PERCENTAGE,
    val autoSyncRates: Boolean = true,
    val galleryName: String = "",
    val managerName: String = "",
    val unionCode: String = "",
    val galleryPhone: String = "",
    val galleryAddress: String = "",
    val galleryLicense: String = "",
    val invoiceLogoUri: String = "",
    val invoiceStampUri: String = "",
    val invoiceSignatureUri: String = "",
    val isBiometricLockEnabled: Boolean = false,
    val isBiometricTipDismissed: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
)

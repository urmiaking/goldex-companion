package com.goldex.companion.ui.wizard

import com.goldex.companion.model.Karat

enum class WizardStep(val stepNumber: Int, val titleFa: String) {
    INTRO(0, "معرفی امکانات"),
    PROFILE(1, "مشخصات"),
    FINANCIAL_DEFAULTS(2, "تنظیمات مالی"),
    INVENTORY(3, "موجودی اول"),
    COMPLETION(4, "تأیید نهایی")
}

data class WizardProfileState(
    val galleryName: String = "",
    val managerName: String = "",
    val unionCode: String = "",
    val phone: String = "",
    val address: String = ""
)

data class WizardFinancialState(
    val profitPercent: String = "7",
    val isVatEnabled: Boolean = true,
    val vatRate: String = "9",
    val baseKarat: Karat = Karat.K18
)

data class WizardInventoryState(
    val vitrinWeight: String = "",
    val vitrinOjrat: String = "",
    val meltWeight: String = "",
    val meltAyar: String = "750",
    val coinTamam: Int = 0,
    val coinNim: Int = 0,
    val coinRob: Int = 0,
    val coinQadim: Int = 0,
    val coinGerami: Int = 0,
    val cashTankhah: String = "",
    val bankBalances: String = ""
)

enum class WizardLicenseChoice {
    TRIAL,
    CODE
}

data class WizardLicenseState(
    val choice: WizardLicenseChoice = WizardLicenseChoice.TRIAL,
    val licenseCode: String = ""
)

data class WizardUiState(
    val currentStep: WizardStep = WizardStep.INTRO,
    val introSlideIndex: Int = 0,
    val profile: WizardProfileState = WizardProfileState(),
    val financial: WizardFinancialState = WizardFinancialState(),
    val inventory: WizardInventoryState = WizardInventoryState(),
    val license: WizardLicenseState = WizardLicenseState(),
    val isSaving: Boolean = false
)

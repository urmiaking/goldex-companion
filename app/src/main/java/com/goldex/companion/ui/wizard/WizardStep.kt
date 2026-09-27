package com.goldex.companion.ui.wizard

import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter

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
    val address: String = "",
    val logoUri: String = "",
    val stampUri: String = ""
)

data class WizardProfileErrors(
    val galleryNameError: String? = null,
    val managerNameError: String? = null,
    val unionCodeError: String? = null,
    val phoneError: String? = null,
    val addressError: String? = null
) {
    val hasErrors: Boolean
        get() = galleryNameError != null || managerNameError != null ||
                unionCodeError != null || phoneError != null || addressError != null

    val firstErrorMessage: String?
        get() = galleryNameError ?: managerNameError ?: unionCodeError ?: phoneError ?: addressError
}

fun validateWizardProfile(profile: WizardProfileState): WizardProfileErrors {
    return WizardProfileErrors(
        galleryNameError = if (profile.galleryName.trim().isBlank()) "وارد کردن نام تجاری طلافروشی / بنکداری اجباری است." else null,
        managerNameError = if (profile.managerName.trim().isBlank()) "وارد کردن نام مدیر مسئول اجباری است." else null,
        unionCodeError = if (profile.unionCode.trim().isBlank()) "وارد کردن شماره پروانه صنف طلا و جواهر اجباری است." else null,
        phoneError = if (profile.phone.trim().isBlank()) "وارد کردن شماره تماس واحد تجاری اجباری است." else null,
        addressError = if (profile.address.trim().isBlank()) "وارد کردن نشانی واحد صنفی اجباری است." else null
    )
}

data class WizardFinancialState(
    val profitPercent: String = "7",
    val isVatEnabled: Boolean = true,
    val vatRate: String = "9",
    val baseKarat: Karat = Karat.K18
)

data class WizardFinancialErrors(
    val profitError: String? = null,
    val vatError: String? = null
) {
    val hasErrors: Boolean
        get() = profitError != null || vatError != null

    val firstErrorMessage: String?
        get() = profitError ?: vatError
}

fun validateWizardFinancial(financial: WizardFinancialState): WizardFinancialErrors {
    val cleanProfit = PersianNumberFormatter.toEnglishDigits(financial.profitPercent)
    val profitVal = cleanProfit.toDoubleOrNull()
    val profitError = if (profitVal == null || profitVal <= 0.0) {
        "درصد سود معتبر وارد کنید (باید بزرگتر از صفر باشد)."
    } else null

    val vatError = if (financial.isVatEnabled) {
        val cleanVat = PersianNumberFormatter.toEnglishDigits(financial.vatRate)
        val vatVal = cleanVat.toDoubleOrNull()
        if (vatVal == null || vatVal < 0.0) {
            "درصد مالیات بر ارزش افزوده معتبر وارد کنید."
        } else null
    } else null

    return WizardFinancialErrors(
        profitError = profitError,
        vatError = vatError
    )
}

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

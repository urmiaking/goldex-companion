package com.goldex.companion.desktop.wizard

import com.goldex.companion.domain.onboarding.OpeningInventoryInput
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter

enum class DesktopWizardStep(val stepNumber: Int, val titleFa: String, val subtitleFa: String) {
    INTRO(1, "معرفی امکانات", "آشنایی با قابلیت‌های جامع زرگری قیراط"),
    PROFILE(2, "مشخصات واحد صنفی", "اطلاعات گالری جهت درج در اسناد و فاکتورها"),
    FINANCIAL(3, "تنظیمات مالی", "پیش‌فرض‌های سود، مالیات و عیار معاملات"),
    INVENTORY(4, "موجودی اول دوره", "ثبت اولیه ویترین، آبشده، سکه‌ها و صندوق"),
    COMPLETION(5, "تأیید و افتتاح دوره", "بررسی نهایی اسناد، فعال‌سازی و صدور سند افتتاحیه")
}

data class DesktopWizardProfileState(
    val galleryName: String = "",
    val managerName: String = "",
    val unionCode: String = "",
    val phone: String = "",
    val address: String = "",
    val logoUri: String = "",
    val signatureUri: String = ""
)

data class DesktopWizardProfileErrors(
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

fun validateDesktopWizardProfile(profile: DesktopWizardProfileState): DesktopWizardProfileErrors {
    return DesktopWizardProfileErrors(
        galleryNameError = if (profile.galleryName.trim().isBlank()) "وارد کردن نام تجاری طلافروشی / بنکداری الزامی است." else null,
        managerNameError = if (profile.managerName.trim().isBlank()) "وارد کردن نام مدیر مسئول الزامی است." else null,
        unionCodeError = if (profile.unionCode.trim().isBlank()) "وارد کردن شماره پروانه اتحادیه طلا و جواهر الزامی است." else null,
        phoneError = if (profile.phone.trim().isBlank()) "وارد کردن شماره تماس الزامی است." else null,
        addressError = if (profile.address.trim().isBlank()) "وارد کردن نشانی واحد صنفی الزامی است." else null
    )
}

data class DesktopWizardFinancialState(
    val profitPercent: String = "7",
    val isVatEnabled: Boolean = true,
    val vatRate: String = "9",
    val baseKarat: Karat = Karat.K18
)

data class DesktopWizardFinancialErrors(
    val profitError: String? = null,
    val vatError: String? = null
) {
    val hasErrors: Boolean
        get() = profitError != null || vatError != null

    val firstErrorMessage: String?
        get() = profitError ?: vatError
}

fun validateDesktopWizardFinancial(financial: DesktopWizardFinancialState): DesktopWizardFinancialErrors {
    val cleanProfit = PersianNumberFormatter.toEnglishDigits(financial.profitPercent)
    val profitVal = cleanProfit.toDoubleOrNull()
    val profitError = if (profitVal == null || profitVal <= 0.0 || profitVal > 100.0) {
        "درصد سود معتبر وارد کنید (عددی بین ۰ تا ۱۰۰)."
    } else null

    val vatError = if (financial.isVatEnabled) {
        val cleanVat = PersianNumberFormatter.toEnglishDigits(financial.vatRate)
        val vatVal = cleanVat.toDoubleOrNull()
        if (vatVal == null || vatVal < 0.0 || vatVal > 100.0) {
            "درصد مالیات بر ارزش افزوده معتبر وارد کنید (عددی بین ۰ تا ۱۰۰)."
        } else null
    } else null

    return DesktopWizardFinancialErrors(
        profitError = profitError,
        vatError = vatError
    )
}

enum class DesktopWizardLicenseChoice(val titleFa: String) {
    TRIAL("فعال‌سازی دوره آزمایشی رایگان"),
    CODE("ورود کد لایسنس تجاری")
}

data class DesktopWizardLicenseState(
    val choice: DesktopWizardLicenseChoice = DesktopWizardLicenseChoice.TRIAL,
    val licenseCode: String = ""
)

typealias DesktopWizardInventoryState = OpeningInventoryInput

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
    val galleryName: String = "جواهری و بنکداری آریا",
    val managerName: String = "حاج احمد کاظمی",
    val unionCode: String = "۴۴۰۲",
    val phone: String = "۰۲۱-۵۵۶۲۳۴۸۱",
    val address: String = "بازار بزرگ تهران، سرای اردیبهشت، پلاک ۴۲"
)

data class WizardFinancialState(
    val profitPercent: String = "7",
    val isVatEnabled: Boolean = true,
    val vatRate: String = "10",
    val baseKarat: Karat = Karat.K18
)

data class WizardInventoryState(
    val vitrinWeight: String = "۱۴۵۰.۲۵",
    val vitrinOjrat: String = "۱۱.۵",
    val meltWeight: String = "۸۵۰.۰۰",
    val meltAyar: String = "۷۵۰",
    val coinTamam: Int = 10,
    val coinNim: Int = 5,
    val coinRob: Int = 12,
    val coinQadim: Int = 4,
    val coinGerami: Int = 8,
    val cashTankhah: String = "۱۸۵۰۰۰۰۰۰",
    val bankBalances: String = "۱۴۲۰۰۰۰۰۰۰"
)

data class WizardUiState(
    val currentStep: WizardStep = WizardStep.INTRO,
    val introSlideIndex: Int = 0,
    val profile: WizardProfileState = WizardProfileState(),
    val financial: WizardFinancialState = WizardFinancialState(),
    val inventory: WizardInventoryState = WizardInventoryState(),
    val isSaving: Boolean = false
)

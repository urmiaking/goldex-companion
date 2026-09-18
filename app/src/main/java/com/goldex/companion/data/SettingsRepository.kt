package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.WageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val priceSource: PriceSource = PriceSource.TGJU,
    val defaultProfitPercent: String = "7",
    val defaultTaxPercent: String = "9",
    val defaultWageType: WageType = WageType.PERCENTAGE,
    val autoSyncRates: Boolean = true,
    val galleryName: String = "جواهری و بنکداری آریا",
    val managerName: String = "حاج احمد کاظمی",
    val unionCode: String = "۴۴۰۲",
    val galleryPhone: String = "۰۲۱-۵۵۶۲۳۴۸۱",
    val galleryAddress: String = "بازار بزرگ تهران، سرای اردیبهشت، پلاک ۴۲",
    val galleryLicense: String = "صنف طلا و جواهر: ۴۴۰۲",
    val invoiceLogoUri: String = "",
    val invoiceStampUri: String = "",
    val isBiometricLockEnabled: Boolean = true,
    val hasCompletedOnboarding: Boolean = false
)

class SettingsRepository(context: Context) : SettingsStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qirat_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun loadSettings(): AppSettings {
        val sourceStr = prefs.getString("key_price_source", PriceSource.TGJU.name) ?: PriceSource.TGJU.name
        val priceSource = try {
            PriceSource.valueOf(sourceStr)
        } catch (_: Exception) {
            PriceSource.TGJU
        }

        val wageTypeStr = prefs.getString("key_default_wage_type", WageType.PERCENTAGE.name) ?: WageType.PERCENTAGE.name
        val defaultWageType = try {
            WageType.valueOf(wageTypeStr)
        } catch (_: Exception) {
            WageType.PERCENTAGE
        }

        return AppSettings(
            priceSource = priceSource,
            defaultProfitPercent = prefs.getString("key_profit_pct", "7") ?: "7",
            defaultTaxPercent = prefs.getString("key_tax_pct", "9") ?: "9",
            defaultWageType = defaultWageType,
            autoSyncRates = prefs.getBoolean("key_auto_sync", true),
            galleryName = prefs.getString("key_gallery_name", "جواهری و بنکداری آریا") ?: "جواهری و بنکداری آریا",
            managerName = prefs.getString("key_manager_name", "حاج احمد کاظمی") ?: "حاج احمد کاظمی",
            unionCode = prefs.getString("key_union_code", "۴۴۰۲") ?: "۴۴۰۲",
            galleryPhone = prefs.getString("key_gallery_phone", "۰۲۱-۵۵۶۲۳۴۸۱") ?: "۰۲۱-۵۵۶۲۳۴۸۱",
            galleryAddress = prefs.getString("key_gallery_address", "بازار بزرگ تهران، سرای اردیبهشت، پلاک ۴۲") ?: "بازار بزرگ تهران، سرای اردیبهشت، پلاک ۴۲",
            galleryLicense = prefs.getString("key_gallery_license", "صنف طلا و جواهر: ۴۴۰۲") ?: "صنف طلا و جواهر: ۴۴۰۲",
            invoiceLogoUri = prefs.getString("key_invoice_logo_uri", "") ?: "",
            invoiceStampUri = prefs.getString("key_invoice_stamp_uri", "") ?: "",
            isBiometricLockEnabled = prefs.getBoolean("key_biometric_lock", true),
            hasCompletedOnboarding = prefs.getBoolean("key_has_completed_onboarding", false)
        )
    }

    override fun saveSettings(newSettings: AppSettings) {
        prefs.edit()
            .putString("key_price_source", newSettings.priceSource.name)
            .putString("key_profit_pct", newSettings.defaultProfitPercent)
            .putString("key_tax_pct", newSettings.defaultTaxPercent)
            .putString("key_default_wage_type", newSettings.defaultWageType.name)
            .putBoolean("key_auto_sync", newSettings.autoSyncRates)
            .putString("key_gallery_name", newSettings.galleryName)
            .putString("key_manager_name", newSettings.managerName)
            .putString("key_union_code", newSettings.unionCode)
            .putString("key_gallery_phone", newSettings.galleryPhone)
            .putString("key_gallery_address", newSettings.galleryAddress)
            .putString("key_gallery_license", newSettings.galleryLicense)
            .putString("key_invoice_logo_uri", newSettings.invoiceLogoUri)
            .putString("key_invoice_stamp_uri", newSettings.invoiceStampUri)
            .putBoolean("key_biometric_lock", newSettings.isBiometricLockEnabled)
            .putBoolean("key_has_completed_onboarding", newSettings.hasCompletedOnboarding)
            .apply()

        _settings.value = newSettings
    }

    override fun loadDarkTheme(): Boolean = prefs.getBoolean("key_dark_theme", false)

    override fun saveDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("key_dark_theme", enabled).apply()
    }
}

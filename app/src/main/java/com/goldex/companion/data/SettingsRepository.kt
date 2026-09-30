package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.WageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



class SettingsRepository(context: Context) : SettingsStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("qirat_settings_prefs", Context.MODE_PRIVATE)

    private val appContext = context.applicationContext
    private val database = com.goldex.companion.data.local.db.GoldexDatabaseProvider.getDatabase(appContext)
    private val syncUnit = com.goldex.companion.data.local.db.GoldexDatabaseProvider.getSyncUnit(appContext)
    private val businessDao = database.syncDao()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val _settings = MutableStateFlow(loadSettingsInternal())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        if (businessDao.business() == null) database.runInTransaction {
            if (businessDao.business() == null) {
                businessDao.business(com.goldex.companion.data.sync.BusinessSettings(payload = com.goldex.companion.data.sync.SyncJson.business(_settings.value).toString()))
                businessDao.asset(com.goldex.companion.data.sync.AssetMetadata("logo", _settings.value.invoiceLogoUri))
                businessDao.asset(com.goldex.companion.data.sync.AssetMetadata("stamp", _settings.value.invoiceStampUri))
            }
        }
        scope.launch {
            businessDao.observeBusiness().collect { _settings.value = loadSettingsInternal() }
        }
    }

    private fun loadSettingsInternal(): AppSettings {
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

        val local = AppSettings(
            priceSource = priceSource,
            defaultProfitPercent = prefs.getString("key_profit_pct", "7") ?: "7",
            defaultTaxPercent = prefs.getString("key_tax_pct", "9") ?: "9",
            defaultWageType = defaultWageType,
            autoSyncRates = prefs.getBoolean("key_auto_sync", true),
            galleryName = prefs.getString("key_gallery_name", "") ?: "",
            managerName = prefs.getString("key_manager_name", "") ?: "",
            unionCode = prefs.getString("key_union_code", "") ?: "",
            galleryPhone = prefs.getString("key_gallery_phone", "") ?: "",
            galleryAddress = prefs.getString("key_gallery_address", "") ?: "",
            galleryLicense = prefs.getString("key_gallery_license", "") ?: "",
            invoiceLogoUri = prefs.getString("key_invoice_logo_uri", "") ?: "",
            invoiceStampUri = prefs.getString("key_invoice_stamp_uri", "") ?: "",
            invoiceSignatureUri = prefs.getString("key_invoice_signature_uri", "") ?: "",
            isBiometricLockEnabled = prefs.getBoolean("key_biometric_lock", false),
            isBiometricTipDismissed = prefs.getBoolean("key_biometric_tip_dismissed", false),
            hasCompletedOnboarding = prefs.getBoolean("key_has_completed_onboarding", false) || businessDao.marker("onboarding-complete") != null
        )
        return businessDao.business()?.let {
            val json = org.json.JSONObject(it.payload)
            fun assetUri(name: String, fallback: String): String {
                val asset = businessDao.asset(name)
                return when {
                    asset != null && asset.localUri.isNotBlank() -> asset.localUri
                    fallback.isNotBlank() -> fallback
                    else -> ""
                }
            }
            com.goldex.companion.data.sync.SyncJson.applyBusiness(local, json).copy(
                invoiceLogoUri = assetUri("logo", local.invoiceLogoUri),
                invoiceStampUri = assetUri("stamp", local.invoiceStampUri),
                invoiceSignatureUri = assetUri("signature", local.invoiceSignatureUri)
            )
        } ?: local
    }

    override fun loadSettings(): AppSettings {
        val loaded = loadSettingsInternal()
        _settings.value = loaded
        return loaded
    }

    override fun saveSettings(newSettings: AppSettings) {
        synchronized(this) {
            syncUnit.transaction {
                val payload = com.goldex.companion.data.sync.SyncJson.business(newSettings)
                for ((name, uri) in listOf(
                    "logo" to newSettings.invoiceLogoUri,
                    "stamp" to newSettings.invoiceStampUri,
                    "signature" to newSettings.invoiceSignatureUri
                )) {
                    val old = businessDao.asset(name)
                    if (old != null && old.localUri == uri) {
                        if (old.remoteId.isNotBlank()) {
                            val assetKey = when (name) {
                                "logo" -> "logoAssetId"
                                "stamp" -> "stampAssetId"
                                else -> "signatureAssetId"
                            }
                            payload.put(assetKey, old.remoteId)
                        }
                    } else businessDao.asset(com.goldex.companion.data.sync.AssetMetadata(name, uri))
                }
                businessDao.business(com.goldex.companion.data.sync.BusinessSettings(payload = payload.toString()))
                syncUnit.changed("businessSettings", "business", payload)
            }
            prefs.edit()
                .putString("key_invoice_logo_uri", newSettings.invoiceLogoUri)
                .putString("key_invoice_stamp_uri", newSettings.invoiceStampUri)
                .putString("key_invoice_signature_uri", newSettings.invoiceSignatureUri)
                .apply()
            _settings.value = loadSettingsInternal()
        }
    }

    override fun setBiometricLockEnabled(enabled: Boolean) {
        synchronized(this) {
            prefs.edit().putBoolean("key_biometric_lock", enabled).apply()
            _settings.update { it.copy(isBiometricLockEnabled = enabled) }
        }
    }

    override fun setBiometricTipDismissed(dismissed: Boolean) {
        synchronized(this) {
            prefs.edit().putBoolean("key_biometric_tip_dismissed", dismissed).apply()
            _settings.update { it.copy(isBiometricTipDismissed = dismissed) }
        }
    }

    override fun setHasCompletedOnboarding(completed: Boolean) {
        synchronized(this) {
            prefs.edit().putBoolean("key_has_completed_onboarding", completed).apply()
            _settings.update { it.copy(hasCompletedOnboarding = completed) }
        }
    }

    override fun loadDarkTheme(): Boolean = prefs.getBoolean("key_dark_theme", false)

    override fun saveDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("key_dark_theme", enabled).apply()
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

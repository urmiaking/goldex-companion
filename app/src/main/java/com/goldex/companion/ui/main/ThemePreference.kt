package com.goldex.companion.ui.main

import com.goldex.companion.data.SettingsStore

/** Keeps the app-shell theme preference independent from financial/profile settings. */
class ThemePreference(
    private val settingsStore: SettingsStore
) {
    fun load(): Boolean = settingsStore.loadDarkTheme()

    fun toggle(current: Boolean): Boolean {
        val updated = !current
        settingsStore.saveDarkTheme(updated)
        return updated
    }
}

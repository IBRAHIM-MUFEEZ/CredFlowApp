package com.radafiq.data.settings

import android.content.Context
import com.radafiq.data.models.IndianAccountCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(
    val label: String
) {
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromStorage(value: String?): AppThemeMode {
            return values().firstOrNull { it.name == value } ?: DARK
        }
    }
}

data class AppSettingsState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val selectedAccountIds: Set<String> = IndianAccountCatalog.defaultSelectedAccountIds()
)

class AppSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()

    fun setThemeMode(themeMode: AppThemeMode) {
        persist(_settings.value.copy(themeMode = themeMode))
    }

    fun setAccountSelected(
        accountId: String,
        isSelected: Boolean
    ) {
        val updatedIds = _settings.value.selectedAccountIds.toMutableSet()

        if (isSelected) {
            updatedIds.add(accountId)
        } else if (updatedIds.size > 1) {
            updatedIds.remove(accountId)
        }

        persist(
            _settings.value.copy(
                selectedAccountIds = IndianAccountCatalog.sanitizeSelectedAccountIds(updatedIds)
            )
        )
    }

    fun exportSettings(): Map<String, Any> {
        return mapOf(
            KEY_THEME_MODE to _settings.value.themeMode.name,
            KEY_SELECTED_ACCOUNT_IDS to _settings.value.selectedAccountIds.toList()
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreSettings(data: Map<String, Any?>) {
        val themeMode = AppThemeMode.fromStorage(data[KEY_THEME_MODE] as? String)
        val selectedAccountIds = when (val rawIds = data[KEY_SELECTED_ACCOUNT_IDS]) {
            is List<*> -> rawIds.filterIsInstance<String>().toSet()
            else -> emptySet()
        }

        persist(
            AppSettingsState(
                themeMode = themeMode,
                selectedAccountIds = if (selectedAccountIds.isEmpty()) {
                    IndianAccountCatalog.defaultSelectedAccountIds()
                } else {
                    IndianAccountCatalog.sanitizeSelectedAccountIds(selectedAccountIds)
                }
            )
        )
    }

    private fun loadSettings(): AppSettingsState {
        val storedThemeMode = AppThemeMode.fromStorage(
            preferences.getString(KEY_THEME_MODE, null)
        )
        val storedAccountIds = preferences.getStringSet(KEY_SELECTED_ACCOUNT_IDS, null)
            ?.toSet()
            .orEmpty()

        return AppSettingsState(
            themeMode = storedThemeMode,
            selectedAccountIds = if (storedAccountIds.isEmpty()) {
                IndianAccountCatalog.defaultSelectedAccountIds()
            } else {
                IndianAccountCatalog.sanitizeSelectedAccountIds(storedAccountIds)
            }
        )
    }

    private fun persist(settings: AppSettingsState) {
        preferences.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putStringSet(KEY_SELECTED_ACCOUNT_IDS, settings.selectedAccountIds.toSet())
            .apply()
        _settings.value = settings
    }

    private companion object {
        const val PREFERENCES_NAME = "radafiq_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SELECTED_ACCOUNT_IDS = "selected_account_ids"
    }
}

package com.example.data

import android.content.Context
import com.example.AppConstants
import com.example.DarkModeOption

class SharedPreferencesRepository(context: Context) : PreferencesRepository {

    private val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    override fun getDarkModeOption(): DarkModeOption {
        return try {
            DarkModeOption.valueOf(
                prefs.getString(AppConstants.DARK_MODE_OPTION_KEY, DarkModeOption.SYSTEM.name)
                    ?: DarkModeOption.SYSTEM.name
            )
        } catch (e: Exception) {
            val oldBool = prefs.getBoolean(AppConstants.DARK_MODE_KEY, false)
            prefs.edit().remove(AppConstants.DARK_MODE_KEY).apply()
            if (oldBool) DarkModeOption.ON else DarkModeOption.SYSTEM
        }
    }

    override fun setDarkModeOption(option: DarkModeOption) {
        prefs.edit().putString(AppConstants.DARK_MODE_OPTION_KEY, option.name).apply()
    }

    override fun getIsDynamicColor(): Boolean {
        return prefs.getBoolean(AppConstants.DYNAMIC_COLORS_KEY, true)
    }

    override fun setIsDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.DYNAMIC_COLORS_KEY, enabled).apply()
    }

    override fun getLanguage(): String {
        return prefs.getString(AppConstants.LANGUAGE_KEY, "") ?: ""
    }

    override fun setLanguage(locale: String) {
        prefs.edit().putString(AppConstants.LANGUAGE_KEY, locale).apply()
    }
}

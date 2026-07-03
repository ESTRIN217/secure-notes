package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.AppConstants
import com.example.DarkModeOption
import kotlinx.coroutines.flow.MutableStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    val darkModeOption = MutableStateFlow(
        try {
            DarkModeOption.valueOf(
                sharedPrefs.getString(AppConstants.DARK_MODE_OPTION_KEY, DarkModeOption.SYSTEM.name)
                    ?: DarkModeOption.SYSTEM.name
            )
        } catch (e: Exception) {
            val oldBool = sharedPrefs.getBoolean(AppConstants.DARK_MODE_KEY, false)
            sharedPrefs.edit().remove(AppConstants.DARK_MODE_KEY).apply()
            if (oldBool) DarkModeOption.ON else DarkModeOption.SYSTEM
        }
    )

    val isDynamicColor = MutableStateFlow(
        sharedPrefs.getBoolean(AppConstants.DYNAMIC_COLORS_KEY, true)
    )

    val language = MutableStateFlow(
        sharedPrefs.getString(AppConstants.LANGUAGE_KEY, "") ?: ""
    )

    fun setDarkModeOption(option: DarkModeOption) {
        darkModeOption.value = option
        sharedPrefs.edit().putString(AppConstants.DARK_MODE_OPTION_KEY, option.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        isDynamicColor.value = enabled
        sharedPrefs.edit().putBoolean(AppConstants.DYNAMIC_COLORS_KEY, enabled).apply()
    }

    fun setLanguage(locale: String) {
        language.value = locale
        sharedPrefs.edit().putString(AppConstants.LANGUAGE_KEY, locale).apply()
    }
}

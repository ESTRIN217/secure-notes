package com.example.data

import com.example.DarkModeOption

interface PreferencesRepository {
    fun getDarkModeOption(): DarkModeOption
    fun setDarkModeOption(option: DarkModeOption)
    fun getIsDynamicColor(): Boolean
    fun setIsDynamicColor(enabled: Boolean)
    fun getLanguage(): String
    fun setLanguage(locale: String)
}

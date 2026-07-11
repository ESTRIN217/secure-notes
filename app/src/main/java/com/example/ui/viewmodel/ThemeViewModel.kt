package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.DarkModeOption
import com.example.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

class ThemeViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val darkModeOption = MutableStateFlow(preferencesRepository.getDarkModeOption())

    val isDynamicColor = MutableStateFlow(preferencesRepository.getIsDynamicColor())

    val language = MutableStateFlow(preferencesRepository.getLanguage())

    fun setDarkModeOption(option: DarkModeOption) {
        darkModeOption.value = option
        preferencesRepository.setDarkModeOption(option)
    }

    fun setDynamicColor(enabled: Boolean) {
        isDynamicColor.value = enabled
        preferencesRepository.setIsDynamicColor(enabled)
    }

    fun setLanguage(locale: String) {
        language.value = locale
        preferencesRepository.setLanguage(locale)
    }
}

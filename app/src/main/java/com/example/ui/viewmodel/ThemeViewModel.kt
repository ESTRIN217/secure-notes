package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.estrin217.tema.AppTheme
import com.estrin217.tema.AppThemes
import com.example.DarkModeOption
import com.example.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Los dos ajustes de aspecto de la app: el tema y el modo (spec 025).
 *
 * El tema sustituye al interruptor de colores dinámicos, y `isDynamicColor` ya no existe: lo que se guarda es
 * [AppTheme], con su identificador, y lo que se pinta lo decide `MyApplicationTheme`. El idioma sigue aquí porque
 * su selector vive también en Ajustes, aunque no sea un ajuste de aspecto.
 */
class ThemeViewModel(private val preferencesRepository: PreferencesRepository) : ViewModel() {

    val darkModeOption = MutableStateFlow(preferencesRepository.getDarkModeOption())

    /** El tema activo. Por defecto es el dinámico, que es lo que la app muestra de fábrica. */
    val appTheme = MutableStateFlow(preferencesRepository.getAppTheme())

    val language = MutableStateFlow(preferencesRepository.getLanguage())

    fun setDarkModeOption(option: DarkModeOption) {
        darkModeOption.value = option
        preferencesRepository.setDarkModeOption(option)
    }

    fun setAppTheme(theme: AppTheme) {
        appTheme.value = theme
        preferencesRepository.setAppTheme(theme)
    }

    fun setLanguage(locale: String) {
        language.value = locale
        preferencesRepository.setLanguage(locale)
    }
}
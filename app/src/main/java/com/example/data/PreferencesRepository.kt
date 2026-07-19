package com.example.data

import com.example.DarkModeOption

interface PreferencesRepository {
    fun getDarkModeOption(): DarkModeOption
    fun setDarkModeOption(option: DarkModeOption)
    fun getIsDynamicColor(): Boolean
    fun setIsDynamicColor(enabled: Boolean)
    fun getLanguage(): String
    fun setLanguage(locale: String)

    fun getAiEnabled(): Boolean
    fun setAiEnabled(enabled: Boolean)
    fun getAiBackend(): String
    fun setAiBackend(backend: String)
    fun getAiEndpointUrl(): String
    fun setAiEndpointUrl(url: String)
    fun getAiModelName(): String
    fun setAiModelName(model: String)
    fun getAiOnDeviceModelPath(): String
    fun setAiOnDeviceModelPath(path: String)
}

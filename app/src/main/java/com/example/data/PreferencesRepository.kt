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
    fun getAiSystemPrompt(): String
    fun setAiSystemPrompt(prompt: String)
    fun getAiTemperature(): Float
    fun setAiTemperature(value: Float)
    fun getAiTopK(): Int
    fun setAiTopK(value: Int)
    fun getAiTopP(): Float
    fun setAiTopP(value: Float)
    fun getAiRepetitionPenalty(): Float
    fun setAiRepetitionPenalty(value: Float)
    fun getAiMaxTokens(): Int
    fun setAiMaxTokens(value: Int)
}

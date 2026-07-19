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

    override fun getAiEnabled(): Boolean {
        return prefs.getBoolean(AppConstants.AI_ENABLED_KEY, false)
    }

    override fun setAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.AI_ENABLED_KEY, enabled).apply()
    }

    override fun getAiBackend(): String {
        return prefs.getString(AppConstants.AI_BACKEND_KEY, "ollama") ?: "ollama"
    }

    override fun setAiBackend(backend: String) {
        prefs.edit().putString(AppConstants.AI_BACKEND_KEY, backend).apply()
    }

    override fun getAiEndpointUrl(): String {
        return prefs.getString(AppConstants.AI_ENDPOINT_URL_KEY, "http://192.168.1.100:11434") ?: "http://192.168.1.100:11434"
    }

    override fun setAiEndpointUrl(url: String) {
        prefs.edit().putString(AppConstants.AI_ENDPOINT_URL_KEY, url).apply()
    }

    override fun getAiModelName(): String {
        return prefs.getString(AppConstants.AI_MODEL_NAME_KEY, "llama3.2") ?: "llama3.2"
    }

    override fun setAiModelName(model: String) {
        prefs.edit().putString(AppConstants.AI_MODEL_NAME_KEY, model).apply()
    }

    override fun getAiOnDeviceModelPath(): String {
        return prefs.getString(AppConstants.AI_ONDEVICE_MODEL_PATH_KEY, "") ?: ""
    }

    override fun setAiOnDeviceModelPath(path: String) {
        prefs.edit().putString(AppConstants.AI_ONDEVICE_MODEL_PATH_KEY, path).apply()
    }
}

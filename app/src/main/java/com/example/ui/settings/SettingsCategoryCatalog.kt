package com.example.ui.settings

import com.example.R

/**
 * Spec 044 enmienda, RF-326…RF-328. Catálogo de las ocho categorías del panel izquierdo.
 * Solo enteros y recursos: se prueba sin `Context` ni Robolectric (RNF-212).
 */
enum class SettingsGroup(val labelRes: Int) {
    GENERAL(R.string.settings_group_general),
    PRIVACY(R.string.settings_privacy_security),
    INFORMATION(R.string.settings_information)
}

data class SettingsCategoryInfo(
    val destination: SettingsDestination,
    val labelRes: Int,
    val summaryRes: Int,
    val group: SettingsGroup
)

/** Orden de la lista: primero Apariencia (RF-329), agrupado por [SettingsGroup]. */
val CATEGORIES: List<SettingsCategoryInfo> = listOf(
    SettingsCategoryInfo(
        SettingsDestination.APPEARANCE,
        R.string.settings_appearance,
        R.string.settings_appearance_desc,
        SettingsGroup.GENERAL
    ),
    SettingsCategoryInfo(
        SettingsDestination.LANGUAGE,
        R.string.settings_language,
        R.string.settings_language_desc,
        SettingsGroup.GENERAL
    ),
    SettingsCategoryInfo(
        SettingsDestination.AI,
        R.string.ai_section,
        R.string.ai_settings_desc,
        SettingsGroup.GENERAL
    ),
    SettingsCategoryInfo(
        SettingsDestination.TOOLS,
        R.string.settings_tools,
        R.string.floating_mode_desc,
        SettingsGroup.GENERAL
    ),
    SettingsCategoryInfo(
        SettingsDestination.PRIVACY,
        R.string.settings_privacy_security,
        R.string.settings_privacy_desc,
        SettingsGroup.PRIVACY
    ),
    SettingsCategoryInfo(
        SettingsDestination.STORAGE,
        R.string.settings_storage_data,
        R.string.settings_storage_manager_desc,
        SettingsGroup.PRIVACY
    ),
    SettingsCategoryInfo(
        SettingsDestination.LEGAL,
        R.string.settings_legal,
        R.string.settings_legal_desc,
        SettingsGroup.INFORMATION
    ),
    SettingsCategoryInfo(
        SettingsDestination.INFORMATION,
        R.string.settings_information,
        R.string.settings_about_desc,
        SettingsGroup.INFORMATION
    )
)

/** RF-326: una consulta vacía no filtra; en cualquier otro caso busca en título o subtítulo. */
fun matchesQuery(title: String, subtitle: String, query: String): Boolean {
    val term = query.trim()
    if (term.isEmpty()) return true
    return title.contains(term, ignoreCase = true) || subtitle.contains(term, ignoreCase = true)
}

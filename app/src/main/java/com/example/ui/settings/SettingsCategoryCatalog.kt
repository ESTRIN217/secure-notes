package com.example.ui.settings

import com.example.R

/**
 * Spec 044 enmienda, RF-326…RF-328 y RF-336. Catálogo de las entradas del panel izquierdo.
 * Solo enteros y recursos: se prueba sin `Context` ni Robolectric (RNF-212).
 */
enum class SettingsGroup(val labelRes: Int) {
    GENERAL(R.string.settings_group_general),
    PRIVACY(R.string.settings_privacy_security),
    STORAGE_DATA(R.string.settings_storage_data),
    LEGAL(R.string.settings_legal),
    APP_INFO(R.string.settings_group_app_info)
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
        SettingsDestination.BACKUP,
        R.string.settings_backup_restore,
        R.string.settings_backup_restore_desc,
        SettingsGroup.STORAGE_DATA
    ),
    SettingsCategoryInfo(
        SettingsDestination.STORAGE_MANAGER,
        R.string.settings_storage_manager,
        R.string.settings_storage_manager_desc,
        SettingsGroup.STORAGE_DATA
    ),
    SettingsCategoryInfo(
        SettingsDestination.LEGAL_INFO,
        R.string.settings_terms_privacy,
        R.string.settings_legal_desc,
        SettingsGroup.LEGAL
    ),
    SettingsCategoryInfo(
        SettingsDestination.LICENSES,
        R.string.settings_licenses,
        R.string.about_licenses_desc,
        SettingsGroup.LEGAL
    ),
    SettingsCategoryInfo(
        SettingsDestination.UPDATE,
        R.string.settings_check_update,
        R.string.settings_check_update_desc,
        SettingsGroup.APP_INFO
    ),
    SettingsCategoryInfo(
        SettingsDestination.CHANGELOG,
        R.string.settings_changelog,
        R.string.settings_changelog_desc,
        SettingsGroup.APP_INFO
    ),
    SettingsCategoryInfo(
        SettingsDestination.ABOUT,
        R.string.settings_about,
        R.string.settings_about_desc,
        SettingsGroup.APP_INFO
    )
)

/** RF-326/RF-339: una consulta vacía no filtra; en cualquier otro caso busca en todo el haystack. */
fun matchesQuery(haystack: List<String>, query: String): Boolean {
    val term = query.trim()
    if (term.isEmpty()) return true
    return haystack.any { it.contains(term, ignoreCase = true) }
}

/** Versión de dos campos, conservada para las pruebas del catálogo. */
fun matchesQuery(title: String, subtitle: String, query: String): Boolean =
    matchesQuery(listOf(title, subtitle), query)

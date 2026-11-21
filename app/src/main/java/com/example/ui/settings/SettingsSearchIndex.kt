package com.example.ui.settings

import com.example.R

/**
 * Spec 044 enmienda, RF-339. Términos del contenido de cada pantalla de detalle que deben poder
 * encontrarse desde la barra de búsqueda de la lista.
 *
 * Es dato puro (solo IDs de recursos): se prueba sin `Context` ni Robolectric. Se reutilizan
 * etiquetas ya existentes, así que no hay que traducir nada nuevo y el índice sigue al idioma de la
 * app. No intenta leer el árbol de composición: no es fiable ni barato.
 */
private val privacyTerms = listOf(
    R.string.password_hint, R.string.password_type_label, R.string.biometric_unlock_label,
    R.string.biometric_enable_title, R.string.screenshot_toggle_label, R.string.settings_privacy_desc
)

private val DETAIL_TERMS: Map<SettingsDestination, List<Int>> = mapOf(
    SettingsDestination.APPEARANCE to listOf(
        R.string.settings_theme, R.string.theme_picker_title, R.string.toggle_dark_theme,
        R.string.settings_dark_mode_system, R.string.settings_dark_mode_on, R.string.settings_dark_mode_off
    ),
    SettingsDestination.LANGUAGE to listOf(
        R.string.settings_language, R.string.settings_lang_default, R.string.settings_lang_en,
        R.string.settings_lang_es, R.string.settings_lang_fr, R.string.settings_lang_it,
        R.string.settings_lang_pt
    ),
    SettingsDestination.AI to listOf(
        R.string.ai_backend, R.string.ai_ollama, R.string.ai_ondevice, R.string.ai_model_name,
        R.string.ai_endpoint_url, R.string.ai_system_prompt, R.string.ai_settings_title,
        R.string.ai_connection_ok
    ),
    SettingsDestination.TOOLS to listOf(
        R.string.floating_mode_title, R.string.floating_mode_desc,
        R.string.floating_mode_permission_title, R.string.floating_mode_tile_label,
        R.string.code_editor, R.string.code_editor_desc
    ),
    // La entrada de la lista es PRIVACY, pero su pantalla es PRIVACY_SETTINGS; cubrimos ambos.
    SettingsDestination.PRIVACY to privacyTerms,
    SettingsDestination.PRIVACY_SETTINGS to privacyTerms,
    SettingsDestination.BACKUP to listOf(
        R.string.backup_restore_title, R.string.backup_local_section, R.string.backup_export,
        R.string.backup_import, R.string.backup_cloud_section, R.string.backup_upload,
        R.string.backup_download, R.string.backup_encrypt_title, R.string.backup_auto_title
    ),
    SettingsDestination.STORAGE_MANAGER to listOf(
        R.string.settings_storage_manager, R.string.storage_overview, R.string.storage_orphan_files,
        R.string.storage_auto_cleanup, R.string.storage_clean_up_now, R.string.storage_clear_cache,
        R.string.storage_ai_models
    ),
    SettingsDestination.LEGAL_INFO to listOf(
        R.string.settings_terms_privacy, R.string.settings_legal_desc
    ),
    SettingsDestination.LICENSES to listOf(
        R.string.settings_licenses, R.string.licenses_title, R.string.about_license_desc
    ),
    SettingsDestination.UPDATE to listOf(
        R.string.settings_check_update, R.string.update_current_version, R.string.update_auto_check,
        R.string.update_download_install
    ),
    SettingsDestination.CHANGELOG to listOf(
        R.string.settings_changelog, R.string.settings_changelog_desc, R.string.update_show_changelog
    ),
    SettingsDestination.ABOUT to listOf(
        R.string.settings_about, R.string.about_title, R.string.about_github,
        R.string.about_website, R.string.about_developer
    )
)

/** Términos adicionales de una entrada; vacío si el destino no tiene pantalla propia indexada. */
fun searchTermsFor(destination: SettingsDestination): List<Int> =
    DETAIL_TERMS[destination].orEmpty()

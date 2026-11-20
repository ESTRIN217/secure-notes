package com.example.ui.settings

/**
 * Spec 044. Destino inicial de la Activity de ajustes.
 *
 * Viaja como `String` en el `Intent` (los enum no son parcels estables) y como clave del scaffold
 * adaptativo, que exige un tipo guardable en `Bundle`. Esta pieza es pura: no importa `android`,
 * así que su conversión se prueba sin Robolectric (RNF-210).
 */
enum class SettingsDestination(val extraValue: String) {
    HUB("hub"),

    // Ocho categorías del panel izquierdo.
    APPEARANCE("appearance"),
    LANGUAGE("language"),
    PRIVACY("privacy"),
    STORAGE("storage"),
    AI("ai"),
    TOOLS("tools"),
    LEGAL("legal"),
    INFORMATION("information"),

    // Sub-pantallas que se pintan directamente en el panel de detalle.
    PRIVACY_SETTINGS("privacy_settings"),
    BACKUP("backup"),
    STORAGE_MANAGER("storage_manager"),
    AI_SETTINGS("ai_settings"),
    LEGAL_INFO("legal_info"),
    LICENSES("licenses"),
    UPDATE("update"),
    ABOUT("about");

    /** `true` solo para las ocho filas de la lista; el hub y las sub-pantallas no lo son. */
    val isCategory: Boolean
        get() = this in CATEGORIES

    companion object {
        val CATEGORIES: List<SettingsDestination> = listOf(
            APPEARANCE, LANGUAGE, PRIVACY, STORAGE, AI, TOOLS, LEGAL, INFORMATION
        )

        /** Un valor vacío, desconocido o con distinta caja cae al hub (RF-324). */
        fun fromExtra(value: String?): SettingsDestination =
            entries.firstOrNull { it.extraValue == value } ?: HUB
    }
}

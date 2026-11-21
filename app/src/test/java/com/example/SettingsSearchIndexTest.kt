package com.example

import com.example.ui.settings.CATEGORIES
import com.example.ui.settings.SettingsDestination
import com.example.ui.settings.matchesQuery
import com.example.ui.settings.searchTermsFor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec 044 enmienda, RF-339. Índice de búsqueda puro: sin Context ni Robolectric. */
class SettingsSearchIndexTest {

    @Test
    fun `every list entry has indexed detail terms`() {
        CATEGORIES.forEach { entry ->
            assertTrue(
                "${entry.destination} sin términos de detalle",
                searchTermsFor(entry.destination).isNotEmpty()
            )
        }
    }

    @Test
    fun `representative detail queries find their destination`() {
        assertFinds("backend", SettingsDestination.AI)
        assertFinds("contraseña", SettingsDestination.PRIVACY)
        assertFinds("huérfanos", SettingsDestination.STORAGE_MANAGER)
        assertFinds("licencias", SettingsDestination.LICENSES)
        assertFinds("versión", SettingsDestination.UPDATE)
    }

    @Test
    fun `unrelated query does not match`() {
        val haystack = resolucionFalsa(SettingsDestination.AI)
        assertFalse(matchesQuery(haystack, "contraseña"))
    }

    /** Resolvedor falso: traducciones de prueba para comprobar la wiring del índice. */
    private fun resolucionFalsa(destination: SettingsDestination): List<String> {
        val nombres = mapOf(
            R.string.ai_backend to "Backend de IA",
            R.string.ai_ollama to "Ollama (HTTP local)",
            R.string.ai_ondevice to "En el dispositivo",
            R.string.ai_model_name to "Nombre del modelo",
            R.string.ai_endpoint_url to "URL del servidor",
            R.string.ai_system_prompt to "Prompt del sistema",
            R.string.ai_settings_title to "Ajustes de IA",
            R.string.ai_connection_ok to "Conectado. Modelos: %1\$s",
            R.string.password_hint to "Contraseña",
            R.string.password_type_label to "Tipo de seguridad",
            R.string.biometric_unlock_label to "Desbloqueo biométrico",
            R.string.biometric_enable_title to "Activar biometría",
            R.string.screenshot_toggle_label to "Permitir capturas",
            R.string.settings_privacy_desc to "Protección con contraseña",
            R.string.storage_orphan_files to "Archivos huérfanos",
            R.string.settings_storage_manager to "Administrador de almacenamiento",
            R.string.settings_licenses to "Licencias de código abierto",
            R.string.licenses_title to "Licencias de código abierto",
            R.string.settings_check_update to "Buscar actualizaciones",
            R.string.update_current_version to "Versión actual"
        )
        return searchTermsFor(destination).map { nombres[it].orEmpty() }
    }

    private fun assertFinds(consulta: String, destination: SettingsDestination) {
        assertTrue(
            "$consulta no encuentra $destination",
            matchesQuery(resolucionFalsa(destination), consulta)
        )
    }
}

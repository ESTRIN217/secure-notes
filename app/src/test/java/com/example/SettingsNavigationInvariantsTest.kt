package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Spec 044, RNF-206 y RF-323. Los accesos a ajustes y la declaración de la Activity se vigilan
 * leyendo la fuente: cambiar un acceso es fácil y ninguna prueba de comportamiento lo notaría
 * en este host (Robolectric no corre en linux/aarch64).
 */
class SettingsNavigationInvariantsTest {

    @Test
    fun `every settings entry point goes through launchSettings`() {
        val screen = sinComentariosNiTextos("src/main/java/com/example/ui/Screen.kt")
        val shortcuts = sinComentariosNiTextos("src/main/java/com/example/ui/ShortcutDestinations.kt")
        val activity = sinComentariosNiTextos("src/main/java/com/example/MainActivity.kt")

        assertTrue(screen.contains("launchSettings("))
        assertTrue(activity.contains("launchSettings("))
        // RF-352: el atajo de ajustes ya no pasa por ShortcutDestinations; apunta directo al alias.
        assertFalse("el atajo no debe enrutarse por launchSettings", shortcuts.contains("launchSettings("))
        for (removed in removedScreens) {
            assertFalse("Screen.$removed sigue vivo", screen.contains("Screen.$removed"))
        }
    }

    @Test
    fun `settings activity is not exported and has no intent filters`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val block = manifest.substringAfter("android:name=\".SettingsActivity\"")
            .substringBefore("/>")
        assertTrue(block.contains("android:exported=\"false\""))
        assertFalse(block.contains("intent-filter"))
    }

    @Test
    fun `the old hub composable is gone`() {
        val source = sinComentariosNiTextos("src/main/java/com/example/ui/settings/SettingsScreen.kt")
        assertFalse(source.contains("fun SettingsScreen("))
    }

    private companion object {
        val removedScreens = listOf(
            "SettingsHub", "PrivacySettings", "BackupRestore", "StorageManager",
            "AiSettings", "LegalInfo", "Licenses", "UpdateInfo", "About"
        )

        /** Quita comentarios y literales: un KDoc que explica por qué algo NO está no debe contar. */
        fun sinComentariosNiTextos(path: String): String {
            var text = File(path).readText()
            text = text.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            text = text.replace(Regex("//[^\\n]*"), "")
            return text.replace(Regex("\"([^\"\\\\]|\\\\.)*\""), "\"\"")
        }
    }
}

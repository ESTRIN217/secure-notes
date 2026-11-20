package com.example

import com.example.ui.settings.SettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 044, RNF-210. El destino del Intent es lógica pura: ninguna prueba de aquí necesita
 * `Context`, `Intent` ni Robolectric.
 */
class SettingsDestinationTest {

    @Test
    fun `unknown or empty extra falls back to hub`() {
        assertEquals(SettingsDestination.HUB, SettingsDestination.fromExtra(null))
        assertEquals(SettingsDestination.HUB, SettingsDestination.fromExtra(""))
        assertEquals(SettingsDestination.HUB, SettingsDestination.fromExtra("no-existe"))
        assertEquals(SettingsDestination.HUB, SettingsDestination.fromExtra("HUB"))
    }

    @Test
    fun `every destination survives a round trip through the extra`() {
        SettingsDestination.entries.forEach { destination ->
            assertEquals(destination, SettingsDestination.fromExtra(destination.extraValue))
        }
    }

    @Test
    fun `there are exactly eight categories`() {
        val categories = SettingsDestination.entries.filter { it.isCategory }
        assertEquals(8, categories.size)
        assertTrue(SettingsDestination.APPEARANCE.isCategory)
        assertFalse(SettingsDestination.HUB.isCategory)
        assertFalse(SettingsDestination.UPDATE.isCategory)
    }
}

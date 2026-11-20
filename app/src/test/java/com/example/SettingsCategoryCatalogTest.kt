package com.example

import com.example.ui.settings.CATEGORIES
import com.example.ui.settings.SettingsGroup
import com.example.ui.settings.matchesQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec 044 enmienda, RF-326…RF-328 y RNF-212. Sin Context ni Robolectric. */
class SettingsCategoryCatalogTest {

    @Test
    fun `there are eight unique categories in three groups`() {
        assertEquals(8, CATEGORIES.size)
        assertEquals(CATEGORIES.size, CATEGORIES.map { it.destination }.toSet().size)
        assertEquals(
            setOf(SettingsGroup.GENERAL, SettingsGroup.PRIVACY, SettingsGroup.INFORMATION),
            CATEGORIES.map { it.group }.toSet()
        )
        assertEquals(CATEGORIES.first().destination, com.example.ui.settings.SettingsDestination.APPEARANCE)
    }

    @Test
    fun `query matches title or summary ignoring case and blank means all`() {
        assertTrue(matchesQuery("Appearance", "Theme", ""))
        assertTrue(matchesQuery("Appearance", "Theme", "   "))
        assertTrue(matchesQuery("Appearance", "Theme", "appear"))
        assertTrue(matchesQuery("Appearance", "Theme", "THEME"))
        assertFalse(matchesQuery("Appearance", "Theme", "privacy"))
    }
}

package com.example

import com.example.ui.settings.CATEGORIES
import com.example.ui.settings.SettingsDestination
import com.example.ui.settings.SettingsGroup
import com.example.ui.settings.matchesQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec 044 enmienda, RF-326…RF-328 y RNF-212. Sin Context ni Robolectric. */
class SettingsCategoryCatalogTest {

    @Test
    fun `there are twelve unique entries in six groups`() {
        assertEquals(12, CATEGORIES.size)
        assertEquals(CATEGORIES.size, CATEGORIES.map { it.destination }.toSet().size)
        assertEquals(
            setOf(
                SettingsGroup.GENERAL, SettingsGroup.PRIVACY, SettingsGroup.STORAGE_DATA,
                SettingsGroup.LEGAL, SettingsGroup.APP_INFO
            ),
            CATEGORIES.map { it.group }.toSet()
        )
        assertEquals(SettingsDestination.APPEARANCE, CATEGORIES.first().destination)
    }

    @Test
    fun `backup and storage manager are direct entries of the new section`() {
        val storageEntries = CATEGORIES.filter { it.group == SettingsGroup.STORAGE_DATA }
        assertEquals(
            listOf(SettingsDestination.BACKUP, SettingsDestination.STORAGE_MANAGER),
            storageEntries.map { it.destination }
        )
    }

    @Test
    fun `legal section contains terms and licenses only`() {
        assertEquals(
            listOf(SettingsDestination.LEGAL_INFO, SettingsDestination.LICENSES),
            CATEGORIES.filter { it.group == SettingsGroup.LEGAL }.map { it.destination }
        )
    }

    @Test
    fun `app info section contains updates changelog and about`() {
        assertEquals(
            listOf(
                SettingsDestination.UPDATE,
                SettingsDestination.CHANGELOG,
                SettingsDestination.ABOUT
            ),
            CATEGORIES.filter { it.group == SettingsGroup.APP_INFO }.map { it.destination }
        )
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

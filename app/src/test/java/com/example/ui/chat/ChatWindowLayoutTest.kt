package com.example.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 046 (RF-359..364, RNF-220): decisiones puras de layout adaptativo del asistente.
 * JVM puro, sin Robolectric.
 */
class ChatWindowLayoutTest {

    @Test
    fun chatLayoutMode_usesWindowBreakpoints() {
        assertEquals(ChatWindowLayout.COMPACT, chatLayoutMode(599))
        assertEquals(ChatWindowLayout.MEDIUM, chatLayoutMode(600))
        assertEquals(ChatWindowLayout.MEDIUM, chatLayoutMode(839))
        assertEquals(ChatWindowLayout.EXPANDED, chatLayoutMode(840))
        assertEquals(ChatWindowLayout.EXPANDED, chatLayoutMode(1199))
        assertEquals(ChatWindowLayout.WIDE, chatLayoutMode(1200))
    }

    @Test
    fun showsInlineHistory_fromMediumUp() {
        assertFalse(showsInlineHistory(ChatWindowLayout.COMPACT))
        assertTrue(showsInlineHistory(ChatWindowLayout.MEDIUM))
        assertTrue(showsInlineHistory(ChatWindowLayout.EXPANDED))
        assertTrue(showsInlineHistory(ChatWindowLayout.WIDE))
    }

    @Test
    fun showsListDetailHistory_fromExpandedUp() {
        assertFalse(showsListDetailHistory(ChatWindowLayout.COMPACT))
        assertFalse(showsListDetailHistory(ChatWindowLayout.MEDIUM))
        assertTrue(showsListDetailHistory(ChatWindowLayout.EXPANDED))
        assertTrue(showsListDetailHistory(ChatWindowLayout.WIDE))
    }

    @Test
    fun usesSecondarySheetForModels_fromMediumUp() {
        assertFalse(usesSecondarySheetForModels(ChatWindowLayout.COMPACT))
        assertTrue(usesSecondarySheetForModels(ChatWindowLayout.MEDIUM))
        assertTrue(usesSecondarySheetForModels(ChatWindowLayout.EXPANDED))
        assertTrue(usesSecondarySheetForModels(ChatWindowLayout.WIDE))
    }

    @Test
    fun shouldShowAssistantRail_fromMediumUp() {
        assertFalse(shouldShowAssistantRail(ChatWindowLayout.COMPACT))
        assertTrue(shouldShowAssistantRail(ChatWindowLayout.MEDIUM))
        assertTrue(shouldShowAssistantRail(ChatWindowLayout.EXPANDED))
        assertTrue(shouldShowAssistantRail(ChatWindowLayout.WIDE))
    }

    @Test
    fun assistantRailWidthDp_expandedAndCollapsed() {
        assertEquals(260, assistantRailWidthDp(true))
        assertEquals(72, assistantRailWidthDp(false))
    }
}

package com.example.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El corte compacto/grande del chat (spec 033, RF-215/RF-223, RNF-140): `600 dp` exacto ya es «grande».
 */
class ScreenWidthTest {

    @Test
    fun `599 es compacto`() {
        assertFalse(isLargeScreenWidth(599))
    }

    @Test
    fun `600 es grande, exactamente en el corte`() {
        assertTrue(isLargeScreenWidth(600))
    }

    @Test
    fun `anchos mayores son grandes`() {
        assertTrue(isLargeScreenWidth(840))
        assertTrue(isLargeScreenWidth(1200))
    }

    @Test
    fun `cero es compacto`() {
        assertFalse(isLargeScreenWidth(0))
    }
}

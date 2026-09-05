package com.example.service

import com.example.ui.floating.ResizeCorner
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingOverlayDragTest {

    @Test
    fun `position inside bounds is unchanged`() {
        assertEquals(
            100 to 200,
            clampOverlayPosition(100, 200, 340, 460, 1080, 2400)
        )
    }

    @Test
    fun `negative position clamps to zero`() {
        assertEquals(
            0 to 0,
            clampOverlayPosition(-50, -30, 340, 460, 1080, 2400)
        )
    }

    @Test
    fun `overflow clamps to screen minus window`() {
        assertEquals(
            (1080 - 340) to (2400 - 460),
            clampOverlayPosition(2000, 5000, 340, 460, 1080, 2400)
        )
    }

    @Test
    fun `window wider than screen pins x to zero`() {
        assertEquals(
            0 to 100,
            clampOverlayPosition(50, 100, 2000, 460, 1080, 2400)
        )
    }

    @Test
    fun `window taller than screen pins y to zero`() {
        assertEquals(
            100 to 0,
            clampOverlayPosition(100, 50, 340, 3000, 1080, 2400)
        )
    }

    @Test
    fun `bottom-right drag grows size without moving origin`() {
        assertEquals(
            ResizedLayout(x = 100, y = 200, width = 400, height = 520),
            applyCornerResize(
                ResizeCorner.BOTTOM_RIGHT, 100, 200, 340, 460, 60, 60,
                280, 360, 993, 1800, 1080, 2400
            )
        )
    }

    @Test
    fun `top-left drag grows size and shifts origin`() {
        assertEquals(
            ResizedLayout(x = 40, y = 140, width = 400, height = 520),
            applyCornerResize(
                ResizeCorner.TOP_LEFT, 100, 200, 340, 460, -60, -60,
                280, 360, 993, 1800, 1080, 2400
            )
        )
    }

    @Test
    fun `resize clamps to min size and adjusts origin`() {
        assertEquals(
            ResizedLayout(x = 160, y = 300, width = 280, height = 360),
            applyCornerResize(
                ResizeCorner.TOP_LEFT, 100, 200, 340, 460, 500, 500,
                280, 360, 993, 1800, 1080, 2400
            )
        )
    }

    @Test
    fun `resize clamps to max size`() {
        assertEquals(
            ResizedLayout(x = 0, y = 0, width = 993, height = 1800),
            applyCornerResize(
                ResizeCorner.BOTTOM_RIGHT, 0, 0, 340, 460, 5000, 5000,
                280, 360, 993, 1800, 1080, 2400
            )
        )
    }
}

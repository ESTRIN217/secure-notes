package com.example.ui.floating

enum class ResizeCorner(
    val signX: Int,
    val signY: Int,
    val movesX: Boolean,
    val movesY: Boolean
) {
    TOP_LEFT(signX = -1, signY = -1, movesX = true, movesY = true),
    TOP_RIGHT(signX = 1, signY = -1, movesX = false, movesY = true),
    BOTTOM_LEFT(signX = -1, signY = 1, movesX = true, movesY = false),
    BOTTOM_RIGHT(signX = 1, signY = 1, movesX = false, movesY = false)
}

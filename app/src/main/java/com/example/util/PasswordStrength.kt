package com.example.util

import androidx.compose.ui.graphics.Color

enum class PasswordStrength { WEAK, MEDIUM, STRONG, VERY_STRONG }

fun checkPasswordStrength(password: String): PasswordStrength {
    if (password.length < 4) return PasswordStrength.WEAK
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when {
        score <= 1 -> PasswordStrength.WEAK
        score <= 3 -> PasswordStrength.MEDIUM
        score <= 4 -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

fun PasswordStrength.toColor(): Color = when (this) {
    PasswordStrength.WEAK -> Color(0xFFE53935)
    PasswordStrength.MEDIUM -> Color(0xFFFB8C00)
    PasswordStrength.STRONG -> Color(0xFF43A047)
    PasswordStrength.VERY_STRONG -> Color(0xFF1E88E5)
}

fun PasswordStrength.toLabelRes(): Int = when (this) {
    PasswordStrength.WEAK -> com.example.R.string.password_weak
    PasswordStrength.MEDIUM -> com.example.R.string.password_medium
    PasswordStrength.STRONG -> com.example.R.string.password_strong
    PasswordStrength.VERY_STRONG -> com.example.R.string.password_very_strong
}

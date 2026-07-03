package com.example

import androidx.compose.ui.graphics.Color

object AppConstants {
    const val PREFS_NAME = "secure_notes_prefs"
    const val CUSTOM_ORDER_KEY = "custom_order_ids"
    const val MASTER_PASSWORD_HASH_KEY = "master_password_hash"
    const val MASTER_PASSWORD_SALT_KEY = "master_password_salt"
    const val MASTER_PASSWORD_IV_KEY = "master_password_iv"
    const val DARK_MODE_KEY = "dark_mode"
    const val DARK_MODE_OPTION_KEY = "dark_mode_option"
    const val DYNAMIC_COLORS_KEY = "dynamic_colors"
    const val LANGUAGE_KEY = "language"
    const val AUTO_UPDATE_CHECK_KEY = "auto_update_check"
    const val DRIVE_LINKED_KEY = "drive_linked"
    const val DRIVE_ACCESS_TOKEN_KEY = "drive_access_token"
    const val LAST_SYNC_TIME_KEY = "last_sync_time"

    val SecurityGreen = Color(0xFF43A047)
}

enum class DarkModeOption { SYSTEM, OFF, ON }

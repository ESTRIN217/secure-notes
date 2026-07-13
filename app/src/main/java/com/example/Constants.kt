package com.example

import androidx.compose.ui.graphics.Color

object AppConstants {
    const val WEB_CLIENT_ID = "53440468121-eba9s7mkgrl8958p7grusf7vmgtsgihq.apps.googleusercontent.com"
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
    const val UPDATE_NOTIFICATIONS_KEY = "update_notifications"
    const val DRIVE_LINKED_KEY = "drive_linked"
    const val DRIVE_ACCESS_TOKEN_KEY = "drive_access_token"
    const val DRIVE_ACCOUNT_EMAIL_KEY = "drive_account_email"
    const val LAST_SYNC_TIME_KEY = "last_sync_time"
    const val PASSWORD_TYPE_KEY = "password_type"
    const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
    const val BIOMETRIC_KEY_ALIAS = "secure_notes_biometric_key"
    const val BIOMETRIC_ENCRYPTED_PASSWORD_KEY = "biometric_encrypted_password"
    const val BIOMETRIC_IV_KEY = "biometric_iv"
    const val INCLUDE_ATTACHMENTS_KEY = "include_attachments"
    const val COPY_ATTACHMENTS_LOCAL_KEY = "copy_attachments_local"

    const val ENCRYPT_BACKUPS_KEY = "encrypt_backups"
    const val AUTO_BACKUP_ENABLED_KEY = "auto_backup_enabled"
    const val AUTO_BACKUP_INTERVAL_KEY = "auto_backup_interval"
    const val LAST_BACKUP_SIZE_CLOUD_KEY = "last_backup_size_cloud"
    const val LAST_BACKUP_SIZE_LOCAL_KEY = "last_backup_size_local"
    const val CACHED_MASTER_PASSWORD_KEY = "cached_master_password"

    val SecurityGreen = Color(0xFF43A047)
}

enum class BackupInterval(val hours: Long) {
    HOURS_6(6),
    HOURS_12(12),
    HOURS_24(24),
    WEEKLY(168)
}

enum class DarkModeOption { SYSTEM, OFF, ON }
enum class PasswordType { PIN, PASSWORD }

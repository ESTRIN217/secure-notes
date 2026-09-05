package com.example

import androidx.compose.ui.graphics.Color

object AppConstants {
    const val WEB_CLIENT_ID = "53440468121-eba9s7mkgrl8958p7grusf7vmgtsgihq.apps.googleusercontent.com"
    const val PREFS_NAME = "secure_notes_prefs"
    const val SECURE_PREFS_NAME = "secure_notes_secure_prefs"
    const val CUSTOM_ORDER_KEY = "custom_order_ids"

    // Master password verification — stored in SecurePrefsStore (Keystore-encrypted)
    const val MASTER_PASSWORD_HASH_KEY = "master_password_hash"
    const val MASTER_PASSWORD_SALT_KEY = "master_password_salt"
    const val MASTER_PASSWORD_IV_KEY = "master_password_iv"

    // Migration flag: true once data moved from regular -> encrypted prefs
    const val MIGRATED_ENCRYPTED_PREFS = "migrated_encrypted_prefs"

    const val DARK_MODE_KEY = "dark_mode"
    const val DARK_MODE_OPTION_KEY = "dark_mode_option"
    const val DYNAMIC_COLORS_KEY = "dynamic_colors"
    const val LANGUAGE_KEY = "language"
    const val AUTO_UPDATE_CHECK_KEY = "auto_update_check"
    const val UPDATE_NOTIFICATIONS_KEY = "update_notifications"
    const val LAST_UPDATE_CHECK_KEY = "last_update_check"
    const val DRIVE_LINKED_KEY = "drive_linked"
    const val DRIVE_ACCESS_TOKEN_KEY = "drive_access_token"
    const val DRIVE_ACCOUNT_EMAIL_KEY = "drive_account_email"
    const val DRIVE_PROFILE_PICTURE_KEY = "drive_profile_picture"
    const val LAST_SYNC_TIME_KEY = "last_sync_time"
    const val PASSWORD_TYPE_KEY = "password_type"
    const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
    const val BIOMETRIC_KEY_ALIAS = "secure_notes_biometric_key"
    const val BIOMETRIC_ENCRYPTED_PASSWORD_KEY = "biometric_encrypted_password"
    const val BIOMETRIC_IV_KEY = "biometric_iv"

    const val ENCRYPT_BACKUPS_KEY = "encrypt_backups"
    const val AUTO_BACKUP_ENABLED_KEY = "auto_backup_enabled"
    const val AUTO_BACKUP_INTERVAL_KEY = "auto_backup_interval"
    const val LAST_BACKUP_SIZE_CLOUD_KEY = "last_backup_size_cloud"
    const val LAST_BACKUP_SIZE_LOCAL_KEY = "last_backup_size_local"
    const val LAST_LOCAL_BACKUP_TIME_KEY = "last_local_backup_time"
    const val CACHED_MASTER_PASSWORD_KEY = "cached_master_password"

    // Auto-lock timeout in minutes (0 = disabled)
    const val SCREENSHOT_ENABLED_KEY = "screenshot_enabled"

    const val AUTO_LOCK_TIMEOUT_KEY = "auto_lock_timeout"
    const val AUTO_LOCK_TIMEOUT_DEFAULT = 5L

    const val FLOATING_MODE_ENABLED_KEY = "floating_mode_enabled"

    // AI preferences
    const val AI_ENABLED_KEY = "ai_enabled"
    const val AI_BACKEND_KEY = "ai_backend"
    const val AI_ENDPOINT_URL_KEY = "ai_endpoint_url"
    const val AI_MODEL_NAME_KEY = "ai_model_name"
    const val AI_ONDEVICE_MODEL_PATH_KEY = "ai_ondevice_model_path"
    const val AI_SYSTEM_PROMPT_KEY = "ai_system_prompt"
    const val AI_TEMPERATURE_KEY = "ai_temperature"
    const val AI_TOP_K_KEY = "ai_top_k"
    const val AI_TOP_P_KEY = "ai_top_p"
    const val AI_REPETITION_PENALTY_KEY = "ai_repetition_penalty"
    const val AI_MAX_TOKENS_KEY = "ai_max_tokens"

    const val TRASH_RETENTION_DAYS = 90L
    const val AUTO_CLEANUP_ENABLED_KEY = "auto_cleanup_enabled"
    const val LARGE_FILE_THRESHOLD_MB = 5L
    const val LARGE_FILE_THRESHOLD_BYTES = LARGE_FILE_THRESHOLD_MB * 1024 * 1024

    val SecurityGreen = Color(0xFF43A047)
}

enum class AutoLockTimeout(val minutes: Long) {
    IMMEDIATELY(-1),
    DISABLED(0),
    MINUTE_1(1),
    MINUTES_5(5),
    MINUTES_15(15),
    MINUTES_30(30)
}

enum class BackupInterval(val hours: Long) {
    HOURS_6(6),
    HOURS_12(12),
    HOURS_24(24),
    WEEKLY(168)
}

enum class DarkModeOption { SYSTEM, OFF, ON }
enum class PasswordType { PIN, PASSWORD }

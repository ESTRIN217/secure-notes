package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Synchronous, device-bound encrypted key/value store.
 *
 * Values are encrypted with an AES-256/GCM key held in the Android Keystore
 * and stored as Base64(iv + ciphertext) in a private SharedPreferences file.
 * Keys themselves are stored with a prefix so their names are not leaked.
 *
 * A one-time migration transparently re-encrypts values that were previously
 * stored with the deprecated EncryptedSharedPreferences wrapper.
 */
class SecurePrefsStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val keyStore: KeyStore =
        KeyStore.getInstance(KEYSTORE).apply { load(null) }

    init {
        ensureKey()
        migrateLegacy()
    }

    @Synchronized
    fun getString(key: String, default: String?): String? {
        val cipherText = prefs.getString(storedKey(key), null) ?: return default
        return try {
            decrypt(cipherText)
        } catch (e: Exception) {
            Log.e(TAG, "Decrypt failed for key: $key", e)
            default
        }
    }

    @Synchronized
    fun putString(key: String, value: String) {
        val cipherText = encrypt(value)
        prefs.edit().putString(storedKey(key), cipherText).apply()
    }

    @Synchronized
    fun remove(key: String) {
        prefs.edit().remove(storedKey(key)).apply()
    }

    @Synchronized
    fun contains(key: String): Boolean = prefs.contains(storedKey(key))

    private fun ensureKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        generator.generateKey()
    }

    private fun secretKey(): SecretKey =
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException("Secure key not present in Keystore")

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val decoded = Base64.decode(payload, Base64.NO_WRAP)
        val iv = decoded.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = decoded.copyOfRange(GCM_IV_LENGTH, decoded.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    @Suppress("DEPRECATION")
    private fun migrateLegacy() {
        synchronized(migrationLock) {
            if (prefs.getBoolean(MIGRATION_DONE_KEY, false)) return
            val legacyPrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                appContext,
                LEGACY_PREFS_FILE,
                androidx.security.crypto.MasterKey.Builder(appContext)
                    .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val legacyAll = legacyPrefs.all
            if (legacyAll.isNotEmpty()) {
                legacyAll.forEach { (key, value) ->
                    if (value is String && value.isNotEmpty()) {
                        try {
                            putString(key, value)
                        } catch (e: Exception) {
                            Log.e(TAG, "Migration failed for key: $key", e)
                        }
                    }
                }
                legacyPrefs.edit().clear().apply()
            }
            prefs.edit().putBoolean(MIGRATION_DONE_KEY, true).apply()
        }
    }

    private fun storedKey(key: String): String = "$KEY_PREFIX$key"

    companion object {
        private const val TAG = "SecurePrefsStore"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "secure_notes_kv_key_v1"
        private const val PREFS_FILE = "secure_notes_kv_prefs"
        private const val LEGACY_PREFS_FILE = "secure_notes_secure_prefs"
        private const val MIGRATION_DONE_KEY = "legacy_migration_done"
        private const val KEY_PREFIX = "sec_"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128

        private val migrationLock = Any()
    }
}

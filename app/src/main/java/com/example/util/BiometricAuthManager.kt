package com.example.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricAuthManager(private val context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

    fun isBiometricAvailable(): Boolean {
        return try {
            val manager = BiometricManager.from(context)
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun createKey(alias: String) {
        try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (e: Exception) {
            Log.e("BiometricAuthManager", "createKey cleanup failed", e)
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        keyGenerator.generateKey()
    }

    fun getEncryptCipher(alias: String): Cipher {
        val secretKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("Biometric key not found in keystore")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey.secretKey)
        return cipher
    }

    fun getDecryptCipher(alias: String, iv: ByteArray): Cipher? {
        return try {
            val secretKey = (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e("BiometricAuthManager", "Key permanently invalidated", e)
            null
        } catch (e: Exception) {
            Log.e("BiometricAuthManager", "getDecryptCipher failed", e)
            null
        }
    }

    fun deleteKey(alias: String) {
        try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (e: Exception) {
            Log.e("BiometricAuthManager", "deleteKey failed", e)
        }
    }

    fun hasKey(alias: String): Boolean {
        return try {
            keyStore.containsAlias(alias)
        } catch (e: Exception) {
            false
        }
    }
}

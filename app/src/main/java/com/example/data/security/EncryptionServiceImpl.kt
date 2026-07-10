package com.example.data.security

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class EncryptionServiceImpl : CipherService {
    private companion object {
        private const val TAG = "EncryptionService"
        private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
        private const val SALT_LENGTH_BYTES = 16
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }

    override fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    override fun generateIv(): String {
        val random = SecureRandom()
        val iv = ByteArray(IV_LENGTH_BYTES)
        random.nextBytes(iv)
        return Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    override fun encrypt(plainText: String, password: String, saltBase64: String, ivBase64: String): Result<String> {
        return try {
            val key = KeyDerivation.deriveKey(password, saltBase64)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Result.success(Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.e(TAG, "encrypt failed", e)
            Result.failure(e)
        }
    }

    override fun decrypt(cipherTextBase64: String, password: String, saltBase64: String, ivBase64: String): Result<String> {
        return try {
            val key = KeyDerivation.deriveKey(password, saltBase64)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            val decryptedBytes = cipher.doFinal(cipherText)
            Result.success(String(decryptedBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "decrypt failed", e)
            Result.failure(e)
        }
    }
}

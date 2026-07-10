package com.example.data.security

interface CipherService {
    fun encrypt(plainText: String, password: String, saltBase64: String, ivBase64: String): String
    fun decrypt(cipherTextBase64: String, password: String, saltBase64: String, ivBase64: String): String
    fun generateSalt(): String
    fun generateIv(): String
}

package com.example.data.model

data class DecryptedNote(
    val note: Note,
    val title: String,
    val content: String,
    val isDecryptionSuccessful: Boolean
)

package com.example.util

import android.content.Context
import com.example.data.model.DecryptedNote

interface Exporter {
    val formatKey: String
    fun export(context: Context, notes: List<DecryptedNote>)
}

package com.example.util

import android.content.Context
import com.example.data.model.DecryptedNote
import com.example.data.model.Note

interface Exporter {
    fun export(context: Context, vararg notes: DecryptedNote)
}

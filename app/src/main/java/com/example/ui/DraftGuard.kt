package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DraftGuard {
    var hasUnsaved by mutableStateOf(false)

    var saveAction: ((newId: Int) -> Unit) -> Unit = {}
    var discardAction: () -> Unit = {}
}

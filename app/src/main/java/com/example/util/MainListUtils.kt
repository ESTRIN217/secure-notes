package com.example.util

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.AppConstants
import com.example.R
import com.example.ui.viewmodel.DecryptedNote

enum class SortOption {
    ALPHABETICAL,
    LAST_MODIFIED,
    CUSTOM
}

enum class MoveDirection { UP, DOWN }

fun reorderNote(noteId: Int, direction: MoveDirection, notesList: List<DecryptedNote>, context: Context) {
    val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    val customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
    val currentIds = if (customOrderStr.isNotEmpty()) {
        customOrderStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
    } else {
        notesList.map { it.note.id }.toMutableList()
    }

    if (!currentIds.contains(noteId)) currentIds.add(noteId)

    val index = currentIds.indexOf(noteId)
    val swapIndex = when (direction) {
        MoveDirection.UP -> if (index > 0) index - 1 else return
        MoveDirection.DOWN -> if (index in 0 until currentIds.size - 1) index + 1 else return
    }

    val temp = currentIds[index]
    currentIds[index] = currentIds[swapIndex]
    currentIds[swapIndex] = temp
    prefs.edit().putString(AppConstants.CUSTOM_ORDER_KEY, currentIds.joinToString(",")).apply()
}

fun swapNotes(id1: Int, id2: Int, notesList: List<DecryptedNote>, context: Context) {
    val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    val customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
    val currentIds = if (customOrderStr.isNotEmpty()) {
        customOrderStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
    } else {
        notesList.map { it.note.id }.toMutableList()
    }

    if (!currentIds.contains(id1)) currentIds.add(id1)
    if (!currentIds.contains(id2)) currentIds.add(id2)

    val idx1 = currentIds.indexOf(id1)
    val idx2 = currentIds.indexOf(id2)
    if (idx1 != -1 && idx2 != -1) {
        val temp = currentIds[idx1]
        currentIds[idx1] = currentIds[idx2]
        currentIds[idx2] = temp
        prefs.edit().putString(AppConstants.CUSTOM_ORDER_KEY, currentIds.joinToString(",")).apply()
    }
}

fun Modifier.fillPackageNameOrScope(): Modifier = this.fillMaxWidth()

fun borderStrokeHelper(isSelected: Boolean, activeColor: Color): BorderStroke {
    return if (isSelected) {
        BorderStroke(2.dp, activeColor)
    } else {
        BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
    }
}

@Composable
fun getNoteBackgroundColor(colorId: Int?, isDark: Boolean = isSystemInDarkTheme()): Color {
    if (colorId == null || colorId == 0) return MaterialTheme.colorScheme.surface
    return when (colorId) {
        1 -> if (isDark) Color(0xFF0D47A1).copy(alpha = 0.25f) else Color(0xFFE3F2FD)
        2 -> if (isDark) Color(0xFF1B5E20).copy(alpha = 0.25f) else Color(0xFFE8F5E9)
        3 -> if (isDark) Color(0xFFE65100).copy(alpha = 0.2f) else Color(0xFFFFFDE7)
        4 -> if (isDark) Color(0xFF880E4F).copy(alpha = 0.25f) else Color(0xFFFCE4EC)
        5 -> if (isDark) Color(0xFF4A148C).copy(alpha = 0.25f) else Color(0xFFF3E5F5)
        6 -> if (isDark) Color(0xFF311B92).copy(alpha = 0.25f) else Color(0xFFEDE7F6)
        else -> MaterialTheme.colorScheme.surface
    }
}

@Composable
fun getColorName(colorId: Int?): String {
    return when (colorId) {
        1 -> stringResource(id = R.string.label_color_blue)
        2 -> stringResource(id = R.string.label_color_green)
        3 -> stringResource(id = R.string.label_color_yellow)
        4 -> stringResource(id = R.string.label_color_pink)
        5 -> stringResource(id = R.string.label_color_purple)
        6 -> stringResource(id = R.string.label_color_orange)
        else -> stringResource(id = R.string.label_color_none)
    }
}

package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.MainActivity

fun openNoteIntent(context: Context, noteId: Int): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra("open_note_id", noteId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

fun newNoteIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra("new_note", true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(SingleNoteWidget::class.java).forEach { SingleNoteWidget().update(context, it) }
        manager.getGlanceIds(NotesListWidget::class.java).forEach { NotesListWidget().update(context, it) }
        manager.getGlanceIds(QuickNoteWidget::class.java).forEach { QuickNoteWidget().update(context, it) }
    }
}

const val WIDGET_PREFS = "secure_notes_widgets"
const val SINGLE_NOTE_PREFIX = "single_note_"

fun readSingleNoteId(context: Context, glanceId: GlanceId): Int {
    val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(glanceId) }.getOrNull()
        ?: return -1
    return context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        .getInt("$SINGLE_NOTE_PREFIX$appWidgetId", -1)
}

suspend fun saveSingleNoteSelection(context: Context, glanceId: GlanceId, noteId: Int) {
    val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(glanceId) }.getOrNull()
        ?: return
    context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit()
        .putInt("$SINGLE_NOTE_PREFIX$appWidgetId", noteId)
        .apply()
    SingleNoteWidget().update(context, glanceId)
}

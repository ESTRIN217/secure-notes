package com.example.widget

import android.content.Context
import com.example.AppConstants
import com.example.R
import com.example.data.local.NoteDatabase
import com.example.data.model.Note
import com.example.data.security.SecurePrefsStore
import com.example.util.RichTextConverter

const val WIDGET_LIST_MAX = 7

fun widgetVisibleNotes(notes: List<Note>): List<Note> {
    return notes
        .filter { !it.isDeleted && !it.isArchived && !it.isEncrypted }
        .sortedByDescending { it.lastModified }
}

fun widgetTitle(context: Context, note: Note): String {
    val title = note.title.trim()
    if (title.isNotEmpty()) return title
    return context.getString(R.string.widget_note_untitled)
}

fun widgetSummary(content: String, context: Context): String {
    return RichTextConverter.contentToPlainText(content, context).trim().take(140)
}

fun isPasswordSet(context: Context): Boolean {
    val appContext = context.applicationContext
    if (SecurePrefsStore(appContext).contains(AppConstants.MASTER_PASSWORD_HASH_KEY)) return true
    return appContext.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        .contains(AppConstants.MASTER_PASSWORD_HASH_KEY)
}

suspend fun loadWidgetNotes(context: Context): List<Note> {
    val dao = NoteDatabase.getDatabase(context.applicationContext).noteDao
    return widgetVisibleNotes(dao.getAllNotes())
}

suspend fun loadNoteById(context: Context, id: Int): Note? {
    val note = NoteDatabase.getDatabase(context.applicationContext).noteDao.getNoteById(id)
    if (note == null || note.isDeleted || note.isEncrypted) return null
    return note
}

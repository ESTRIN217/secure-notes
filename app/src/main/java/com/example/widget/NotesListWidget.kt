package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.model.Note

class NotesListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = runCatching { loadWidgetNotes(context).take(WIDGET_LIST_MAX) }.getOrDefault(emptyList())
        val locked = notes.isEmpty() && isPasswordSet(context)
        provideContent {
            GlanceTheme {
                NotesListContent(context, notes, locked)
            }
        }
    }

    @Composable
    private fun NotesListContent(context: Context, notes: List<Note>, locked: Boolean) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clickable(actionStartActivity(android.content.Intent(context, MainActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(com.example.R.string.widget_list_title),
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
            when {
                notes.isNotEmpty() -> {
                    LazyColumn {
                        items(notes) { note ->
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(actionStartActivity(openNoteIntent(context, note.id)))
                            ) {
                                Text(text = widgetTitle(note), style = TextStyle(fontWeight = FontWeight.Bold), maxLines = 1)
                                val summary = widgetSummary(note.content)
                                if (summary.isNotBlank()) {
                                    Text(text = summary, maxLines = 2)
                                }
                            }
                        }
                    }
                }
                locked -> {
                    Text(text = context.getString(com.example.R.string.widget_locked_item), maxLines = 3)
                }
                else -> {
                    Text(text = context.getString(com.example.R.string.widget_empty_list), maxLines = 2)
                }
            }
        }
    }
}

class NotesListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesListWidget()
}

package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(actionStartActivity(android.content.Intent(context, MainActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(com.example.R.string.widget_list_title),
                    style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
                    maxLines = 1
                )
            }
            when {
                notes.isNotEmpty() -> {
                    LazyColumn {
                        items(notes) { note -> NoteListMiniCard(context, note) }
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

    @Composable
    private fun NoteListMiniCard(context: Context, note: Note) {
        val summary = widgetSummary(note.content, context)
        Column(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 6.dp)
                .background(GlanceTheme.colors.surfaceVariant).cornerRadius(12.dp)
                .padding(12.dp).clickable(actionStartActivity(openNoteIntent(context, note.id)))
        ) {
            Text(text = widgetTitle(context, note), style = cardTitleStyle(), maxLines = 1)
            if (summary.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(text = summary, style = cardSummaryStyle(), maxLines = 2)
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(text = formatWidgetDate(note.lastModified), style = cardDateStyle(), maxLines = 1)
        }
    }

    @Composable
    private fun cardTitleStyle() = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = GlanceTheme.colors.onSurfaceVariant
    )

    @Composable
    private fun cardSummaryStyle() = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant)

    @Composable
    private fun cardDateStyle() = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
}

private fun formatWidgetDate(lastModified: Long): String {
    return SimpleDateFormat("LLL dd, HH:mm", Locale.getDefault()).format(Date(lastModified))
}

class NotesListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesListWidget()
}

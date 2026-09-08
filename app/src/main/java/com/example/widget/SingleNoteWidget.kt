package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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

class SingleNoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val noteId = readSingleNoteId(context, id)
        val note = if (noteId > 0) {
            runCatching { loadNoteById(context, noteId) }.getOrNull()
        } else {
            null
        }
        val locked = note == null && isPasswordSet(context)
        provideContent {
            GlanceTheme {
                SingleNoteContent(context, note, locked)
            }
        }
    }

    @Composable
    private fun SingleNoteContent(context: Context, note: Note?, locked: Boolean) {
        val tapAction = if (note != null) {
            actionStartActivity(openNoteIntent(context, note.id))
        } else {
            actionStartActivity(android.content.Intent(context, MainActivity::class.java))
        }
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .clickable(tapAction),
            verticalAlignment = Alignment.Top
        ) {
            when {
                note != null -> {
                    Text(
                        text = widgetTitle(note),
                        style = TextStyle(fontWeight = FontWeight.Bold),
                        maxLines = 2
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = widgetSummary(note.content).ifBlank { "…" },
                        maxLines = 6
                    )
                }
                locked -> {
                    Text(
                        text = context.getString(com.example.R.string.widget_locked_item),
                        style = TextStyle(fontWeight = FontWeight.Bold),
                        maxLines = 4
                    )
                }
                else -> {
                    Text(
                        text = context.getString(com.example.R.string.widget_empty_note),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleNoteWidget()
}

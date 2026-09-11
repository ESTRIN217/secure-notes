package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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
        val (fabDescription, openAction) = openTarget(context, note)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                when {
                    note != null -> NoteBody(context, note)
                    locked -> StaticBody(context.getString(com.example.R.string.widget_locked_item))
                    else -> StaticBody(context.getString(com.example.R.string.widget_empty_note))
                }
            }
            FloatingOpenButton(fabDescription, openAction)
        }
    }

    @Composable
    private fun ColumnScope.NoteBody(context: Context, note: Note) {
        val body = widgetSummary(note.content, context).ifBlank { "…" }
        Text(text = widgetTitle(context, note), style = noteTitleStyle(), maxLines = 2)
        Spacer(modifier = GlanceModifier.height(4.dp))
        LazyColumn(modifier = GlanceModifier.defaultWeight()) {
            items(listOf(body)) { paragraph -> Text(text = paragraph, style = noteBodyStyle()) }
        }
        Spacer(modifier = GlanceModifier.height(64.dp))
    }

    @Composable
    private fun StaticBody(message: String) {
        Text(text = message, style = noteTitleStyle(), maxLines = 4)
    }

    @Composable
    private fun FloatingOpenButton(description: String, onClick: Action) {
        CircleIconButton(
            imageProvider = ImageProvider(com.example.R.drawable.open_in_new_24px),
            contentDescription = description,
            onClick = onClick
        )
    }

    @Composable
    private fun noteTitleStyle() = TextStyle(
        fontWeight = FontWeight.Bold,
        color = GlanceTheme.colors.onSurface
    )

    @Composable
    private fun noteBodyStyle() = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
}

private fun openTarget(context: Context, note: Note?): Pair<String, Action> {
    if (note != null) {
        return context.getString(com.example.R.string.widget_open_note) to
            actionStartActivity(openNoteIntent(context, note.id))
    }
    return context.getString(com.example.R.string.floating_mode_open_app) to
        actionStartActivity(android.content.Intent(context, MainActivity::class.java))
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleNoteWidget()
}

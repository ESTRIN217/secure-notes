package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.model.Note
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class SingleNoteConfigActivity : ComponentActivity() {

    private var notes by mutableStateOf<List<Note>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@SingleNoteConfigActivity).getGlanceIdBy(appWidgetId)
            notes = runCatching { loadWidgetNotes(this@SingleNoteConfigActivity) }.getOrDefault(emptyList())
            setContent {
                MyApplicationTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ConfigList { note ->
                            lifecycleScope.launch {
                                saveSingleNoteSelection(this@SingleNoteConfigActivity, glanceId, note.id)
                                val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                setResult(RESULT_OK, result)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ConfigList(onSelect: (Note) -> Unit) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.widget_config_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (notes.isEmpty()) {
                Text(text = stringResource(R.string.widget_empty_list))
                return
            }
            LazyColumn {
                items(notes, key = { it.id }) { note ->
                    ListItem(
                        headlineContent = { Text(widgetTitle(note)) },
                        supportingContent = { Text(widgetSummary(note.content), maxLines = 2) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(note) }
                    )
                }
            }
        }
    }
}

package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Release(
    val version: String,
    val title: String,
    val body: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(onDismiss: () -> Unit) {
    var releases by remember { mutableStateOf<List<Release>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val url = URL("https://api.github.com/repos/ESTRIN217/secure-notes/releases?per_page=100")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val response = withContext(Dispatchers.IO) {
                connection.inputStream.bufferedReader().readText()
            }
            val jsonArray = JSONArray(response)
            val list = mutableListOf<Release>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val version = obj.optString("tag_name", "v?").removePrefix("v")
                val title = obj.optString("name", "")
                val body = obj.optString("body", "")
                val rawDate = obj.optString("published_at", "")
                val date = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate
                list.add(Release(version, title, body, date))
            }
            releases = list
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            error = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_changelog),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.changelog_loading))
                        }
                    }
                }
                error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    if (releases.isEmpty()) {
                        Text(
                            text = stringResource(R.string.changelog_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(releases) { release ->
                                ChangelogReleaseItem(release = release)
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/ESTRIN217/secure-notes/releases")
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.about_github))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogReleaseItem(release: Release) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsBadge(text = "v${release.version}")
            Text(
                text = release.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        SettingsCardGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (release.title.isNotEmpty() && release.title != release.version && release.title != "v${release.version}") {
                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                MarkdownText(
                    markdown = release.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    linkColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

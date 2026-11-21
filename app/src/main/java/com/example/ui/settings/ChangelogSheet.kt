package com.example.ui.settings

import androidx.compose.foundation.layout.WindowInsets
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.estrin217.markdown.MarkdownEngine
import com.estrin217.markdown.MarkdownText
import com.example.ui.markdown.markdownColors
import com.example.R
import com.example.data.AppUpdateConfig
import com.example.data.updates.GithubRelease
import com.example.data.updates.UpdateFetcher

private const val TAG = "ChangelogSheet"

/**
 * Spec 044 enmienda, RF-338. Las notas de cada release, pintadas con el motor de Markdown (spec 018,
 * RF-105). El texto se entrega **literal**: las notas se escriben en GitHub, donde el HTML crudo es
 * legítimo; pasarlas por la adaptación del chat retiraría etiquetas que aquí sí significan algo.
 *
 * Antes vivía en un `ModalBottomSheet`; ahora el mismo contenido se reutiliza en el panel de detalle.
 * El motor se provee una sola vez por encima de la lista, nunca por release.
 */
@Composable
fun ChangelogContent(modifier: Modifier = Modifier) {
    var releases by remember { mutableStateOf<List<GithubRelease>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            releases = UpdateFetcher().fetchReleases()
        } catch (e: Exception) {
            Log.e(TAG, "failed to load changelog", e)
            error = true
        }
        isLoading = false
    }

    MarkdownEngine.ProvidesMarkdownEngine(colores = markdownColors()) {
        Column(modifier = modifier.fillMaxWidth()) {
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
                releases.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.changelog_empty),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(releases) { release -> ChangelogReleaseItem(release = release) }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(AppUpdateConfig.releasesWebUrl)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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

/** Pantalla del registro de cambios en el panel de detalle (RF-338). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen() {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        ChangelogContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ChangelogReleaseItem(release: GithubRelease) {
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
            SettingsBadge(text = stringResource(R.string.update_current_version_label, release.version))
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
                    color = MaterialTheme.colorScheme.onSurface,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

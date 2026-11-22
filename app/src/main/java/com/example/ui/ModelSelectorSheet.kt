package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.AiBackend
import com.example.data.ai.DownloadState
import com.example.data.ai.ModelState
import com.example.data.ai.OnDeviceModel
import com.example.ui.chat.favoriteOllamaKey
import com.example.ui.chat.favoriteOnDeviceKey
import com.example.ui.chat.favoritesFirst
import com.example.ui.chat.filterOllamaModels
import com.example.ui.chat.filterOnDeviceModels

/**
 * Estado del selector de modelo (spec 033, RF-223): plano, ya recolectado por la pantalla.
 * Sin `StateFlow` ni ViewModel dentro.
 */
data class ModelSelectorState(
    val backend: AiBackend,
    val selectedOnDeviceId: String?,
    /** Ids de modelos on-device cuyo `.gguf` está en disco ahora mismo. */
    val downloadedIds: Set<String>,
    /** Catálogo completo a listar (no solo los descargados, RF-230). */
    val catalogModels: List<OnDeviceModel>,
    val modelName: String,
    /** Última lista `/api/tags` conocida (vacía si nunca hubo conexión); se usa sin llamada a red. */
    val ollamaModels: List<String>,
    val isTesting: Boolean,
    /** Error de la última prueba de conexión, ya localizado por quien lo pasa; `null` si no hay. */
    val connectionError: String?,
    /** Claves de favorito (`gguf:<id>` / `ollama:<nombre>`), RF-233. */
    val favorites: Set<String>,
    /** Id del modelo cuya descarga está en curso; `null` si no hay ninguna (RF-232). */
    val downloadingModelId: String?,
    /** Estado de la descarga activa (progreso); [DownloadState.Idle] si no hay. */
    val downloadState: DownloadState = DownloadState.Idle,
    /** Estado del motor on-device (RF-231): `READY` compromete el ciclo de vida del modelo activo. */
    val onDeviceModelState: ModelState = ModelState.NOT_LOADED,
    /** Claves de modelos ocultos (RF-244): se quitan de catálogo, hoja y buscador. */
    val hiddenModels: Set<String> = emptySet(),
    /** `context_length` por modelo de Ollama desde `/api/show` (RAM/proceso; puede estar vacía). */
    val contextLengths: Map<String, Long> = emptyMap()
)

/**
 * Selector de modelo del chat (spec 033, RF-222…RF-237). Se monta dentro del campo, junto al botón:
 * [ModelSelectorSheet] en <600 dp; el panel completo (≥600 dp) vive en `ModelManagerPanel.kt`.
 *
 * No llama a la red por sí mismo: «Probar conexión» lo hace vía [onTestConnection]; descargar o
 * eliminar usa las acciones del ViewModel; la búsqueda y el orden son puros (`ui/chat/ModelSearch.kt`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModelSelectorSheet(
    state: ModelSelectorState,
    onSelectOnDevice: (OnDeviceModel) -> Unit,
    onSelectOllama: (String) -> Unit,
    onTestConnection: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadModel: (OnDeviceModel) -> Unit,
    onLoadModel: (OnDeviceModel) -> Unit,
    onUnloadModel: () -> Unit,
    onShowInfo: (OnDeviceModel) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredCatalog = remember(state.catalogModels, query, state.favorites, state.hiddenModels) {
        favoritesFirst(
            filterOnDeviceModels(state.catalogModels, query).filterNot { favoriteOnDeviceKey(it.id) in state.hiddenModels }
        ) { favoriteOnDeviceKey(it.id) in state.favorites }
    }
    val filteredOllama = remember(state.ollamaModels, query, state.favorites, state.hiddenModels) {
        favoritesFirst(
            filterOllamaModels(state.ollamaModels, query).filterNot { favoriteOllamaKey(it) in state.hiddenModels }
        ) { favoriteOllamaKey(it) in state.favorites }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item { SectionTitle(stringResource(R.string.chat_model_selector_title)) }
            item { SearchField(query = query, onQueryChange = { query = it }) }
            if (filteredCatalog.isEmpty() && filteredOllama.isEmpty()) {
                item { EmptyNote(stringResource(R.string.chat_model_no_results, query)) }
            }
            item { SectionHeader(stringResource(R.string.chat_model_on_device)) }
            if (filteredCatalog.isEmpty()) {
                item { EmptyNote(stringResource(R.string.chat_model_none_downloaded)) }
            } else {
                items(filteredCatalog, key = { it.id }) { model ->
                    val downloaded = model.id in state.downloadedIds
                    OnDeviceRow(
                        model = model,
                        selected = state.backend == AiBackend.ON_DEVICE && state.selectedOnDeviceId == model.id,
                        downloaded = downloaded,
                        favorite = favoriteOnDeviceKey(model.id) in state.favorites,
                        isDownloadingThis = state.downloadingModelId == model.id,
                        isAnyDownloadActive = state.downloadingModelId != null,
                        downloadState = state.downloadState,
                        engineState = state.onDeviceModelState,
                        iconSize = 40.dp,
                        onRowClick = {
                            if (downloaded) {
                                onSelectOnDevice(model)
                                onDismiss()
                            } else {
                                onDownloadModel(model)
                            }
                        },
                        onToggleFavorite = { onToggleFavorite(favoriteOnDeviceKey(model.id)) },
                        onShowInfo = { onShowInfo(model) },
                        onLifecycle = {
                            if (downloaded) {
                                if (state.onDeviceModelState == ModelState.READY && state.selectedOnDeviceId == model.id) {
                                    onUnloadModel()
                                } else {
                                    onLoadModel(model)
                                }
                            } else {
                                onDownloadModel(model)
                            }
                        }
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.chat_model_ollama)) }
            if (state.ollamaModels.isEmpty()) {
                item {
                    OllamaRow(
                        name = state.modelName.ifBlank { stringResource(R.string.chat_model_none) },
                        selected = state.backend == AiBackend.OLLAMA,
                        favorite = favoriteOllamaKey(state.modelName) in state.favorites,
                        iconSize = 40.dp,
                        onRowClick = {
                            onSelectOllama(state.modelName)
                            onDismiss()
                        },
                        onToggleFavorite = { onToggleFavorite(favoriteOllamaKey(state.modelName)) }
                    )
                }
                item { TestConnectionRow(state = state, onTestConnection = onTestConnection) }
            } else {
                items(filteredOllama, key = { it }) { name ->
                    OllamaRow(
                        name = name,
                        selected = state.backend == AiBackend.OLLAMA && state.modelName == name,
                        favorite = favoriteOllamaKey(name) in state.favorites,
                        iconSize = 40.dp,
                        onRowClick = {
                            onSelectOllama(name)
                            onDismiss()
                        },
                        onToggleFavorite = { onToggleFavorite(favoriteOllamaKey(name)) }
                    )
                }
            }
        }
    }

    // El ⓘ de las filas on-device sube por `onShowInfo` (spec 040, RF-281); el diálogo anterior se eliminó.
}

@Composable
private fun ModelText(model: OnDeviceModel, onClick: () -> Unit, modifier: Modifier) {
    val ram = stringResource(R.string.ai_model_min_ram, model.fileSizeMb, model.minRamMb)
    val subtitle = listOf(model.quantLabel, ram).filter { it.isNotBlank() }.joinToString(" · ")
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(text = model.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FavoriteButton(favorite: Boolean, modelName: String, size: androidx.compose.ui.unit.Dp, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(size)) {
        Icon(
            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(R.string.chat_model_favorite_desc, modelName),
            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * El control de ciclo de vida del modelo on-device (RF-231), como «Cargar modelo»/«Descargar
 * modelo» de Ajustes → IA, pero en una fila y con iconos: sin archivo → `Download` (descarga);
 * en disco y no cargado → `PlayArrow` (cargar); cargado ahora → `Stop` (descargar);
 * `LOADING` en la fila activa → spinner deshabilitado.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LifecycleButton(
    downloaded: Boolean,
    isSelected: Boolean,
    engineState: ModelState,
    isDownloadingThis: Boolean,
    isAnyDownloadActive: Boolean,
    downloadState: DownloadState,
    modelName: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    if (isDownloadingThis) {
        val progress = (downloadState as? DownloadState.Downloading)?.progress
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.size(size).padding(start = 4.dp)) {
            if (progress != null) {
                androidx.compose.material3.LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp)
                )
            } else {
                androidx.compose.material3.LoadingIndicator(modifier = Modifier.size(24.dp))
            }
        }
        return
    }
    if (engineState == ModelState.LOADING && isSelected) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.size(size).padding(start = 4.dp)) {
            androidx.compose.material3.LoadingIndicator(modifier = Modifier.size(24.dp))
        }
        return
    }
    val isActiveLoaded = engineState == ModelState.READY && isSelected
    IconButton(onClick = onClick, enabled = !isAnyDownloadActive || downloaded, modifier = Modifier.size(size)) {
        Icon(
            imageVector = when {
                !downloaded -> Icons.Default.Download
                isActiveLoaded -> Icons.Default.Stop
                else -> Icons.Default.PlayArrow
            },
            contentDescription = stringResource(
                when {
                    !downloaded -> R.string.chat_model_download_desc
                    isActiveLoaded -> R.string.chat_model_unload_desc
                    else -> R.string.chat_model_load_desc
                },
                modelName
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OllamaRow(
    name: String,
    selected: Boolean,
    favorite: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    onRowClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
        RadioButton(selected = selected, onClick = onRowClick)
        Column(modifier = Modifier.weight(1f).clickable(onClick = onRowClick).padding(vertical = 8.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FavoriteButton(favorite = favorite, modelName = name, size = iconSize, onToggle = onToggleFavorite)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TestConnectionRow(state: ModelSelectorState, onTestConnection: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        state.connectionError?.let { error ->
            Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            if (state.isTesting) {
                LoadingIndicator(modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.chat_model_testing), style = MaterialTheme.typography.bodyMedium)
            } else {
                OutlinedButton(onClick = onTestConnection) {
                    Text(stringResource(R.string.chat_model_test_connection))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))

@Composable
private fun SectionHeader(text: String) =
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))

@Composable
private fun EmptyNote(text: String) =
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))


@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.chat_model_search_hint)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun OnDeviceRow(
    model: OnDeviceModel,
    selected: Boolean,
    downloaded: Boolean,
    favorite: Boolean,
    isDownloadingThis: Boolean,
    isAnyDownloadActive: Boolean,
    downloadState: DownloadState,
    engineState: ModelState,
    iconSize: androidx.compose.ui.unit.Dp,
    onRowClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowInfo: () -> Unit,
    onLifecycle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
        RadioButton(selected = selected, onClick = onRowClick)
        ModelText(model = model, onClick = onRowClick, modifier = Modifier.weight(1f))
        FavoriteButton(favorite = favorite, modelName = model.displayName, size = iconSize, onToggle = onToggleFavorite)
        IconButton(onClick = onShowInfo, modifier = Modifier.size(iconSize)) {
            Icon(Icons.Default.Info, contentDescription = stringResource(R.string.chat_model_info_desc, model.displayName), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LifecycleButton(
            downloaded = downloaded,
            isSelected = selected,
            engineState = engineState,
            isDownloadingThis = isDownloadingThis,
            isAnyDownloadActive = isAnyDownloadActive,
            downloadState = downloadState,
            modelName = model.displayName,
            size = iconSize,
            onClick = onLifecycle
        )
    }
}

package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.AiBackend
import com.example.data.ai.DownloadState
import com.example.data.ai.ModelState
import com.example.data.ai.OnDeviceModel
import com.example.ui.chat.LLAMA_CPP_CONTEXT_TOKENS
import com.example.ui.chat.favoriteOllamaKey
import com.example.ui.chat.favoriteOnDeviceKey
import com.example.ui.chat.favoritesFirst
import com.example.ui.chat.filterOllamaModels
import com.example.ui.chat.filterOnDeviceModels
import com.example.ui.chat.formatTokens
import com.example.ui.chat.loadedContextLabel
import java.util.Locale

/**
 * Panel de gestión de modelos (spec 033, RF-238…RF-247): se monta dentro del assistant en
 * pantallas grandes (≥600 dp). Categorías plegadas, buscador en vivo, tabla
 * Model · Context · Status · Actions con el mismo contrato que el selector del chat.
 *
 * Sin VM ni Estado: recibe [ModelSelectorState] y callbacks; la decisión de ocultar/eliminar/cargar
 * viaja por eventos y la toma el ViewModel. Solo pinta y lanza eventos (Constitución #3).
 */
/** Pesos de columnas de la tabla de modelos (spec 046 enmienda, RF-372/RNF-225). */
private object ModelColumns {
    const val CONTEXT_WEIGHT = 0.22f
    const val STATUS_WEIGHT = 0.18f
    const val ACTIONS_WEIGHT = 0.22f
    val MIN_CONTEXT = 72.dp
    val MIN_STATUS = 64.dp
    val MIN_ACTIONS = 88.dp
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModelManagerPanel(
    state: ModelSelectorState,
    onClose: () -> Unit,
    onSelectOnDevice: (OnDeviceModel) -> Unit,
    onSelectOllama: (String) -> Unit,
    onDownloadModel: (OnDeviceModel) -> Unit,
    onLoadModel: (OnDeviceModel) -> Unit,
    onUnloadModel: () -> Unit,
    onDeleteModel: (OnDeviceModel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleHidden: (String) -> Unit,
    onShowInfo: (OnDeviceModel) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }
    val configuration = LocalConfiguration.current
    val formatLocale = configuration.locales[0] ?: java.util.Locale.getDefault()
    var query by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<OnDeviceModel?>(null) }
    var collapsed by remember { mutableStateOf(setOf<String>()) }
    val visibleLocal = remember(state.catalogModels, state.hiddenModels, query, state.favorites) {
        favoritesFirst(
            filterOnDeviceModels(state.catalogModels, query)
                .filterNot { favoriteOnDeviceKey(it.id) in state.hiddenModels }
        ) { favoriteOnDeviceKey(it.id) in state.favorites }
    }
    val hiddenLocal = remember(state.catalogModels, state.hiddenModels, query) {
        filterOnDeviceModels(state.catalogModels, query).filter { favoriteOnDeviceKey(it.id) in state.hiddenModels }
    }
    val visibleOllama = remember(state.ollamaModels, state.hiddenModels, query, state.favorites) {
        favoritesFirst(
            filterOllamaModels(state.ollamaModels, query).filterNot { favoriteOllamaKey(it) in state.hiddenModels }
        ) { favoriteOllamaKey(it) in state.favorites }
    }
    val hiddenOllama = remember(state.ollamaModels, state.hiddenModels, query) {
        filterOllamaModels(state.ollamaModels, query).filter { favoriteOllamaKey(it) in state.hiddenModels }
    }
    val contextLengths = state.contextLengths

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PanelTopBar(onClose = onClose)
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                PanelHeader()
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.chat_model_search_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                )
                TableHeaderRow()
                HorizontalDivider()
                val localTitle = stringResource(R.string.models_local_models, visibleLocal.size)
                val ollamaTitle = stringResource(R.string.chat_model_ollama) + " " + visibleOllama.size
                LazyColumn(modifier = Modifier.weight(1f)) {
                    sectionItem(
                        title = localTitle,
                        sectionId = "local",
                        collapsed = collapsed.contains("local"),
                        onToggleCollapse = { _ -> collapsed = collapsed.toggle("local") }
                    )
                    if (!collapsed.contains("local")) {
                        if (!showHidden) {
                            items(visibleLocal, key = { it.id }) { model ->
                                LocalModelRow(
                                    model = model,
                                    state = state,
                                    contextLabel = localContextLabel(model, state, contextLengths, formatLocale),
                                    onRowClick = {
                                        if (model.id in state.downloadedIds) {
                                            onSelectOnDevice(model)
                                            onClose()
                                        } else {
                                            onDownloadModel(model)
                                        }
                                    },
                                    onStatusAction = statusAction(model, state, onDownloadModel, onLoadModel, onUnloadModel),
                                    onToggleFavorite = { onToggleFavorite(favoriteOnDeviceKey(model.id)) },
                                    onToggleHidden = { onToggleHidden(favoriteOnDeviceKey(model.id)) },
                                    onDelete = { deleteTarget = model },
                                    onShowInfo = { onShowInfo(model) }
                                )
                            }
                        } else {
                            items(hiddenLocal, key = { it.id }) { model ->
                                LocalModelRow(
                                    model = model,
                                    state = state,
                                    contextLabel = contextLengths[favoriteOnDeviceKey(model.id)]?.let { formatTokens(it, formatLocale) } ?: "—",
                                    isHiddenView = true,
                                    onRowClick = {},
                                    onStatusAction = {},
                                    onToggleFavorite = { onToggleFavorite(favoriteOnDeviceKey(model.id)) },
                                    onToggleHidden = { onToggleHidden(favoriteOnDeviceKey(model.id)) },
                                    onDelete = {},
                                    onShowInfo = {}
                                )
                            }
                        }
                    }
                    sectionItem(
                        title = ollamaTitle,
                        sectionId = "ollama",
                        collapsed = collapsed.contains("ollama"),
                        onToggleCollapse = { _ -> collapsed = collapsed.toggle("ollama") }
                    )
                    if (!collapsed.contains("ollama")) {
                        items(visibleOllama, key = { it }) { name ->
                            val context = contextLengths[favoriteOllamaKey(name)]?.let { formatTokens(it, formatLocale) } ?: "—"
                            OllamaModelRow(
                                name = name,
                                state = state,
                                contextLabel = context,
                                inHiddenView = false,
                                onRowClick = {
                                    onSelectOllama(name)
                                    onClose()
                                },
                                onToggleFavorite = { onToggleFavorite(favoriteOllamaKey(name)) }
                            )
                        }
                        items(hiddenOllama, key = { "hidden_$it" }) { name ->
                            val context = contextLengths[favoriteOllamaKey(name)]?.let { formatTokens(it, formatLocale) } ?: "—"
                            OllamaModelRow(
                                name = name,
                                state = state,
                                contextLabel = context,
                                inHiddenView = true,
                                onRowClick = {},
                                onToggleFavorite = { onToggleFavorite(favoriteOllamaKey(name)) },
                                onToggleHidden = { onToggleHidden(favoriteOllamaKey(name)) }
                            )
                        }
                    }
                    if (query.isBlank().not() && visibleLocal.isEmpty() && visibleOllama.isEmpty() && !showHidden) {
                        item { EmptyNote(stringResource(R.string.chat_model_no_results, query)) }
                    }
                }
                HiddenToggleRow(
                    showHidden = showHidden,
                    hiddenCount = hiddenLocal.size + hiddenOllama.size,
                    onToggle = { showHidden = !showHidden }
                )
            }
        }
    }

    deleteTarget?.let { target ->
        DeleteModelDialog(
            model = target,
            onConfirm = {
                onDeleteModel(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun PanelTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PanelHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.models_panel_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.models_panel_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.models_col_model), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.models_col_context), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(ModelColumns.CONTEXT_WEIGHT).widthIn(min = ModelColumns.MIN_CONTEXT))
        Text(stringResource(R.string.models_col_status), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(ModelColumns.STATUS_WEIGHT).widthIn(min = ModelColumns.MIN_STATUS))
        Text(stringResource(R.string.models_col_actions), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(ModelColumns.ACTIONS_WEIGHT).widthIn(min = ModelColumns.MIN_ACTIONS))
    }
}

private fun statusAction(
    model: OnDeviceModel,
    state: ModelSelectorState,
    onDownload: (OnDeviceModel) -> Unit,
    onLoad: (OnDeviceModel) -> Unit,
    onUnload: () -> Unit
): () -> Unit = {
    when {
        model.id !in state.downloadedIds -> onDownload(model)
        state.onDeviceModelState == ModelState.READY && state.selectedOnDeviceId == model.id -> onUnload()
        else -> onLoad(model)
    }
}

/** Celda Context: la del modelo, o «motor / modelo» cuando es el activo y está cargado (RF-314/RF-315). */
@Composable
private fun localContextLabel(
    model: OnDeviceModel,
    state: ModelSelectorState,
    contextLengths: Map<String, Long>,
    locale: Locale
): String {
    val own = contextLengths[favoriteOnDeviceKey(model.id)]
    val isEngineActive = state.backend == AiBackend.ON_DEVICE &&
        state.selectedOnDeviceId == model.id &&
        state.onDeviceModelState == ModelState.READY
    if (!isEngineActive) return own?.let { formatTokens(it, locale) } ?: "—"
    val parts = loadedContextLabel(LLAMA_CPP_CONTEXT_TOKENS, own)
    return stringResource(R.string.models_context_split, parts.engine, parts.own)
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionItem(
    title: String,
    sectionId: String,
    collapsed: Boolean,
    onToggleCollapse: (String) -> Unit
) {
    item {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleCollapse(sectionId) }.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(
                    if (collapsed) Icons.Default.ChevronRight else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun Set<String>.toggle(sectionId: String): Set<String> =
    if (sectionId in this) this - sectionId else this + sectionId

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocalModelRow(
    model: OnDeviceModel,
    state: ModelSelectorState,
    contextLabel: String,
    isHiddenView: Boolean = false,
    onRowClick: () -> Unit,
    onStatusAction: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    val downloaded = model.id in state.downloadedIds
    val isSelected = state.backend == AiBackend.ON_DEVICE && state.selectedOnDeviceId == model.id
    val favorite = favoriteOnDeviceKey(model.id) in state.favorites
    val downloadingThis = state.downloadingModelId == model.id
    val rowMod = if (isHiddenView || downloadingThis) Modifier else Modifier.clickable(onClick = onRowClick)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (isHiddenView) {
            Spacer(modifier = Modifier.width(36.dp))
        } else {
            FavoriteHeart(favorite = favorite, onToggle = onToggleFavorite, modelName = model.displayName, size = 36.dp)
        }
        Column(modifier = rowMod.weight(1f).padding(horizontal = 4.dp)) {
            Text(text = model.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (model.quantLabel.isNotBlank()) Chip(model.quantLabel)
                Chip("${model.fileSizeMb} MB")
            }
        }
        Text(contextLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(ModelColumns.CONTEXT_WEIGHT).widthIn(min = ModelColumns.MIN_CONTEXT))
        Box(modifier = Modifier.weight(ModelColumns.STATUS_WEIGHT).widthIn(min = ModelColumns.MIN_STATUS), contentAlignment = Alignment.Center) {
            if (isHiddenView) {
                UnhideButton(onToggleHidden = onToggleHidden, modelName = model.displayName)
            } else {
                StatusControl(
                    downloaded = downloaded,
                    isSelected = isSelected,
                    downloadingThis = downloadingThis,
                    engineState = state.onDeviceModelState,
                    downloadState = state.downloadState,
                    modelName = model.displayName,
                    onAction = onStatusAction
                )
            }
        }
        if (isHiddenView) {
            Spacer(modifier = Modifier.weight(ModelColumns.ACTIONS_WEIGHT).widthIn(min = ModelColumns.MIN_ACTIONS))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(ModelColumns.ACTIONS_WEIGHT).widthIn(min = ModelColumns.MIN_ACTIONS)
            ) {
                IconButton(onClick = onShowInfo, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.chat_model_info_desc, model.displayName),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RowActionsMenu(
                    model = model,
                    favorite = favorite,
                    onToggleFavorite = onToggleFavorite,
                    onToggleHidden = onToggleHidden,
                    onDelete = onDelete
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun OllamaModelRow(
    name: String,
    state: ModelSelectorState,
    contextLabel: String,
    inHiddenView: Boolean,
    onRowClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: (() -> Unit)? = null
) {
    val favorite = favoriteOllamaKey(name) in state.favorites
    val selected = state.backend == AiBackend.OLLAMA && state.modelName == name
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (inHiddenView) Modifier else Modifier.clickable(onClick = onRowClick))
            .padding(vertical = 10.dp)
    ) {
        if (inHiddenView) {
            UnhideButton(onToggleHidden = onToggleHidden ?: {}, modelName = name)
        } else {
            FavoriteHeart(favorite = favorite, onToggle = onToggleFavorite, modelName = name, size = 36.dp)
        }
        Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
        Text(contextLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(ModelColumns.CONTEXT_WEIGHT).widthIn(min = ModelColumns.MIN_CONTEXT))
        Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(84.dp))
        Spacer(modifier = Modifier.weight(ModelColumns.ACTIONS_WEIGHT).widthIn(min = ModelColumns.MIN_ACTIONS))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusControl(
    downloaded: Boolean,
    isSelected: Boolean,
    downloadingThis: Boolean,
    engineState: ModelState,
    downloadState: DownloadState,
    modelName: String,
    onAction: () -> Unit
) {
    if (downloadingThis) {
        val progress = (downloadState as? DownloadState.Downloading)?.progress
        if (progress != null) {
            LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.size(22.dp))
        } else {
            LoadingIndicator(modifier = Modifier.size(22.dp))
        }
        return
    }
    if (isSelected && engineState == ModelState.LOADING) {
        LoadingIndicator(modifier = Modifier.size(22.dp))
        return
    }
    val isActive = isSelected && engineState == ModelState.READY
    val isLoadError = isSelected && engineState == ModelState.ERROR
    IconButton(onClick = onAction, enabled = downloaded) {
        Icon(
            imageVector = if (downloaded) Icons.Default.PowerSettingsNew else Icons.Default.Download,
            contentDescription = stringResource(
                if (!downloaded) R.string.chat_model_download_desc
                else if (isActive) R.string.chat_model_unload_desc
                else R.string.chat_model_load_desc,
                modelName
            ),
            tint = if (isActive) MaterialTheme.colorScheme.primary else if (isLoadError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun FavoriteHeart(favorite: Boolean, onToggle: () -> Unit, modelName: String, size: androidx.compose.ui.unit.Dp) {
    IconButton(onClick = onToggle, modifier = Modifier.size(size)) {
        Icon(
            imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(R.string.chat_model_favorite_desc, modelName),
            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowActionsMenu(
    model: OnDeviceModel,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                leadingIcon = { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                text = { Text(stringResource(if (favorite) R.string.models_remove_favorite else R.string.models_add_favorite)) },
                onClick = {
                    expanded = false
                    onToggleFavorite()
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                text = { Text(stringResource(R.string.models_delete_disk), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                text = { Text(stringResource(R.string.models_hide_model)) },
                onClick = {
                    expanded = false
                    onToggleHidden()
                }
            )
        }
    }
}

@Composable
private fun UnhideButton(onToggleHidden: () -> Unit, modelName: String) {
    IconButton(onClick = onToggleHidden, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = stringResource(R.string.models_show_model),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HiddenToggleRow(showHidden: Boolean, hiddenCount: Int, onToggle: () -> Unit) {
    if (hiddenCount == 0) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedButton(onClick = onToggle) {
            Text(stringResource(if (showHidden) R.string.models_show_all else R.string.models_show_hidden, hiddenCount))
        }
    }
}

@Composable
private fun Chip(text: String) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyNote(text: String) =
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp))

@Composable
private fun DeleteModelDialog(model: OnDeviceModel, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.models_delete_title)) },
        text = { Text(stringResource(R.string.models_delete_body, model.displayName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.models_delete_disk)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

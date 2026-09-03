package com.estrin217.pdfviewer.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.fragment.compose.AndroidFragment
import androidx.pdf.viewer.fragment.PdfViewerFragment
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.pdfviewer.R
import com.estrin217.pdfviewer.data.PageDimension
import com.estrin217.pdfviewer.data.PdfFileInfo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Invert color matrix for Night / Dark Reading Mode
private val InvertColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: PdfViewerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val isNightMode by viewModel.isNightMode.collectAsState()
    val controlsVisible by viewModel.controlsVisible.collectAsState()
    val showJumpDialog by viewModel.showJumpDialog.collectAsState()
    val showInfoDialog by viewModel.showInfoDialog.collectAsState()
    val useAndroidXPdf by viewModel.useAndroidXPdf.collectAsState()
    var pdfFragment by remember { mutableStateOf<PdfViewerFragment?>(null) }


    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Sync current page with scroll position
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                viewModel.setCurrentPage(index)
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isNightMode) Color(0xFF101114) else MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = controlsVisible && uiState is PdfViewerUiState.Loaded,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                if (uiState is PdfViewerUiState.Loaded) {
                    val loaded = uiState as PdfViewerUiState.Loaded
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = loaded.fileInfo.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = stringResource(
                                        R.string.page_counter,
                                        currentPage + 1,
                                        loaded.pageCount
                                    ) + " • " + loaded.fileInfo.sizeFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { viewModel.closeDocument() },
                                modifier = Modifier.testTag("close_document_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.close)
                                )
                            }
                        },
                        actions = {
                            if (useAndroidXPdf) {
                                // Search in document via androidx.pdf
                                IconButton(
                                    onClick = {
                                        pdfFragment?.let {
                                            it.isTextSearchActive = !it.isTextSearchActive
                                        }
                                    },
                                    modifier = Modifier.testTag("androidx_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FindInPage,
                                        contentDescription = stringResource(R.string.search_in_document)
                                    )
                                }
                            } else {
                                // Night mode toggle (classic view)
                                IconButton(
                                    onClick = { viewModel.toggleNightMode() },
                                    modifier = Modifier.testTag("night_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = stringResource(R.string.night_mode),
                                        tint = if (isNightMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                // Jump to page (classic view)
                                IconButton(
                                    onClick = { viewModel.showJumpToPageDialog(true) },
                                    modifier = Modifier.testTag("jump_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FindInPage,
                                        contentDescription = stringResource(R.string.jump_to_page)
                                    )
                                }
                            }
                            // Toggle between Jetpack androidx.pdf and Classic reader mode
                            IconButton(
                                onClick = { viewModel.toggleViewerEngine(context) },
                                modifier = Modifier.testTag("toggle_engine_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = stringResource(if (useAndroidXPdf) R.string.viewer_mode_androidx else R.string.viewer_mode_classic),
                                    tint = if (useAndroidXPdf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // File Info
                            IconButton(
                                onClick = { viewModel.showFileInfoDialog(true) },
                                modifier = Modifier.testTag("file_info_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = stringResource(R.string.file_info)
                                )
                            }
                            // Share
                            IconButton(
                                onClick = { viewModel.shareCurrentPdf(context) },
                                modifier = Modifier.testTag("share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.share_file)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (controlsVisible && uiState is PdfViewerUiState.Loaded) innerPadding.calculateTopPadding() else 0.dp,
                    bottom = 0.dp
                )
        ) {
            when (val state = uiState) {
                is PdfViewerUiState.Empty -> {
                }

                is PdfViewerUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                is PdfViewerUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.widthIn(max = 480.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = stringResource(R.string.error_loading_pdf),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.closeDocument() }
                                    ) {
                                        Text("Volver al inicio")
                                    }
                                }
                            }
                        }
                    }
                }

                is PdfViewerUiState.Loaded -> {
                    if (useAndroidXPdf) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("androidx_pdf_container")
                        ) {
                            AndroidFragment<PdfViewerFragment>(
                                modifier = Modifier.fillMaxSize(),
                                onUpdate = { fragment ->
                                    pdfFragment = fragment
                                    if (fragment.documentUri != state.uri) {
                                        fragment.documentUri = state.uri
                                    }
                                }
                            )
                        }
                    } else {
                        PdfPagesViewer(
                            viewModel = viewModel,
                            loadedState = state,
                            isNightMode = isNightMode,
                            zoomScale = zoomScale,
                            listState = listState,
                            onTapPage = { viewModel.toggleControls() },
                            onDoubleTapPage = {
                                if (zoomScale > 1.2f) viewModel.resetZoom() else viewModel.setZoom(2.0f)
                            }
                        )

                        // Floating Bottom Controls
                        AnimatedVisibility(
                            visible = controlsVisible,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            BottomViewerControls(
                                currentPage = currentPage,
                                pageCount = state.pageCount,
                                zoomScale = zoomScale,
                                onPreviousPage = {
                                    if (currentPage > 0) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(currentPage - 1)
                                        }
                                    }
                                },
                                onNextPage = {
                                    if (currentPage < state.pageCount - 1) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(currentPage + 1)
                                        }
                                    }
                                },
                                onJumpToPageClick = { viewModel.showJumpToPageDialog(true) },
                                onZoomIn = { viewModel.zoomIn() },
                                onZoomOut = { viewModel.zoomOut() },
                                onResetZoom = { viewModel.resetZoom() }
                            )
                        }
                    }
                }
            }

            // Jump to Page Dialog
            if (showJumpDialog && uiState is PdfViewerUiState.Loaded) {
                val loaded = uiState as PdfViewerUiState.Loaded
                JumpToPageDialog(
                    currentPage = currentPage,
                    pageCount = loaded.pageCount,
                    onDismiss = { viewModel.showJumpToPageDialog(false) },
                    onConfirm = { page ->
                        viewModel.showJumpToPageDialog(false)
                        coroutineScope.launch {
                            listState.animateScrollToItem(page)
                        }
                    }
                )
            }

            // File Info Dialog
            if (showInfoDialog && uiState is PdfViewerUiState.Loaded) {
                val loaded = uiState as PdfViewerUiState.Loaded
                FileInfoDialog(
                    fileInfo = loaded.fileInfo,
                    onDismiss = { viewModel.showFileInfoDialog(false) }
                )
            }
        }
    }
}

@Composable
private fun PdfPagesViewer(
    viewModel: PdfViewerViewModel,
    loadedState: PdfViewerUiState.Loaded,
    isNightMode: Boolean,
    zoomScale: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTapPage: () -> Unit,
    onDoubleTapPage: () -> Unit
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
        viewModel.setZoom(newScale)
        if (newScale > 1.05f) {
            panOffset += offsetChange
        } else {
            panOffset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTapPage() },
                    onDoubleTap = { onDoubleTapPage() }
                )
            }
            .transformable(state = transformState)
            .graphicsLayer {
                scaleX = zoomScale
                scaleY = zoomScale
                translationX = panOffset.x
                translationY = panOffset.y
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(loadedState.dimensions) { pageIndex, dimension ->
                PdfPageItem(
                    pageIndex = pageIndex,
                    dimension = dimension,
                    viewModel = viewModel,
                    isNightMode = isNightMode,
                    totalPages = loadedState.pageCount
                )
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    dimension: PageDimension,
    viewModel: PdfViewerViewModel,
    isNightMode: Boolean,
    totalPages: Int
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp),
        contentAlignment = Alignment.Center
    ) {
        val availableWidthPx = with(density) { maxWidth.toPx().roundToInt() }
        val aspectRatio = dimension.height.toFloat() / dimension.width.toFloat().coerceAtLeast(1f)
        val pageHeightDp = maxWidth * aspectRatio

        val bitmap by produceState<Bitmap?>(initialValue = viewModel.getCachedPage(pageIndex), key1 = pageIndex) {
            value = viewModel.renderPage(pageIndex, availableWidthPx)
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNightMode) Color(0xFF1E1F23) else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeightDp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = bitmap
                if (currentBitmap != null && !currentBitmap.isRecycled) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.page_label, pageIndex + 1),
                        contentScale = ContentScale.Fit,
                        colorFilter = if (isNightMode) ColorFilter.colorMatrix(InvertColorMatrix) else null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Page badge on bottom right corner
                Surface(
                    color = (if (isNightMode) Color.White else Color.Black).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "${pageIndex + 1} / $totalPages",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isNightMode) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomViewerControls(
    currentPage: Int,
    pageCount: Int,
    zoomScale: Float,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onJumpToPageClick: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit
) {
    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(12.dp, shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Previous Page Button
            IconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 0,
                modifier = Modifier.testTag("prev_page_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Página anterior",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Page Indicator Pill (clickable for quick jump)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onJumpToPageClick)
                    .testTag("page_indicator_pill")
            ) {
                Text(
                    text = "${currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Next Page Button
            IconButton(
                onClick = onNextPage,
                enabled = currentPage < pageCount - 1,
                modifier = Modifier.testTag("next_page_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Página siguiente",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            HorizontalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Zoom Out
            IconButton(
                onClick = onZoomOut,
                enabled = zoomScale > 1.0f,
                modifier = Modifier.testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = stringResource(R.string.zoom_out),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom Reset (if zoomed)
            if (zoomScale > 1.05f) {
                Text(
                    text = "${(zoomScale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable(onClick = onResetZoom)
                        .padding(horizontal = 4.dp)
                )
            }

            // Zoom In
            IconButton(
                onClick = onZoomIn,
                enabled = zoomScale < 4.0f,
                modifier = Modifier.testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = stringResource(R.string.zoom_in),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun JumpToPageDialog(
    currentPage: Int,
    pageCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputPage by remember { mutableStateOf((currentPage + 1).toString()) }
    var sliderValue by remember { mutableFloatStateOf((currentPage + 1).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.jump_to_page),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Selecciona una página entre 1 y $pageCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        inputPage = it.roundToInt().toString()
                    },
                    valueRange = 1f..pageCount.toFloat().coerceAtLeast(1f),
                    steps = if (pageCount > 2) (pageCount - 2).coerceAtMost(50) else 0
                )

                OutlinedTextField(
                    value = inputPage,
                    onValueChange = { str ->
                        val clean = str.filter { it.isDigit() }
                        inputPage = clean
                        clean.toIntOrNull()?.let {
                            if (it in 1..pageCount) {
                                sliderValue = it.toFloat()
                            }
                        }
                    },
                    label = { Text("Número de página") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = inputPage.toIntOrNull()?.coerceIn(1, pageCount) ?: (currentPage + 1)
                    onConfirm(target - 1)
                }
            ) {
                Text(stringResource(R.string.go_to))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun FileInfoDialog(
    fileInfo: PdfFileInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.file_info),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoItem(label = "Nombre", value = fileInfo.name)
                InfoItem(label = "Tamaño", value = fileInfo.sizeFormatted)
                InfoItem(label = "Total de páginas", value = "${fileInfo.pageCount} páginas")
                InfoItem(
                    label = "Ubicación / URI",
                    value = fileInfo.uriString.take(80) + if (fileInfo.uriString.length > 80) "..." else ""
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

package com.example.ui

import android.util.Log
import android.graphics.Bitmap
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Attachment
import com.example.data.model.DecryptedNote
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingStrokeCodec
import com.example.data.model.parseNoteContentAndAttachments
import com.example.data.model.createRawContent
import com.example.data.model.parseTags
import com.example.perf.LocalPerformanceProfile
import com.example.ui.drawing.StrokeGeometryCache
import com.example.ui.viewmodel.NotesViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasScreen(
    noteId: Int,
    jsonPath: String?,
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    
    // Load existing drawing if jsonPath is provided (I/O + parseo JSON fuera del hilo principal)
    LaunchedEffect(jsonPath) {
        if (!jsonPath.isNullOrEmpty()) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    File(jsonPath).takeIf { it.exists() }
                        ?.let { DrawingStrokeCodec.strokesFromJson(it.readText()) }
                }.getOrNull()
            }
            if (loaded != null) strokes.addAll(loaded)
        }
    }
    var currentPoints = remember { mutableStateListOf<Offset>() }

    // Geometría reutilizada entre frames: cero `Path`/`Stroke` nuevos en el hilo de dibujo.
    val geometry = remember { StrokeGeometryCache() }
    val useHardwareLayer = LocalPerformanceProfile.current.useHardwareLayerForDrawing
    val canvasShape = remember { RoundedCornerShape(16.dp) }

    val colors = listOf(
        Color.Black,
        Color(0xFFE53935), // Red
        Color(0xFF1E88E5), // Blue
        Color(0xFF43A047), // Green
        Color(0xFFFFB300), // Yellow
        Color(0xFF8E24AA), // Purple
        Color(0xFF00ACC1)  // Cyan
    )
    
    var selectedColor by remember { mutableStateOf(colors[0]) }
    var selectedWidth by remember { mutableStateOf(8f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val canvasCardBorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)

    Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Text(
                        text = stringResource(R.string.drawing_canvas_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
            },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
              }
            },
            actions = {
              IconButton(
                onClick = { strokes.clear() },
                modifier = Modifier.testTag("clear_canvas_btn")
              ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.drawing_clear), tint = MaterialTheme.colorScheme.error)
              }
              IconButton(
                            onClick = {
                                val size = canvasSize
                                if (size.width <= 0 || size.height <= 0) {
                                    Toast.makeText(context, context.getString(R.string.toast_drawing_empty), Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                val snapshot = strokes.toList()
                                scope.launch {
                                    persistDrawing(
                                        context = context,
                                        viewModel = viewModel,
                                        noteId = noteId,
                                        jsonPath = jsonPath,
                                        strokes = snapshot,
                                        size = size,
                                        onBack = onBack
                                    )
                                }
                            },
                            modifier = Modifier.testTag("save_canvas_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.drawing_save), tint = MaterialTheme.colorScheme.primary)
                        }
            }
          )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // Main Drawing Area.
            // El recorte + el contenido del lienzo viven en UNA sola GraphicsLayer: el trazo se
            // rasteriza a texture y el resto de la pantalla no se vuelve a dibujar al pintar.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .then(
                        if (useHardwareLayer) {
                            Modifier.graphicsLayer {
                                shape = canvasShape
                                clip = true
                            }
                        } else {
                            Modifier.clip(canvasShape)
                        }
                    )
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, canvasShape)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints.clear()
                                currentPoints.add(offset)
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    strokes.add(DrawingStroke(currentPoints.toList(), selectedColor, selectedWidth))
                                    currentPoints.clear()
                                }
                            },
                            onDragCancel = {
                                currentPoints.clear()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentPoints.add(change.position)
                            }
                        )
                    }
                    .testTag("drawing_canvas_area")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Lectura de estado SOLO dentro de la fase de dibujo: cada punto nuevo
                    // invalida el draw, nunca la recomposición de la pantalla.
                    geometry.trimTo(strokes.size)
                    var index = 0
                    for (stroke in strokes) {
                        if (stroke.points.size > 1) {
                            drawPath(
                                path = geometry.pathFor(index, stroke),
                                color = stroke.color,
                                style = geometry.styleFor(stroke.width)
                            )
                        } else if (stroke.points.isNotEmpty()) {
                            drawCircle(
                                color = stroke.color,
                                radius = stroke.width / 2,
                                center = stroke.points.first()
                            )
                        }
                        index++
                    }

                    val live = currentPoints
                    if (live.size > 1) {
                        drawPath(
                            path = geometry.livePath(live),
                            color = selectedColor,
                            style = geometry.styleFor(selectedWidth)
                        )
                    } else if (live.isNotEmpty()) {
                        drawCircle(
                            color = selectedColor,
                            radius = selectedWidth / 2,
                            center = live.first()
                        )
                    }
                }
            }

            // Controls/Styling Toolbar at bottom
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .border(canvasCardBorderStroke, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stroke Width Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LineWeight, contentDescription = stringResource(R.string.drawing_stroke_width), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = context.getString(R.string.drawing_size_label, selectedWidth.toInt()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        @Suppress("DEPRECATION")
                        Slider(
                            value = selectedWidth,
                            onValueChange = { selectedWidth = it },
                            valueRange = 2f..48f,
                            modifier = Modifier.weight(1f).testTag("stroke_width_slider")
                        )
                    }

                    // Color Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Default.ColorLens, contentDescription = stringResource(R.string.drawing_color_picker), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            colors.forEach { color ->
                                val isSelected = selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = color }
                                        .testTag("color_btn_${color.toArgb()}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DrawingOutputFiles(val pngFile: File, val jsonFile: File)

/** Decide los nombres de archivo manteniendo el comportamiento previo (reutiliza el existente). */
private fun resolveOutputFiles(
    context: Context,
    match: DecryptedNote?,
    jsonPath: String?,
    noteId: Int
): DrawingOutputFiles {
    val directory = context.filesDir
    if (!jsonPath.isNullOrEmpty() && match != null) {
        val (_, attachments) = parseNoteContentAndAttachments(match.content)
        val existing = attachments.firstOrNull { it.path == jsonPath }
        val png = existing?.let { File(it.name) }
            ?: File(directory, "drawing_${noteId}_${System.currentTimeMillis()}.png")
        return DrawingOutputFiles(png, File(jsonPath))
    }
    val timestamp = System.currentTimeMillis()
    return DrawingOutputFiles(
        File(directory, "drawing_${noteId}_${timestamp}.png"),
        File(directory, "drawing_${noteId}_${timestamp}.json")
    )
}

/** Rasteriza los trazos reutilizando un único Paint y un único Path por trazo. */
private fun renderToBitmap(strokes: List<DrawingStroke>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    val path = android.graphics.Path()
    for (stroke in strokes) {
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.width
        if (stroke.points.size > 1) {
            path.reset()
            val first = stroke.points.first()
            path.moveTo(first.x, first.y)
            for (i in 1 until stroke.points.size) {
                val p = stroke.points[i]
                path.lineTo(p.x, p.y)
            }
            canvas.drawPath(path, paint)
        } else if (stroke.points.isNotEmpty()) {
            val p = stroke.points.first()
            canvas.drawCircle(p.x, p.y, stroke.width / 2, paint)
        }
    }
    return bitmap
}

/** Escritura del PNG y del JSON de trazos en Dispatchers.IO, liberando el bitmap pase lo que pase. */
private suspend fun writeDrawingFiles(
    bitmap: Bitmap,
    targets: DrawingOutputFiles,
    strokes: List<DrawingStroke>
) {
    try {
        withContext(Dispatchers.IO) {
            targets.pngFile.parentFile?.mkdirs()
            FileOutputStream(targets.pngFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            targets.jsonFile.parentFile?.mkdirs()
            FileOutputStream(targets.jsonFile).use { it.write(DrawingStrokeCodec.strokesToJson(strokes).toByteArray()) }
        }
    } finally {
        bitmap.recycle()
    }
}

/**
 * Rasteriza y persiste el dibujo. El bitmap se construye en `Dispatchers.Default` y los archivos
 * se escriben en `Dispatchers.IO`: el pulsador de "guardar" ya no bloquea el frame.
 */
private suspend fun persistDrawing(
    context: Context,
    viewModel: NotesViewModel,
    noteId: Int,
    jsonPath: String?,
    strokes: List<DrawingStroke>,
    size: IntSize,
    onBack: () -> Unit
) {
    try {
        val existing = viewModel.notesList.value.find { it.note.id == noteId }
        val targets = resolveOutputFiles(context, existing, jsonPath, noteId)
        val bitmap = withContext(Dispatchers.Default) { renderToBitmap(strokes, size.width, size.height) }
        writeDrawingFiles(bitmap, targets, strokes)
        val match = existing ?: return onBack()
        val content = mergeDrawingAttachment(match, targets, jsonPath)
        viewModel.saveNoteAndGetId(
            id = noteId,
            title = match.title,
            content = content,
            isEncrypted = match.note.isEncrypted,
            tagsList = match.note.parseTags(),
            backgroundColor = match.note.backgroundColor,
            backgroundImagePath = match.note.backgroundImagePath,
            isPinned = match.note.isPinned,
            isFavorite = match.note.isFavorite,
            isArchived = match.note.isArchived
        )
        viewModel.notifyNoteExternallyUpdated(noteId)
        onBack()
    } catch (e: Exception) {
        Log.e("DrawingCanvasScreen", "save drawing failed", e)
        Toast.makeText(context, context.getString(R.string.toast_drawing_save_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/** Reemplaza (o añade) el adjunto de tipo "drawing" en el contenido de la nota. */
private fun mergeDrawingAttachment(
    match: DecryptedNote,
    targets: DrawingOutputFiles,
    jsonPath: String?
): String {
    val (cleanText, currentAttachments) = parseNoteContentAndAttachments(match.content)
    val newAttachment = Attachment(
        type = "drawing",
        path = targets.jsonFile.absolutePath,
        name = targets.pngFile.absolutePath
    )
    val updated = if (jsonPath.isNullOrEmpty()) {
        currentAttachments + newAttachment
    } else {
        val mutable = currentAttachments.toMutableList()
        val index = mutable.indexOfFirst { it.type == "drawing" && it.path == jsonPath }
        if (index >= 0) mutable[index] = newAttachment else mutable.add(newAttachment)
        mutable.toList()
    }
    return createRawContent(cleanText, updated)
}

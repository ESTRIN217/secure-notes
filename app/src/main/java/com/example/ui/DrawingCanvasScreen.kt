package com.example.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.NotesViewModel
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

// Helper models for serialization of attachments
data class Attachment(val type: String, val path: String, val name: String = "")

fun parseNoteContentAndAttachments(rawContent: String): Pair<String, List<Attachment>> {
    val delimiter = "\n\n---Attachments---\n"
    if (rawContent.contains(delimiter)) {
        val parts = rawContent.split(delimiter, limit = 2)
        val text = parts[0]
        val jsonStr = parts.getOrNull(1) ?: "[]"
        val list = mutableListOf<Attachment>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Attachment(
                    type = obj.getString("type"),
                    path = obj.getString("path"),
                    name = obj.optString("name", "")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(text, list)
    }
    return Pair(rawContent, emptyList())
}

fun createRawContent(text: String, attachments: List<Attachment>): String {
    if (attachments.isEmpty()) return text
    val delimiter = "\n\n---Attachments---\n"
    val arr = JSONArray()
    attachments.forEach { att ->
        val obj = org.json.JSONObject()
        obj.put("type", att.type)
        obj.put("path", att.path)
        obj.put("name", att.name)
        arr.put(obj)
    }
    return text + delimiter + arr.toString()
}

fun parseTags(rawContent: String): List<String> {
    val tags = mutableListOf<String>()
    try {
        val tagDelimiter = "\n\n---Tags---\n"
        if (rawContent.contains(tagDelimiter)) {
            val parts = rawContent.split(tagDelimiter)
            if (parts.size > 1) {
                val jsonArr = JSONArray(parts[1])
                for (i in 0 until jsonArr.length()) {
                    tags.add(jsonArr.getString(i))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return tags
}

fun createRawTags(tags: List<String>): String {
    if (tags.isEmpty()) return ""
    val delimiter = "\n\n---Tags---\n"
    val arr = JSONArray()
    tags.forEach { tag -> arr.put(tag) }
    return delimiter + arr.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasScreen(
    noteId: Int,
    jsonPath: String?,
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    
    // Load existing drawing if jsonPath is provided
    LaunchedEffect(jsonPath) {
        if (!jsonPath.isNullOrEmpty()) {
            try {
                val file = File(jsonPath)
                if (file.exists()) {
                    val jsonContent = file.readText()
                    val jsonArray = JSONArray(jsonContent)
                    val loadedStrokes = mutableListOf<DrawingStroke>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val color = Color(obj.getInt("color"))
                        val width = obj.getDouble("width").toFloat()
                        val pointsArray = obj.getJSONArray("points")
                        val points = mutableListOf<Offset>()
                        for (j in 0 until pointsArray.length()) {
                            val ptObj = pointsArray.getJSONObject(j)
                            points.add(Offset(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
                        }
                        loadedStrokes.add(DrawingStroke(points, color, width))
                    }
                    strokes.addAll(loadedStrokes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load drawing", Toast.LENGTH_SHORT).show()
            }
        }
    }
    var currentPoints = remember { mutableStateListOf<Offset>() }
    
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

    Scaffold(
        topBar = {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Drawing Canvas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
                        IconButton(
                            onClick = { strokes.clear() },
                            modifier = Modifier.testTag("clear_canvas_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Canvas", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(
                            onClick = {
                                if (canvasSize.width <= 0 || canvasSize.height <= 0) {
                                    Toast.makeText(context, "Canvas is empty or not initialized", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                try {
                                    // Render to Bitmap
                                    val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bitmap)
                                    canvas.drawColor(android.graphics.Color.WHITE)

                                    val paint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeCap = android.graphics.Paint.Cap.ROUND
                                        strokeJoin = android.graphics.Paint.Join.ROUND
                                    }

                                    strokes.forEach { stroke ->
                                        paint.color = stroke.color.toArgb()
                                        paint.strokeWidth = stroke.width
                                        val path = android.graphics.Path()
                                        if (stroke.points.size > 1) {
                                            val first = stroke.points.first()
                                            path.moveTo(first.x, first.y)
                                            for (i in 1 until stroke.points.size) {
                                                val pt = stroke.points[i]
                                                path.lineTo(pt.x, pt.y)
                                            }
                                            canvas.drawPath(path, paint)
                                        } else if (stroke.points.isNotEmpty()) {
                                            val pt = stroke.points.first()
                                            val fillPaint = android.graphics.Paint().apply {
                                                isAntiAlias = true
                                                style = android.graphics.Paint.Style.FILL
                                                color = stroke.color.toArgb()
                                            }
                                            canvas.drawCircle(pt.x, pt.y, stroke.width / 2, fillPaint)
                                        }
                                    }

                                    // Save to file
                                    val directory = context.filesDir
                                    val timestamp = System.currentTimeMillis()
                                    val pngFile = File(directory, "drawing_${noteId}_${timestamp}.png")
                                    val jsonFile = File(directory, "drawing_${noteId}_${timestamp}.json")
                                    
                                    // Save PNG
                                    FileOutputStream(pngFile).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    
                                    // Save JSON Path Data
                                    val jsonArray = JSONArray()
                                    strokes.forEach { stroke ->
                                        val strokeObj = org.json.JSONObject()
                                        strokeObj.put("color", stroke.color.toArgb())
                                        strokeObj.put("width", stroke.width)
                                        val pointsArray = JSONArray()
                                        stroke.points.forEach { pt ->
                                            val ptObj = org.json.JSONObject()
                                            ptObj.put("x", pt.x.toDouble())
                                            ptObj.put("y", pt.y.toDouble())
                                            pointsArray.put(ptObj)
                                        }
                                        strokeObj.put("points", pointsArray)
                                        jsonArray.put(strokeObj)
                                    }
                                    FileOutputStream(jsonFile).use { out ->
                                        out.write(jsonArray.toString().toByteArray())
                                    }

                                    // Update Note attachments
                                    val list = viewModel.notesList.value
                                    val match = list.find { it.note.id == noteId }
                                    if (match != null) {
                                        val (cleanText, currentAttachments) = parseNoteContentAndAttachments(match.content)
                                        // Store the JSON file path for editing
                                        val newAttachment = Attachment(type = "drawing", path = jsonFile.absolutePath, name = pngFile.absolutePath)
                                        val newAttachmentsList = currentAttachments + newAttachment
                                        val rawContent = createRawContent(cleanText, newAttachmentsList)

                                        viewModel.saveNote(
                                            id = noteId,
                                            title = match.title,
                                            content = rawContent,
                                            isEncrypted = match.note.isEncrypted,
                                            tagsList = try {
                                                val arr = JSONArray(match.note.tagsJson)
                                                List(arr.length()) { arr.getString(it) }
                                            } catch (e: Exception) { emptyList() },
                                            backgroundColor = match.note.backgroundColor,
                                            backgroundImagePath = match.note.backgroundImagePath,
                                            isPinned = match.note.isPinned,
                                            isFavorite = match.note.isFavorite,
                                            isArchived = match.note.isArchived
                                        )
                                        Toast.makeText(context, "Drawing added to note", Toast.LENGTH_SHORT).show()
                                    }
                                    onBack()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Error saving drawing: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("save_canvas_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save Drawing", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // Main Drawing Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .background(Color.White)
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
                    if (canvasSize == IntSize.Zero) {
                        canvasSize = IntSize(size.width.toInt(), size.height.toInt())
                    }

                    // Render existing strokes
                    strokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val path = Path().apply {
                                val first = stroke.points.first()
                                moveTo(first.x, first.y)
                                for (i in 1 until stroke.points.size) {
                                    val pt = stroke.points[i]
                                    lineTo(pt.x, pt.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                style = Stroke(
                                    width = stroke.width,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        } else if (stroke.points.isNotEmpty()) {
                            drawCircle(
                                color = stroke.color,
                                radius = stroke.width / 2,
                                center = stroke.points.first()
                            )
                        }
                    }

                    // Render active stroke
                    if (currentPoints.size > 1) {
                        val path = Path().apply {
                            val first = currentPoints.first()
                            moveTo(first.x, first.y)
                            for (i in 1 until currentPoints.size) {
                                val pt = currentPoints[i]
                                lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = selectedColor,
                            style = Stroke(
                                width = selectedWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else if (currentPoints.isNotEmpty()) {
                        drawCircle(
                            color = selectedColor,
                            radius = selectedWidth / 2,
                            center = currentPoints.first()
                        )
                    }
                }
            }

            // Controls/Styling Toolbar at bottom
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
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
                        Icon(Icons.Default.LineWeight, contentDescription = "Stroke Width", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Size: ${selectedWidth.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(16.dp))
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
                        Icon(Icons.Default.ColorLens, contentDescription = "Color Picker", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

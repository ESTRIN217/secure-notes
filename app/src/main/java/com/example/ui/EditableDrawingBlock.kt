package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DataBlock
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingStrokeCodec
import java.io.File

private val drawingColors = listOf(
    Color.Black,
    Color(0xFFE53935), // Red
    Color(0xFF1E88E5), // Blue
    Color(0xFF43A047), // Green
    Color(0xFFFFB300), // Yellow
    Color(0xFF8E24AA), // Purple
    Color(0xFF00ACC1)  // Cyan
)

private const val DRAWING_ALIGN_CENTER = "center"
private const val DRAWING_ALIGN_LEFT = "left"
private const val DRAWING_ALIGN_RIGHT = "right"

/** Dibuja trazos normalizados (0..1) escalados al tamaño del canvas. */
@Composable
fun DrawingStrokesView(
    strokes: List<DrawingStroke>,
    modifier: Modifier = Modifier,
    activeStroke: DrawingStroke? = null
) {
    val all = remember(strokes, activeStroke) {
        if (activeStroke == null) strokes else strokes + activeStroke
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        all.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            if (stroke.points.size > 1) {
                val path = Path().apply {
                    val first = stroke.points.first()
                    moveTo(first.x * w, first.y * h)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        lineTo(pt.x * w, pt.y * h)
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
            } else {
                val pt = stroke.points.first()
                drawCircle(
                    color = stroke.color,
                    radius = stroke.width / 2,
                    center = Offset(pt.x * w, pt.y * h)
                )
            }
        }
    }
}

private fun strokesFromBlock(block: DataBlock): List<DrawingStroke> =
    if (block.isWysiwygDrawing) DrawingStrokeCodec.strokesFromJson(block.content) else emptyList()

private fun commitStrokes(block: DataBlock, strokes: List<DrawingStroke>): DataBlock =
    block.copy(
        content = DrawingStrokeCodec.strokesToJson(strokes),
        meta = block.meta + ("wysiwyg" to "true")
    )

/** Migra un dibujo legado (JSON+PNG en archivos) a trazos normalizados autocontenidos. */
private fun migrateLegacyBlock(context: android.content.Context, block: DataBlock): DataBlock? {
    if (!block.isLegacyDrawing || block.content.isNullOrBlank()) return null
    return try {
        val jsonFile = File(block.content)
        if (!jsonFile.exists()) return null
        val absolute = DrawingStrokeCodec.strokesFromJson(jsonFile.readText())
        if (absolute.isEmpty()) return commitStrokes(block, emptyList())
        val bmp = block.meta["previewPath"]?.takeIf { it.isNotBlank() }
            ?.let { BitmapFactory.decodeFile(it) }
        val (w, h) = if (bmp != null) bmp.width to bmp.height else 1080 to 1350
        commitStrokes(block, DrawingStrokeCodec.normalize(absolute, w, h))
    } catch (e: Exception) {
        null
    }
}

@Composable
fun EditableDrawingBlock(
    block: DataBlock,
    isActive: Boolean,
    onActivate: () -> Unit,
    onChange: (DataBlock) -> Unit,
    onDelete: () -> Unit,
    onInsertBelow: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveTo: () -> Unit,
    onConvertTo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var strokes by remember(block.content) { mutableStateOf(strokesFromBlock(block)) }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var canvasPx by remember { mutableStateOf(IntSize.Zero) }
    var selectedColor by remember { mutableStateOf(drawingColors[0]) }
    var selectedWidth by remember { mutableFloatStateOf(8f) }
    var showBlockOptions by remember { mutableStateOf(false) }
    var showAlignSheet by remember { mutableStateOf(false) }

    val alignment = block.meta["align"] ?: DRAWING_ALIGN_CENTER
    val showCaption = block.meta["showCaption"] == "true"
    val caption = block.meta["caption"] ?: ""

    LaunchedEffect(isActive, block.content) {
        if (isActive && block.isLegacyDrawing) {
            val migrated = migrateLegacyBlock(context, block)
            if (migrated != null) onChange(migrated)
        }
    }

    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val borderWidth = if (isActive) 2.dp else 1.dp
    val activeStroke = if (currentPoints.isNotEmpty()) {
        DrawingStroke(currentPoints.toList(), selectedColor, selectedWidth)
    } else null
    val canvasFraction = if (alignment == DRAWING_ALIGN_CENTER) 1f else 0.5f
    val canvasHorizontalAlign = when (alignment) {
        DRAWING_ALIGN_LEFT -> Alignment.CenterStart
        DRAWING_ALIGN_RIGHT -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    Column(modifier = modifier.fillMaxWidth()
    .padding(bottom = 5.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = canvasHorizontalAlign
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(canvasFraction)
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(enabled = !isActive) { onActivate() }
                    .then(
                        if (isActive) Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints.clear()
                                    if (canvasPx.width > 0 && canvasPx.height > 0) {
                                        currentPoints.add(
                                            Offset(
                                                offset.x / canvasPx.width,
                                                offset.y / canvasPx.height
                                            )
                                        )
                                    }
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        val newStrokes = strokes + DrawingStroke(
                                            currentPoints.toList(),
                                            selectedColor,
                                            selectedWidth
                                        )
                                        strokes = newStrokes
                                        currentPoints.clear()
                                        onChange(commitStrokes(block, newStrokes))
                                    }
                                },
                                onDragCancel = {
                                    currentPoints.clear()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (canvasPx.width > 0 && canvasPx.height > 0) {
                                        currentPoints.add(
                                            Offset(
                                                change.position.x / canvasPx.width,
                                                change.position.y / canvasPx.height
                                            )
                                        )
                                    }
                                }
                            )
                        } else Modifier
                    )
                    .onSizeChanged { canvasPx = it },
                contentAlignment = Alignment.Center
            ) {
                DrawingStrokesView(
                    strokes = strokes,
                    activeStroke = activeStroke,
                    modifier = Modifier.fillMaxSize()
                )
                if (!isActive && strokes.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.drawing_tap_to_draw),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    IconButton(
                        onClick = { showBlockOptions = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_block_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
            }
        }
    }
}

        if (showCaption) {
            EditableDrawingCaption(
                caption = caption,
                isActive = isActive,
                onActivate = onActivate,
                onCaptionChange = { newCaption ->
                    onChange(block.copy(meta = block.meta + ("caption" to newCaption)))
                },
                onDelete = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isActive) {
            DrawingBlockToolbar(
                strokesCount = strokes.size,
                selectedColor = selectedColor,
                selectedWidth = selectedWidth,
                onColorSelected = { selectedColor = it },
                onWidthSelected = { selectedWidth = it },
                onUndo = {
                    if (strokes.isNotEmpty()) {
                        val newStrokes = strokes.dropLast(1)
                        strokes = newStrokes
                        onChange(commitStrokes(block, newStrokes))
                    }
                },
                onClear = {
                    strokes = emptyList()
                    onChange(commitStrokes(block, emptyList()))
                }
            )
        }
    }

    if (showBlockOptions) {
        BlockOptionsSheet(
            title = stringResource(R.string.block_drawing),
            onDismiss = { showBlockOptions = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_convert_title),
                    icon = Icons.Default.SwapHoriz,
                    onClick = {
                        showBlockOptions = false
                        onConvertTo()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_insert_below),
                    icon = Icons.Default.ArrowDownward,
                    onClick = {
                        showBlockOptions = false
                        onInsertBelow()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_drawing_replace),
                    icon = Icons.Default.Edit,
                    onClick = {
                        showBlockOptions = false
                        currentPoints.clear()
                        strokes = emptyList()
                        onChange(commitStrokes(block, emptyList()))
                        onActivate()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_drawing_description),
                    icon = Icons.Default.Description,
                    toggle = showCaption,
                    onClick = {
                        showBlockOptions = false
                        onChange(
                            block.copy(
                                meta = block.meta + ("showCaption" to (!showCaption).toString())
                            )
                        )
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_drawing_align),
                    icon = Icons.Default.FormatAlignCenter,
                    onClick = {
                        showBlockOptions = false
                        showAlignSheet = true
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_duplicate),
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showBlockOptions = false
                        onDuplicate()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_move_to),
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    onClick = {
                        showBlockOptions = false
                        onMoveTo()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.btn_delete),
                    icon = Icons.Default.Delete,
                    danger = true,
                    onClick = {
                        showBlockOptions = false
                        onDelete()
                    }
                )
            )
        )
    }

    if (showAlignSheet) {
        val currentAlign = when (alignment) {
            DRAWING_ALIGN_LEFT -> DRAWING_ALIGN_LEFT
            DRAWING_ALIGN_RIGHT -> DRAWING_ALIGN_RIGHT
            else -> DRAWING_ALIGN_CENTER
        }
        BlockOptionsSheet(
            title = stringResource(R.string.block_drawing_align),
            onDismiss = { showAlignSheet = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_align_center),
                    icon = Icons.Default.FormatAlignCenter,
                    toggle = currentAlign == DRAWING_ALIGN_CENTER,
                    onClick = {
                        showAlignSheet = false
                        onChange(block.copy(meta = block.meta + ("align" to DRAWING_ALIGN_CENTER)))
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_align_left),
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    toggle = currentAlign == DRAWING_ALIGN_LEFT,
                    onClick = {
                        showAlignSheet = false
                        onChange(block.copy(meta = block.meta + ("align" to DRAWING_ALIGN_LEFT)))
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_align_right),
                    icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                    toggle = currentAlign == DRAWING_ALIGN_RIGHT,
                    onClick = {
                        showAlignSheet = false
                        onChange(block.copy(meta = block.meta + ("align" to DRAWING_ALIGN_RIGHT)))
                    }
                )
            )
        )
    }
}

@Composable
private fun DrawingBlockToolbar(
    strokesCount: Int,
    selectedColor: Color,
    selectedWidth: Float,
    onColorSelected: (Color) -> Unit,
    onWidthSelected: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = selectedWidth,
                    onValueChange = onWidthSelected,
                    valueRange = 2f..48f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onUndo,
                    enabled = strokesCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.drawing_undo),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.drawing_clear),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                drawingColors.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
            }
        }
    }
}
}

@Composable
private fun EditableDrawingCaption(
    caption: String,
    isActive: Boolean,
    onActivate: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = caption, selection = TextRange(caption.length)))
    }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) isFocused = false
    }

    if (!isFocused && caption != fieldValue.text) {
        fieldValue = TextFieldValue(text = caption, selection = TextRange(caption.length))
    }

    val hintColor = if (isActive) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                onCaptionChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) onActivate()
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            ),
            singleLine = false
        )
        if (fieldValue.text.isEmpty()) {
            Text(
                text = stringResource(R.string.block_drawing_caption_hint),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = hintColor,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

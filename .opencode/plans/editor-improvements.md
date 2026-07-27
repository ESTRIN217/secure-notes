# Editor Improvements: Implementation Plan

## Mejora 1: Checklist Toggle Inline

### Archivo: `BlockEditor.kt`

Agregar `import com.example.util.toggleNthChecklistItem` y pasar `onChecklistToggle` a `NoteContentBlockCard`:

```kotlin
// Add import
import com.example.util.toggleNthChecklistItem

// In BlockEditor, lines 96-99, replace:
                        onUrlClicked = { _, _ -> }
                    )
// With:
                        onUrlClicked = { _, _ -> },
                        onChecklistToggle = { globalIndex, _ ->
                            val newText = toggleNthChecklistItem(rawContent, globalIndex)
                            if (newText != rawContent) {
                                onRawContentChange(newText)
                            }
                        }
                    )
```

---

## Mejora 2: Toolbar Inserts in Focused Block

### Arquitectura

Usar un `MutableState<String?>` compartido llamado `pendingTagInsert`. NoteEditorScreen escribe tags, BlockEditor los consume insertándolos en el bloque con focus.

### Archivo: `EditableTextBlock.kt`

Agregar parámetros `onFocusChange` y `onCursorChange`:

```kotlin
fun EditableTextBlock(
    rawText: String,
    onChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    onSplit: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
)

// Inside, replace:
    var textFieldValue by remember(rawText) {
        mutableStateOf(TextFieldValue(text = rawText, selection = TextRange(rawText.length)))
    }
// With (remove the remember(rawText) key so we control updates):
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = rawText, selection = TextRange(rawText.length))) }

// Also add focus tracking:
    var isFocused by remember { mutableStateOf(false) }

// In the OutlinedTextField:
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val oldText = textFieldValue.text
            val newText = newValue.text

            // Detect Enter for block splitting
            if (onSplit != null && newText.length > oldText.length) {
                // Find the position of the first new \n that wasn't in oldText
                val newlineIdx = newText.indexOf('\n')
                if (newlineIdx >= 0 && (oldText.length <= newlineIdx || oldText[newlineIdx] != '\n')) {
                    val before = newText.substring(0, newlineIdx)
                    val after = newText.substring(newlineIdx + 1)
                    textFieldValue = TextFieldValue(text = before, selection = TextRange(before.length))
                    onChange(before)
                    onSplit(after)
                    return@OutlinedTextField
                }
            }

            textFieldValue = newValue
            onChange(newText)
            onCursorChange(newValue.selection.start)
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                onFocusChange(focusState.isFocused)
                if (focusState.isFocused) {
                    onCursorChange(textFieldValue.selection.start)
                }
            },
        ...
    )
```

Add imports:
```kotlin
import androidx.compose.ui.focus.onFocusChanged
```

### Archivo: `BlockEditor.kt`

Agregar estado de foco y `pendingTagInsert`:

```kotlin
@Composable
fun BlockEditor(
    rawContent: String,
    onRawContentChange: (String) -> Unit,
    attachments: List<Attachment>,
    noteId: Int,
    onNavigateToMediaViewer: (String, String) -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    pendingTagInsert: MutableState<String?>,
    modifier: Modifier = Modifier
) {
    val blockRanges by remember(rawContent) {
        mutableStateOf(parseEditorBlockRanges(rawContent))
    }
    var activeBlockIndex by remember { mutableIntStateOf(-1) }
    var activeCursorOffset by remember { mutableIntStateOf(0) }

    // Insert tag when pendingTagInsert is set
    LaunchedEffect(pendingTagInsert.value) {
        val tag = pendingTagInsert.value ?: return@LaunchedEffect
        if (activeBlockIndex == -1) return@LaunchedEffect
        
        val (block, range) = blockRanges.getOrNull(activeBlockIndex) ?: return@LaunchedEffect
        if (block !is NoteContentBlock.TextBlock) return@LaunchedEffect
        
        val blockRaw = rawContent.substring(range)
        val insertPos = activeCursorOffset.coerceIn(0, blockRaw.length)
        val newBlockRaw = blockRaw.substring(0, insertPos) + tag + blockRaw.substring(insertPos)
        val newContent = rawContent.replaceRange(range, newBlockRaw)
        onRawContentChange(newContent)
        pendingTagInsert.value = null
    }

    Column( ...
        blockRanges.forEachIndexed { index, (block, range) ->
            when (block) {
                is NoteContentBlock.TextBlock -> {
                    val rawSubstring = rawContent.substring(range)
                    EditableTextBlock(
                        rawText = rawSubstring,
                        onChange = { newRawText ->
                            val newContent = rawContent.replaceRange(range, newRawText)
                            onRawContentChange(newContent)
                        },
                        onFocusChange = { focused ->
                            if (focused) activeBlockIndex = index
                            else if (activeBlockIndex == index) activeBlockIndex = -1
                        },
                        onCursorChange = { cursor ->
                            activeCursorOffset = cursor
                        }
                    )
                }
                ...
            }
        }
    )
}
```

Add imports:
```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
```

### Archivo: `NoteEditorScreen.kt`

1. Agregar `pendingTagInsert` state:
```kotlin
// Near line 183 (history state):
val pendingTagInsert = remember { mutableStateOf<String?>(null) }
```

2. Modificar `applyTag` y `applyTagWithVal` para usar `pendingTagInsert` en lugar de `contentValue`:

```kotlin
// Replace the applyTag lambda (lines 820-838) with:
val applyTag: (String) -> Unit = { tag ->
    pendingTagInsert.value = "<$tag></$tag>"
}

// Replace the applyTagWithVal lambda (lines 840-858) with:
val applyTagWithVal: (String, String) -> Unit = { tag, value ->
    pendingTagInsert.value = "<$tag=$value></$tag>"
}
```

3. Pasar `pendingTagInsert` a BlockEditor:
```kotlin
BlockEditor(
    ...
    pendingTagInsert = pendingTagInsert,
)
```

Note: `insertAtCursor` se mantiene para los diálogos (insertar imagen, video, tabla, url, etc.) que aún insertan al final del raw content. Esto es aceptable como comportamiento legacy.

---

## Mejora 3: Block Splitting (Enter → nuevo bloque)

### Archivo: `EditableTextBlock.kt`

(Incluido en Mejora 2 arriba — el `onSplit` callback ya está implementado en `onValueChange`)

### Archivo: `BlockEditor.kt`

Manejar `onSplit` en `EditableTextBlock`:

```kotlin
EditableTextBlock(
    ...
    onSplit = { afterText ->
        if (afterText.isNotBlank()) {
            val (_, range) = blockRanges[index]
            val beforeText = rawContent.substring(range.first, range.last + 1)
            // Insert a new TextBlock after this one
            // We insert the raw text of the "after" part after the current range
            val insertPos = range.last + 1
            val newContent = rawContent.substring(0, insertPos) + afterText + rawContent.substring(insertPos)
            onRawContentChange(newContent)
        }
    }
)
```

## Verification

```bash
./gradlew assembleDebug
```

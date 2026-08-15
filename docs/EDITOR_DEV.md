# Editor de Bloques — Guía para Desarrolladores

> Complemento técnico de `docs/EDITOR.md`. Para el manual de usuario y la sintaxis markup, consulta ese documento. Este documento describe **cómo funciona el editor por dentro** y **cómo añadir un bloque nuevo**.

---

## 1. Arquitectura

El editor es **presentacional (controlado)**: `BlockEditor` recibe la lista de bloques y reporta cambios hacia arriba. Nada se persiste dentro del editor.

```
NoteEditorScreen (estado local + persistencia)
  └─ BlockEditor(blocks, onBlocksChange, ...)          orquesta la lista + drag & drop
       └─ BlockRow(block, index, callbacks)            despacha UN bloque por tipo
            ├─ EditableTextBlock                       base de todos los bloques de texto
            ├─ EditableChecklistBlock / Collapsible / Table / Code / Image / Video / Audio
            ├─ EditableFileBlock / Bookmark / Drawing
            └─ ReadOnlyTextBlock                       (bloques de solo lectura)
```

- **`NoteEditorScreen`** mantiene `blocks: MutableList<DataBlock>` (estado local), aplica los cambios que llegan por `onBlocksChange`, guarda historial (`saveBlocksToHistory()`) y persiste en `NotesViewModel` (StateFlow → Room).
- **`BlockRow`** es el *dispatcher*: un `when (block.type)` que instancia el componente editable de cada tipo y traduce sus callbacks a `DataBlock`s.
- Todo bloque de texto se renderiza con **`EditableTextBlock`**, que internamente trabaja con `AnnotatedString` (Compose) construido desde `TextSegment`s.

### Flujo de un cambio de texto

1. El usuario escribe en `EditableTextBlock` → `onValueChange` re-parsea el texto del `TextFieldValue` a `List<TextSegment>` (vía `RichTextConverter.parseAnnotatedString` + estilos pendientes).
2. `onChange(segments)` sube a `BlockRow`, que construye `block.copy(content = "", richTextJson = TextSegment.serialize(segments))` y lo sube a `onBlocksChange`.
3. `NoteEditorScreen` reemplaza el bloque en la lista y persiste.
4. La nueva `blocks` vuelve a bajar a `BlockEditor`; `EditableTextBlock` detecta el cambio por `segmentsJson` y reconstruye el `AnnotatedString` **sin perder cursor** (OffsetMapping).

> **Regla de oro:** `richTextJson` es la fuente de verdad. `content` solo existe para compatibilidad legacy/export. Al escribir `onChange`, guardar SIEMPRE `richTextJson`.

---

## 2. Contrato de callbacks de `EditableTextBlock`

Firma (los importantes):

```kotlin
@Composable
fun EditableTextBlock(
    segments: List<TextSegment>,
    blockType: BlockType = BlockType.TEXT,
    onChange: (List<TextSegment>) -> Unit,                      // edición de texto
    onFocusChange: (Boolean) -> Unit,                           // activar bloque
    onCursorChange: (Int) -> Unit,
    onSelectionChange: (IntRange) -> Unit,
    onSplit: ((before: List<TextSegment>, after: List<TextSegment>) -> Unit)?, // Enter
    onMoveToPreviousBlock: () -> Unit,
    onMoveToNextBlock: () -> Unit,
    onDeleteBlock: () -> Unit,
    onConvertToText: () -> Unit,                                // convertir a TEXT
    onEmptyBackspace: (() -> Unit)? = null,                     // Backspace en campo vacío
    modifier: Modifier = Modifier,
    numberIndex: Int? = null,                                   // numeración de listas
    requestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {},
    pendingInsert: MutableState<String?>,                       // "/" + slash menu
    pendingSelection: MutableState<IntRange?>,
    pendingTypingStyle: TextSegment? = null,                    // modo escritura del toolbar
    showPrefix: Boolean = true,
    forcePlain: Boolean = false,
    highlightLanguage: String? = null,
    softWrap: Boolean = true,
    textStyle: TextStyle? = null                                // override de estilo (ej. tachado de checklist)
)
```

### Comportamientos internos a respetar

- **Backspace en campo vacío** (orden de precedencia, `EditableTextBlock.kt:330`):
  1. `onEmptyBackspace != null` → se invoca (p. ej. checklist convierte a TEXT en lugar de borrar).
  2. `blockType in exitOnEmptyTypes` → `onConvertToText()` (HEADING/listas → texto).
  3. si no → `onDeleteBlock()` (fusiona con el bloque anterior).
- **Enter** → `onSplit(before, after)` si hay texto; si no, `onMoveToNextBlock()`/inserta bloque nuevo.
- **`textStyle`** anula el estilo por defecto derivado de `blockType`. Los bloques que reutilizan `EditableTextBlock` con un tipo distinto (p. ej. checklist con `blockType = TEXT`) lo usan para forzar estilos condicionales.
- El **slash menu** y la inserción de media se gestionan desde `NoteEditorScreen` vía `pendingInsert`/`pendingSelection`; `EditableTextBlock` solo reporta el estado y limpia el placeholder (`clearSlashPlaceholder`).

---

## 3. Modelo de datos

```kotlin
data class DataBlock(
    val type: BlockType,
    val content: String = "",
    val meta: Map<String, String> = emptyMap(),
    val richTextJson: String? = null
)
```

| Campo | Uso |
|---|---|
| `type` | `BlockType` (TEXT…BOOKMARK) |
| `content` | markup HTML-like legacy (export, compat) |
| `meta` | propiedades del bloque (estado, uri, colores…) — solo `String` |
| `richTextJson` | **fuente de verdad** del texto formateado (`TextSegment.serialize`) |

### Claves de `meta` en uso

| Tipo de bloque | Claves |
|---|---|
| CHECKLIST_ITEM | `checked`, `indentLevel` |
| COLLAPSIBLE | `summary`, `indentLevel` |
| TABLE | `table` (JSON de `TableData`) |
| IMAGE / VIDEO / AUDIO / VOICE / FILE / DRAWING | `fileUri`, `fileName`, `caption`, `showCaption`, `align`, `color`, `wysiwyg`; dibujo: `strokes` (JSON) |
| BOOKMARK | `url` (en `content`), `title`, `description`, `favicon` |
| Texto / listas | `indentLevel` |

### `TextSegment` — el átomo de formato

```kotlin
data class TextSegment(
    val text: String,
    val bold: Boolean = false, val italic: Boolean = false,
    val underline: Boolean = false, val strikethrough: Boolean = false,
    val colorHex: String? = null, val bgColorHex: String? = null,
    val fontFamily: String? = null, val fontSizeSp: Float? = null,
    val code: Boolean = false, val url: String? = null,
    val noteId: Int? = null, val equation: String? = null,
    val baseline: TextBaseline? = null
)
```

- **`ensureSegments()`** (`DataBlock`): devuelve `richTextJson` deserializado si existe; si no, parsea `content` desde el markup legacy. **Todo render y todo onChange debe pasar por aquí** para no perder formato.
- **`TextSegment.hasSameStyle`** compara estilo ignorando texto — útil en tests.
- Cambios de estilo se aplican con **`RichTextConverter.applySpanStyle`** sobre la selección (`onSelectionChange` + toolbar).

### Conversión markup ⇄ segmentos

`RichTextConverter` (en `com.example.util`) es el puente entre los tres mundos:

```
markup (content HTML-like)  ──markupToSegments──▶  List<TextSegment>
List<TextSegment>           ──segmentsToAnnotatedString──▶ AnnotatedString (Compose)
List<TextSegment>           ──segmentsToMarkup/Html/Md──▶  exportación
```

`segmentsToAnnotatedString` usa `LinkAnnotation.Url` para enlaces inline **clickeables también en edición** (requisito desde el bloque BOOKMARK).

---

## 4. Cómo añadir un bloque nuevo

Checklist de una integración completa:

1. **`BlockType`** (`com.example.data.model.DataBlock.kt`) — añadir el tipo al enum.
2. **`DataBlock`** — si el bloque lleva texto formateado, incluirlo en `segmentsBlockTypes` y gestionar `ensureSegments()`. Si es "legacy" de markup, mantener la migración en `migrateLegacyContent`/`fromLegacyHtml`.
3. **Componente editable** en `com.example.ui` — un `EditableXxxBlock.kt` (convención de nombre). Para bloques de texto reutiliza `EditableTextBlock` dentro de un `Row` con tu icono/prefijo; para media, usa los patrones de `EditableImageBlock`/`EditableVideoBlock`.
4. **`BlockRow`** (`BlockEditor.kt`) — añadir la rama del `when (block.type)` que instancie tu componente y traduzca callbacks a `block.copy(...)`. Respeta `Modifier.weight(1f)` si es una fila de texto.
5. **`SlashCommandMenu.kt`** — entrada en `BLOCK_COMMANDS` con `blockType` o `action: BlockAction`. Si necesita diálogo, añadir un `BlockAction` nuevo y gestionarlo en `NoteEditorScreen`.
6. **`NoteEditorScreen`** — inserción real (handler del comando), `refreshXxxMeta` si aplica (p. ej. `refreshBookmarkMeta`), y persistencia.
7. **Render en preview** — `NoteContentBlock.RenderContent`/`NoteContentBlockRenderer` y `NoteContentBlockConverter` (lista de notas).
8. **Exportadores** — rama en `MarkdownConverter`/`HtmlConverter` (o `RichTextConverter.blocksToMarkdown/Html`) y `util/export/*`.
9. **Tests** — al menos un test de round-trip de serialización del `meta`/`richTextJson` (patrón: `FileBlockMetaTest`, `BookmarkBlockMetaTest`, `ChecklistBlockTest`). Los tests de screenshot requieren `@GraphicsMode(NATIVE)` + `@Config(sdk = [36])`.

### Convenciones

- Funciones ≤ 20 líneas; guard clauses en vez de `if/else` anidados; máx. 2 niveles de nullable.
- `Result<T>` para I/O; `Log.e()` mínimo en cada `catch`.
- No añadir comentarios salvo que se pidan.
- Los callbacks deben mantener el **contrato de no-perdida**: si un bloque lleva formato, nunca aplanar a `ListOf(TextSegment(text = ...))`.

---

## 5. Gotchas

- **Room**: `.fallbackToDestructiveMigration()` — un cambio de esquema borra datos en silencio.
- **Configuration cache**: invalidar con `./gradlew clean --no-configuration-cache` si el build actúa raro.
- **KSP** (Room/Moshi): los cambios de anotaciones requieren build limpio (`kotlin.incremental=false`).
- **Tests**: los de Robolectric fallan en entornos sin runtime nativo (`DefaultNativeRuntimeLoader`) — es ambiental, no del código. Los tests de lógica pura (serialización, conversión) corren en JVM sin Robolectric.
- **Escapar llaves en regex**: el motor ICU de Android crashea con `}` sin escapar (bug visto en `DrawingStrokeCodec`).
- **Enlaces inline**: para que un enlace sea clickeable en modo edición hace falta `LinkAnnotation.Url` en el `visualTransformation` de `EditableTextBlock`, no solo en solo lectura.

# Editor de Bloques — Secure Notes

> Documentación del editor de notas de Secure Notes: un editor **WYSIWYG puro** basado en bloques (estilo Notion) con formato enriquecido en línea.

---

## Parte 1 — Manual de usuario

El editor funciona con **bloques** (cada párrafo, título, lista, tabla… es un bloque) y, dentro de cada bloque de texto, puedes aplicar **formato enriquecido en línea** (negrita, color, subíndice, etc.). Todo lo que escribes se ve exactamente como quedará guardado: **WYSIWYG puro**.

### Gestos y atajos básicos

| Acción | Cómo se hace |
|---|---|
| **Insertar un bloque** | Escribe `/` para abrir el menú de bloques y busca por nombre (o toca `+` en la barra flotante) |
| **Crear bloque nuevo** | Pulsa `Enter` al final de un bloque de texto |
| **Borrar / fusionar bloques** | `Backspace` sobre un bloque vacío lo elimina o lo fusiona con el anterior |
| **Reordenar bloques** | Mantén pulsado y arrastra el bloque a otra posición |
| **Convertir bloque** | Usa el menú `/` o los botones de la barra flotante sobre el bloque activo |
| **Deshacer / Rehacer** | Botones de la barra flotante |

> La **barra flotante** aparece sobre el teclado y muestra los formatos del texto bajo el cursor. Cuando un formato está activo (o se va a aplicar al siguiente carácter), su botón queda resaltado.

---

### Bloque de texto y formato

Todas las opciones se aplican a la **selección** actual. Si no hay selección, se activan como **modo de escritura**: el formato se aplica a todo lo que escribas a continuación y el botón se desactiva al pulsarlo de nuevo.

| Formato | Descripción |
|---|---|
| **Negrita** | Texto en negrita (`Ctrl/Cmd+B`) |
| **Itálica** | Texto en cursiva (`Ctrl/Cmd+I`) |
| **Subrayado** | Texto subrayado (`Ctrl/Cmd+U`) |
| **Tachado** | Texto tachado (rayado) |
| **Color del texto** | Cambia el color de las letras mediante la paleta de colores |
| **Color de fondo** | Añade un fondo de color al texto (resaltado) |
| **Enlace** | Inserta un enlace a una URL o a una página del bloc. Puedes seguir escribiendo en la misma línea después del enlace |
| **Código en línea** | Estilo `monospace` para una línea o fragmento de código |
| **Subíndice** | Texto en subíndice (`H₂O`) |
| **Superíndice** | Texto en superíndice (`x²`) |
| **Fuente** | Cambia la familia tipográfica del texto (serif, monospace, etc.) |
| **Tamaño** | Ajusta el tamaño de la fuente en puntos |
| **Ecuaciones** | Inserta una ecuación LaTeX renderizada matemáticamente |
| **Aumentar sangría** | Añade un nivel de indentación al bloque (listas anidadas, citas…) |
| **Disminuir sangría** | Elimina un nivel de indentación del bloque |

> **Nota sobre subíndice/superíndice:** al activarlos con el cursor al final del texto, el botón se ilumina y todo lo que escribas sale en sub/superíndice. Al desactivarlos, el botón se apaga y el texto nuevo vuelve a escribirse normal; lo ya formateado se conserva.

---

### Bloques básicos

| Bloque | Descripción |
|---|---|
| **Texto** | Bloque de párrafo estándar |
| **H1 · H2 · H3 · H4** | Títulos jerárquicos (grande, mediano, pequeño, extra pequeño) |
| **Lista con viñetas** | Elemento de lista desordenada (`•`) |
| **Lista numerada** | Elemento de lista ordenada (`1.`) |
| **Lista de tareas** | Elemento de checklist con casilla marcable y **formato enriquecido** (negrita, color, enlaces…) en cada ítem |
| **Lista desplegable** | Bloque colapsable con resumen y contenido expandible |
| **Página** | Crea y abre una página nueva enlazada |
| **Destacado** | Bloque *callout* resaltado para llamadas de atención |
| **Cita** | Bloque de cita con barra lateral (`▎`) |
| **Tabla** | Tabla editable con filas, columnas y encabezado |
| **Divisor** | Regla horizontal que separa secciones |
| **Enlace a página** | Enlaza una nota existente del bloc dentro del documento |

---

### Bloques de código y multimedia

| Bloque | Descripción |
|---|---|
| **Código** | Bloque de código con resaltado de sintaxis, ajuste de línea y lenguaje seleccionable |
| **Imagen** | Inserta una imagen (galería o cámara) en línea, con leyenda, alineación y reemplazo |
| **Vídeo** | Inserta un vídeo local o de YouTube/shorts con miniatura 16:9; se abre externamente |
| **Audio** | Adjunta un archivo de audio con reproductor embebido (velocidad, salto, título editable) |
| **Voz** | Graba y adjunta una nota de voz |
| **Archivo** | Adjunta cualquier archivo con apertura externa (FileProvider), color y descripción |
| **Marcador web** | Vista previa de enlace con título, descripción y favicon; refrescable y convertible en mención inline |
| **Dibujo** | Lienzo de dibujo editable con trazos a color (se integra como bloque) |

> Todos los bloques multimedia se insertan como **bloques reales** (menú `/` o barra `+`) y se pueden duplicar, mover, convertir y eliminar desde el menú de opciones del bloque.

---

## Parte 2 — Referencia técnica

### Modelo de datos

El contenido de una nota es una lista JSON de `DataBlock`:

```kotlin
data class DataBlock(
    val type: BlockType,
    val content: String = "",              // markup interno (ver sintaxis)
    val meta: Map<String, String> = emptyMap(),
    val richTextJson: String? = null        // segmentos estilizados (fuente de verdad)
)
```

```kotlin
enum class BlockType {
    TEXT, HEADING1, HEADING2, HEADING3, HEADING4,
    BULLET_LIST, NUMBERED_LIST, CHECKLIST_ITEM,
    QUOTE, CODE_BLOCK, CALLOUT, PAGE, PAGE_LINK,
    IMAGE, VIDEO, AUDIO, DRAWING, VOICE, FILE,
    TABLE, HORIZONTAL_RULE, COLLAPSIBLE
}
```

- **`richTextJson`** guarda la lista de `TextSegment` (texto + estilos) y es la **fuente de verdad** del editor.
- **`content`** contiene el markup HTML-like (misma información, formato de texto) y se usa para compatibilidad, exportación y contenido legacy.
- **Tablas** se serializan como `TableData` (headers, rows, columnWeights, bgColorHex, showHeader) dentro de `meta["table"]`.
- **Checklist / Collapsible** guardan estado en `meta` (`checked`, `summary`). Cada ítem de checklist usa `richTextJson` para su formato enriquecido.
- **Media** (imagen, vídeo, audio, voz, archivo, dibujo) guardan `fileUri`, `fileName`, `caption`, `showCaption`, `align`, `color` y el flag `wysiwyg` en `meta`; los trazos del dibujo se serializan como JSON en `meta["strokes"]`.
- **Marcador web** guarda `url` (en `content`), `title`, `description` y `favicon` en `meta`.
- **Indentación** de bloques de texto/listas se guarda en `meta["indentLevel"]`.

### Sintaxis markup interna (HTML-like)

El cuerpo de cada bloque de texto usa tags tipo HTML que parsea `RichTextParser`/`RichTextConverter`:

| Tag | Efecto |
|---|---|
| `<b>` / `</b>` | Negrita |
| `<i>` / `</i>` | Itálica |
| `<u>` / `</u>` | Subrayado |
| `<s>` / `</s>` | Tachado |
| `<code>` / `</code>` | Código en línea (monospace) |
| `<color=#RRGGBB>` | Color del texto |
| `<bg=#RRGGBB>` | Color de fondo del texto |
| `<url=URL>` | Enlace (URL o `note://id`) |
| `<sub>` / `<super>` | Subíndice / Superíndice |
| `<font=familia>` | Familia tipográfica |
| `<size=N>` | Tamaño en sp |
| `<eq>LaTeX</eq>` | Ecuación renderizada |
| `<indent>` | Sangría (4 espacios) |
| `<h1>`–`<h3>`, `<normal>` | Tamaños de encabezado / normal |
| `<item checked="…">` | Checklist |
| `<ol>`/`<ul>`/`<li>` | Listas |
| `<img>`, `<video>`, `<audio>`, `<hr>`, `<details>` | Media y reglas / colapsables |

**Escapado con backslash** — los caracteres markdown se escapan con `\` para mostrarse literales:

```
\*  \`  \_  \~  \[  \]  \(  \)  \<  \>  \\
```

**Markdown en línea** — también se interpreta al escribir:

```
**negrita**        *itálica*        `código`
~~tachado~~        [texto](url)
```

### Persistencia y migración

- **Fuente de verdad** → `richTextJson` (`TextSegment.serialize/deserialize`).
- **Compatibilidad** → `content` en markup; `ensureSegments()` devuelve `richTextJson` si existe o parsea `content`.
- **Notas legacy** (texto plano con tags) se convierten a bloques mediante `DataBlock.migrateLegacyContent` → `preprocessMarkdownBlocks` → `convertTextToBlocks`. Al cambiar el markup, mantener funcional `fromLegacyHtml` y las expresiones regulares legacy.

### Arquitectura del editor

| Capa | Archivos |
|---|---|
| **Editor visual** | `BlockEditor.kt` (orquesta bloques y `BlockRow`), `EditableTextBlock.kt` (texto WYSIWYG, base de todos los bloques de texto) |
| **Bloques específicos** | `EditableChecklistBlock.kt`, `EditableCollapsibleBlock.kt`, `EditableTableBlock.kt`, `WysiwygCodeBlock.kt`, `EditableImageBlock.kt`, `EditableVideoBlock.kt`, `EditableAudioBlock.kt`, `EditableFileBlock.kt`, `EditableBookmarkBlock.kt`, `EditableDrawingBlock.kt`, `ReadOnlyTextBlock.kt` |
| **Menú de bloques** | `SlashCommandMenu.kt` (`BLOCK_COMMANDS` con secciones Basic / Link / Media + `BlockAction` para diálogos) |
| **Barra flotante** | `FloatingEditorToolbar.kt` (modos MAIN / TEXT_FORMAT / SEARCH) + `MoreFormattingSheet.kt`, `FontSizeSheet.kt`, `TextBgColorSheet.kt`, `ColorSelectionDialog.kt` |
| **Conversión markup ⇄ segmentos** | `RichTextConverter.kt` (`markupToSegments`, `segmentsToMarkup`, `applySpanStyle`, `segmentsToAnnotatedString`), `OffsetMapper.kt` |
| **Renderizado legacy / preview** | `RichTextParser.kt`, `HtmlTagParser.kt`, `NoteContentBlockRenderer.kt`, `NoteContentBlockCard.kt` |
| **Utilidades de bloque** | `VideoUrlHelper.kt` (YouTube/shorts), `BookmarkMetadataFetcher.kt` (Jsoup), `DrawingStrokeCodec.kt` / `DrawingStroke.kt`, `AudioPlayerWidget.kt`, `CodeHighlighter.kt`, `CodeLanguages.kt`, `MathRenderer.kt` (LaTeX) |
| **Exportación** | `RichTextConverter` (markdown/html), `util/export/` (`MarkdownExporter`, `HtmlExporter`, `TxtExporter`, `JsonExporter`, `PdfExporter`) |
| **Modelo** | `DataBlock.kt`, `RichText.kt` (`TextSegment`), `DrawingStroke.kt` (`com.example.data.model`) |

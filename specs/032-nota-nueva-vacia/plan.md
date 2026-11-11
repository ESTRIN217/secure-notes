# Spec 032 — Plan (nota nueva vacía se descarta)

> RF y RNF referencian `spec.md`. Tests primero, y las pruebas nuevas son **puras**: esta plataforma no
> ejecuta Robolectric (RNF-135, RNF-139).

## Archivos y responsabilidades

| Archivo | Responsabilidad | RF / RNF |
|---|---|---|
| `app/src/main/java/com/example/ui/DraftEmptiness.kt` (**nuevo**) | La pieza pura: `isNoteEmpty(title, blocks, attachments)`, `blockHasContent(block)`, `segmentsHaveText(raw: String?)`, `tableHasCell(json: String?)`, `drawingHasStrokes(block)`. Sin `android`, sin Compose, sin `Context`. | RF-210, RNF-135 |
| `app/src/main/java/com/example/ui/DraftWrite.kt` | Añade a `DraftWriteGate` la propiedad `createdRowId: Int` — la fila que esta sesión insertó (`noteId == 0 && state > 0`), `0` si no la hay. | RF-208, RF-212 |
| `app/src/main/java/com/example/ui/NoteEditorScreen.kt` | `hasContentToSave()` pasa a la regla nueva; `handleSaveAndExit` deja de guardar/tostar si está vacía y, si el borrador creó fila esta sesión, la borra; el autoguardado y `ON_STOP` heredan la regla por `saveCurrentNote()`; `draftGuard?.discardAction` se cablea al borrado defensivo. | RF-207, RF-208, RF-209, RF-211, RF-212, RF-214 |
| `app/src/main/java/com/example/ui/viewmodel/NotesViewModel.kt` | Nuevo `fun deleteNoteById(id: Int)` — IO, `getNoteById` → `noteDao.deleteNote`, `refreshHomeWidgets()`, `Log.e` en fallo. | RF-208, RF-212, RNF-137 |
| `app/src/test/java/com/example/DraftEmptinessTest.kt` (**nuevo**) | La regla, bloque a bloque y en los casos límite de la spec. | RF-210, RNF-135 |
| `app/src/test/java/com/example/DraftWriteTest.kt` | Casos nuevos de `createdRowId`. | RF-208 |

Nada más cambia: ni `strings.xml`, ni `build.gradle.kts`, ni dependencias ni `oss-licenses.json` (RNF-136).

## Funciones puras

```kotlin
fun isNoteEmpty(title: String, blocks: List<DataBlock>, attachments: List<Attachment>): Boolean
fun blockHasContent(block: DataBlock): Boolean
private fun textBlockHasContent(block: DataBlock): Boolean   // segmentsBlockTypes
private fun segmentsHaveText(richTextJson: String?): Boolean  // TextSegment.deserialize
private fun tableHasCell(tableJson: String?): Boolean         // TableData.fromJson
private fun drawingHasStrokes(block: DataBlock): Boolean
```

Reglas por tipo (la definición de «Vacía» de la spec, literal):

```
HORIZONTAL_RULE            -> false
IMAGE VIDEO AUDIO VOICE FILE BOOKMARK -> content.isNotBlank()
PAGE PAGE_LINK             -> meta["noteId"]?.isNotBlank() == true
TABLE                      -> alguna celda no blank (TableData.fromJson)
DRAWING                    -> wysiwyg: runCatching { JSONArray(content).length() > 0 }
                                  .getOrDefault(content.isNotBlank())
                               legacy : content.isNotBlank()
resto (texto)              -> algún segmento con text/equationLatex no vacío;
                              sin richTextJson: content sin etiquetas no blank
```

`isNoteEmpty = title.isBlank() && attachments.isEmpty() && blocks.none(::blockHasContent)`

`title` se evalúa con `isBlank()` (el editor guarda `title.trim()` igualmente en el guardado, así que no
hace falta recortar aquí).

## Algoritmo

1. `DraftEmptiness.kt` nace con sus pruebas en rojo (`DraftEmptinessTest`).
2. `DraftWriteGate.createdRowId` nace con sus pruebas en rojo (añadidas a `DraftWriteTest`).
3. `NotesViewModel.deleteNoteById` (sin prueba directa: es IO sobre Room; se cubre por invariantes ya
   existentes si las hubiera y por la revisión; el borrado definitivo es del DAO).
4. Cableado en `NoteEditorScreen`:
   - `hasContentToSave()` → `!isNoteEmpty(title, blocks, attachments)` («hay contenido que merezca fila»;
     RF-210).
   - `canPersistChanges() = { noteId != 0 || hasContentToSave() }` — **persistir es otra cosa que tener
     contenido**: una nota existente vaciada sí se persiste (RF-210, RF-214) para que las tachadas del
     usuario lleguen a la base de datos; un borrador solo persiste con contenido. La usan el autoguardado
     (`canPersistChanges() && draftWriteId(...) != 0`) y `saveCurrentNote()` (que heredan `ON_STOP` y
     atrás).
   - `handleSaveAndExit`:
     ```
     if (noteId != 0 || hasContentToSave()) { saveCurrentNote(); toast }
     else if (draftWrite.createdRowId > 0) viewModel.deleteNoteById(draftWrite.createdRowId)
     onBack()
     ```
     (RF-207, RF-208)
   - `draftGuard?.discardAction = { if (draftWrite.createdRowId > 0 && !hasContentToSave())
         viewModel.deleteNoteById(draftWrite.createdRowId) }` (RF-212; hoy inalcanzable porque el diálogo
     exige `writeState == 0`, queda como defensa si el guard cambia)
   - El borrado **nunca** a mitad de sesión (RNF-138): el autoguardado solo desiste de persistir la nota
     vacía; el id de la fila lo lleva el borrador, no un campo nuevo del editor.
5. `isDraftUnsaved(noteId, state, hasContent = hasContentToSave())` queda con la forma actual; el resultado
   correcto cae de la regla nueva (RF-211).

## Decisiones justificadas

- **La pieza pura decide, el Composable cablea.** La decisión de «vacía» es dato puro (`String`,
  `List<DataBlock>`, `List<Attachment>`), sin disco ni Room: se prueba sin Robolectric, como manda el
  proyecto. Alternativa descartada: extender `hasContentToSave()` inline en el `@Composable` — sería una
  regla sin prueba.
- **Se borra al salir, no a mitad de sesión.** Al vaciar una nota que ya tiene fila, borrarla al instante
  rompería el re-escribir (cada tecla re-insertaría) y las referencias de adjuntos ya hechas. Atrás y
  «Descartar» son salidas definitivas y son los únicos sitios de borrado. Alternativa descartada: borrar en
  el autoguardado.
- **Borrado definitivo** (`noteDao.deleteNote`), no papelera (RNF-137). Alternativa descartada:
  `moveToTrash` — llenaría la papelera de notas fantasma.
- **`createdRowId` en `DraftWriteGate`, no una variable nueva en el editor.** El id de la fila vive en la
  puerta desde la spec 027; el editor no debe duplicar ese estado.
- **`hasContentToSave()` conserva su nombre y firma.** Los call sites existentes (autoguardado, `ON_STOP`,
  atrás, guard) pasan a significar lo correcto sin tocarlos. Alternativa descartada: renombrar y tocar
  todos los call sites — churn sin cambio.
- **`isNoteEmpty` no llama a `ensureSegments()`.** `ensureSegments()` pasa por `RichTextConverter` (imports
  de Compose/`android`); aquí se lee `richTextJson` directamente con `TextSegment.deserialize` (probado en
  tests puros ya) y `content` legacy con un `Regex` local.
- **`TABLE` cuenta por celdas, no por `meta["table"]`.** Una tabla 3×3 vacía serializa `"rows":[["","",""]]`
  — no vacía por string, vacía por datos.
- **`DRAWING` wysiwyg cuenta por trazos** (`JSONArray.length()`), por el mismo motivo (`"[]"` no es contenido).
- **`HORIZONTAL_RULE` no es contenido.** Una línea sola no es información; es decoración.
- **Notas existentes vaciadas** (`id > 0`): no se borran ni se deja de persistir el contenido vacío; la spec
  habla de notas **nuevas** (RF-214, spec § Fuera de alcance).

## Estrategia de tests

`DraftEmptinessTest` (puro, `:app`):

1. La nota nueva de fábrica (`blocks = emptyList()`) está vacía — la regresión de esta spec.
2. Título con espacios → vacía; título con contenido → no.
3. Adjunto cualquiera → no vacía.
4. Un `TEXT` con solo estilos (bold) y sin texto → vacía; con texto o ecuación → no.
5. Bloque legacy (`richTextJson == null`, `content = "<b>texto</b>"`) → no; `content = "<b></b>"` → vacía.
6. Cada tipo de bloque con y sin contenido (tabla, dibujo `"[]"`/`"[…]"`, PAGE con/sin `noteId`, BOOKMARK,
   `HORIZONTAL_RULE` solo → vacía).
7. Una lista con **un solo bloque vacío entre otros** está vacía; una con un bloque con contenido no.
8. `isNoteEmpty` con título en blanco + contenido en bloque → no vacía.

`DraftWriteTest` — casos añadidos:

9. `createdRowId == 0` al empezar con `noteId == 0`; tras `write { 7 }` → `7`; durante `WRITE_IN_FLIGHT` → `0`;
   con `noteId == 5` → `0` (nota existente, la sesión no creó fila).

`./gradlew test` — los 38 fallos de plataforma y los 3 preexistentes siguen siendo los mismos (RNF-139).
Los tests nuevos pasan en este host (`linux/aarch64`).

## Invariante de fuentes (opcional, como en spec 027)

`EditorInvariantsTest` puede ganar el caso «`hasContentToSave` delega en `isNoteEmpty`» si la
comprobación literal de fuentes es legible; si `EditorInvariantsTest` ya cubre el patrón «el editor no
reescribe la regla», basta con RF-210.

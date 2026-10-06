# Plan — Spec 006 · Rediseño de la pantalla de chat con IA

Spec: `specs/006-rediseno-chat-ia/spec.md` · Madre: `specs/001-limites-constitucion/`

> **La spec manda.** Este plan no añade requisitos: solo decide *cómo* se llega a lo que la spec dice.
> Donde la spec deja algo abierto, la decisión está escrita aquí con su alternativa descartada.

---

## 1. Punto de partida: qué está hecho y qué no

Los tres prototipos son **código existente y editable**, no un punto de partida que haya que
reconstruir:

| Archivo | Estado real hoy | Qué le falta |
|---|---|---|
| `ui/FloatingChatInputField.kt` | Los dos `@Preview` **ya borrados**. Cuerpo compilando. | Firma, decisiones por lógica pura, `contentDescription` literales, `enabled` sin adjuntos ni bloqueo en generación, sin elevación de sombra, chips sin `key`, import muerto `Preview` |
| `ui/EmptyChatWelcome.kt` | Los dos `@Preview` **ya borrados**. Cuerpo compilando. | `userName = "Alexander"` con saludo literal, 4 sugerencias literales, sin lógica pura, sin `key`/`contentType`, import muerto (`Preview`) |
| `ui/AdjuntosAiBottonSheet.kt` | Previews borrados. **Ya solo tiene las 2 opciones** (Nota, Archivo) — D1 aplicado a mano. | Sin `ModalBottomSheet` ni `onDismissRequest`, `onClick = {}`, `AttachmentListTile` duplica `SettingsListTile`, subtítulos literales, nombre de archivo con errata |

Consecuencias para este plan:

- **No hay tarea de borrar previews.** Ya está hecho. El criterio de cierre 1 de la spec pasa a ser una
  **comprobación**: `./gradlew :app:assembleDebug` y dejar el resultado real escrito.
- Los tres archivos siguen **sin trackear** en git, y `find app/src/main -name '*Botton*'` falla hoy.
  Eso es exactamente lo que RF-39 quiere: el criterio se cumple cuando el archivo se renombra.
- **⚠️ Colisión de nombres**: hay dos `fun EmptyChatWelcome` públicas en `com.example.ui`
  (`AiChatScreen.kt:~1073` y `EmptyChatWelcome.kt`).

  > **Corrección posterior a la implementación.** Este plan decía que `assembleDebug` fallaba por esa
  > colisión, y **es falso**: son **dos sobrecargas homónimas con firmas distintas**, la llamada
  > resolvía a la vieja por argumentos con nombre y Kotlin lo acepta sin quejarse. Verificado:
  > `./gradlew :app:assembleDebug` dio `BUILD SUCCESSFUL` **antes de tocar nada**. Los 7 errores de
  > compilación estaban **solo** en los `@Preview`, borrados por la persona usuaria antes de empezar.
  > Descolisionar **sí** era necesario —RF-39 quiere una sola pantalla de bienvenida, y con la firma
  > nueva la vieja habría quedado como duplicada muerta—, pero **no era una urgencia de compilación**.
  > T1 se mantiene y sigue siendo la primera tarea, por RF-39 y no por el build.

---

## 2. Verificación: el comando de la entrega

Este es **el** comando. Todas las tareas lo referencian y el criterio de cierre 3 de la spec lo da por
fijado:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.example.Chat*'
```

**Por qué este y no otro.** `--tests` con filtro sigue compilando **todo** el conjunto de pruebas
antes de ejecutar, así que el comando **demuestra también D4**: que `compileDebugUnitTestKotlin` ya
compila. Y al filtrar, no ejecuta las suites con entorno de pruebas (Robolectric/Roborazzi), que en
esta plataforma no tienen runtime nativo para `linux/aarch64` y no pueden darse por verdes.

Las cuatro suites nuevas se llaman **`ChatSuggestionsTest`, `ChatGreetingTest`, `ChatInputStateTest` y
`ChatAttachmentSheetOptionTest`**, todas en `package com.example` bajo
`app/src/test/java/com/example/`, como las pruebas puras que ya existen. La cuarta empieza por `Chat` a
propósito, para que **un solo patrón** las cubra.

Comandos de apoyo, cada uno con su papel:

```bash
./gradlew :app:assembleDebug          # criterio de cierre 1; resources incluidos (AAPT2)
./gradlew :app:compileDebugUnitTestKotlin   # si hay que ver el error de compilación de tests tal cual
```

**Convergencia si el conjunto de pruebas no compilara.** Si T11 descubre que
`compileDebugUnitTestKotlin` falla, T11 escribe el error real en `tasks.md` y `MEMORY.md`, el criterio
de cierre 2 de la spec se cumple **documentando ese resultado**, y el comando de la entrega pasa a ser
el que quede anotado ahí. **No se toca ningún archivo de build para hacerlo posible** (eso sería
dependencia nueva, RNF-10).

> **⚠️ `UP-TO-DATE` no es un verde.** La implementación dio con esto y es un error a evitar de la misma
> familia que el de AAPT2: el primer `BUILD SUCCESSFUL` del comando de la entrega salió **`UP-TO-DATE`**
> con `compileDebugKotlin FROM-CACHE`, es decir **sin ejecutar un solo caso**. Solo con
> `--rerun --no-build-cache` se ejecutan los **53** casos de verdad. Un `BUILD SUCCESSFUL` del que no se
> sabe si ejecutó nada no prueba nada, igual que un `compileDebugKotlin` limpio no prueba que los
> recursos sean válidos. Cuando T11 ejecute el comando, tiene que comprobar que hay casos ejecutados en
> la salida, y si no los hay, repetirlo con `--rerun --no-build-cache`.

**Rojo antes que verde.** En cada tarea de lógica pura (T2–T5) el orden dentro de la tarea es: escribir
la prueba, ejecutar el comando y ver que **falla**, anotar ese rojo en `tasks.md`, escribir la lógica,
ejecutar de nuevo y ver que pasa. El rojo de la primera ejecución es un fallo de compilación —la
función todavía no existe— y eso es un rojo legítimo.

---

## 3. Archivos

### Nuevos

| Archivo | Qué contiene |
|---|---|
| `app/src/main/java/com/example/ui/chat/ChatSuggestions.kt` | L1, L2 |
| `app/src/main/java/com/example/ui/chat/ChatGreeting.kt` | L3 |
| `app/src/main/java/com/example/ui/chat/ChatInputState.kt` | L4, L6, L7 y `ATTACHMENT_CHIP` |
| `app/src/main/java/com/example/ui/chat/AttachmentSheetOption.kt` | L5 |
| `app/src/test/java/com/example/ChatSuggestionsTest.kt` | L1, L2 |
| `app/src/test/java/com/example/ChatGreetingTest.kt` | L3 |
| `app/src/test/java/com/example/ChatInputStateTest.kt` | L4, L6, L7 |
| `app/src/test/java/com/example/ChatAttachmentSheetOptionTest.kt` | L5 |

### Modificados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/example/ui/FloatingChatInputField.kt` | Firma sin `viewModel`, decisiones por L4, `contentDescription` desde recursos, elevación, chips con `key`, import muerto fuera |
| `app/src/main/java/com/example/ui/EmptyChatWelcome.kt` | Sin `userName`, saludo y etiquetas desde recursos, `suggestions` de L1, `key`+`contentType`, import muerto fuera |
| `app/src/main/java/com/example/ui/AdjuntosAiBottonSheet.kt` → **`AttachmentAiSheet.kt`** | `ModalBottomSheet` + `onDismissRequest`, `SettingsListTile`, L5, subtítulos desde recursos, `AttachmentListTile` borrada |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | `bottomBar` sustituido, `DropdownMenu` fuera, hoja montada, welcome cableado, `EmptyChatWelcome` viejo borrado |
| `app/src/main/res/values*/strings.xml` (los 9) | 6 claves nuevas + 3 rellenadas en `en-rGB` |

### No tocados

`MessageBubble`, `MessageStatusIcon`, `ChatMessageList`, `StreamingBubble`, `TypingIndicator`,
`ChatHistoryDrawerContent`, `NoteAttachmentSheet` (RNF-13) · `com/example/data/**`,
`com/example/util/export/**` (RNF-14) · `viewmodel/**` · `build.gradle.kts`,
`gradle/libs.versions.toml` (RNF-10) · `AiBottomSheet.kt` (código muerto, fuera de alcance) ·
cualquier otro archivo de `app/src/test/**` (RF-19).

**Los siete intocables viven dentro de `AiChatScreen.kt`.** El trabajo ahí es **quirúrgico**: diff por
declaración, no reescritura del archivo. El criterio de RNF-13
(`git diff -U0 … | grep -E '^-…fun (MessageBubble|…)'`) es el que lo vigila.

---

## 4. Funciones puras (los cuatro archivos de lógica)

Los cuatro **no importan `androidx.compose`**, **no acceden a `R`** y **no leen el reloj**. Todas son
públicas, ≤ 20 líneas, con cláusulas de guarda.

### `chat/ChatSuggestions.kt` — L1, L2

```kotlin
enum class ChatSuggestionAction { GENERATE, SUMMARIZE }

data class ChatSuggestion(
    val id: String,                 // "poem" | "ideas" | "explain" | "summarize"
    val label: String,              // ya resuelta con stringResource en el Composable
    val action: ChatSuggestionAction
)

fun visibleSuggestions(
    hasSelectedText: Boolean,
    hasNoteContent: Boolean,
    hasAttachments: Boolean
): List<ChatSuggestion>            // las 4, o las 3 de generación si no hay nada que resumir

fun requestFor(
    suggestion: ChatSuggestion,
    selectedText: String,
    noteContent: String,
    summarizePrompt: String,
    targetLanguage: String
): AiRequest
```

`requestFor` en pseudocódigo:

```
si suggestion.action == SUMMARIZE:
    AiRequest(action = SUMMARIZE, prompt = summarizePrompt,
              selectedText = selectedText, context = noteContent,
              targetLanguage = targetLanguage)
si no:
    AiRequest(action = GENERATE, prompt = suggestion.label,
              selectedText = selectedText, context = noteContent,
              targetLanguage = targetLanguage)
```

Las etiquetas llegan **ya resueltas**: el archivo de lógica no sabe de recursos ni de `R`.

### `chat/ChatGreeting.kt` — L3

```kotlin
fun greetingIndexFor(day: Long, count: Int): Int
```

```
si count <= 0: devolver 0
r = day mod count
devolver r si r >= 0, y r + count si no      // días negativos no dan índice negativo
```

Cíclico por construcción: `greetingIndexFor(d + count, count) == greetingIndexFor(d, count)`.

### `chat/ChatInputState.kt` — L4, L6, L7

```kotlin
internal const val ATTACHMENT_CHIP = 4

enum class ChatHint { FOLLOWUP, GENERATE, SUMMARIZE, FIX_GRAMMAR }
enum class WriteTarget { DRAFT, EDITING }

fun canSend(text: String, hasAttachments: Boolean, isProcessing: Boolean): Boolean
fun shouldSend(text: String, hasAttachments: Boolean): Boolean
fun hintFor(hasHistory: Boolean, action: AiAction): ChatHint
fun chipKey(index: Int, name: String): String

fun effectiveText(draft: String, editing: String?): String
fun writeTarget(hasEditing: Boolean): WriteTarget
fun adoptPendingEditing(pendingEditing: String?): Pair<String, String?>

fun shouldClearInput(action: AiAction): Boolean
```

```
canSend    = isProcessing || text.isNotBlank() || hasAttachments
shouldSend = !(text.isBlank() && !hasAttachments)        // el guard de hoy, sin cambiarlo
hintFor    = FOLLOWUP si hasHistory; si no, según action
chipKey    = "chip_" + index + "_" + name                // única por construcción (decisión 7)
effectiveText  = editing si editing != null, si no draft
writeTarget    = EDITING si hasEditing, si no DRAFT
adoptPendingEditing(e) = (e, null)  // la rama del LaunchedEffect, sin effects
shouldClearInput      = action == GENERATE
```

### `chat/AttachmentSheetOption.kt` — L5

```kotlin
enum class AttachmentSource { NOTE, FILE }

fun attachmentSourceFor(id: String): AttachmentSource     // "note" -> NOTE, "file" -> FILE
```

Sin MIME: los tres tipos de archivo **se quedan escritos en `AiChatScreen.kt`**, donde están hoy,
porque el criterio de RF-35 los busca literalmente en ese archivo. Ver «Decisiones» D-4.

---

## 5. Interfaz: qué muestra cada pieza

Los tres Composables **solo pintan y lanzan eventos**. Las decisiones llegan ya hechas desde
`AiChatScreen`, que es quien tiene el historial, la acción, el idioma y los recursos. Esa es la lectura
de RNF-11 que hace coherente toda la entrega.

### `FloatingChatInputField` (sin `viewModel`)

```kotlin
@Composable
fun FloatingChatInputField(
    attachments: List<FileAttachment>,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,          // de L4
    isProcessing: Boolean,
    hint: ChatHint,            // de L4; aquí se traduce con stringResource
    onRemoveAttachment: (Int) -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Pinta: `Surface` redondeada con `tonalElevation = 3.dp` **y** `shadowElevation = 8.dp`; `LazyRow` de
fichas con `key = { chipKey(it.first, it.second.name) }` y `contentType = { ATTACHMENT_CHIP }`; campo
`enabled = !isProcessing`; botón de enviar/detener con `enabled = canSend || isProcessing`, color
animado, `errorContainer` en modo detener y rebote de escala; tres `contentDescription` desde recursos
(`R.string.attach`, `R.string.ai_send`, `R.string.ai_stop`).

**Descartado: mantener `AiViewModel` como parámetro** (lo hace el prototipo). Se descarta porque el
campo no debe leer un `StateFlow` —eso es recoger datos, no pintar— y porque quitarlo elimina el
`collectAsStateWithLifecycle` del archivo.

### `EmptyChatWelcome` (sin `userName`)

```kotlin
@Composable
fun EmptyChatWelcome(
    greeting: String,
    suggestions: List<ChatSuggestion>,
    onSuggestion: (ChatSuggestion) -> Unit
)
```

Pinta: `SuggestionChip` en un `LazyRow` con `key = { it.id }` y `contentType = { SUGGESTION_CHIP }`.
**No** recibe ni lee el nombre de la persona usuaria.

### `AttachmentAiSheet`

```kotlin
@Composable
fun AttachmentAiSheet(
    onPickNote: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit
)
```

Pinta: `rememberBottomSheetState` + `ModalBottomSheet(onDismissRequest = onDismiss)` con **dos**
`SettingsListTile` (de `com.example.ui.settings`, ya existente), con `leadingIcon` y subtítulo desde
recursos. `AttachmentListTile` **se borra**: es una copia de `SettingsListTile` (RF-34).

Cada opción: `attachmentSourceFor(id)` → `onDismiss()` **primero**, luego `onPickNote()` o
`onPickFile()`.

### Cableado en `AiChatScreen` (quirúrgico)

- **Borra** `bottomBar = { … }` completo (líneas ~321-490) y pon `FloatingChatInputField(…)`.
- **Borra** el estado `showAttachmentMenu`, el `DropdownMenu` y su `Box` envoltorio.
- **Añade** `showAttachmentSheet`, monta `AttachmentAiSheet` junto al `NoteAttachmentSheet` que ya
  existe **sin tocarlo**, y `onPickNote` deja `showNotePicker = true`, `notePickerSearchQuery = ""` y
  llama a `loadAvailableNotes()`; `onPickFile` lanza `filePickerLauncher` con los tres tipos que ya
  están escritos ahí.
- **Borra** el `fun EmptyChatWelcome` viejo (líneas ~1073-1137) y deja el punto de la pantalla vacía
  hasta que T10 lo cablee.
- **Cablea** el welcome nuevo: `remember { todayIndex() }`, `greeting` con `stringResource` +
  `greetingIndexFor`, `suggestions` con `visibleSuggestions`, y `onSuggestion` llamando a
  `viewModel.execute(requestFor(…), effectiveSessionId)`.
- **Conserva** tal cual: el `enabled = !isProcessing` del campo (ahora en el nuevo), el hueco de
  `sendEnabled`, el `LaunchedEffect` que carga la edición, la rama de edición que re-pregunta con
  `AiAction.GENERATE`, el haptic `LongPress`, y el mapeo acción→petición de la rama de envío
  (decisión 11).

---

## 6. Recursos (RF-40)

Claves nuevas, en los **9** idiomas, traducciones decentes (no marcadores):

| Clave | `values/` | Para |
|---|---|---|
| `ai_suggestion_explain_concept` | Explain a concept | 4.ª sugerencia |
| `ai_chat_greeting_1` | Ready when you are | Saludo, 1.ª entrada |
| `ai_chat_greeting_2` | What shall we write today? | Saludo, 2.ª entrada |
| `ai_chat_greeting_3` | Let's put something on paper | Saludo, 3.ª entrada |
| `attach_note_subtitle` | Pick a note from your library | Subtítulo de la opción Nota |
| `attach_file_subtitle` | Pick a text, JSON or XML file | Subtítulo de la opción Archivo |

Se **reutilizan** `ai_suggestion_poem`, `ai_suggestion_ideas`, `ai_summarize` y
`ai_chat_prompt_summarize`, que ya están traducidas.

Claves **rellenadas** en `values-en-rGB/strings.xml`, que hoy faltan: `attach_note` → *Attach note*,
`attach_file` → *Attach file*, `remove` → *Remove*.

**Se entregarán 3 saludos**, no los 2 mínimos de RF-40: con un ciclo de dos días el saludo parece un
fallo. El criterio de RF-40 pide «cinco claves nuevas como mínimo» y con tres saludos son seis, así que
sigue cumpliéndose.

**AAPT2 manda**: los apóstrofos crudos rompen `mergeDebugResources` **después** de que Kotlin compila
limpio. Todo apóstrofo se escribe `\'`. En los nueve idiomas, no solo en `values/`.

---

## 7. Orden de tareas y por qué

1. **T1 descolisión** — desbloquea `assembleDebug`, que es la puerta de verificación de todo lo demás.
2. **T2–T5 lógica pura** — antes que la interfaz, porque la interfaz la consume; y cada una con su
   prueba en rojo antes (principio 4).
3. **T6 recursos** — después de la lógica (no depende) y **antes** de la interfaz: un `R.string` que no
   existe hace fallar a Kotlin, y con AAPT2 el fallo llega tarde.
4. **T7–T9 las tres piezas** — campo, welcome y hoja, cada una con sus decisiones ya puras.
5. **T10 cableado** — une todo en `AiChatScreen.kt`; al final para no tocar el archivo más de una vez.
6. **T11 comprobación** — ejecuta **todos** los criterios y deja los resultados escritos.
7. **T12 `MEMORY.md` y T13 `AGENTS.md`** — cierre documental.

---

## 8. Decisiones y alternativas descartadas

- **D-1 · El campo no recibe `AiViewModel`.** *Descartado:* mantenerlo, como hace el prototipo. El
  campo debe pintar y lanzar eventos (RNF-11), no leer un `StateFlow`; además el `AiChatScreen` ya
  recoge ese flujo.
- **D-2 · El `bottomBar` conserva su `Surface` y su elevación, y el campo lleva la forma redondeada.**
  *Descartado:* hacer que cada uno aporte su elevación, que duplicaría la sombra. La `Surface` exterior
  del campo conserva `tonalElevation = 3.dp` **y** `shadowElevation = 8.dp` (RF-33 i).
- **D-3 · El día se calcula en `AiChatScreen` con `remember` sin claves**, y el saludo se resuelve allí
  con `stringResource`. *Descartado:* calcularlo dentro de `EmptyChatWelcome`, que obligaría a que el
  welcome leyera recursos y decidiera el saludo por su cuenta.
- **D-4 · Los tres tipos de archivo se quedan escritos en `AiChatScreen.kt`.** L5 mapea opción→acción
  y **no** lleva la lista de tipos. *Descartado:* mover la lista a `AttachmentSheetOption.kt`, que sería
  lo limpio, porque el criterio aprobado de RF-35 la busca literalmente en `AiChatScreen.kt`. Es una
  tensión real entre la spec y su propio criterio, y se resuelve a favor del criterio; si en la
  validación se prefiere lo contrario, es un cambio de una línea más un ajuste del criterio.
- **D-5 · `EmptyChatWelcome` recibe `greeting: String` y `suggestions`.** *Descartado:* que el welcome
  los resuelva, o que reciba `hasSelectedText`/`hasNoteContent` y llame a L1 por su cuenta. Ambas lo
  convierten en un Composable que decide.
- **D-6 · `AttachmentAiSheet` es dueña de su `ModalBottomSheet`.** *Descartado:* que la pantalla monte
  el `ModalBottomSheet` y la hoja sea solo contenido; entonces el hoja no «se puede descartar» por sí
  sola, que es lo que pide RF-34.
- **D-7 · Las cuatro suites se llaman `Chat*Test`.** *Descartado:* `AttachmentSheetOptionTest`, que
  obligaría a un segundo patrón en el comando.
- **D-8 · Las funciones de lógica son públicas, sin `internal`.** *Descartado:* `internal`, que obliga a
  depender de `-Xfriend-paths` de AGP para que las pruebas las vean.
- **D-9 · Una tarea por archivo de lógica, no por lógica.** L4, L6 y L7 comparten
  `ChatInputState.kt`; separarlas daría tres ciclos rojo/verde sobre el mismo archivo sin ganancia. Las
  tres lógicas tienen sus casos en la misma suite.

---

## 9. Riesgos asumidos (los verificará el revisor en la fase de validación)

- **Riesgo 10 de la spec — nada comprobado en dispositivo.** El campo flotante puede quedar tapado por
  el teclado, la hoja puede no cerrarse a tiempo y la elevación puede leerse distinta. **Alcance de esta
  entrega:** no se comprueba nada en dispositivo. `tasks.md` lo registra como pendiente y no como
  incumplido. Lo mismo para el **orden** de RF-35 (cerrar antes de abrir) y la búsqueda vacía del
  selector: su única verificación es leer el diff y mirar la pantalla.
- **Riesgo 11 de la spec — verificación limitada a `grep` y pruebas puras.** Los criterios verifican
  que el texto está escrito, no que la pantalla se pinte bien. **Alcance:** T11 ejecuta los `grep` y
  deja su salida real; la apariencia se juzga en dispositivo, fuera de esta entrega.
- **Riesgo 8 de la spec — árbol de trabajo sucio.** `ui/table/` y `TableSelectionGeometryTest.kt`
  siguen sin seguimiento, y `EditableTableBlock.kt` modificado, todo de la spec 005. **Alcance:** los
  criterios de `git diff` van acotados a ruta y el de archivos nuevos excluye `ui/table/`; esta entrega
  **no** los toca ni los añade a los criterios.
- **Riesgo 2 de la spec — `./gradlew test`.** El comando de la entrega verifica que el conjunto de
  pruebas **compila**, que es lo que D4 pedía. Si en T11 apareciera un fallo de compilación ajeno, se
  documenta y no se tapa.
- **Riesgo 3 de la spec — la suite con entorno de pruebas no corre en esta plataforma** (sin runtime
  nativo para `linux/aarch64`). **Alcance:** las cuatro suites de esta entrega son puras y sí corren; lo
  que no corre es Robolectric/Roborazzi, y por eso el comando filtra con `--tests`. **Dato nuevo que
  aportó la implementación:** el primer verde del comando de la entrega fue **`UP-TO-DATE`** con
  `compileDebugKotlin FROM-CACHE`, o sea **`BUILD SUCCESSFUL` sin ejecutar un solo caso**; solo con
  `--rerun --no-build-cache` corren los **53** casos. **Consecuencia para T11:** un `BUILD SUCCESSFUL`
  del que no se sabe si ejecutó algo no se acepta como verde, y hay que comprobar el número de casos en
  la salida. Es un error a evitar de la misma familia que el de AAPT2, y va al registro de errores de
  `MEMORY.md` en T12.
- **Riesgo de AAPT2.** Kotlin en verde no dice nada de los recursos. T6 termina con `assembleDebug`,
  no con `compileDebugKotlin`.
- **Límite de tamaño de tarea.** Cada tarea cabe en una llamada del @implementer y termina con un
  resultado observable. T1–T10 son implementación; T11 es comprobación; T12 y T13 son cierre
  documental. Son 13 en total porque las dos de cierre documental van **separadas**, cada una con su
  RF, como pidió la spec.

---

## 10. Tabla tarea → RF

| Tarea | Qué hace | RF |
|---|---|---|
| T1 | Descolisión: borra el `EmptyChatWelcome` viejo | RF-39 |
| T2 | `ChatSuggestions.kt` — L1, L2 | RF-37, RF-38, RF-41, RNF-11 |
| T3 | `ChatGreeting.kt` — L3 | RF-36, RF-41, RNF-11 |
| T4 | `ChatInputState.kt` — L4, L6, L7, `ATTACHMENT_CHIP` | RF-33 a, c, e, f, h, RF-39, RF-41, RNF-11 |
| T5 | `AttachmentSheetOption.kt` — L5 | RF-34, RF-35, RF-41, RNF-11 |
| T6 | Las 9 `strings.xml`: 6 claves nuevas + 3 en `en-rGB` | RF-40 |
| T7 | `FloatingChatInputField.kt` | RF-33 a–j, RF-39 |
| T8 | `EmptyChatWelcome.kt` | RF-36, RF-37, RF-38, RF-40 |
| T9 | `AdjuntosAiBottonSheet.kt` → `AttachmentAiSheet.kt` | RF-34, RF-35, RF-39, RF-40 |
| T10 | Cableado en `AiChatScreen.kt` | RF-33, RF-34, RF-35, RF-39, RNF-13 |
| T11 | Comprobación: todos los criterios, con salida real | todos |
| T12 | `MEMORY.md` ≤ 50 líneas | RF-20 |
| T13 | `AGENTS.md`: nota de tests y nota de `key` | D4 y RNF-15 (cierre 6) |
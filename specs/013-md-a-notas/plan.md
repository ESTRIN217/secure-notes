# Plan — Spec 013 · Del Markdown del editor a la base de notas

Spec: `specs/013-md-a-notas/spec.md` · Encadena con: `specs/012-markdown-gfm-editor/`

Cubre RF-76 a RF-83 y RNF-33 a RNF-38.

## 1. Archivos y responsabilidades

| Archivo | Cambio |
|---|---|
| `:editor-de-codigo`/`ui/CodeEditorApp.kt` | Nuevo parámetro `onSendToNotes: ((name: String, markdown: String) -> Unit)? = null` y el item de menú, visible solo si hay anfitrión y el lenguaje es Markdown (RF-76). |
| `:editor-de-codigo`/`res/values*/strings.xml` (×8) | `menu_send_to_notes`, `toast_note_created`, `toast_note_failed`. |
| `app/util/FileImporter.kt` | Se extrae `markdownToBlockContent(raw): String`, pura y testeable (RF-83, RNF-38). `convertToBlockContent` la llama. |
| `app/ui/viewmodel/NotesViewModel.kt` | `createNoteFromMarkdown(name, markdown): Result<Int>` (RF-78, RNF-33). |
| `app/CodeEditorActivity.kt` | Escribe el traspaso en `cacheDir` y arranca `MainActivity` (RF-81, RNF-34). |
| `app/MainActivity.kt` | Consume el traspaso, crea la nota, la abre y borra el fichero (RF-82, RNF-35). |
| `app/ui/NoteEditorTabsHost.kt` | El anfitrión de la pestaña crea la nota y la abre en una pestaña (RF-80). |

## 2. El contrato entre la librería y la app

`CodeEditorApp` gana **un** parámetro opcional:

```kotlin
fun CodeEditorApp(
    viewModel: CodeEditorViewModel,
    topTabs: @Composable () -> Unit = {},
    onSendToNotes: ((name: String, markdown: String) -> Unit)? = null
)
```

Tres propiedades, y las tres importan:

1. **Es opcional con valor `null`**, así que los tres llamantes actuales (`CodeEditorActivity` y
   `TextTabContent`) siguen compilando sin cambios. Una interfaz nueva obligatoria habría obligado a
   tocar llamantes que no la usan.
2. **`null` significa «sin función»**, y el item de menú no se compone. Una librería que ofrece una
   acción que no puede cumplir es una acción que falla en pantalla.
3. **Recibe `String`, no tipos de nota.** Si recibiera un `Note`, la librería dependería del modelo de
   `:app`, que es justo lo que no puede: es una librería aparte, con su propia base.

El item se compone solo si `onSendToNotes != null && language == MARKDOWN`. Esa condición es la misma
que `EditorModes.supportsPreview` (RF-76): si no hay vista previa para un archivo, no tiene sentido
ofrecer enviarlo.

**Alternativa descartada —un interface `NoteSink` inyectado en el ViewModel.** Descartada: el ViewModel
de la librería no sabe de notas, y meterlo ahí le daría una dependencia de la cosa que precisamente
no conoce. El callback en el borde del Composable mantiene la frontera donde está.

## 3. Guardar antes de convertir (RF-77)

El orden importa y es fácil de invertir:

```
viewModel.saveCurrentFile()      // primero: el disco queda al día
onSendToNotes(nombre, texto)     // después: se envía el texto que se ve en pantalla
```

Se envía `textFieldValue.text`, **no** lo releído de disco. Si se enviara lo de disco y el guardado
fuera asíncrono, habría una carrera: el anfitrión podría leer el contenido viejo. Con el texto en
memoria no hay carrera posible.

`saveCurrentFile()` ya es `launch` sobre `viewModelScope` y no bloquea, así que la cadena no espera:
se guarda y se envía en paralelo. Es aceptable porque **el enviado no lee del disco**.

## 4. La conversión (RF-83, RNF-38)

`FileImporter` tiene hoy `convertToBlockContent(context, uri, type)`, que lee un `Uri` y devuelve el
contenido de bloque. Se extrae la parte que no necesita `Context`:

```kotlin
fun markdownToBlockContent(raw: String): String {
    if (raw.isBlank()) return DataBlock.serialize(emptyList())
    return DataBlock.serialize(DataBlock.migrateLegacyContent(raw))
}

suspend fun convertToBlockContent(context: Context, uri: Uri, type: ImportFileType): String? {
    if (!isTextLike(type)) return null
    val raw = readTextFromUri(context, uri)
    if (raw.isBlank()) return null
    return markdownToBlockContent(raw)          // HTML y JSON llegan por su vía
}
```

**Por qué extraer en vez de duplicar.** El importador del FAB y el envío desde el editor tienen que
producir **exactamente la misma nota**. Si el segundo tuviera su propia conversión, divergirían en
cuanto una cambiara, y nadie lo notaría hasta comparar dos notas que deberían ser iguales. Una
prueba que compara ambos caminos sobre el mismo texto es lo que fija que no divergen.

`migrateLegacyContent` no reconoce citas ni cercas como bloques: quedan como texto dentro de un bloque
`TEXT`. Es la limitación ya documentada de la spec 012 y **no se arregla aquí** (RF-83 la reutiliza
deliberadamente, no la extiende).

## 5. El título

```
"notas/Introduccion.md"  →  "Introduccion"
"README"                 →  "README"
""                       →  "documento"
```

`substringAfterLast('/')` y sin la extensión. Sin directorio: una nota no debe heredar la carpeta del
almacenamiento (caso límite 5). Sin la extensión: el título es el nombre, no el nombre de fichero.

La función es pura y testeable: `noteTitleFrom(fileName)`, sin dependencias. Vive en
`FileImporter` al lado de la conversión, porque es la misma preocupación.

## 6. El traspaso entre Activities (RF-81, RNF-34, RNF-35)

`CodeEditorActivity` vive **fuera** del flujo de bloqueo de la bóveda, y por eso no puede escribir en
la base de notas: lo haría sin contraseña maestra. Además no tiene el `NotesViewModel`, que se crea en
`MainActivity` con su `NoteDatabase`.

El camino es: escribir un fichero → arrancar `MainActivity` con **la ruta** → leer, crear, borrar.

```
CodeEditorActivity                 cacheDir/md_handoff.json      MainActivity
      │                                    │                          │
      ├─ escribe {name, markdown} ───────▶ │                          │
      ├─ startActivity(EXTRA_RUTA) ───────────────────────────────────▶ │
      │                                                                ├─ si bloqueada: espera
      │                                                                ├─ createNoteFromMarkdown
      │                                                                ├─ navega a la nota
      │◀─── borra el fichero ◀─────────────────────────────────────────┤
```

**Por qué un fichero y no un extra del Intent.** El límite de Binder es ~1 MB por transacción. El
contenido de bloque JSON de una nota de 5 MB lo revienta, y el síntoma es una pantalla en blanco sin
explicación. Un `cacheDir` con un fichero de texto no tiene ese límite (RNF-34).

**El nombre del fichero es fijo**, sin parte del nombre del archivo del usuario: el nombre va **dentro**
del JSON. Es lo que permite que `grep` sobre `cacheDir` no revele nada (RNF-36).

**Por qué borrar (RNF-35).** `cacheDir` no lo limpia el sistema de forma fiable, y un Markdown sin
cifrar que se queda ahí indefinidamente es exactamente lo que este proyecto evita. Se borra en el
camino de éxito **y** después de que se lea aunque la app estuviera bloqueada.

## 7. La pestaña del editor dentro del editor de notas (RF-80)

`NoteEditorTabsHost.TextTabContent` **ya tiene** el `NotesViewModel` y las pestañas. Ahí no hay
traspaso: se crea la nota y se abre con las herramientas de que ya dispone.

```kotlin
val noteId = viewModel.createNoteFromMarkdown(name, markdown).getOrElse { /* avisar */ }
tabsManager.openNote(noteId)
tabsManager.selectTabByKey(OpenTab.Note(noteId).key)
```

Sin `startActivity` y sin `cacheDir`. Es el camino corto, y por eso la pestaña es el anfitrión
**preferido** cuando está disponible.

## 8. Un solo escritor (RNF-33)

Ni `CodeEditorActivity` ni `TextTabContent` escriben en `noteDao`. Los dos llaman a
`NotesViewModel.createNoteFromMarkdown`, que delega en `saveNoteAndGetId` — la vía que ya copia
adjuntos, cifra si toca, actualiza `lastModified` y refresca los widgets.

Eso importa por una razón concreta: si la Activity escribiera directamente, **se saltaría el refresco
de los widgets** y la nota nueva no aparecería en el widget hasta reiniciar la app. No es un detalle
estético: hay una clase `WidgetNotesTest` que vigila precisamente esa ruta.

## 9. Estrategia de tests

| Prueba | Qué fija | Cubre |
|---|---|---|
| `MarkdownNoteConversionTest` | `markdownToBlockContent` sobre un GFM de verdad; **el mismo texto por el importador y por el envío produce el mismo bloque JSON**; entrada vacía; el título sale del nombre sin extensión ni directorio | RF-83, RNF-38, casos 1 y 5 |
| `NoteHandoffTest` | El sobre se escribe y se lee; se borra tras consumirse; un sobre con la app bloqueada **no** crea nada y **no** borra el fichero; el nombre del fichero es fijo y no lleva datos del usuario | RF-82, RNF-34, RNF-35, RNF-36, caso 6 |

Las dos son JUnit puro: no necesitan `ContentResolver`, ni `NotesViewModel`, ni Android. Es lo que
permite que se ejecuten en esta plataforma (RNF-27).

**Lo que no se puede comprobar aquí**: que el menú aparezca, que la navegación funcione y que la nota
llegue a verse. Eso es riesgo declarado, como en las specs 005-012.

## 10. Decisiones y alternativas descartadas

1. **Callback en el Composable, no `NoteSink` en el ViewModel.** El ViewModel de la librería no sabe
   de notas; meter el sink ahí le daría la dependencia que precisamente no puede tener. Ver sección 2.
2. **Nota nueva siempre, nunca reutilizar.** Decidido por la persona usuaria. La alternativa —buscar
   por título y ofrecer reemplazar— evita duplicados pero **puede sobrescribir** una nota que el
   usuario no sabía que existía, y eso es peor que un duplicado visible.
3. **Traspaso por fichero, no por Intent.** El límite de Binder (~1 MB) no aguanta una nota grande, y
   reventarlo se ve como una pantalla en blanco (RNF-34).
4. **Reutilizar `markdownToBlockContent` en vez de escribir una conversión nueva.** Dos conversiones
   divergirían en cuanto una cambiara, sin que nadie lo notara (RF-83).
5. **`CodeEditorActivity` no escribe en la base de notas.** Vive fuera del flujo de bloqueo y no tiene
   el `NotesViewModel`; escribir desde ahí saltaría tanto la bóveda como el refresco de widgets.
6. **Sin etiquetas ni elección de nota.** El enunciado era «pasar el `.md` a la base de notas»: una
   acción, un destino. Un diálogo antes de cada envío convierte un paso en dos.
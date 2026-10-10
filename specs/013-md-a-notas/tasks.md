# Tasks — Spec 013 · Del Markdown del editor a la base de notas

Plan: `specs/013-md-a-notas/plan.md` · Encadena con: `specs/012-markdown-gfm-editor/`

6 tareas. El orden es el del dato: primero lo que se puede **probar aquí**, después lo que solo se
puede compilar, y el cierre al final.

---

- [x] **T1. La conversión pura y el título.** RF-78, RF-83, RNF-38
  - `FileImporter.markdownToBlockContent(raw: String): String` — extraída de `convertToBlockContent`,
    sin `Context` ni `Uri`.
  - `convertToBlockContent` la llama, así que **el FAB y el editor producen la misma nota** (RF-83).
    HTML y JSON siguen por su vía.
  - `noteTitleFrom(fileName)`: sin directorios y sin extensión; vacío → nombre por defecto (casos 1 y 5).
  - `MarkdownNoteConversionTest` en rojo **antes**, y la comparación explícita entre los dos caminos.
  - Hecho cuando: la prueba nueva en verde, `FileImporter` sin `Context` en la función nueva, y el
    importador existente sigue en verde **sin editar `FileImporterTest`**.
  - **Hecho**: 17 pruebas nuevas en verde; `FileImporterTest`, `RichTextConverter`,
    `RichTextParser` y `TableData` siguen en verde **sin editar**. `FileImporter` +40 −6, y las dos
    funciones nuevas no tocan `Context`.
  - **Defecto PREEXISTENTE encontrado**: `NoteContentBlockConverter.convertTextToBlocks` recibe el
    texto ya recortado con `.trim()`, y su regex `^\d+\.\s` exige un espacio que para entonces ya no
    está, así que **`1. primera` sale como bloque `TEXT` y no como lista numerada**. No lo arreglé:
    RF-83 obliga a que el envío y el FAB produzcan la misma nota, y arreglarlo solo en un camino
    rompería justamente eso. Queda con prueba que fija el comportamiento actual y anotado en
    `MEMORY.md` como algo a arreglar en su propia spec.

- [x] **T2. El sobre de traspaso.** RF-81, RF-82, RNF-34, RNF-35, RNF-36
  - `MarkdownHandoff`: `write(context, name, markdown)` y `read(path): Pair<String, String>?`, con un
    **nombre de fichero fijo** que no lleva nada del usuario (RNF-36).
  - Se serializa con `org.json`, que ya está: cero dependencias (RNF-37).
  - `NoteHandoffTest` en rojo antes: escribir/leer, **borrar tras consumir**, y **no borrar** si la
    app está bloqueada (caso límite 6).
  - Hecho cuando: la prueba en verde y `git diff -- app/build.gradle.kts gradle/libs.versions.toml` vacío.
  - **Hecho**: 13 pruebas en verde a la primera pasada. `app/build.gradle.kts` sin cambios; el
  `libs.versions.toml` que aparece en el diff es el **commonmark de la spec 012**, no de esta.
  - `consume()` borra **siempre**, también cuando el sobre estaba corrupto: uno a medio consumir en
    `cacheDir` es un Markdown sin cifrar que se queda ahí, que es lo que este proyecto no puede dejar.
  - El `FILE_NAME` es fijo (`md_handoff.json`) y el nombre del archivo del usuario va **dentro** del
    JSON, nunca en la ruta: una ruta acaba en un `Log.e`, y el nombre del usuario no puede estar ahí
    (RNF-36). Hay una prueba que lo fija por nombre, no por inspección.

- [x] **T3. El callback en la librería y su menú.** RF-76, RF-77
  - `CodeEditorApp(…, onSendToNotes: ((String, String) -> Unit)? = null)`: **opcional**, para no
    obligar a tocar llamantes que no lo usan.
  - El item aparece solo si hay anfitrión **y** el lenguaje es Markdown; si no, no se compone (RF-76).
  - Al pulsarlo: `viewModel.saveCurrentFile()` y después `onSendToNotes(nombre, textoActual)`. Se
    envía **el texto en memoria**, no lo releído de disco: sin carrera posible (RF-77).
  - 3 strings nuevas en los **8 locales** de la librería, con `\'` si llevan apóstrofo.
  - Hecho cuando: `:editor-de-codigo` compila, el item se ve solo en `.md`, y los 8 `strings.xml`
    tienen el mismo número de claves.
  - **Hecho**: 135 pruebas en el módulo (2 Robolectric de siempre), 127 claves en los **8** locales.
  - La condición del menú no está escrita dentro del `if`: es `EditorModes.canSendToNotes(...)`, con
    **tres** condiciones (anfitrión, Markdown, archivo) y dos pruebas que la fijan, incluida una que
    la compara con `supportsPreview` para los 25 lenguajes. Sin eso, la regla viviría en un `if` del
    Composable, sin nadie que la pudiera comprobar.
  - `Icons.AutoMirrored.Filled.Note` necesita su **import propio**: da `Unresolved reference` aunque el
    icono exista. Es la segunda vez que este icono da guerra (la otra, `Icons.Filled.Note`, está
    obsoleto). Comprobado con `javap` que el de `AutoMirrored` sí existe.

- [x] **T4. La nota en el ViewModel.** RF-78, RF-79, RNF-33
  - `NotesViewModel.createNoteFromMarkdown(name, markdown): Result<Int>` — delega en
    `saveNoteAndGetId(id = 0, …)` y devuelve el id. **Ni una línea escribe en `noteDao`** (RNF-33).
  - Convierte con `markdownToBlockContent` y el título con `noteTitleFrom`.
  - Markdown vacío crea la nota igual, con el bloque vacío (caso límite 2).
  - Hecho cuando: `grep -n "noteDao" NotesViewModel.kt` no muestra ninguna llamada nueva de
    `insertNote`, y `:app:compileDebugKotlin` en verde.
  - **Hecho**: 7 pruebas en verde, `:app:compileDebugKotlin` en verde. `createNoteFromMarkdown` son
    **12 líneas**, no mencionan `noteDao` y delegan en `saveNoteAndGetId` (RNF-33 comprobado sobre
    el cuerpo exacto de la función, no sobre un `sed` que se traga el cierre de la clase).
  - La prueba replica **los tres valores que compone** —id 0, título y contenido— en vez de
    instanciar el ViewModel, que necesitaría `Application` y la base de datos. El riesgo está en esos
    tres valores: un `id` distinto de 0 haría UPDATE en vez de INSERT, y la nota no se crearía.

- [x] **T5. Los dos anfitriones.** RF-80, RF-81, RF-82, RNF-35
  - **`TextTabContent`** (el camino corto): ya tiene `NotesViewModel` y las pestañas. Crea la nota y
    la abre con `openNote` + `selectTabByKey`. Sin `startActivity` y sin `cacheDir` (RF-80).
  - **`CodeEditorActivity`** (fuera de la bóveda): escribe el sobre en `cacheDir`, arranca
    `MainActivity` con **la ruta** y no toca la base de notas (RF-81, RNF-34).
  - **`MainActivity`**: consume el sobre, crea la nota, navega y **borra el fichero** (RF-82). Si la
    app está bloqueada, espera al desbloqueo en vez de crear nada.
  - `Log.e` registra **la clase** de la excepción, nunca su mensaje: la ruta lleva el nombre del
    archivo del usuario (RNF-36).
  - Hecho cuando: `:app:assembleDebug` en verde y ningún `noteDao.insertNote` nuevo fuera del
    ViewModel.
  - **Hecho**: `:app:compileDebugKotlin` en verde y las tres clases de la 013 en verde (27 pruebas).
    `CodeEditorActivity` no menciona `noteDao`, `NoteDao` ni `NotesViewModel` — la única aparición es
    **en un comentario** que explica por qué no los usa (RF-81). El `Intent` lleva la ruta, nunca el
    contenido (RNF-34).
  - El consumo va en un `LaunchedEffect(isUnlocked)` y **no** dentro del `DisposableEffect` del
    `intentRelay`: el sobre puede llegar con la app bloqueada, y el efecto tiene que **repetirse**
    cuando el estado cambie, no correr una vez (caso límite 6). Bloqueado, el Markdown espera en
    `cacheDir`; se borra al consumir, nunca antes.
  - Se añadió `NotesViewModel.launchInViewModelScope` para que quien recibe el sobre no invente un
    `CoroutineScope` propio que no sobreviva a la rotación.

- [x] **T6. Cierre: strings, suite y memoria.** todas
  - Los strings de `:app` que hagan falta, en los **9 locales** a la vez, con `\'` en los apóstrofos.
  - `./gradlew :app:testDebugUnitTest --tests '*Markdown*' --tests '*NoteHandoff*' --rerun` en verde.
  - `./gradlew :editor-de-codigo:testDebugUnitTest --rerun`: siguen las **2** Robolectric de siempre.
  - `./gradlew :app:assembleDebug` sin errores.
  - `git diff -- app/build.gradle.kts gradle/libs.versions.toml` vacío (RNF-37).
  - `MEMORY.md`: la regla del traspaso y que el editor no escribe en la base de notas.
  - Hecho cuando: todo termina bien y `MEMORY.md` nombra las dos reglas.
  - **Hecho**: 2 strings en los **9 locales** de `:app` (`note_created_from`, `note_create_failed`);
    las 3 clases de la 013 en verde (27 pruebas); `:app:assembleDebug` **verde**. `build.gradle.kts`,
    `NoteDatabase.kt` y `DataBlock.kt` con diff **vacío** (RNF-37), y el único `Log.` del camino nuevo
    está en un comentario (RNF-36).
  - El Snackbar del aviso se añadió **al final de T5**, no en T6 como estaba previsto: sin él, el
    `onSendResult` no tenía a dónde reportar y RF-79 se quedaba sin cumplir. Se unpió en un `Box`
    que envuelve a `DraftGuardDialog` y `TabContent`, porque el `SnackbarHost` tiene que estar en el
    árbol de composición aunque el diálogo no se muestre.
  - **`values-en-rGB` tiene 857 claves frente a 1160 del resto**: hueco **preexistente**, no de esta
    spec (el diff de esta entrega añade 2 líneas ahí, y son las correctas). Al no ser clave faltante
    no rompe el build; las ausentes caen a `values/` en runtime. No se rellenó porque son 303
    traducciones ajenas a esta entrega.
  - **Arreglado tras verlo en dispositivo: el camino largo no hacía nada.** El sobre llegaba por
    `onNewIntent`, se guardaba en una `var` estática, y el `LaunchedEffect` que debía consumirlo
    tenía por clave `isUnlocked` —que **ya no cambiaba** al llegar el Intent—, así que nunca volvía a
    correr y el Markdown esperaba en `cacheDir` para siempre. **El error no era la guarda, era el
    reloj**: ningún `StateFlow` lo habría salvado. Arreglo: `deliverNoteHandoff` consume **en el acto**
    si la app está abierta, y guarda la ruta **solo** para el caso bloqueado (RF-82, caso límite 6).
  - El aviso del camino largo es un `Toast`, no un `Snackbar`: `MainActivity` **no tiene `Scaffold`**,
    y añadir uno solo para esto arriesgaría el layout. Es el mismo mecanismo que ya usa el importador
    de archivos de ahí mismo.
  - `NoteHandoffTimingTest` (5 casos) fija lo que **sí** se puede comprobar sin `Activity`: que
    `consume` lee y borra en el mismo paso, incluso corrupto. Su KDoc dice explícitamente que **no**
    fija *cuándo* se llama, porque eso necesita el ViewModel y la Activity.
  - **Pendiente que no se puede cerrar aquí**: en dispositivo, que el item aparezca solo en `.md`, que
    la nota se cree y se abra **por los dos caminos**, y que el bloqueo la difiera en vez de perderla.
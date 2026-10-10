# Tasks — Spec 012 · Markdown GFM: resaltado, mitad y mitad y vista previa

Plan: `specs/012-markdown-gfm-editor/plan.md`

8 tareas. Los dos núcleos puros van primero: son los únicos que se pueden **probar en esta
plataforma**, así que cualquier error sale antes de haber tocado un `WebView` que nadie ve desde aquí.

---

- [x] **T1. El tokenizador de bloques.** RF-64, RF-65, RNF-26
  - `syntax/MarkdownTokens.kt` nuevo: `MdTokenKind`, `MdToken`, `MarkdownTokenizer.tokenize()`.
  - Sin `import androidx.compose`, sin `Context`, sin `R`, sin reloj. El cursor avanza por documento y
    cada rama consume su rango exacto, así que los tokens salen **ordenados y sin solapes** por
    construcción.
  - Primera pasada: *front matter*, cercas (con backticks o tildes, con cadena de lenguaje, y hasta
    el final si no cierran), tablas con tubería (cabecera, separadora y datos), ATX, setext, rayas,
    citas, listas y marcadores de tarea, definiciones de referencia.
  - `MarkdownTokenizerTest` en rojo **antes**, con el invariante universal ya escrito: ordenado, sin
    solapes, dentro de `0..length`.
  - Hecho cuando: `grep -c '^import androidx.compose'` devuelve **0**, y los casos de cerca sin
    cerrar, `---` de tabla ≠ raya, y setext están en verde.

- [x] **T2. El tokenizador en línea.** RF-64, RF-65, RNF-26
  - Segunda pasada sobre los tramos que la primera dejó sin cubrir: escapes, código en línea con
    tiradas de acentos graves, autolinks, HTML embebido, enlaces e imágenes en línea / por referencia /
    colapsados, `~~tachado~~`, `**fuerte**`, `*cursiva*`, `_cursiva_`.
  - Los delimitadores de énfasis **no abren ni cierran dentro de una palabra** (`a*b*c` no es cursiva).
  - Un token de bloque nunca se reemite en línea: la cerca salta la segunda pasada entera (RF-65).
  - Hecho cuando: el caso `val x = 1` dentro de una cerca ```` ```kotlin ```` produce **cero** tokens
    `INLINE_CODE`, y la suite completa de `MarkdownTokenizerTest` está en verde.

- [x] **T3. El tokenizador muta a rojo.** RF-65
  - **Antes** de seguir: romper a propósito T2 admitting tokens en línea dentro de una cerca, y
    comprobar que el caso se **pone rojo**. `MEMORY.md` lo dice: una prueba que no se ha visto fallar
    no está probada.
  - Hecho cuando: existe en el historial un run con la mutación en rojo, y al revertirlo vuelve a
    verde.
  - **Hecho, con dos mutaciones y no una.** La primera (`inlineEnd = markdown.length`, o sea, la
    pasada en línea entra también dentro de la cerca) puso en rojo **2 invariantes**, pero el test con
    nombre explícito —«el contenido de una cerca no recibe nada en línea»— siguió **verde**: la cerca
    la protegía el `covers()` de la segunda pasada, no el corte de rango. La segunda (anular
    `covers()`) puso en rojo **7 pruebas**, entre ellas las del título, la celda y la definición de
    referencia. Juntas cubren las dos defensas, y ninguna está probada por separado.

- [x] **T4. El resaltador consume los tokens.** RF-64, RNF-28
  - `SyntaxHighlighter.highlightMarkdown` deja de llamar a `applyRegex` y mapea `MdTokenKind` →
    `SpanStyle` con el mapa del plan (sección 3).
  - **No se añade ningún campo a `SyntaxTheme`.** El mapa vive en `SyntaxHighlighter.kt`.
  - `SyntaxColors.kt` **no se toca** (RNF-28).
  - Casos nuevos en `CodeEditorUnitTest`: Markdown entra en la lista de lenguajes con spans, el texto
    vuelve **idéntico** al original, y el cuerpo de una cerca no lleva el color de código en línea.
  - Hecho cuando: los 14 casos preexistentes de los otros lenguajes siguen verdes **sin editar el
    fichero**, y `git diff -- editor-de-codigo/.../syntax/SyntaxColors.kt` está vacío.
  - **Hecho**: `SyntaxColors.kt` con diff vacío y 97 pruebas en verde salvo las 2 Robolectric de
    siempre. El mapa reparte los 22 tipos sobre los colores que los temas ya traen.
  - **Dos cosas que la prueba enseñó y el código no.** (a) El span de búsqueda **no cambia el color**,
    solo pinta el fondo: pedir `color` daba transparente y la aserción no significaba nada. (b) Para
    saber qué color gana hay que quedarse con el **último** span que cubre el tramo, no con el mayor
    (el base cubre el documento entero) ni con el menor (el de búsqueda es más corto que el token).
    Los tres criterios dan una respuesta distinta, y solo el correcto refleja lo que hace Compose.

- [x] **T5. El renderizador Markdown → HTML.** RF-70, RF-71, RF-72, RNF-26, RNF-29
  - `build.gradle.kts`: commonmark core + `ext-gfm-tables` + `ext-gfm-strikethrough`, versión
    **0.13.0**, la misma que ya resuelve `compose-markdown`. No se sube de versión (decisión 2).
  - `markdown/MarkdownRenderer.kt` nuevo: `PreviewTheme` (hex, **no** `Color`), `toHtmlDocument()`.
  - Cadena: parser con extensiones → `HtmlRenderer` con `escapeHtml(true)` → `neutralizeRemoteSources()`
    → `markTaskItems()` → documento con `<style>` embebido, sin fuentes ni hojas externas.
  - `MarkdownRendererTest`: tabla, tachado y `<li class="task">` presentes; `<script>`, `<iframe>` y
    `onerror=` **escapados**; `src` remoto sustituido por marcador; `javascript:` y `data:` fuera; el
    CSS lleva el fondo y el color del `PreviewTheme`; entrada vacía da documento válido.
  - `oss-licenses.json` y `THIRD-PARTY-NOTICES`: entrada commonmark-java (BSD-2-Clause).
  - Hecho cuando: la suite nueva en verde, ambos ficheros de licencias mencionan commonmark, y
    `grep -c '^import androidx.compose'` en el renderizador devuelve **0**.
  - **Hecho**: 20 pruebas en verde, `oss-licenses.json` y `THIRD-PARTY-NOTICES` con commonmark 0.13.0
    (BSD-2-Clause), y `:app:compileDebugKotlin` en verde con el catálogo actualizado.
  - **Dos trampas que costaron una vuelta cada una.** (a) Las extensiones de GFM hay que dárselas a
    **los dos** constructores: registradas solo en el `Parser`, la tabla y el tachado se parseaban
    bien y el renderizador los pintaba **vacíos** (`<p></p>` en vez de `<del>`). (b) Un `Regex("""…\s…""")`
    en Kotlin busca una barra seguida de una `s`, **no** un espacio: el patrón no casaba y el bloqueo
    de imágenes remotas no se aplicaba. Las dos las delató un volcado del HTML real, no una aserción.
  - **El marcador de imagen bloqueada no guarda la URL en ningún atributo**, ni en un `title` para
    que se lea al pasar el ratón: seguiría siendo un destino que el `WebView` podría intentar.

- [x] **T6. `ViewMode` y la disponibilidad de modos.** RF-66, RF-69, RNF-31
  - `ui/EditorViewMode.kt` nuevo: `enum ViewMode { EDIT, VIEW, SPLIT, PREVIEW }` y
    `EditorModes.availableFor(language)` — puro y testeable.
  - `CodeEditorViewModel`: `isEditMode` deja de ser un `StateFlow` paralelo y pasa a derivarse de
    `viewMode` (RNF-31). `setViewMode()` y mapa `fileId → ViewMode`, de modo que volver a un archivo
    restaura su modo (RF-69).
  - `setLanguage()` **revalida**: si el archivo nuevo no admite el modo activo, vuelve a `EDIT`
    (caso límite 7).
  - `EditorViewModeTest`: Markdown da 4 modos, Kotlin/JSON/texto plano dan 2; el cambio de archivo
    restaura y no arrastra; `SPLIT` y `PREVIEW` implican no editable.
  - Hecho cuando: no queda ningún `MutableStateFlow` llamado `isEditMode`, y la suite nueva en verde.
  - **Hecho**: 9 pruebas en verde y 126 en el módulo (2 Robolectric de siempre). `isEditMode` es ahora
    un `val` derivado de `viewMode`, no un `StateFlow` paralelo.
  - `toggleEditMode()` alterna solo **dos** estados (EDIT ↔ VIEW) y desde `SPLIT`/`PREVIEW` vuelve a
    `EDIT`: «escribir o ver el resultado» tiene dos respuestas, no cuatro, y un ciclo de cuatro
    obligaría a leer la etiqueta para saber cuántas pulsaciones faltan. Los otros dos se eligen en el
    selector desplegable.

- [x] **T7. La vista previa y el reparto en dos mitades.** RF-67, RF-68, RF-73, RF-74, RNF-30
  - `ui/components/MarkdownPreview.kt` nuevo, modelado sobre `InAppYouTubePlayer` de `:visor-media`:
    `javaScriptEnabled = false`, `domStorageEnabled = false`, `allowContentAccess = false`,
    `allowFileAccess = true`, `allowFileAccessFromFileURLs = false`; base
    `file://<carpeta del archivo>` y `https://localhost/` si no hay archivo en disco (RF-73).
  - `WebViewClient.shouldOverrideUrlLoading` manda `http`, `https` y `mailto` fuera con un Intent.
  - `onDispose`: `stopLoading()`, `loadUrl("about:blank")`, `destroy()` (RNF-30).
  - `previewHtml: StateFlow<String>` en el ViewModel, calculado en `viewModelScope` con
    `Dispatchers.Default` y **debounce** sobre el texto; la composición no llama al renderizador.
  - Si el render falla, el editor **sigue visible** y el fallo se registra (RF-74).
  - `CodeEditorApp`: `Row` con `weight(1f)` por lado y `VerticalDivider` en `SPLIT`. En `PREVIEW`,
    `CodeEditorView` **no se compone** (RF-68).
  - El chip actual se convierte en el disparador del menú de modos **disponibles**; el ancho del chip no
    cambia al cambiar de modo (decisión 6).
  - Hecho cuando: `./gradlew :editor-de-codigo:compileDebugKotlin` en verde, y no hay ninguna
    proporción ni manejador de arrastre en el fichero (RF-67 son mitades fijas).
  - **Hecho**: 133 pruebas en el módulo (2 Robolectric de siempre), `PreviewLayoutTest` con 7 casos
    sobre qué panes existen, y `:app:assembleDebug` **en verde**. El `WebView` es lo único sin probar.
  - **`onRelease` no sirve para destruir el `WebView`**: se dispara con la vista aún en el árbol, y
    `destroy()` antes de eso deja la pantalla en negro un instante. Lo correcto es guardar la
    referencia y destruirla en un `DisposableEffect`, que corre **después** — es lo que ya hacía
    `InAppYouTubePlayer` en `:visor-media`, y aquí se copió el error contrario.
  - El chipselector abre el desplegable **solo cuando hay más de dos modos**: con dos, un toque
    alterna como siempre; con cuatro, ciclar obligaría a leer la etiqueta para saber cuántas
    pulsaciones faltan.
  - Los dos `Deprecated` del compilador en `configure()` son **intencionados** y están comentados
    como tales: son la constancia de que el bloqueo es deliberado y no un olvido de la plataforma.

- [x] **T8. Cierre: strings, build y memoria.** todas
  - **6 claves nuevas** en los **8 locales** de `:editor-de-codigo` **a la vez**: los nombres de los dos
    modos nuevos con su `contentDescription`, el menú de modos, y el texto de estado. Los ocho
    `strings.xml` deben acabar con el mismo número de claves.
  - **Apóstrofos crudos** a `\'` en `strings.xml` (el fallo sale en `mergeDebugResources`, después de
    que Kotlin compile limpio).
  - `./gradlew :editor-de-codigo:testDebugUnitTest --rerun` en verde salvo las **dos** Robolectric
    preexistentes de `ExternalFileStorageRobolectricTest`.
  - `./gradlew :app:assembleDebug` sin errores.
  - `git diff --stat` de los ocho `strings.xml` muestra las mismas líneas añadidas en los ocho.
  - `MEMORY.md`: el tokenizador por precedencia, commonmark 0.13 por `compose-markdown`, y que la
    vista previa no toca la red.
  - Hecho cuando: todo termina bien y `MEMORY.md` nombra las tres.
  - **Hecho**: los 8 `strings.xml` con **126 claves** cada uno y 2 líneas añadidas en cada uno (16 en
    total); `SyntaxColors.kt` con diff vacío; sin apostrofes crudos; 133 pruebas con `--rerun` (2
    Robolectric de siempre); `:app:assembleDebug` y `:app:compileDebugKotlin` en verde; y sin
    regresión en las pruebas de `:app` que tocan Markdown (`RichTextConverter`, `RichTextParser`,
    `FileImporter`, `PlainTextExport`).
  - **Pendiente que no se puede cerrar aquí**: en dispositivo, que el `WebView` pinte, que el reparto
    50/50 sea el esperado y que el chip no desborde con la etiqueta más larga de los 8 locales. Las
    pruebas puras comprueban la aritmética del tokenizador y del HTML, **no** que la pantalla se vea.
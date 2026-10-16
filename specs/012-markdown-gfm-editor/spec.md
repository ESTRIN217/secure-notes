# Spec 012 — Markdown GFM: resaltado, mitad y mitad y vista previa

Estado: implementada (2026-10-10), T1-T8 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**: «todo se ve bien».

> **Nota de procedencia.** No hereda de ninguna spec anterior: es el primer trabajo sobre Markdown en
> `:editor-de-codigo`. Encadena con `specs/013-md-a-notas/`, que se redactará después y que recogerá
> la acción «Enviar a notas» (esta spec deja preparado el hueco: el callback, pero no el menú).
> `RF-64` es el más alto en uso (spec 011) y `RNF-26` el más alto también (spec 011).

## Contexto y objetivo

El editor de código ya reconoce Markdown como lenguaje (`SupportedLanguage.MARKDOWN`) y lo colorea
con **siete expresiones regulares** (`SyntaxHighlighter.highlightMarkdown`). Siete regex no es un
resaltado de Markdown, es una aproximación que además está mal ordenada:

1. El código en línea (```` `x` ````) se aplica **antes** que las cercas (```` ```…``` ````), así que
   el interior de un bloque cercado sale coloreado como código en línea en lugar de como cadena.
2. No reconoce casi nada de GFM: tablas con tubería, listas de tarea, `~~tachado~~`, enlaces por
   referencia, `<autolinks>`, HTML embebido, *front matter*, encabezados setext ni el `#` de cierre.
3. Cada regex escribe su estilo encima del anterior sin criterio de precedencia, así que **el orden de
   las llamadas decide el resultado** y no la gramática del lenguaje.

Encima, el editor no tiene forma de **ver** un Markdown: solo tiene un conmutador Editar/Ver, y «Ver»
es el mismo texto con colores, sin renderizar. Escribir documentación es escribir Markdown, y la
mayoría de quien escribe Markdown necesita verlo renderizado mientras lo escribe.

El objetivo es doble: **resaltar Markdown como GFM de verdad**, y **poder verlo**, en dos modos nuevos
—mitad y mitad y vista previa— sobre una vista previa que no toca la red.

## Usuarios

- Quien escribe Markdown en el editor de código: quiere ver el resultado mientras escribe, nomentalizar
  la sintaxis.
- Quien edita documentación ya escrita: quiere que las tablas, las listas de tarea y los enlaces se
  marcen como son, no como texto plano con `#` pintado.

## Definiciones

- **GFM** (*GitHub Flavored Markdown*): el dialecto de Markdown que usa GitHub. Sobre el Markdown
  común añade, entre otros, tablas con tubería, tachado, listas de tarea y autolinks.
- **Token**: un tramo del documento con un significado, con su posición exacta. Un token **nunca**
  solapa con otro de la misma pasada: si dos construcciones se pisan, gana la de mayor prioridad y la
  otra no se emite.
- **Vista previa**: el documento Markdown convertido a HTML y pintado en un `WebView`.
- **Modo**: la forma en que se presenta el archivo activo. `EDIT`, `VIEW`, `SPLIT`, `PREVIEW`.

## Requisitos funcionales

- **RF-64**: CUANDO se abre o se edita un archivo Markdown, EL SISTEMA lo resalta con las
  construcciones de GFM. Verificable: `MarkdownTokenizerTest` cubre, con posiciones exactas —
  encabezados ATX (`#` a `######`, con `#` de cierre opcional), encabezados setext (bajo una línea de
  `=` o `-`), cercas con backticks o tildes y su cadena de lenguaje, tablas con tubería (fila de
  cabecera, fila separadora y filas de datos) con sus alineaciones, listas de tarea (`- [ ]`, `- [x]`),
  listas con y sin anidamiento, citas (`>` y `>>`), rayas de separación, saltos de emphasis
  (`**fuerte**`, `*cursiva*`, `_cursiva_`), tachado (`~~tachado~~`), código en línea con tiradas de
  acentos graves, enlaces e imágenes en línea, por referencia (`[t][r]`, `[r]: url`) y colapsados
  (`[r]`), autolinks (`<https://…>`), HTML embebido y *front matter* (`---` en la primera línea).

- **RF-65**: SI dos construcciones de Markdown se pisan en el mismo tramo, ENTONCES EL SISTEMA
  colorea **solo la de mayor prioridad**, siguiendo el orden bloque → en línea, y dentro de un bloque
  cercado **no emite ningún token en línea**. Verificable: una prueba fija que la línea
  ````val x = 1```` dentro de una cerca ```````kotlin`````` **no** produce token de código en línea, y
  que un `---` dentro de una tabla es fila separadora y **no** raya de separación.

- **RF-66**: MIENTRAS el archivo activo **no** sea Markdown, EL SISTEMA ofrece **únicamente** los modos
  `EDIT` y `VIEW`, exactamente como antes. Verificable: `EditorViewModeTest` comprueba que
  `availableFor(KOTLIN)`, `availableFor(JSON)` y `availableFor(PLAIN_TEXT)` devuelven exactamente esos
  dos, y `availableFor(MARKDOWN)` devuelve los cuatro.

- **RF-67**: CUANDO el archivo activo es Markdown y el modo es `SPLIT`, EL SISTEMA muestra el editor a
  la izquierda y la vista previa a la derecha, en **dos mitades iguales**, con un divisor entre
  ambas. Verificable: los dos lados reciben `weight(1f)` y no hay ningún estado de proporción ni
  manejador de arrastre en el fichero.

- **RF-68**: CUANDO el modo es `PREVIEW`, EL SISTEMA muestra **solo** la vista previa, sin el editor
  ni el carril de números de línea. Verificable: en `PREVIEW` no se compone `CodeEditorView`, y con el
  editor abierto el número de línea actual no avanza.

- **RF-69**: CUANDO se cambia de archivo, EL SISTEMA restaura el modo que tenía ese archivo, sin
  arrastrar el modo del anterior. Verificable: `EditorViewModeTest` fija que tras poner `PREVIEW` en un
  archivo y seleccionar otro, el segundo vuelve a `EDIT`, y que al volver al primero recupera
  `PREVIEW`.

- **RF-70**: EL SISTEMA renderiza la vista previa **sin JavaScript**, **sin acceso a la red** y **sin
  cargar recursos remotos**. Verificable: el `WebView` se construye con `javaScriptEnabled = false` y
  `domStorageEnabled = false`; `MarkdownRendererTest` comprueba que una imagen `http(s)://` sale
  sustituida por un marcador en el HTML, y que un `src="javascript:…"` desaparece.

- **RF-71**: EL SISTEMA escapa el HTML incrustado en el documento, de modo que una etiqueta suelta **no
  se ejecuta ni se interpreta**. Verificable: `MarkdownRendererTest` comprueba que `<script>`,
  `<iframe>` y `onerror=` salen **escapados como texto** en el HTML generado.

- **RF-72**: MIENTRAS haya una vista previa visible, EL SISTEMA la pinta con el **tema del editor**
  (claro u oscuro) y con los colores de `SyntaxTheme`. Verificable: el `PreviewTheme` que recibe el
  renderizador sale del `SyntaxTheme` activo, y su CSS lleva ese fondo, ese color de texto y ese
  contraste, verificado comparando la cadena que se genera para un tema claro y uno oscuro.

- **RF-73**: CUANDO el archivo Markdown tiene imágenes **locales relativas** (por ejemplo
  `![d](diagrams/flujo.png)`), EL SISTEMA las muestra en la vista previa. Verificable: el `WebView` se
  carga con `loadDataWithBaseURL` apuntando a la carpeta del archivo y con acceso de lectura a
  ficheros habilitado.

- **RF-74**: SI la vista previa no puede construirse (por ejemplo, el motor falla), ENTONCES EL
  SISTEMA **mantiene el editor visible** y registra el fallo, sin tumbar la pantalla ni dejar el
  editor atrapado. Verificable: el `try` que produce el HTML tiene alternativa, y el modo `SPLIT`
  sigue componiendo el editor con independencia del resultado.

- **RF-75**: CUANDO el texto cambia, EL SISTEMA regenera el HTML **fuera del hilo principal** y no
  bloquea la escritura. Verificable: el cálculo ocurre en `viewModelScope` con `Dispatchers.Default`
  y su resultado se publica por `StateFlow`; la composición no llama al renderizador.

## Requisitos no funcionales

- **RNF-26 — Los dos núcleos son lógica pura.** `MarkdownTokenizer` y `MarkdownRenderer` no importan
  nada de `androidx.compose`, ni `Context`, ni `R`, ni el reloj. Verificable:
  `grep -c '^import androidx.compose'` devuelve **0** en ambos ficheros, y sus pruebas corren sin
  `RobolectricTestRunner`.

- **RNF-27 — Lo que es puro se prueba aquí.** Todas las pruebas nuevas son JUnit puro y **sí se
  ejecutan en esta plataforma**, donde Robolectric no funciona. Verificable: la suite nueva pasa con
  `./gradlew :editor-de-codigo:testDebugUnitTest --rerun`, y las dos pruebas Robolectric preexistentes
  de `ExternalFileStorageRobolectricTest` siguen siendo las únicas que fallan, por el motivo ya
  documentado.

- **RNF-28 — Cero campos nuevos en `SyntaxTheme`.** Los tokens se mapean a los colores que ya
  existen. Verificable: `git diff` de `SyntaxColors.kt` vacío; el mapa token→color cabe en el
  renderizador y no añade campos a la clase de tema.

- **RNF-29 — Una sola dependencia nueva, y no es una descarga.** `commonmark-java` **ya está en el
  APK** por `compose-markdown`; declararla en `:editor-de-codigo` no añade ningún artefacto nuevo al
  empaquetado ni cambia la versión que resuelve Gradle. Verificable: `oss-licenses.json` y
  `THIRD-PARTY-NOTICES` suman la entrada, y el árbol de dependencias de `:editor-de-codigo` no
  introduce ningún artefacto que no estuviera ya.

- **RNF-30 — El `WebView` no sobrevive a quien lo crea.** Se destruye en `onDispose`, igual que
  `InAppYouTubePlayer` en `:visor-media`. Verificable: `MarkdownPreview` llama `stopLoading()`,
  `loadUrl("about:blank")` y `destroy()` en su `DisposableEffect`.

- **RNF-31 — Una sola fuente de verdad para los modos.** `isEditMode` deja de ser un `StateFlow`
  paralelo: pasa a derivarse de `ViewMode`. Verificable: no queda ningún `MutableStateFlow` con nombre
  `isEditMode`, y `CodeEditorView` recibe `isEditable` derivado.

- **RNF-32 — Los strings nuevos van a los 8 locales de la librería a la vez**, como exige
  `AGENTS.md`, con las mayúsculas del nombre de clave en inglés y el texto en el idioma del locale.
  Verificable: los ocho `strings.xml` de `:editor-de-codigo` tienen el mismo número de claves.

## Casos límite

1. **Cerca sin cerrar.** Un ```` ``` ```` que no termina antes del final del archivo: el bloque llega
   hasta el final, y **todo** lo que hay dentro es código, aunque parezca una lista o un título.
2. **Tabla de una sola columna.** El mínimo de GFM son dos columnas; con una, la fila no es tabla y
   se trata como texto.
3. **Anidamiento con tabs.** Un ítem sangrado con tabulador se cuenta como anidado igual que con
   espacios.
4. **Acentos graves desbalanceados.** `` `a ` b` `` no es código en línea: una apertura que no cierra
   en la misma línea se trata como texto.
5. **Archivo Markdown vacío o de una sola línea sin salto final.** La vista previa muestra el
   documento vacío, no una pantalla en blanco sin explicación, y el editor sigue editable.
6. **`---` en la primera línea.** Es *front matter* si hay un `---` de cierre; si no lo hay, es una
   raya de separación. La decisión se toma con la línea de cierre, no con la primera.
7. **Cambiar de `.md` a `.kt` con `PREVIEW` activo.** El archivo nuevo no puede quedarse en `PREVIEW`
   (RF-66): vuelve a `EDIT` y no arrastra el modo.
8. **Rotación con `SPLIT` activo.** Los dos lados se recomponen con la misma proporción porque es fija;
   no hay nada que persistir entre configuraciones.

## Fuera de alcance

- **El paso del `.md` a la base de notas.** Es la spec 013. Esta spec deja el hueco preparado (el
  callback y la lógica pura de conversión), pero **no** añade el item de menú ni toca `:app`.
- **El divisor arrastrable** y la proporción que el usuario pueda ajustar. Las mitades son fijas.
- **Sincronización de scroll** entre editor y vista previa.
- **Matemática en la vista previa.** No hay MathJax ni KaTeX:_requires_ JavaScript, y el `WebView` va
  sin JavaScript (RF-70).
- **Resaltado GFM en el editor de notas.** `DataBlock.migrateLegacyContent` y `RichTextParser` quedan
  como están; elReconocimiento de Markdown en las notas es otra spec.
- **Temas de la vista previa más allá del `SyntaxTheme` activo.** No hay selector de tema propio.

## Criterios de finalización

- [x] `grep -c '^import androidx.compose'` devuelve **0** en `MarkdownTokens.kt` y en `MarkdownRenderer.kt`.
- [x] `./gradlew :editor-de-codigo:testDebugUnitTest --rerun` en verde, salvo las **dos** pruebas
      Robolectric preexistentes de `ExternalFileStorageRobolectricTest`, que fallan por el motivo ya
      documentado.
- [x] `git diff -- editor-de-codigo/src/main/java/com/estrin217/editordecodigo/syntax/SyntaxColors.kt`
      vacío (RNF-28).
- [x] `git diff --stat` de los ocho `strings.xml` de `:editor-de-codigo` muestra las mismas líneas
      añadidas en los ocho (RNF-32).
- [x] `oss-licenses.json` y `THIRD-PARTY-NOTICES` incluyen commonmark-java.
- [x] `./gradlew :app:assembleDebug` sin errores.

## Dudas abiertas

Ninguna. Las decisiones que podían tomarse por dos caminos —motor de render, alcance del resaltado,
si el traspaso a notas crea nota nueva o reutiliza, si el divisor se arrastra— las respondió la
persona usuaria antes de redactar, y están argumentadas en el plan.
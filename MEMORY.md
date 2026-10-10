# MEMORY.md — secure-notes 
Memoria entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte. 
## Estado actual
- **Spec 012 (Markdown GFM) IMPLEMENTADA**, T1-T8 cerradas. Resaltado GFM con tokenizador propio,
  modos `SPLIT` 50/50 y `PREVIEW` con `WebView` (sin JS, sin red). **133 pruebas** en
  `:editor-de-codigo`, `:app:assembleDebug` verde. **Nada se ha visto en un dispositivo.**
  `specs/012-markdown-gfm-editor/`, RF-64…RF-75 y RNF-26…RNF-32.
- **Spec 013 (`.md` → base de notas) IMPLEMENTADA**, T1-T6 cerradas. Acción «Enviar a notas» en el
  editor de código: **siempre crea nota nueva**. **27 pruebas** nuevas en `:app`, `assembleDebug`
  verde. RF-76…RF-83 y RNF-33…RNF-38. **Sin verificación visual.**
- **Specs 005-011 (tabla) implementadas** y encadenadas; la 011 se **verificó en dispositivo** y
  deja el tirador entero dentro con carril de 36 dp. Sin verificación visual: 005, 008 y 009.
- **Spec 006 (chat con IA) implementada**: tres prototipos adoptados y siete lógicas puras en
  `ui/chat/`, sin Compose ni reloj. 53 casos verdes. Sin ver en dispositivo.
- **Inset del teclado unificado** en editor y chat (decisión 12): una sola fuente, en el `bottomBar`.
- `./gradlew test` completo **no está verde** y no puede estarlo aquí: Robolectric sin runtime nativo
  para `linux/aarch64`. Los tests **puros sí se ejecutan**. Las 2 pruebas Robolectric de
  `ExternalFileStorageRobolectricTest` son la línea de base de `:editor-de-codigo`.

## Decisiones (y por qué) 
- **14. El editor de código NO escribe en la base de notas.** `CodeEditorActivity` vive **fuera** del
  flujo de bloqueo de la bóveda y no tiene el `NotesViewModel`: escribir desde ahí saltaría la
  contraseña maestra y el refresco de widgets. Deja un sobre en `cacheDir` con nombre **fijo**
  (`md_handoff.json`, porque la ruta acaba en un log y el nombre del archivo es del usuario) y
  `MainActivity` consume. Con la app bloqueada **espera**: no crea ni borra, que perdería el trabajo.
- **13. commonmark-java 0.13.0, no 0.22**: `compose-markdown` ya arrastra core + `ext-gfm-tables` +
  `ext-gfm-strikethrough` en el APK, así que declararlos en `:editor-de-codigo` **no descarga nada**.
  Subir a 0.22 forzaría la versión transitiva que usan `UpdateInfoScreen` y `ChangelogSheet`. En 0.13 no
  hay extensión de listas de tarea: se cubren con un post-proceso sobre el HTML propio.
  El jar es bytecode Java 7 sin `java.awt`/`java.sql`: comprobado antes de decidir.
- **12. El inset del teclado se aplica UNA vez, en el `bottomBar`, con `union`**: 
  `WindowInsets.ime.union(WindowInsets.navigationBars)` gana el `ime` con el teclado abierto y las
  barras de navegación con el teclado cerrado, sin tener que decidir en código. El síntoma que
  había (barra invisible pero **su hueco visible**) era doble conteo: `innerPadding` del `Scaffold`
  ya reservaba la altura del `bottomBar`, y el `.imePadding()` del `Box` de contenido la volvía a
  sumar. Con `adjustResize` implícito y `enableEdgeToEdge()`, el teclado tapa el `bottomBar`: sin
  inset propio, la barra queda **detrás** del teclado. ⚠️ **Pendiente de revisión visual**: Both
  `Scaffold` conservan su `.navigationBarsPadding()`, que reduce la caja del Scaffold mientras el
  `union` actúa dentro del `bottomBar`. Superficies distintas, se cree que no se duplica, pero no
  está verificado en dispositivo.
- **9. La cabecera es una fila y una columna, no un dato aparte** (spec 008): «activar fila de cabecera» es estilo sobre la fila 0, igual que «activar columna de cabecera» lo es sobre la columna 0. Se descartó guardar en qué fila está la cabecera porque obligaría a la geometría a seguir distinguiéndola. Consecuencias aceptadas: desactivar la fila la convierte en dato (no la borra) y la fila 0 ya se puede borrar y mover. Al leer una nota vieja, los títulos antiguos se anteponen **siempre**: descartarlos sería borrar texto del usuario.
- **5. La sugerencia de resumen conserva su visibilidad condicional** (texto seleccionado, contenido de nota o adjuntos): sin ella sería un botón que pide resumir la nada, y en los tres puntos de entrada el contexto llega vacío.
- **6. La rotación del saludo es diaria, determinista y cíclica**: es la única forma de ejercitarlo con JUnit puro y evita que el texto cambie bajo los ojos de quien mira. No se promete que dos días cualesquiera den saludos distintos.
- **7. La clave de las fichas de adjuntos es posicional + nombre**, única por construcción: `FileAttachment` no tiene id estable y `removePendingAttachment` borra por índice, así que con dos adjuntos homónimos una clave por nombre lanza `IllegalArgumentException`. **Excepción consciente** a la `key` estable de `AGENTS.md`: al cambiar la clave Compose **destruye y recompone** los nodos, y aquí la ficha no tiene estado interno que perder.
- **8. Qué texto de ayuda corresponde lo decide la lógica pura**, que devuelve un identificador del dominio; el texto lo resuelve el Composable con `stringResource`. Así es comprobable sin recursos.
- **11. El mapeo acción→petición de la rama de envío se queda en el Composable**: existe desde antes y ninguna RF depende de él.
- **Sin visión en la capa de modelo**: se descartaron foto y cámara en la hoja de adjuntos porque los adjuntos llegan al modelo serializados como texto y el motor en el dispositivo es de solo texto. Añadirlas habría sido una hoja con opciones que no pueden funcionar.
- El host de IA vive en la `Application`, no en el ViewModel: el ViewModel se destruye al salir de la Activity y el host debe sobrevivir. Descarga por **referencias + inactividad**: recargar re-lee el GGUF.
- La barra de la tabla va en un **margen constante propio** (el salto venía de que `AnimatedVisibility` **cambiaba el alto**) y no se puede superponer porque **el editor no pone espacio entre bloques**: taparía el bloque de arriba.
- Foco de un grupo con un **conjunto** de elementos, no con «el enfocado»: el desenfoque de A puede llegar tras el foco de B y con un solo valor se borrarían. El estado **final** no puede depender del orden.
- Contorno de selección en **capa superpuesta**, no borde por celda: por eso `selectionOutline` devuelve **una** región o `null`; mientras pueda devolver varias, el dibujado no puede saber si las une, y no unirlas es la escalera.

## Aprendizajes y errores a evitar 
- **Un test que reimplementa lo que prueba no prueba nada.** Pasó dos veces: `MarkdownPreviewContractTest`
  daba verde con el bug puesto (mutaba el Composable y el test llamaba al renderizador directamente), y
  `NoteHandoffTimingTest` reimplementaba la lógica. **O ejerce el código real, o su KDoc dice qué SÍ
  fija y qué NO.** Para lo segundo: extraer el cruce a una función real compartida.
- **Un bug de integración es invisible a los tests por pieza**: `MarkdownRendererTest` comprobaba que
  el renderizador funciona (funciona), `PreviewLayoutTest` que se componen los paneles (se componen).
  El fallo estaba en *quién llamaba a qué*, un cruce que no existía como código.
- **Un estado escrito desde `onNewIntent` no lo ve un `LaunchedEffect` cuya clave no cambia.** El bug de
  «enviar a notas no hace nada»: el sobre se guardaba en una `var` estática y el efecto tenía por clave
  `isUnlocked`, que **ya no cambiaba** al llegar el Intent. **El error no era la guarda, era el reloj**;
  ningún `StateFlow` lo habría salvado. Arreglo: consumir en el acto donde llega.
- **Una `var` estática en el companion no dispara recomposición**, aunque la lea un `Snackbar`.
- **`escapeHtml(true)` de commonmark vuelve visible un doble render**: reprocesar el documento lo
  escapa entero, sin error. Hay un test que documenta que reprocesar **sí** lo cambia.
- **Commonmark-java 0.13 hay que dársela a los DOS constructores**: solo en el `Parser`, la tabla y el
  tachado se parsean bien y el `HtmlRenderer` los pinta **vacíos** (`<p></p>`).
- **`Regex("""…\s…""")` en Kotlin busca barra + `s`, no un espacio**, y falla en silencio. Con
  comillas normales. Salió en un filtro de seguridad.
- **Para saber qué color gana en un `AnnotatedString`, el ÚLTIMO span que cubre el tramo**: el mayor es
  el span base (cubre todo) y el menor es el de búsqueda. Y el de búsqueda **no cambia el color, solo
  el fondo**: pedir `color` da transparente y no prueba nada.
- **Un índice escrito a ojo se desplaza en cuanto el fixture cambia**: anclar con `indexOf`, nunca con
  números sueltos. Cuatro asserts apuntaban al carácter equivocado y seguían verdes.
- **Una expectativa de memoria es una suposición**: `[a](b)` son **dos** tokens, `*b*` no incluye los
  espacios, `[ref]` **sí** es un enlace colapsado, `a*b*c` no es énfasis.
- **`onRelease` de `AndroidView` se dispara con la vista aún en el árbol**: `destroy()` ahí deja la
  pantalla en negro. Guardar la referencia y destruir en `DisposableEffect`, como `InAppYouTubePlayer`.
- **`Icons.AutoMirrored.Filled.Note` necesita su propio import**; `Icons.Filled.Note` está obsoleto.
- **`StringIndexOutOfBounds: Index -1`** en un tokenizador es `md[i-1]` con `i==0`, o un `indexOf` que
  devolvió −1 usado como posición.
- **Kotlin NO admite referencias adelantadas a funciones locales.** Colocar el bloque **después** de
  las funciones que llama.
- **Apóstrofos crudos** en `strings.xml` rompen `mergeDebugResources`, **después** de que Kotlin
  compila limpio. Y en `strings.xml` los saltos son `\n` literal, nunca 0x0A.
- **Un `BUILD SUCCESSFUL` puede no ejecutar nada**: `testDebugUnitTest` sale `UP-TO-DATE`; usar
  `--rerun`. Misma familia que el de AAPT2 y que el del `catch` que traga excepciones.
- **Un `try/catch` que se traga la excepción en un filtro de seguridad no da error, solo no filtra.**
  Y `applyRegex` en el resaltador hace justo eso.
- **Una mutación que nadie ve roja es un test que no prueba.** T3 de la spec 012: la primera mutación
  dejó en verde el test que decía cubrir, porque lo defendía otra capa.
- **Una tabla no oculta texto.** Ni `singleLine`, ni `maxLines`, ni alto máximo.
- **Geometría**: un tirador más ancho que el carril solo funciona entero dentro; un carril más estrecho
  que el tirador lo obliga a alinear por su borde derecho; una geometría que reparte un alto a todas las
  filas miente en cuanto una crece.
- **Una lista paralela obliga a un caso especial en cada capa**: los `headers` de la tabla obligaron a
  `HEADER_ROW`, `headerHeightPx`, `setHeader` y `moveColumn` muerto. Montar el fixture **por la ruta de
  lectura**, no por el constructor.

## Próximos pasos 
- **Verificar en dispositivo, spec 012**: que el `WebView` pinte y no deje la pantalla en negro al
  cambiar de modo; que el reparto 50/50 sea el esperado y las dos mitades no se desalineen al
  desplazar; que el chip no desborde con «Mitad y mitad» en los 8 locales; y que una lista de tarea
  salga con casilla. Las pruebas puras comprueban **qué se compone** y el HTML, no que se vea.
- **Defecto conocido, sin spec**: el enfasis dentro de una celda de tabla o del texto de un título no
  se tokeniza, porque el token de bloque cubre el texto entero. Sale del color de la celda, no del
  enfasis. Es coherente con RF-65, pero conviene mirarlo en una pantalla.
- **Verificar en dispositivo, spec 009**: que una celda larga se lea **entera** en varias líneas, que
  el tinte de cabecera cubra la fila entera y no solo el alto de su texto, y que el contorno caiga
  sobre la celda enfocada en una fila más alta que las de arriba. **Nada de esto se ha visto**: las
  pruebas puras solo comprueban la aritmética de la geometría, no que las celdas alcancen esos altos.
- **Verificar en dispositivo, spec 008**: que la fila 0 se pinte como cabecera y se pueda borrar y arrastrar como las demás; que con la opción apagada siga visible como fila de datos; que el tirador de columna salga al enfocar una celda de la fila 0 con la opción apagada; y que el HTML exportado conserve la cabecera. **Nada de esto se ha visto en una pantalla**: es riesgo declarado.
- **Unificar el segundo modelo de tabla, sin spec**: `NoteContentBlock.TableBlock` sigue guardando los títulos aparte y lo usan la lectura sin edición, el TTS y la previsualización. Al unificarlo se arreglaría de paso el defecto conocido de `BlockEditor.kt:1011`.
- **Verificar en dispositivo, spec 005**: el margen de 60dp por tabla puede resultar excesivo, y una tabla como primer bloque (nada de la 005 se ha visto); 90/120 Hz y jank con `FrameMetrics`; que abrir/cerrar el asistente carga/libera el modelo (`logcat -s AiModelHost`).
- **Verificar en dispositivo, spec 006** (riesgo declarado): que la píldora no quede tapada por el teclado, que el `errorContainer` en modo detener tenga contraste, que el campo bloqueado se lea como bloqueado, que cuatro líneas no se salgan de la `Surface` de 28 dp, que el rebote se perciba, que la hoja se cierre de verdad antes de abrir el otro selector (RF-35), que el saludo más largo (francés, 37 caracteres) quepa en pantalla estrecha, que las etiquetas nuevas quepan en el `LazyRow` y que el **relleno horizontal de 32 dp por lado** del campo nuevo —el doble de los 16 del `bottomBar` viejo, explícitamente **fuera del alcance** de la spec («las medidas exactas del nuevo campo»), así que no es incumplimiento— no se vea disparado en pantalla estrecha.
- **Defecto conocido, sin spec**: `BlockEditor.kt:1011` tiene un stub para `BlockType.TABLE` que ignora `meta["table"]` → la tabla sale vacía en TTS y previsualización. Son ~4 líneas.
- **Defecto conocido, sin spec**: `BlockEditor.kt:92` comparte un único `pendingSelection` entre todos los bloques y `EditableChecklistBlock` no informa de su selección.
- Spec 002, T4-T5: generar los dos goldens que faltan y fijar `roborazzi.test.verify`.

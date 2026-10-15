# MEMORY.md — secure-notes 

Memoria entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte. 

## Estado actual
- **Spec 017 (Markdown en el chat con IA) IMPLEMENTADA**, T1-T7 cerradas. `specs/017-markdown-chat-ia/`, RF-98…RF-103, RNF-48…RNF-52. El cuerpo de los mensajes se pinta con **Markwon directo** (`ui/markdown/`, 3 ficheros: `ChatMarkdownSource` puro, `ChatMarkwon` con el motor y `ChatMarkdownText` con el `AndroidView`). **45 pruebas** nuevas (36 puras + 9 invariantes): suite **656/41**, línea base **611/41**, **cero regresiones**; `assembleDebug` verde; **cero artefactos nuevos** (647 coordenadas en el árbol de `:app` antes y después, comprobado con `comm`). **Nada de esto se ha visto en un dispositivo.** `compose-markdown` **se queda** para `ChangelogSheet` y `UpdateInfoScreen`.

- **Spec 016 (GFM en el markup heredado) IMPLEMENTADA**, T1-T8 cerradas. `specs/016-gfm-legacy/`, RF-90…RF-97, RNF-44…RNF-47. Gramática pura nueva en `util/GfmMarkup.kt` (**56** pruebas, 0 imports de Compose) y cableado en `RichTextParser`. **117 pruebas puras verdes** (56 + 61), `assembleDebug` verde, **cero dependencias nuevas** y **cero regresiones**: la suite completa da 609/41 y la línea base 529/**41**. **Nada de esto se ha visto en un dispositivo.**

- **Spec 014 (descomponer el editor de notas) IMPLEMENTADA**, T1-T8 cerradas. `NoteEditorScreen.kt` de **3504 a 2342 líneas**, partida en 4 ficheros (`NoteEditorDialogs` 445, `NoteEditorSheets` 595, `NoteEditorAiSurfaces` 244, más `TabsHost`). Paquete `com.example.ui.editor/` con **diez** lógicas puras (sin `import androidx.compose` ni `import android`, con una prueba que lo vigila) y **79 pruebas** nuevas en `:app` (72 de lógica + 6 invariantes). Invariantes en `EditorInvariantsTest`: tamaño (2342), estado muerto (0), nunca asignado (0), funciones largas (7), guardados (2) y **techo de 600 líneas por fichero extraído**. ✅ **VERIFICADO EN DISPOSITIVO por quien Decide tras T5: «todo perfecto en pantalla».** ⚠️ **T6-T8 NO se han visto en una pantalla**: son movimiento puro de superficie, y lo único que puede romperse es que una hoja no se abra o que su cierre haga otra cosa. RF-84…RF-89, RNF-39…RNF-43.

- **Spec 012 (Markdown GFM) IMPLEMENTADA**, T1-T8 cerradas. Resaltado GFM con tokenizador propio, modos `SPLIT` 50/50 y `PREVIEW` con `WebView` (sin JS, sin red). **133 pruebas** en `:editor-de-codigo`, `:app:assembleDebug` verde. **Nada se ha visto en un dispositivo.** `specs/012-markdown-gfm-editor/`, RF-64…RF-75 y RNF-26…RNF-32.

- **Spec 013 (`.md` → base de notas) IMPLEMENTADA**, T1-T6 cerradas. Acción «Enviar a notas» en el editor de código: **siempre crea nota nueva**. **27 pruebas** nuevas en `:app`, `assembleDebug` verde. RF-76…RF-83 y RNF-33…RNF-38. **Sin verificación visual.**

- **Specs 005-011 (tabla) implementadas** y encadenadas; la 011 se **verificó en dispositivo** y deja el tirador entero dentro con carril de 36 dp. Sin verificación visual: 005, 008 y 009.

- **Spec 006 (chat con IA) implementada**: tres prototipos adoptados y siete lógicas puras en `ui/chat/`, sin Compose ni reloj. 53 casos verdes. Sin ver en dispositivo.

- **Inset del teclado unificado** en editor y chat (decisión 12): una sola fuente, en el `bottomBar`.

- `./gradlew test` completo **no está verde** y no puede estarlo aquí: Robolectric sin runtime nativo para `linux/aarch64`. Los tests **puros sí se ejecutan**. Las 2 pruebas Robolectric de `ExternalFileStorageRobolectricTest` son la línea de base de `:editor-de-codigo`.

## Decisiones (y por qué)

- **19. El chat se pinta con Markwon directo, no con la librería de Markdown que usa el changelog.** Revisados sus AAR y su código: su «resaltado de sintaxis» solo pintaba nodos delimitados por `==` que nada produce (**no había resaltado real**), y su `AndroidView` va envuelto en una `key(markdown.hashCode())`, o sea que **destruye y recrea la vista en cada cambio de texto**. Escribir el `AndroidView` son ~60 líneas y a cambio el gesto y el color dejan de ser cosa de la librería. Las seis piezas de Markwon entran como `runtime` por `compose-markdown` y hay que declararlas aparte porque **`runtime` no llega al classpath de compilación**; el APK **no crece** (esto también se comprobó con `comm`, no de memoria).

- **20. El gesto se queda porque el `TextView` NO lleva método de movimiento.** Es lo que hace que no se quede con los toques: el menú de pulsación larga y el deslizamiento los sigue atendiendo la burbuja (RF-102), y de paso el motor **no abre nada por su cuenta** (su resolutor solo salta desde el toque de un enlace, y aquí no hay método de movimiento). El hit-test del enlace es nuestro: `getOffsetForHorizontal` → `getSpans(offset, offset, URLSpan)` → `isWebLink`, y **se devuelve `true` solo si abre de verdad**. Por eso RF-100 y RF-102 no se pisan: un `javascript:` no abre **ni se traga el menú**.

- **21. `<item>` se traduce a casilla `[ ]` y no a viñeta `- `.** Decidido con una **sonda** contra `RichTextParser`, no de memoria: hoy `<item>` y `<item checked="false">` pintan **lo mismo**. Traducirlo a viñeta le quitaría una casilla a quien hoy la ve, y este cambio no va de eso. La sonda enseñó además dos cosas más: un `<url=…>` sin cerrar **oculta también la dirección** (va dentro de la etiqueta), y el cierre `</item>` **se ve a la vista** hoy (`☐ Comprar pan</item>`), un fallo que la traducción no arrastra. **Dos expectativas de la spec resultaron falsas y las corrigió la sonda.**

- **22. La réplica en curso NO usa el motor (RF-103), y no porque no se pueda.** Se podría: la vista ya no se recrea. Es porque el texto a medio escribir **cambiaría de motor a mitad**, y una tabla sin línea separadora se vería como texto y un renglón después como tabla. Ese parpadeo se ve más que la inconsistencia con la burbuja ya guardada. Es la mejora natural de una spec posterior, y hay un invariante que lo vigila.

- **16. El setext GFM le gana a la raya, y por eso hubo que tocar también la capa de bloques.** `Texto\n---` es un encabezado de segundo nivel, no una raya (decidido por quien Decide; cambia notas existentes). Solo con `RichTextParser` **no se veía nunca**: `preprocessMarkdownBlocks` convertía ese `---` en `<hr/>` antes de que el texto llegara al analizador. Las dos capas tienen que decidir con la **misma** regla («¿la línea de arriba tenía texto?»), que es el mismo fallo que la spec 012 pagó con `---` dentro de una tabla.

- **17. La regla de énfasis del editor de notas es la de `MarkdownInline` (spec 012), no una nueva.** El plan unggulava una regla propia —«letra o dígito pegado por cualquiera de los dos lados, no aplica»— y era **falsa**: rechazaba `*cursiva*`, porque el `*` de cierre va pegado a la letra. La correcta tiene dos mitades: **abrir** mira lo de detrás *y* lo de delante, **cerrar** solo lo de delante. Motivo: editor de código y editor de notas son **dos superficies del mismo dialecto** y no pueden decidir distinto. `MarkdownInline` es `internal` de otra biblioteca, así que se replicó la regla con una prueba a cada lado, y no el fichero.

- **18. Las referencias por referencia necesitan una pre-pasada**, porque una definición `[r]: url` puede estar **después** del enlace que la usa y el analizador es de una sola pasada. Sale temprano si el documento no tiene ni un corchete (la mayoría de las notas son HTML propio). Gana la **primera** definición si se repite: si una nota trajera una definición falsa al final, sobreescribir con ella una legítima al principio cambiaría a dónde lleva un enlace que el usuario escribió.

- **15. El techo de 600 líneas de RNF-43 se aplica a lo que sale del composable, no al `NoteEditorScreen.kt`.** Escrita literal («ningún fichero del editor de notas supera las 600 líneas») es imposible: el propio `NoteEditorScreen.kt` mide 2342, y bajarlo de 600 exige **partir el composable**, que es justo lo que la 014 declara fuera de alcance. Así que el monolito se mide con su umbral propio, que baja en cada tarea, y las 600 líneas son el techo de lo extraído. Lo mismo con `BlockEditor` (1030), `EditableTableBlock` (1456) y `FloatingEditorToolbar` (759): ya pasaban de 600 antes de la 014 y son de otro fichero. **No se les baja el presupuesto para que pasen**: un invariante que nace rojo entrena a ignorar el rojo.

- **14. Los `InsertMediaDialog` ×2 no se movieron, y no es un pendiente de movimiento.** Catorce parámetros cada uno, ocho de ellos «qué hacer cuando» repetidos en los dos. El problema no es dónde viven sino cómo están modelados, y eso es la primera decisión de la 015. 

- **14. El editor de código NO escribe en la base de notas.** `CodeEditorActivity` vive **fuera** del flujo de bloqueo de la bóveda y no tiene el `NotesViewModel`: escribir desde ahí saltaría la contraseña maestra y el refresco de widgets. Deja un sobre en `cacheDir` con nombre **fijo** (`md_handoff.json`, porque la ruta acaba en un log y el nombre del archivo es del usuario) y `MainActivity` consume. Con la app bloqueada **espera**: no crea ni borra, que perdería el trabajo.

- **13. commonmark-java 0.13.0, no 0.22**: `compose-markdown` ya arrastra core + `ext-gfm-tables` + `ext-gfm-strikethrough` en el APK, así que declararlos en `:editor-de-codigo` **no descarga nada**. Subir a 0.22 forzaría la versión transitiva que usan `UpdateInfoScreen` y `ChangelogSheet`. En 0.13 no hay extensión de listas de tarea: se cubren con un post-proceso sobre el HTML propio. El jar es bytecode Java 7 sin `java.awt`/`java.sql`: comprobado antes de decidir.

- **12. El inset del teclado se aplica UNA vez, en el `bottomBar`, con `union`**:  `WindowInsets.ime.union(WindowInsets.navigationBars)` gana el `ime` con el teclado abierto y las barras de navegación con el teclado cerrado, sin tener que decidir en código. El síntoma que había (barra invisible pero **su hueco visible**) era doble conteo: `innerPadding` del `Scaffold` ya reservaba la altura del `bottomBar`, y el `.imePadding()` del `Box` de contenido la volvía a sumar. Con `adjustResize` implícito y `enableEdgeToEdge()`, el teclado tapa el `bottomBar`: sin inset propio, la barra queda **detrás** del teclado. ⚠️ **Pendiente de revisión visual**: Both `Scaffold` conservan su `.navigationBarsPadding()`, que reduce la caja del Scaffold mientras el `union` actúa dentro del `bottomBar`. Superficies distintas, se cree que no se duplica, pero no está verificado en dispositivo.

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

- **Escribir las pruebas primero pagaba la cuarta parte de los bugs.** En la spec 016, de los tres bugs de gramática, **uno lo pilló una prueba ya escrita** (el autolink sin el `<` de apertura, que hacía que `<https://…>` siguiera desapareciendo: el fallo exacto que la spec venía a arreglar) y los otros dos salieron al copiar una regla que ya existía. Los tests primero no es un ritual, es lo que encuentra el fallo antes de cablearlo.

- **Una regla de la spec 012 era justo lo que hacía falta, y el plan la había reescrito peor.** Antes de inventar una regla de énfasis, había que **leer la que ya está desplegada** en `MarkdownInline`. Dos superficies del mismo dialecto que decidan distinto es un bug esperando, y se evita copiando la regla y fijándola con una prueba a cada lado.

- **Una tirada sin su cierre tiene que quedar visible ENTERA, no un carácter.** Devolver `null` y dejar que el bucle avance uno solo hace que el resto de la tirada vuelva a evaluarse como tirada nueva, y esa sí cierra con la que hay al final. Hay que copiar la tirada entera y saltar por encima.

- **El ancla `^` de Kotlin con `Regex.find(text, start)` no se mueve del índice 0.** Tercera vez que lo paga la spec 016 (ya en la 014). Buscar sobre **la línea**, no sobre el documento.

- **Una fila separadora que es de una columna no es tabla**, y `pipeCount - 1 >= 2` es lo que lo dice: la mutación que lo afloja a `>= 1` pone roja **una sola** prueba, y esa única prueba es la que sostiene el mínimo de columnas de GFM.

- **Un test que reimplementa lo que prueba no prueba nada.** Pasó dos veces: `MarkdownPreviewContractTest` daba verde con el bug puesto (mutaba el Composable y el test llamaba al renderizador directamente), y `NoteHandoffTimingTest` reimplementaba la lógica. **O ejerce el código real, o su KDoc dice qué SÍ fija y qué NO.** Para lo segundo: extraer el cruce a una función real compartida.

- **Un bug de integración es invisible a los tests por pieza**: `MarkdownRendererTest` comprobaba que el renderizador funciona (funciona), `PreviewLayoutTest` que se componen los paneles (se componen). El fallo estaba en *quién llamaba a qué*, un cruce que no existía como código.

- **Un estado escrito desde `onNewIntent` no lo ve un `LaunchedEffect` cuya clave no cambia.** El bug de «enviar a notas no hace nada»: el sobre se guardaba en una `var` estática y el efecto tenía por clave `isUnlocked`, que **ya no cambiaba** al llegar el Intent. **El error no era la guarda, era el reloj**; ningún `StateFlow` lo habría salvado. Arreglo: consumir en el acto donde llega.

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

- **Los comentarios de bloque de Kotlin se ANIDAN.** Escribir `audio/*` dentro de un KDoc abre un
  comentario dentro del comentario: el `*/` que cierra el KDoc cierra el anidado y el de fuera queda
  abierto hasta el final del fichero. El error sale como `Syntax error: Unclosed comment` **en la
  línea donde el comentario debería cerrar**, muy lejos de la causa. Salió en la 014.

- **Kotlin NO admite referencias adelantadas a funciones locales.** Colocar el bloque **después** de
  las funciones que llama. ⚠️ Matiz que costó un `BUILD FAILED` en la 014: **sí** se puede capturar una
  `var` declarada después desde el cuerpo de una lambda (`LaunchedEffect`), porque la lambda se
  evalúa cuando corre. Lo que se resuelve al compilar es la **llamada a la función local**, y esa sí
  exige orden.

- **`^` no ancla al principio de cada línea en Kotlin** sin `RegexOption.MULTILINE`, y el índice que
  devuelve un `findAll` es un **desplazamiento en caracteres, no un número de línea**: usarlo como
  índice de lista deja el rango vacío y descarta todas las coincidencias **en silencio**. Pasó en dos
  invariantes de la 014 que daban verde sin comprobar nada. Todo recorrido por fuente lleva ahora una
  **autocomprobación** que verifica que está leyendo el archivo.

- **Al unificar dos código, comparar el comportamiento rama por rama, no la forma.** La 014: los dos
  mandos de formato (conmutable y con valor) compartían esqueleto pero **decidían distinto** con el
  cursor suelto —el conmutable mira la letra de debajo, el de valor solo lo pendiente—. Unificar sin
  esa comparación habría cambiado el color al pulsar sobre una letra ya coloreada.

- **`applyBlocksChange` repone el cursor al final del bloque.** No usarlo donde antes se asignaba
  `blocks` directamente: al convertir a título, el cursor habría saltado al final en cada `h1`.

- **Kotlin NO hace smart cast sobre una propiedad delegada.** `if (inPlaceAction != null)` con
  `inPlaceAction` de `collectAsStateWithLifecycle()` no estrecha el tipo: salía «Smart cast to
  'AiAction' is impossible». Se lee a un `val` y se pasa ese.

- **Cerrar un diálogo son cuatro operaciones, no una.** `onDismissRequest`, «cancelar», «aceptar» y
  «cerrar» **no coinciden** en ningún diálogo: el de URL limpia los campos en «cancelar» pero no al
  tocar fuera; el de imagen y vídeo sí limpia en los dos. Y en `UrlActionDialog` al editar hay que
  cerrarse **antes** de abrir el de escribir, así que un `onClose` compartido del padre las pisaría.
  **Un patrón que acierta cinco de seis es el que hace que el sexto se rompa sin que se entienda por
  qué**: hay que leer cada diálogo, no copiar el del vecino.

- **Un `when` de un brazo por cada constante de una enumeración es una tabla de despacho, no lógica.**
  Partirlo en varios ficheros lo hace más disperso, no más legible. Aplicar la regla de las 20 líneas
  a lógica, y dejarlo escrito en el invariante para que el número no parezca arbitrario.

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
- **Defecto encontrado en la 014, sin arreglar a propósito** (esta spec es de cero cambio de
  comportamiento, así que queda reportado, no tocado): `removeTransformForTag("normal", …)` **no
  tiene brazo `normal`** y cae en `else`, o sea que el botón «normal» de la barra **no hace nada**.
  El brazo que sí limpia todo está en `applyTransformForTag`. Son ~2 líneas.
- **Asimetría conservada a propósito, spec 014**: con el cursor sobre una letra ya coloreada y sin
  color pendiente, pulsar el color **vuelve a colorear** en vez de borrarlo, porque el mando de valor
  decide solo por el estilo pendiente. El conmutable sí mira la letra. Está documentado en
  `applyTagToSelection` y fijado por dos pruebas. Corregirlo es otra spec.
- **Spec 015 (siguiente)**, en este orden:
  1. **Decidir cómo se modelan los `InsertMediaDialog` ×2** (14 parámetros, 8 de ellos eventos).
     Es la primera decisión, porque de ella depende si salen como un diálogo o como dos.
  2. Agrupar el estado del editor (95 declaraciones) y recordar los ~11 manejadores de las barras.
     Es lo que queda de `NoteEditorScreen`, y es de riesgo alto porque el estado se comparte **por
     identidad** con `BlockEditor` (`pendingInsert`, `pendingSelection` y `pendingTagInsert` se le
     pasan como objetos `MutableState`, y `toolbarParseResult` lo escribe el hijo y lo lee la
     barra). **Es también lo único que puede bajar `NoteEditorScreen.kt` de las 600 líneas**: sin
     partir el composable, RNF-43 no se cumple para ese fichero.
  3. Desbloquea partir `handlePickedFile`, `openExternalFile`, `moveBlockToAnotherNote` y
     `handleSlashSelect`.
- **Verificar en dispositivo, T6-T8 de la 014**: solo superficie movida, así que lo comprobable es
  poco pero real: que la **hoja de voz** siga grabando y que cerrar con una grabación en curso la
  detenga (es la trampa de la decisión 7: los `remember` viven dentro de la hoja); que la **paleta**
  siga aplicando color e imagen de fondo; que el **panel de IA** mande la nota entera en resumir y
  corregir, y que «generar» siga tomando lo seleccionado; y que los **cinco diálogos** acepten,
  cancelen y cierren como antes. Nada de esto se ha visto en una pantalla.
- **Verificar en dispositivo, specs 005-013**: **ninguna de las specs 005 a 013 se ha visto en una
  pantalla**, y todas comparten el mismo riesgo: las pruebas puras comprueban **qué se compone** y la
  aritmética, no que se vea. Por spec: 012, el `WebView` pinta y no deja la pantalla negra al
  cambiar de modo, el reparto 50/50 no se desalinea y las listas de tarea salen con casilla. 009, que
  una celda larga se lea **entera** en varias líneas. 008, que la fila 0 se comporte como las demás.
  005, que el margen de 60 dp por tabla no sea excesivo. 006, que la píldora no quede tapada por el
  teclado y que la hoja se cierre de verdad antes de abrir el otro selector (RF-35). 013, que la
  acción del editor de código llegue a crear la nota. Detalle largo en `specs/*/plan.md`.
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
- **Verificar en dispositivo, spec 006** (riesgo declarado): que la píldora no quede tapada por el
  teclado, que el `errorContainer` en modo detener tenga contraste, que el campo bloqueado se lea como
  bloqueado, que el saludo más largo (francés, 37 caracteres) quepa en pantalla estrecha y que las
  etiquetas nuevas quepan en el `LazyRow`. El relleno horizontal de 32 dp del campo nuevo es
  **explícitamente fuera del alcance** de la spec, así que no es incumplimiento.
- **Defecto conocido, sin spec**: `BlockEditor.kt:1011` tiene un stub para `BlockType.TABLE` que ignora `meta["table"]` → la tabla sale vacía en TTS y previsualización. Son ~4 líneas.
- **Defecto conocido, sin spec**: `BlockEditor.kt:92` comparte un único `pendingSelection` entre todos los bloques y `EditableChecklistBlock` no informa de su selección.
- Spec 002, T4-T5: generar los dos goldens que faltan y fijar `roborazzi.test.verify`.

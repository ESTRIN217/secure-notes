# Tasks — Spec 014: El editor de notas, partido en capas

Plan: `specs/014-descomponer-editor/plan.md` · Spec: `specs/014-descomponer-editor/spec.md`

**Línea base, medida antes de empezar.** `NoteEditorScreen.kt` = 3504 líneas; la función
`NoteEditorScreen` ocupa 116-3496; 95 `remember`; 49 funciones locales; **4** declaraciones que
nadie lee y **3** que nunca se asignan; **8** llamadas al guardado completo; **10** funciones
locales de más de 20 líneas; 5 insertadores con el mismo preámbulo.
`:app:compileDebugKotlin` verde sin avisos. Pruebas comprobables: **411 en 33 clases, con 3 fallos
preexistentes** (`HtmlExportMediaTest.legacy item markup migrates to checklist html` y los dos de
`OnDeviceModelSourceTest`). **Los 3 tienen que seguir siendo exactamente esos 3 al terminar.**

Cada tarea se cierra con `compileDebugKotlin` en verde. Las pruebas van **antes** del código.

---

- [x] **T1. El tamaño del editor y el estado muerto se comprueban solos.** RF-88, RF-84
  - `EditorInvariantsTest.kt` en `:app`, prueba pura que lee el fuente del editor y falla si el
    archivo supera el umbral de líneas, y una que busca declaraciones locales cuyo nombre no
    aparece en ninguna otra línea del editor.
  - **Primero en rojo**: se fija el umbral en `líneas actuales + 1` y se ve fallar. Solo después se
    baja al valor real. Un invariante que nunca falla no está probado.
  - Hecho cuando: la prueba pasa con el umbral a 3504, y al subirlo a 3505 **falla** (comprobado,
    no supuesto). **Cumplido**: los cinco invariantes se bajaron uno a uno y **los cinco se pusieron
    rojos** con el número esperado.
  - Hecho cuando: el recuento de la línea base (411 comprobables, 3 preexistentes) queda anotado en
    el propio fichero de prueba, para que el siguiente que lo ejecute sepa contra qué compara.

  **Hallazgos de T1 que corrigen la línea base de la spec**

  - El guardado completo son **8** llamadas, no 7: se había escapado la de `moveBlockToAnotherNote`
    (línea 916). Lo encontró la propia prueba, no la revisión.
  - Hay un **quinto** trozo de código muerto, `insertEnd` (línea 998): se declara dentro del efecto
    que inserta el resultado de la IA letra a letra y no lo lee nadie. Vive a 8 espacios de
    sangría, dentro del efecto, y por eso el primer recuento no lo vio.
  - **Dos invariantes pasaban por vacío** en su primer intento: `^` no ancla al principio de cada
    línea en Kotlin sin `RegexOption.MULTILINE`, y el índice que devuelve el emparejado de una
    expresión regular es un **desplazamiento en caracteres**, no un número de línea: usado como
    índice dejaba el rango vacío y descartaba las 41 funciones en silencio. Por eso ahora cada
    recorrido lleva una **autocomprobación** que verifica que está leyendo el archivo: un recorrido
    roto devuelve una lista vacía y la prueba se pone verde sin comprobar nada.

- [x] **T2. La lógica comprobable sale a `ui/editor/` y gana pruebas.** RF-87, RF-86
  - Cuatro ficheros nuevos: `SegmentTagTransforms.kt` (436-480), `ToolbarStyleState.kt` (368-434),
    `NoteSearchMatches.kt` (el núcleo de 302-356), `BlockFileKinds.kt` (1424-1431), más
    `BlockInsertPlan.kt`, que es la decisión compartida por los cinco insertadores y que usa T4.
  - Pruebas puras nuevas para los cinco, **en rojo antes**: **29 casos**.
  - `computeToolbarStyle` devuelve solo el conjunto de estilos: se **borran** `activeFontColor` y
    `activeBgColor`, que nadie leía (decisión 2 del plan).
  - Hecho cuando: `grep -c '^import androidx.compose'` da **0** en los cuatro ficheros. **Cumplido**.
    Y no es una inspección a ojo: hay una prueba que lo comprueba, y se ha **visto ponerse roja** al
    añadir un `import androidx.compose.ui.graphics.Color` a mano.
  - Hecho cuando: `--tests '*EditorLogicTest*' --rerun` en verde. **Cumplido, 29 casos**.
  - Hecho cuando: `compileDebugKotlin` verde y `NoteEditorScreen.kt` **baja** de líneas.
    **Cumplido: de 3504 a 3374.** `MAX_LINEAS` se baja a 3374 para que la suite siga verde.

  **Lo que T2 Destapó de la lógica real** (nada de esto estaba en el plan, salió al leer el código)

  - **Los estilos de la barra se suman, no se pisan.** Con el cursor sobre un tramo en negrita y
    pendiente de escribir en cursiva, los dos botones están encendidos. Se documenta porque lo
    contrario es lo esperable, y hay prueba que lo fija.
  - **Con el cursor justo detrás del último carácter no se enciende ningún estilo.** El tramo solo
    pesa si el cursor es *estrictamente* anterior al final, y solo hay excepción si hay estilo
    pendiente. Es sorprendente y también queda fijado por prueba.
  - **Los rangos de la búsqueda llevan final exclusivo**, igual que el rango de texto que usaba el
    editor: `0..3` son tres caracteres. Se documenta en la función y en la prueba, porque en Kotlin
    un `IntRange` se lee como inclusivo y aquí no lo es.
  - **Las apariciones se solapan**: "aaa" buscando "a" devuelve tres coincidencias, porque la
    búsqueda avanza de una en una. Fijado por prueba.

- [x] **T3. Se borra el código muerto y el guardado deja de estar copiado.** RF-84, RF-85, RF-86
  - Fuera: `insertAtCursor`, `isSearchActive`, `showSearchMoreOptions`, `insertEnd`,
    `hasNavigatingToDrawing` (y su uso). **Cinco**, verificado con `grep` a cero.
  - `persistNote(content: String)` **sin guarda**, y las siete llamadas de la nota actual pasan por
    ella. La guarda de `saveCurrentNote()` y la de salir **no se mueven** (decisión 4 del plan). La
    llamada de `moveBlockToAnotherNote` **no se toca**: guarda la nota destino del bloque movido, no
    la que se está editando.
  - Hecho cuando: `MAX_GUARDADOS` baja a **2** en el invariante de T1 y sigue en verde, y el
    recorrido de estado muerto no encuentra nada. **Cumplido**: 2 llamadas, y los cinco nombres
    borrados dan cero ocurrencias. Con el umbral ya en 0, los dos recorridos se **volvieron a
    comprobar** inyectando código muerto a mano: los dos se pusieron rojos nombrándolo.
  - Hecho cuando: `compileDebugKotlin` verde y las comprobables siguen en 3 fallos, los mismos.

  **Dos cosas que T3 destapó**

  - **Kotlin no admite referencias adelantadas a funciones locales, pero sí capturar variables.**
    Esto no es un detalle retórico: es justo la diferencia entre por qué el código viejo compilaba y
    por qué el nuevo no. Los dos efectos de la IA vivían **antes** de `contentForSave` y ya usaban
    `attachments`, declarado más abajo, y eso estaba bien porque el cuerpo de un `LaunchedEffect` es
    una lambda que se evalúa cuando corre. En cuanto el guardado pasó a ser una **función local**
    llamada desde esas lambdas, Kotlin la resuelve al compilar y exigía orden. Por eso
    `contentForSave` y `persistNote` se movieron **por encima** de los efectos de la IA, con el
    comentario que lo explica en el sitio: el siguiente que los ordene por lo que se ve los vuelve a
    bajar y rompe la compilación.
  - **La guarda de «salir» era la de `saveCurrentNote` más un flag muerto.**
    `!hasNavigatingToDrawing && (título o contenido o adjuntos)` es literalmente `hasContentToSave()`,
    porque el flag nunca se activaba. Cambiar una por la otra no altera qué se guarda, y es lo que
    permite que el guardado al salir pase por el mismo camino único.

  **Sobre el tamaño**: T3 dejó el archivo en 3318 líneas, **tres más** que al empezar la tarea. No es
  un error: el borrado quitó unas 30 y el documento de `persistNote`, el comentario del orden de
  declaración y el del guardado *streaming* añadieron más de las que se fueron. El recuento de
  líneas mide peso, no avance, y por eso el invariante de tamaño no es la medida de éxito de esta
  entrega: lo son `MAX_ESTADO_MUERTO` y `MAX_GUARDADOS`.

- [x] **T4. Los insertadores pasan a ser uno, y los mandos puros salen del composable.** RF-89, RF-87
  - `BlockCommands.kt`: `headingTypeForTag`, `moveBlockBy`, `deleteBlockAt`, `convertBlockAt`.
    Pruebas nuevas para los cuatro, **10 casos**. `moveBlockToAnotherNote` y `handleSlashSelect`
    **no** salen: cierran sobre 17 y 14 variables, y separarlos es la spec 015.
  - `insertBlockAtCursor(block)` sustituye a los **cinco** insertadores de medio, que eran byte a byte
    idénticos. `handleSlashSelect` era un **sexto**, con la misma forma pero **otra regla**.
  - Hecho cuando: no queda ni un preámbulo `replaceInPlace` duplicado. **Cumplido, cero.**
  - Hecho cuando: `compileDebugKotlin` verde. **Cumplido.** El editor baja a **3240 líneas**.

  **Cuatro cosas que T4 destapó**

  - **`handleSlashSelect` sustituye por otra regla.** No cuando el bloque está vacío, sino cuando
    **empieza por `/`** — que es el texto que se estaba escribiendo. Confundir las dos rompe la
    barra de comandos: al elegir `/imagen` con el cursor en un párrafo vacío, se insertaría en lugar
    de sustituir. La forma sí se comparte, la regla no, así que `insertionIndex` se extrajo aparte y
    la regla se quedó en quien llama. El código viejo tenía además un
    `block.copy(content = block.content)`, que es copiar un bloque para no cambiarlo.
  - **`convertBlockToHeading` no pasaba por `applyBlocksChange`, y no debe.** Se intentó al unificar
    y `applyBlocksChange` **repone el cursor al final del bloque** y rehace la selección. Como la
    conversión a título se dispara precisamente mientras el cursor está ahí, hacerlo habría
    teletransportado el cursor al final en cada `h1`. Se detectó leyendo, antes de compilar.
  - **`insertBookmarkBlock` cierra el menú de la barra antes de validar la URL.** Con URL vacía hace
    `return` temprano, así que delegar ese cierre en `insertBlockAtCursor` habría dejado el menú de
    la barra abierto al cancelar. Se conserva explícito, con el motivo escrito.
  - **La prueba que escribí para `insertionIndex` estaba mal y el código bien.** Pedía
    `insertionIndex(-3, false, 5) == 0` cuando lo correcto es 1: un índice por debajo de la lista se
    acota al 0 y, al ser inserción, cae en el 1. Se corrigió la expectativa, no el código.

  **Sobre «pruebas en rojo antes»**: con una API que no existe, el test no compila, y eso no es una
  prueba en rojo sino un error de compilación. La señal útil se resolvió de otro modo: `deleteBlockAt`
  se escribió **a propósito sin** la regla de «no dejar la nota sin bloques», y el test la cazó
  (`expected null, but was:<[]>`). Eso demuestra que el test vigila la regla, no que compila.

- [x] **T5. Los mandos con más de veinte líneas salen del composable.** RF-87, RNF-41
  - **Hecho**: `TagApplication.kt` con `applyTagToSelection` (el esqueleto que `applyTag` y
    `applyTagWithVal` tenían **escrito dos veces**) más `setTagValue`, `clearTagValue` y `tagValueOf`.
    `applyTag` baja de 50 a 31 líneas, `applyTagWithVal` de 70 a 22. Se corrige de paso la
    indentación de ocho espacios de más.
  - **Hecho**: `NoteFileImport.kt` con `isAudioFile` y `externalTargetFor` (`PDF`, `SIN_TIPO`,
    `TEXTO`, `VER_CON_OTRA_APP`). La decisión de qué es un archivo y con qué se abre **ya no está
    enterrada entre I/O y `Toast`**, y la cadena de cuatro `if` sueltos de `openExternalFile` pasa a
    ser un `when` sobre el enum. Pruebas nuevas de T5: **23 casos**.
  - `handleBlockCommand` (109) se queda: es una tabla de despacho, un brazo del `when` por acción.
    Lo que sí era ilegible —un brazo que hacía cuatro cosas— ya se ha sacado.
  - `MAX_FUNCIONES_LARGAS` baja de **10 a 7**, y el motivo de cada una de las siete queda escrito en
    el propio invariante, para que el número no parezca arbitrario.
  - Hecho cuando: `compileDebugKotlin` verde. **Cumplido.** Editor en **3182 líneas**.

  **Lo que T5 destapó, y es lo más importante de la spec**

  - **Al unificar dos código hay que comparar el comportamiento rama por rama, no la forma.**
    `applyTag` y `applyTagWithVal` eran el mismo esqueleto, y la primera versión de la función común
    **cambió el comportamiento del segundo**. Los dos deciden distinto con el cursor suelto: el
    conmutable mira **la letra de debajo y lo pendiente**; el de valor mira **solo lo pendiente**. Con
    el cursor sobre una letra ya coloreada y sin color pendiente, el conmutable la borra y el de valor
    **vuelve a colorearla**. Unificar «arreglando» el segundo habría roto el color al pulsar sobre
    texto ya coloreado. Se detectó leyendo, antes de compilar; la versión final lleva las dos reglas
    como parámetros separados, con la diferencia explicada en el KDoc y fijada por dos pruebas.
  - **`removeTransformForTag("normal", …)` no tiene brazo `normal`** y cae en `else`: el botón
    «normal» de la barra no hace nada. El que sí limpia todo está en `applyTransformForTag`.
    **No se arregla aquí** —esta spec es de cero cambio de comportamiento— y se reporta en
    `MEMORY.md` para otra spec. Son dos líneas.
  - **El invariante de estado muerto cazó un error mío**: al reemplazar `applyTag` por rango de
    líneas perdí el guardia `if (tag !in STYLE_TAGS) return`. Es una regresión silenciosa —una
    etiqueta desconocida habría dejado de ignorarse— y se vio porque `STYLE_TAGS` apareció como
    declarado y no leído. Sin ese invariante, el guardia se habría perdido sin que nadie se entere.

  **Una estrella sobre «pruebas en rojo antes»**: con una API que no existe el test no compila, y eso
  no es una prueba en rojo sino un error de compilación. La señal útil se Sacó de otro modo: el
  detector se **sembró con código muerto a propósito** y se vio ponerse rojo nombrándolo; y
  `deleteBlockAt` se escribió **sin** su regla de «no dejar la nota sin bloques» y el test la cazó
  (`expected null, but was:<[]>`). Un invariante en cero que no se ha visto fallar no está probado.

## Estado al dejar T5

| | antes | tras T5 |
|---|---|---|
| líneas de `NoteEditorScreen.kt` | 3504 | **3185** |
| pruebas comprobables en `:app` | 411 | **473** |
| fallos comprobables | 3 (preexistentes) | **3, los mismos** |
| estado que nadie lee / nunca se asigna | 4 / 3 | **0 / 0** |
| llamadas al guardado completo | 8 | **2** |
| funciones locales de más de 20 líneas | 10 | **7** |
| lógica pura en `ui/editor/` | — | **7 ficheros, 0 imports de composición** |

`BUILD FAILED` en la suite completa es el Robolectric de plataforma (38), igual que la línea base.

- [x] **T6. Los diálogos salen a su propio fichero.** RNF-43 — **5 de 6, cerrado aquí a propósito**
  - **Hecho**: `NoteEditorDialogs.kt` con `InsertUrlDialog`, `InsertBookmarkDialog`, `EquationDialog`,
    `InsertTableDialog` y `UrlActionDialog`. Parámetros con nombre, devuelven eventos, **ninguno
    declara estado**.
  - **Dos núcleos puros salieron además**: `TableShape.kt` (`buildTableRows`, la lógica de cabecera
    de RF-48 que estaba escondida en el botón de «aceptar») y `LinkLookup.kt` (`findUrlAt`, un bucle
    que estaba **escrito dos veces** en el diálogo del enlace, una para editar y otra para borrar).
    Pruebas nuevas: **8 casos**, incluido el límite `offset < fin` del enlace.
  - **Los `InsertMediaDialog` ×2 se quedan fuera de T6, y no por falta de sitio.**
    `NoteEditorDialogs.kt` son 445 líneas con los otros cinco. Su problema no es **dónde** viven sino
    **cómo están modelados**: catorce parámetros cada uno, ocho de ellos «qué hacer cuando»
    repetidos en los dos. Sacarlos sin decidir antes cómo se modelan es copiarlos a otro fichero,
    que es lo contrario de lo que se vino a hacer. Es la primera decisión de la 015, y por escrito
    queda aquí para que no se pierda: **no es un movimiento pendiente, es una decisión pendiente.**
  - Editor en **2906 líneas** al cerrar T6, y en **2342** tras T7.

  **Lo que T6 destapó**

  - **Confirmar, cancelar y cerrar tocando fuera no hacen lo mismo.** En los diálogos de enlace y
    marcador, confirmar y cancelar limpian los campos; tocar fuera **solo cierra**. Unifiqué los tres
    en un `onDismiss` a la primera —que es lo que dicta el patrón— y el diálogo empezaba a borrar
    `urlEditRange` al cerrarse por fuera, cuando ese rango lo fija otro camino que todavía puede
    necesitarlo. Se detectó releyendo el diff. Cada diálogo lleva **dos** callbacks de cierre y el
    motivo está en el KDoc.
    ⚠️ **Pero no es una regla general**: en el de ecuación los tres caminos **sí** coinciden y lleva
    uno solo. Copiar el patrón sin leer cada diálogo rompe la mitad de las veces.
  - **Cada acción de un diálogo puede tener que cerrar ella misma, y en un orden distinto.** En
    `UrlActionDialog` no hay un `onCerrar` compartido: al editar, este diálogo tiene que cerrarse
    **antes** de abrir el de escribir. Si el cierre lo hiciera el padre, los dos se pisarían. Es el
    caso contrario al anterior —allí sobraba un cierre, aquí falta— y por eso conviene leer cada
    diálogo en vez de copiar el patrón del vecino.
  - **Un bucle escrito dos veces es un bucle que se desincroniza.** El de buscar el enlace bajo el
    cursor estaba copiado literal en «editar» y en «borrar». Ahora es `findUrlAt`, con prueba del
    caso límite: el enlace termina en `start + largo - 1`, así que la posición justo detrás **no** es
    suya — con `<=` en vez de `<`, un clic al final del enlace lo editaría sin haberlo pulsado.

- [x] **T7. Las hojas y la superficie de IA salen a su propio fichero.** RNF-43
  - `NoteEditorSheets.kt` (paleta 2744-2891, voz/archivo 2917-3253) y `NoteEditorAiSurfaces.kt`
    (2651-2742, 3255-3339).
  - **El `if (showVoiceFileSheet)` se queda en el punto de llamada**: los `remember` de dentro no
    sobreviven al cierre de la hoja, y mover el `if` al hijo cambiaría eso (decisión 7 del plan).
  - Las hojas **no** se meten dentro del `Box` que lleva `padding(innerPadding)`.
  - Hecho cuando: ningún fichero del editor supera las 600 líneas y `compileDebugKotlin` verde.

  **Hecho**: `NoteEditorSheets.kt` (595) y `NoteEditorAiSurfaces.kt` (244). Editor de **2906 a 2342**.

  **Lo que T7 destapó**

  - **RNF-43 escrita tal cual es imposible, y conviene decir por qué.** Dice «ningún fichero del
    editor de notas supera las 600 líneas», pero el propio `NoteEditorScreen.kt` mide 2342. Que
    baje de 600 exige **partir el composable**, que es exactamente lo que la spec 014 declara fuera
    de alcance —agrupar el estado que lo sostiene—. Así que el invariante nuevo vigila las 600
    líneas **sobre lo que sale del composable**, y el monolito se sigue midiendo con su umbral
    propio, que baja en cada tarea. No es relajar el requisito: es que, aplicado a un solo
    fichero, medía lo que no quería medir.
  - **La primera versión del invariante metió a `BlockEditor` (1030), `EditableTableBlock` (1456) y
    `FloatingEditorToolbar` (759), y salió rojo de partida.** No se les baja el presupuesto para
    dejarlos pasar: no se meten, y está escrito por qué. Un invariante que nace rojo entrena a su
    lector a ignorar el rojo, que es peor que no tenerlo.
  - **`NoteEditorSheets.kt` salió en 601 líneas**, una por encima del techo. Se recortó el KDoc, no
    el código: el techo es un presupuesto de **peso**, y el peso está en lo que se ejecuta.
  - **Kotlin no hace smart cast sobre una propiedad delegada.** `inPlaceAction` llega por
    `collectAsStateWithLifecycle()`, así que `if (inPlaceAction != null)` no estrecha el tipo. Se lee
    una vez a un `val` y se pasa ese, con el motivo escrito al lado.
  - **La hoja de voz tiene tres estados excluyentes por construcción, no por casualidad.** El
    `if / else if / else` original ya los separaba bien; lo que no se leía era el panel entero de
    golpe. Separado en `RecordButton` / `StopButton` / `RecordedDraftRow` se ve que no puede haber
    dos a la vez, y el `when` que lo sustituye no deja margen a que un día se añada un cuarto.

- [x] **T8. Cierre: el invariante baja a su valor final y la memoria recoge el corte.** RF-88, RNF-39, RNF-41
  - Se baja el umbral del invariante al tamaño real que queda y se **vuelve a ver fallar** subiendo
    una vez, para comprobar que el invariante sigue vigilando.
  - `assembleDebug` verde.
  - `MEMORY.md`: lo que se hizo, lo que queda para la 015 (estado agrupado y barras) y por qué el
    corte es aquí.
  - Hecho cuando: el recuento de líneas del editor **baja de 3504**, el de comprobables **sube de
    411**, y los fallos comprobables siguen siendo **3 y los mismos**.
  - Hecho cuando: `git diff -- app/src/main/res` vacío — esta entrega no toca ningún `strings.xml`.

  **Cierre verificado**

  - `MAX_LINEAS` bajado a **2342** y **visto ponerse rojo** a 2341, nombrando el fichero. El
    invariante nuevo de las 600 líneas se vio rojo **dos veces**: a 594 por `NoteEditorSheets`, y
    después por `NoteEditorScreen`, que fue lo que destapó la contradicción de RNF-43.
  - `EditorLogicTest` 72 y `EditorInvariantsTest` **6** (uno nuevo) en verde. `assembleDebug` verde.
  - `git diff -- app/src/main/res` **vacío**.
  - Suite completa: **528 tests, 41 fallos — los mismos 41** de la línea base: 38 de Robolectric sin
    runtime nativo para `linux/aarch64` y 3 preexistentes, con los mismos nombres. **Comprobables:
    490** (antes 411).
  - Los seis imports que T7 dejó sin usar se borran: `verticalScroll`, `CircleShape`, `clip`,
    `TextOverflow`, `MediaPlayer` y `MediaRecorder`. Los otros que ya sobraban **se quedan**: no son
    de esta entrega, y quitarlos ensuciaría el diff de un recorte que promete cero cambios de más.

---

## Estado al cerrar la entrega

| | antes | ahora |
|---|---|---|
| líneas de `NoteEditorScreen.kt` | 3504 | **2342** |
| ficheros en que se partió | 1 | **4** (`Dialogs` 445, `Sheets` 595, `AiSurfaces` 244, `TabsHost` 473) |
| pruebas comprobables en `:app` | 411 | **490** |
| fallos comprobables | 3 (preexistentes) | **3, los mismos** |
| estado que nadie lee / nunca se asigna | 4 / 3 | **0 / 0** |
| llamadas al guardado completo | 8 | **2** |
| funciones locales de más de 20 líneas | 10 | **7** |
| lógica pura en `ui/editor/` | — | **10 ficheros, 0 imports de composición** |
| invariantes que vigilan el editor | 0 | **6** |

Suite completa: **528 tests, 41 fallos**, los mismos 41 de la línea base —38 de Robolectric sin
runtime nativo para `linux/aarch64` y 3 preexistentes. `assembleDebug` verde.

---

## Orden y por qué

T1 primero porque fija la línea de verdad: sin ella, «no se ha roto nada» es una impresión. T2 y T3
siguen porque son lo que más riesgo tiene de romper y lo que más fácil de comprobar. T4-T5 mueven
lógica, que es donde se concentran las funciones largas. T6-T7 son movimiento de superficie: mucho
código, poco riesgo, y por eso van después, con la lógica ya en verde. T8 cierra.

## Fuera de esta entrega

El agrupamiento de las 95 declaraciones de estado y el recuerdo de los manejadores de las barras van
a la **spec 015**. No es que sean difíciles: es que el estado se comparte **por identidad** con el
editor de bloques (`pendingInsert`, `pendingSelection` y `pendingTagInsert` se le pasan como objetos
`MutableState`, y `toolbarParseResult` lo escribe el hijo y lo lee la barra mil líneas más abajo).
Tocar eso con el resto a medio mover es multiplicar el riesgo.
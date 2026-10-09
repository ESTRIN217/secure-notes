# MEMORY.md — secure-notes 
Memoria entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte. 
## Estado actual 
- **Spec 010 (el carril se estrecha) implementada**: el carril del tirador de fila pasa de 48 a
  **24 dp**, así que la tabla recupera 24 dp de ancho en pantallas estrechas. El tirador **sigue
  midiendo 48 dp** y sobresale 12 dp por fuera de la tarjeta; para que ese desborde siga siendo
  pulsable, la capa de tiradores se desplaza a la izquierda y el tirador de columna compensa.
  **63 pruebas puras en verde** (46 geometría, 17 `TableRowHeightsTest`) y `assembleDebug` limpio.
  `ui/table/` **sin tocar**: `gutterWidth` es un parámetro.
- **Spec 009 (celda multilínea y geometría por fila) implementada**: la tabla **mide cada fila por
  separado**. Antes era un `rowHeightPx` único repartido a todas (`List(rows.size) { rowHeightPx }`),
  así que el contorno sumaba alturas falsas en cuanto una fila crecía. **93 pruebas puras en verde**
  (17 `TableRowHeightsTest` nueva, 44 geometría, 25 `TableDataTest`, 7 texto plano) y `assembleDebug`
  limpio. Enmienda el **RF-54** de la 008.
- **Spec 008 (la cabecera es una fila más) implementada**: la fila de cabecera era una lista paralela a `rows` y ahora **es la fila 0**. Fuera `TableData.headers`, `HEADER_ROW`, `headerHeightPx`, `setHeader` y el composable `TableHeaderRow` (110 líneas). **107 pruebas puras en verde** (25 `TableDataTest` nueva, 40 geometría, 7 texto plano, 35 parser) y `assembleDebug` limpio. Enmienda **RF-28 y RF-29** de la spec 005 (la primera fila ya tiene tirador con *eliminar*).
- **Spec 006 (rediseño del chat con IA) implementada y comprobada en todo lo comprobable aquí**: **T1–T13 HECHAS** (implementación, comprobación y cierre documental). Los tres prototipos están adoptados y conectados —`FloatingChatInputField` es el `bottomBar`, `AttachmentAiSheet` la hoja de adjuntos, `EmptyChatWelcome` la bienvenida, y `AiChatScreen.kt` los cablea—, con las **siete lógicas puras** en `ui/chat/` (L1/L2, L3, L4/L6/L7, L5): sin imports de Compose, sin `R`, sin reloj. La carga de la edición al abrir la sesión la decide **L6** (`adoptPendingEditing`), no el Composable (RNF-11). **53 casos verdes** (`./gradlew :app:testDebugUnitTest --tests 'com.example.Chat*'`) y `assembleDebug` con **cero avisos** en `main`.
- **Nada de la spec 006 se ha visto en un dispositivo**, y no se puede en esta plataforma: riesgo declarado, no criterio incumplido. Los `grep` verifican que el texto esté escrito, no que la pantalla se pinte bien.
- `./gradlew test` **completo no está verde**: `355 tests completed, 41 failed` en `:app` (+2 en `:editor-de-codigo`). **40** son de plataforma (`AssertionError at DefaultNativeRuntimeLoader.java:212`: Robolectric sin runtime nativo para `linux/aarch64`) y **3** son preexistentes y ajenos a esta entrega (`HtmlExportMediaTest.kt:117`, `OnDeviceModelSourceTest.kt:63` y una comparación `huggingface.co` vs `https://huggingface.co`). Los tests **puros sí se ejecutan** aquí.
- **La nota `BROKEN` de `AGENTS.md` sobre `compileDebugUnitTestKotlin` era falsa y ya no está**: con `--rerun` compila bien; solo los dos avisos preexistentes de `createComposeRule` en `src/test`.
- **Spec 007 (tiradores de 48 dp) implementada**, T1-T5 hechas y T6-T8 sin cerrar; su carril de 48 dp y los tres tiradores son el suelo de la 008.
- **Spec 005 implementada** (`specs/005-tabla-seleccion/`): selección y foco de la tabla. Los manejadores se superponían a la primera columna y **tapaban su primera letra**; ahora van en un carril y solo se dibuja el de la fila activa. **Sin verificación visual.**

## Decisiones (y por qué) 
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
- **Un carril más estrecho que el tirador obliga a alinearlo por su borde derecho, nunca centrado.**
  Centrado, la mitad derecha cae sobre el relleno **y el texto** de la primera celda: es el defecto
  que el carril vino a cerrar. Pegando el borde derecho al final del carril, el tirador ocupa carril
  y relleno y se detiene donde empieza el texto. Y lo que sobresale de un hijo queda fuera de los
  límites de su padre, así que el padre hay que **ensancharlo** o el toque no llega a esa parte.
- **Una geometría que reparte un mismo alto a todas las filas miente en cuanto una crece.** El bug
  llevaba años oculto: `singleLine` y el alto fijo de 44 dp hacían que todas las filas midieran lo
  mismo. Al volver multilínea aflora, y entonces el contorno y los tiradores caen sobre filas que no
  son. **Un alto por fila, y el que se realimenta a las celdas es un `heightIn(min = …)`, nunca un
  `height(…)`**: con alto exacto, una celda que crece tras la medida recorta el texto.
- **Una tabla no oculta texto.** Ni `singleLine`, ni `maxLines`, ni alto máximo. La elipsis es la
  señal de que el dato no cabe, y en una tabla no basta: hay que leerlo.
- **La capa que dibuja no siempre es la que miente.** El contorno y los tiradores salían mal por
  cómo se construían las medidas, no por el dibujado: `TableSelectionGeometry` ya sumaba altos reales
  y tiene prueba que lo fija. Antes de tocar el dibujado, comprobar quién le da los datos.
- **Una lista paralela obliga a un caso especial en cada capa.** El `headers` de la tabla no era solo un modelo raro: forzaba `HEADER_ROW`, `headerHeightPx`, un `setHeader` que descartaba lo escrito, un `moveColumn` muerto sin cabecera, un tirador que resaltaba sin mover y `<th>` fuera de su `<tr>`. La prueba que lo detecta es **montar el fixture por la ruta de lectura, no por el constructor**: con el constructor, 13 de 25 pruebas pasaban sin comprobar nada.
- **Quitar `<tr>` con `replace` no comprueba nada**: `html.replace("<tr>","")` deja el `<th>` dentro y la aserción falla siempre. Hay que quitar las filas **bien formadas** con `Regex("<tr>.*?</tr>")` y mirar lo que queda fuera.
- **Una dependencia entre capas retrasa el verde**: el modelo de la tabla no se pudo probar en verde hasta cerrar el editor, porque `:app` es un solo módulo. El orden «modelo → editor → verificar» no está mal, pero el «hecho cuando» de la primera tarea no se cumple hasta la última.
- **Un `BUILD SUCCESSFUL` puede no ejecutar nada**: `testDebugUnitTest` salió `UP-TO-DATE` con `compileDebugKotlin FROM-CACHE`. Para comprobar tests, `--rerun`; si aun así sale `FROM-CACHE`, `--no-build-cache`. Misma familia que el de AAPT2: un éxito temprano no demuestra lo que parece demostrar.
- **Apóstrofos crudos** en `strings.xml` rompen `mergeDebugResources`, **después** de que Kotlin compila limpio.
- **`Icons.Filled.Note` y `Icons.Outlined.Note` están los dos obsoletos** en esta versión (comprobado en el bytecode con `javap`); el único que no lo está es `Icons.AutoMirrored.Filled.Note`.
- **Dos sobrecargas homónimas con firmas distintas no son una redeclaración**: Kotlin compila sin quejarse. Lo que sí bloqueaba la compilación eran los `@Preview` rotos.
- En Kotlin **una referencia a recurso no puede terminar en `"`**: un `grep` anclado con comilla de cierre da 0 aunque la referencia exista; hay que anclar por `)` o por el separador que siga de verdad. Pasó tres veces en la spec 006.
- `Modifier.onFocusChanged` **no** observa el foco de los descendientes. Un control superpuesto **más ancho que el relleno de la celda se come texto**: hay que darle carril propio (se vio en un vídeo, no se lee en el código). Si Coil recibe una factoría `diskCache`, `.directory(...)` es **obligatorio**: no hay crash, las imágenes simplemente no cargan. `remember` en un `for` **exige `key()`**.
- material3 1.5.0-alpha28: `FloatingToolbarDefaults.containerShape` es **internal**, el `DragHandle` horizontal no es público, `TextRange` vive en `...ui.text` y `AnimatedVisibility` dentro de un `Box` resuelve a la sobrecarga de `ColumnScope`.
- Una prueba que no se ha visto fallar no está probada: mutar el código y comprobar que el caso se pone rojo. La lógica pura va **sin imports de Compose** a propósito, para poder ejercitarla sin el entorno de pruebas; criterio: `grep -c '^import androidx.compose'` → 0.
- **En `strings.xml` los saltos son la secuencia literal `\n`, nunca un 0x0A.** Un script que escriba `\n` dentro de un literal Python normal mete un newline real y **`mergeDebugResources` pasa igual**: lo delata el `git diff --stat` (12 líneas tocadas en vez de 2). Misma familia que AAPT2 y que el `UP-TO-DATE`: el verde no demuestra lo que parece.
- **Afirmar una errata exige mirar el diccionario o el código, no el ojo.** Dos veces fallé en la revisión del README: (a) `bloc de notas` es correcto — DLE 1ª acepción «cuaderno» y Notepad en Windows se llama así; `bloques` son las unidades del editor (`DataBlock`/`BlockType`), así que la mezcla no es inconsistente; (b) «E2EE»/«de ponto a ponta» **sí es defendible**: la clave nunca sale del dispositivo y Drive solo ve ciphertext. Regla: las 9 locales divergen entre sí, así que una frase de marketing se contrasta con `strings.xml` + código antes de llamarla error.

## Próximos pasos 
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

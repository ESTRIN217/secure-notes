# Tareas — Spec 015: Editor de notas, estado aislado y tablas completas

Plan: `specs/015-editor-estado-y-tablas/plan.md` · Spec:
`specs/015-editor-estado-y-tablas/spec.md`

Las pruebas de lógica pura se escriben primero y se comprueba su fallo antes de producción. Cada
tarea termina con sus pruebas selectivas y compilación aplicable. En este entorno, Robolectric puede
fallar antes de ejecutar por falta de runtime nativo para `linux/aarch64`; se separan esos fallos
de cualquier regresión real.

---

- [x] **T1. Fijar el orden TTS de las celdas de tabla con pruebas puras.** RF-154, RF-157, RNF-95
  - Extender `TtsChunksTest` con tabla de varias filas, celdas vacías/solo espacios y bloques antes y
    después; verificar título y orden exacto de fragmentos.
  - Hecho cuando: la prueba nueva falló por la omisión actual de tablas (8 tests, 1 fallo de
    aserción), sin fallo de compilación.

- [x] **T2. Incluir en TTS las celdas no vacías, por filas.** RF-154, RF-157, RNF-93, RNF-95
  - Añadir transformación pura de `TableData` a fragmentos de celda e integrarla en `buildTtsChunks`.
  - Hecho cuando: todas las pruebas TTS pasan, tabla vacía añade cero fragmentos y las pruebas
    anteriores conservan resultado y orden. **Cumplido:** `TtsChunksTest`, 8 tests en verde.

- [x] **T3. Definir conversión de tabla a datos de vista previa con tests en rojo.** RF-153, RNF-93,
  RNF-95
  - Crear pruebas puras para tabla con cabecera, sin cabecera, sin filas y con filas de anchura
    irregular.
  - Hecho cuando: la conversión actual causa fallos de aserción esperados, no errores de
    compilación. **Cumplido:** los tres casos ejecutaron y fallaron en sus aserciones.

- [x] **T4. Alimentar la vista previa con las filas persistidas.** RF-153, RNF-93
  - Reemplazar el `TableBlock` ficticio construido desde `content` por los datos convertidos desde
    `meta["table"]`, preservando la regla de cabecera de `TableData`.
  - Hecho cuando: las pruebas puras pasan y la vista previa recibe cabecera/filas iguales a las
    almacenadas; el parseo legacy o la recuperación conserva el comportamiento definido.
    **Cumplido:** `TablePreviewDataTest` y `TtsChunksTest` pasan juntos (13 tests); JSON,
    HTML legacy y fallback inválido cubiertos.

- [x] **T5. Modelar la propiedad de selección pendiente en una función pura.** RF-155, RNF-95
  - Añadir pruebas para selección coincidente, otra nota, otro índice, snapshot de bloque cambiado,
    ausencia de selección y consumo único.
  - Hecho cuando: los casos de propiedad aún no implementados fallan por aserción y no dependen de
    Robolectric. **Cumplido:** los 5 casos de comportamiento fallaron primero por aserción; los 6
    tests puros pasan con la decisión y el estado consumido.

- [x] **T6. Aplicar la selección solo a su bloque y consumirla una vez.** RF-155, RNF-93, RNF-95
  - Transportar rango junto con su origen (nota, índice y snapshot); descartar al cambiar nota,
    bloque o contenido antes de que el editor aplique el rango.
  - Hecho cuando: pruebas puras pasan, `EditableTextBlock` solo cambia selección con origen
    coincidente, y cambiar de bloque no modifica el texto destino. **Cumplido:** el estado con
    origen llega desde `NoteEditorScreen` a `BlockEditor` y solo el editor activo compara/consume
    nota, índice y snapshot; tests puros y compilación pasan.

- [x] **T7. Agrupar el estado compartido de selección y barras.** RF-155, RF-156, RNF-94
  - Añadir primero invariantes de fuente para las fronteras del estado y registrar valores iniciales
    y ciclo de vida; después mover el estado cohesivo de selección, foco, cursor y barras a sus
    propietarios sin agrupar indiscriminadamente todos los `remember`.
  - Hecho cuando: invariantes y compilación pasan, y el estado compartido no cambia de identidad ni
      se reinicia al recomponer. **Cumplido:** `NoteEditorInteractionState` agrupa contenido,
      selección, foco, cursor, formato pendiente y estado de barra; se crea por `noteId`, conserva
      los valores iniciales anteriores y el invariante mide ahora 2340 líneas (antes 2342).
      `EditorInvariantsTest` y pruebas dirigidas de selección/tablas/TTS: 26 tests en verde.

- [x] **T8. Extraer selección de comandos y operaciones de archivos.** RF-156, RNF-94
  - Probar antes las decisiones aislables; después dar hogar explícito a `handleSlashSelect`,
    `handlePickedFile` y `openExternalFile`, conservando sus reglas de inserción, permisos y
    manejo de errores. No rediseñar `InsertMediaDialog`.
  - Hecho cuando: las pruebas puras pasan, la compilación queda verde y el selector de comandos y
    ambos caminos de apertura mantienen el destino y resultado anteriores. **Cumplido:** la
    decisión slash vive en `SlashCommandSelection`; las operaciones de archivo en
    `NoteEditorFileOperations`. `EditorInvariantsTest`, `EditorLogicTest` y
    `SlashCommandSelectionTest` pasan con compilación (`BUILD SUCCESSFUL`); pantalla en 2284 líneas.

- [x] **T9. Extraer movimiento entre notas sin alterar el guardado.** RF-156, RNF-93, RNF-94
  - Añadir/ajustar pruebas para la decisión de origen/destino y su preservación de datos; después
    extraer la operación de movimiento usando el estado ya agrupado.
  - Hecho cuando: la nota origen/destino conserva bloques y adjuntos como antes, el guardado de la
    nota destino sigue siendo distinto del guardado normal, y la compilación pasa. **Cumplido:**
    `NoteBlockMovePlan` transforma listas sin mutar el origen; conserva el contenido y adjuntos del
    destino y añade el bloque movido. El editor mantiene el guardado explícito del destino y aplica
    el resultado al estado agrupado. `NoteBlockMovePlanTest`, `EditorInvariantsTest` y
    `EditorLogicTest` pasan (84 tests, `BUILD SUCCESSFUL`); pantalla en 2277 líneas.

- [x] **T10. Validar, verificar en dispositivo y cerrar.** RF-153, RF-154, RF-155, RF-156, RNF-93,
  RNF-94, RNF-96
  - Ejecutar pruebas TTS, tabla, selección y `TableDataTest`; ejecutar `:app:compileDebugKotlin`,
    `:app:assembleDebug` y la suite completa; comparar resultados por clase con la línea base.
    En dispositivo, probar tabla con/sin cabecera en vista previa; TTS con celdas vacías; cambio de
    bloque/nota con selección pendiente; comandos y apertura de archivo.
  - Hecho cuando: formato JSON sin cambios, pruebas puras pasan, no aparecen fallos nuevos de
    plataforma y no se eliminó ni desactivó ningún test; cada escenario manual queda registrado y
    `MEMORY.md` refleja el cierre verificado.
  - **Validación automática cumplida:** `./gradlew :app:testDebugUnitTest --tests
    'com.example.TtsChunksTest' --tests 'com.example.TablePreviewDataTest' --tests
    'com.example.PendingTextSelectionTest' --tests 'com.example.TableDataTest' --tests
    'com.example.NoteBlockMovePlanTest' --tests 'com.example.EditorInvariantsTest' --tests
    'com.example.EditorLogicTest' --tests 'com.example.SlashCommandSelectionTest' --rerun` pasa
    sus 132 tests; `./gradlew :app:compileDebugKotlin :app:assembleDebug` termina en
    `BUILD SUCCESSFUL`.
    `./gradlew test` mantiene el número de fallos de línea base: `:app` 768 completadas/41
    fallidas y `:editor-de-codigo` 149/2; 40 fallos son del runtime Robolectric `linux/aarch64`,
    y los tres restantes son los fallos preexistentes de `HtmlExportMediaTest` y
    `OnDeviceModelSourceTest`.
  - **Verificación manual completada (dispositivo conectado):** la tabla conserva sus datos al alternar
    la fila de encabezado; TTS inicia para una nota con celdas vacías y no vacías; la selección de
    `Ants` se descarta al pasar al bloque de tabla y no aparece en una nota nueva; el comando slash
    `Tabla` se probó al crear la tabla. Un archivo sintético propio se abrió desde Descargas y se
    importó como nota. La persona usuaria confirmó que el orden audible del TTS es correcto. Se
    eliminaron las notas temporales y el archivo de prueba.

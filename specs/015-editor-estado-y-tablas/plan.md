# Plan — Spec 015: Editor de notas, estado aislado y tablas completas

Spec: `specs/015-editor-estado-y-tablas/spec.md` · Estado: implementada

**Progreso:** T1–T10 completadas. La validación automática pasa en pruebas dirigidas,
`compileDebugKotlin` y `assembleDebug`; la suite completa conserva sus fallos de línea base. En el
dispositivo conectado se comprobaron los datos de tabla con cabecera activa/inactiva, TTS con celdas
vacías, selección al cambiar de bloque/nota, comando slash e importación de archivo sintético. La
persona usuaria confirmó que el orden audible del TTS es correcto; se limpiaron los datos temporales.

TTS y vista previa de tablas están implementados; la selección pendiente se captura con su
nota/bloque de origen y `EditableTextBlock` la consume solo si la identidad coincide.
`NoteEditorInteractionState` agrupa estado de contenido, selección y barras, con ciclo de vida por
nota. La decisión slash es pura en `SlashCommandSelection`, y las operaciones de importar/abrir
archivos pertenecen a `NoteEditorFileOperations`. `NoteBlockMovePlan` calcula el movimiento de
bloques sin mutar el origen ni perder adjuntos del destino; la pantalla conserva su guardado
explícito de la nota destino.

Cobertura: **RF-153** (tabla en vista previa), **RF-154/157** (TTS), **RF-155** (selección
pertenece a su bloque), **RF-156** (operaciones conservan destino), **RNF-93** (sin cambio de
datos), **RNF-94** (sin regresiones), **RNF-95** (lógica pura comprobable), **RNF-96** (sin
dependencias ni migración).

## 1. Línea base y restricciones

- Medir antes de tocar: `NoteEditorScreen.kt`, `BlockEditor.kt`, recuento y clases de pruebas puras
  pertinentes (`TtsChunksTest`, `EditorLogicTest`, `EditorInvariantsTest`, `TableDataTest`).
- La forma persistida de tabla es `DataBlock(type = TABLE, meta["table"] = TableData.toJson())`;
  no se cambia.
- `DataBlock` no tiene un id estable. La selección temporal debe identificarse por nota, posición
  de bloque y una instantánea del bloque de origen. Si cualquiera difiere al consumirla, se
  descarta. No se inventa un id persistido.
- Robolectric no dispone de runtime nativo para `linux/aarch64`; usar pruebas JUnit puras para las
  reglas extraídas, ejecutar las pruebas selectivas y documentar cualquier bloqueo del resto.
- No se rediseñan `InsertMediaDialog`; no se añaden dependencias ni migraciones.

## 2. Archivos y responsabilidades

| Archivo/componente | Responsabilidad | RF |
|---|---|---|
| `app/src/main/java/com/example/util/NoteEditorUtils.kt` o helper puro extraído a `util/` | Convertir el contenido de tabla a fragmentos TTS en orden por filas, omitiendo celdas en blanco | RF-154, RF-157, RNF-95 |
| `app/src/main/java/com/example/ui/BlockEditor.kt` y helper puro de conversión | Convertir `TableData.rows` a cabecera y filas de presentación según `showHeader`; leer `meta["table"]`, probar el parser legacy y conservar el fallback actual para datos inválidos | RF-153, RNF-93 |
| `app/src/main/java/com/example/ui/NoteContentBlockRenderer.kt` | Renderizar el modelo de tabla ya poblado, conservando el orden y la presentación existentes | RF-153 |
| `app/src/main/java/com/example/ui/NoteEditorScreen.kt` | Agrupar el estado cohesivo del editor, mantener notas/bloques activos y pasar las acciones a sus consumidores | RF-155, RF-156 |
| `app/src/main/java/com/example/ui/NoteEditorFileOperations.kt` | Importar archivo seleccionado y abrir archivo externo conservando destinos, permisos y errores | RF-156, RNF-94 |
| `app/src/main/java/com/example/ui/editor/SlashCommandSelection.kt` | Decidir de forma pura si sustituir el placeholder slash o insertar un bloque | RF-156, RNF-95 |
| `app/src/main/java/com/example/ui/editor/NoteBlockMovePlan.kt` | Preparar el bloque de origen y contenido de destino sin mutar ni perder adjuntos | RF-156, RNF-93, RNF-94, RNF-95 |
| `app/src/main/java/com/example/ui/BlockEditor.kt`, `EditableTextBlock.kt`, `WysiwygCodeBlock.kt` | Entregar una selección pendiente solo al bloque de origen y consumirla una sola vez | RF-155 |
| Paquete `app/src/main/java/com/example/ui/editor/` | Contener el modelo de propiedad de selección y las operaciones de editor extraídas si necesitan compartir estado | RF-155, RF-156, RNF-95 |
| `app/src/test/java/com/example/TtsChunksTest.kt` | Fijar flattening TTS, celdas vacías y orden entre tipos de bloque | RF-154, RF-157 |
| Nuevas pruebas puras `TablePreviewDataTest` y `PendingTextSelectionTest` | Verificar conversión de tabla y propiedad/invalidez de la selección | RF-153, RF-155, RNF-95 |
| Pruebas actuales de editor y sus invariantes | Vigilar que las acciones no pierdan su destino y que continúe la separación hecha en 014 | RF-156, RNF-94 |

Los nombres de helpers de prueba nuevos son descriptivos; se ajustarán a las convenciones reales
del paquete al implementar, sin cambiar los contratos de usuario de la spec.

## 3. Interfaces y lógica comprobable

### 3.1 Fragmentos TTS de tabla

La transformación recibe `TableData` y produce `List<String>` sin `Context` ni acceso a plataforma.
Cada celda se recorta para decidir si es vacía y se conserva su texto visible, en orden de filas.
`buildTtsChunks` integra esos valores en la posición del bloque tabla y conserva el título y los
fragmentos de los otros bloques.

### 3.2 Datos de presentación de tabla

Una función pura recibe `TableData` y devuelve los campos que ya consume `NoteContentBlock.TableBlock`:
cabecera, filas y alineaciones por defecto. Si `showHeader` está activo, la primera fila se separa
como cabecera; si no, todas las filas son filas de datos. Una tabla vacía produce listas vacías, no
contenido ficticio.

### 3.3 Selección pendiente

El valor pendiente contiene el rango y una clave de origen compuesta por `noteId`, índice de bloque
y la instantánea `DataBlock`. La operación pura de consumo recibe la selección y la identidad actual:

```
consume(selection, currentNoteId, currentBlockIndex, currentBlock):
    if selection is null: return None
    if selection.origin != (currentNoteId, currentBlockIndex, currentBlock): return Discard
    return Apply(selection.range) and clear pending
```

La pantalla captura origen cuando publica la selección. `BlockEditor` entrega el valor solo al
editor del bloque coincidente; la transición a otra nota/bloque invalida el valor. El rango se
consume una sola vez. El modelo conserva el `MutableState`/flujo Compose existente solo en los
bordes de UI; la comparación de propiedad es pura.

### 3.4 Estado del editor y operaciones pendientes

Agrupar únicamente estado que se comparte por identidad y que es necesario para dar hogar a
operaciones locales pendientes. Empezar por selección y barras que cruzan
`NoteEditorScreen`/`BlockEditor`; añadir estado que una operación concreta requiera al extraerla, en
lugar de agrupar de golpe todos los `remember`. Las funciones extraídas reciben entradas explícitas
y callbacks estrechos; la interfaz sigue pintando estado y emitiendo eventos. No cambiar la
semántica de guardado, acciones de archivo, slash-command, movimiento entre notas o inserción.
Dividir el inventario por responsabilidades, no volcarlo en un contenedor indiscriminado.

## 4. Algoritmo

```
buildTtsChunks(title, blocks):
    append title when non-blank
    for block in document order:
        if block is TABLE:
            append each trimmed non-blank cell by row, left to right
        else:
            append the existing plain-text chunk when non-blank

tablePreviewData(table):
    rows = table.rows
    if table.showHeader and rows is not empty:
        headers = rows.first
        bodyRows = rows.drop(1)
    else:
        headers = empty
        bodyRows = rows
    return headers, bodyRows, default alignments

onPendingSelectionChanged(selection, active note/block):
    if origin does not match active note/block snapshot:
        clear selection
        do not change text selection
    else:
        clamp range to current text
        apply once and clear pending value
```

## 5. Decisiones y alternativas

1. **No añadir identificador persistido a `DataBlock`.** Un id cambiaría el modelo/serialización y
   no es necesario para una selección efímera. Se valida con nota, posición y snapshot; añadir un
   id o migración queda descartado.
2. **Leer tabla por celdas, en lugar de serializar Markdown o separadores.** La persona usuaria
   eligió celdas no vacías por fila. Así TTS no anuncia sintaxis ni encabezados artificiales.
3. **Usar las filas existentes para la vista previa.** `TableData` ya es fuente de verdad; no
   construir una tabla ficticia ni modificar renderer para ocultar que faltan datos.
4. **Mantener la semántica actual de acciones.** La extracción es refactor, no rediseño: se
   descarta ampliar el alcance a `InsertMediaDialog` y cualquier otro comportamiento.
5. **Pruebas puras antes de la integración Compose.** Las reglas de propiedad, flattening y
   transformación se prueban fuera de Robolectric; la compilación y comprobaciones de integración
   cubren sus call sites.

## 6. Estrategia de pruebas

- Antes de cambiar producción, añadir pruebas puras en rojo para tabla TTS, datos de vista previa y
  decisión de selección coincidente/obsoleta.
- En cada tarea de integración ejecutar la selección más pequeña que cubra el cambio:
  `./gradlew :app:testDebugUnitTest --tests 'com.example.TtsChunksTest' --tests 'com.example.TablePreviewDataTest' --tests 'com.example.PendingTextSelectionTest' --rerun`.
- Para T8, `./gradlew :app:testDebugUnitTest --tests 'com.example.EditorInvariantsTest' --tests 'com.example.EditorLogicTest' --tests 'com.example.SlashCommandSelectionTest' --rerun` pasó (`BUILD SUCCESSFUL`).
- Para T9, `./gradlew :app:testDebugUnitTest --tests 'com.example.NoteBlockMovePlanTest' --tests 'com.example.EditorInvariantsTest' --tests 'com.example.EditorLogicTest' --rerun` pasó: 84 tests (`BUILD SUCCESSFUL`).
- Para T10, las pruebas selectivas de TTS, tablas, selección, movimiento e invariantes pasan.
  `./gradlew :app:compileDebugKotlin :app:assembleDebug` termina en `BUILD SUCCESSFUL`.
  `./gradlew test` conserva `:app` 768/41 y `:editor-de-codigo` 149/2; 40 fallos son Robolectric
  en `linux/aarch64` y los otros tres son los casos preexistentes de `HtmlExportMediaTest` y
  `OnDeviceModelSourceTest`. Las comprobaciones manuales se hicieron en el Infinix de pruebas; la
  persona usuaria confirmó el orden audible de TTS.
- Ejecutar `./gradlew :app:compileDebugKotlin` después de cambios de interfaz/composición.
- Al cierre ejecutar `./gradlew test` y comparar clases de fallo con línea base; en este host las
  suites Robolectric fallan antes del test por falta de runtime nativo `linux/aarch64`. Reportar las
  pruebas puras que sí ejecutan y cualquier error nuevo por separado.
- Ejecutar `./gradlew :app:assembleDebug` para validar recursos y paquete integrado.
- La comprobación manual en dispositivo cubre: tabla con y sin cabecera en vista previa; TTS en
  tabla con celdas vacías y texto antes/después; cambio de bloque/nota con una acción de selección
  pendiente; acciones habituales del menú y apertura de archivo.

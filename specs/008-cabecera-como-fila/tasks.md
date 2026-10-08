# Tasks — Spec 008 · La fila de cabecera es una fila más

Plan: `specs/008-cabecera-como-fila/plan.md` · Suelo: `specs/005-tabla-seleccion/` y
`specs/007-tiradores-tabla/`

9 tareas, en orden de dependencia: primero el modelo, porque el editor y los exportadores compilan
contra él. Cada «Hecho cuando» es un comando o un resultado observable, y cada tarea empieza por su
prueba en rojo.

---

- [x] **T1. `TableDataTest` en rojo.** RF-46, RF-48, RF-50, RF-51, RF-52, RNF-19
  - Pruebas puras nuevas en `app/src/test/java/com/example/TableDataTest.kt`, sin Robolectric.
  - Casos: lectura de la forma antigua (`headers` + `rows`), lectura de la forma nueva, ida y vuelta,
    `<tr><th>` en la primera fila con la opción activa, ningún `<th>` fuera de un `<tr>`, primera fila
    con `<td>` y presente con la opción apagada, `columnCount` desde la fila más ancha.
  - **Las tablas se montan leyendo JSON o HTML, no con el constructor.** Montadas por el
    constructor, un modelo con los títulos aparte se parecería al nuevo —sin ninguna fila de
    cabecera— y **13 de las 25 pruebas habrían pasado sin comprobar nada**. Leyendo, la mitad cae en
    rojo justo por lo que se quiere cambiar.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests '*TableData*' --rerun` da
    **25 pruebas, 12 en rojo**, y las 12 son de las formas de cabecera y no de otra cosa.

- [x] **T2. `TableData`: la cabecera es la primera fila.** RF-46, RF-48, RF-50, RF-51, RF-52, RNF-19
  - Fuera el campo `headers`. `rows` es la única fuente y `columnCount` sale de la fila más ancha.
  - `fromJson` lee las dos formas y antepone los títulos antiguos siempre que la lista no esté vacía.
  - `toHtml` emite cada fila dentro de su `<tr>`, con `<th>` solo en la primera y con la opción activa.
  - `fromLegacyHtml` antepone igual. `default3x3` pasa a 4 filas.
  - Hecho cuando: T1 está en verde con `--rerun`, y
    `grep -n "headers" app/src/main/java/com/example/data/model/DataBlock.kt` no devuelve ninguna
    declaración de lista de títulos.
  - **La lectura se partió en tres funciones puras** (`readRows`, `readWeights` y dos ayudantes de
    `JSONObject`) porque en un solo bloque la función se pasaba de las 20 líneas de la constitución #3.
  - **La comprobación en verde de T2 no se pudo hacer aquí**: el editor compila contra este modelo,
    así que hasta cerrar T4-T6 `:app` no compila y la prueba no llega a ejecutarse. Se verificó
    **después**, en T9: 25 de 25. Es la misma familia que el `UP-TO-DATE` que ya está anotado en
    `MEMORY.md`.

- [x] **T3. `TableSelectionGeometryTest` en rojo y `TableSelectionGeometry.kt` sin cabecera.** RF-53
  - Reescribir la prueba: sin `CellKey(-1, …)`, sin `headerHeight`, sin `showHeader`.
  - Fuera `HEADER_ROW`, `TableMetrics.headerHeight` y `TableMetrics.showHeader`; `rowTop` desde 0;
    `rowHandleCenters` con un centro por fila.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*' --rerun` está
    en verde y `grep -rn "HEADER_ROW\|headerHeight" app/src/main/java/com/example/` no devuelve nada.
  - **40 de 40 en verde.** Se añade `a negative row has no outline because there is no row before the
    first`: fija que un índice de fila negativo ya no señala nada, que es justo lo que la constante
    eliminada permitía.
  - Dos pruebas se escribieron casi iguales y una comprobaba una igualdad trivial consigo misma
    (`rowHandleCenters(metrics)` contra `rowHandleCenters(metrics.copy())`); se quitaron por no
    comprobar nada.

- [x] **T4. El estado del bloque de tabla.** RF-46, RF-49, RF-52
  - Fuera `TableBlockState.headers`, `headerHeightPx` y `setHeader`.
  - `columnCount`, `focusedRow`, `focusedCellForOutline` y `moveFocus` sin el caso de la fila negativa.
  - `moveColumn` sin la guardia `headers.isEmpty()`; `insertColumnLeft/Right` y `deleteColumn` solo
    sobre filas; `fitToContent` mide la primera fila si la opción está activa.
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` no da errores en este archivo y
    `grep -n "headers\|setHeader\|headerHeightPx" app/src/main/java/com/example/ui/EditableTableBlock.kt`
    no devuelve nada.

- [x] **T5. Una fila, un composable.** RF-47, RF-54
  - `TableHeaderRow` se borra; `TableRow` recibe `esFilaCabecera` y pinta la primera fila.
  - `TableCell` sustituye `isHeaderCol` por `esCabecera` (fila o columna) y admite la barra de ancho.
  - La barra de ancho y el tirador de columna pasan a la primera fila con la opción activada o no.
  - `TableCell` con el mismo `heightIn`, el mismo `singleLine`, el mismo relleno y el mismo
    reconocimiento de enlaces esté o no en la primera fila.
  - Fuera los parámetros muertos `focusedRow` y `focusedCol` de `TableRow`.
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` termina sin errores y
    `grep -c "TableHeaderRow" app/src/main/java/com/example/ui/EditableTableBlock.kt` devuelve 0.

- [x] **T6. Los tiradores de fila y de columna.** RF-49, RF-53, enmienda a RF-28 y RF-29
  - Fuera `headerSlot`, `dataRowCount` y la guarda `canDeleteRow && rowIndex != HEADER_ROW`.
  - Un tirador por fila, el de la activa, con eliminar disponible también en la primera.
  - El tirador de columna aparece al enfocar una celda de la primera fila, sin mirar la opción.
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` termina sin errores y el diff muestra que
    `RowHandle` ya no recibe `rowIndex = -1` por ningún camino.

- [x] **T7. Los cuatro exportadores.** RF-48, RF-50, RF-51
  - `RichTextConverter`: texto plano, Markdown y HTML recorren filas; Markdown marca la primera en
    negrita con la opción activa.
  - `PdfBlockRenderer.drawTable` recorre filas y pinta la primera si la opción está activa.
  - `NoteEditorUtils.parsePipeTableToHtml` emite los títulos dentro de un `<tr>`.
  - Las tres pruebas que construyen `TableData` se actualizan **sin tocar sus aserciones**.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests '*PlainTextExport*' --tests '*HtmlExportMedia*' --rerun`
    está en verde y `git diff --stat` de los tres ficheros de prueba solo muestra la construcción.

- [x] **T8. El diálogo de insertar tabla.** RF-48
  - `NoteEditorScreen`: con la opción marcada se crea la primera fila con los títulos y `showHeader`
    activo; sin ella, `showHeader` apagado y ninguna fila de cabecera.
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` termina sin errores y el diálogo deja de pasar
    una lista de cadenas vacías como si fueran títulos.

- [x] **T9. Cierre técnico y memoria.** RNF-20, RNF-21
  - `./gradlew :app:testDebugUnitTest --tests '*TableData*' --tests '*TableSelectionGeometry*' --tests
    '*PlainTextExport*' --tests '*HtmlExportMedia*' --tests '*RichTextParser*' --rerun` en verde.
  - `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug` sin errores.
  - `git diff -- app/src/main/res` vacío.
  - `MEMORY.md`: la regla permanente «la cabecera es la primera fila / la primera columna: es un
    pintado, no un dato», el error a evitar (una lista paralela obliga a un caso especial en cada
    capa) y la verificación pendiente en dispositivo.
  - Hecho cuando: los cuatro comandos terminan correctamente, `MEMORY.md` está en 50 líneas o menos y
    nombra la regla.
  - **Medido**: `compileDebugUnitTestKotlin` y `assembleDebug` en verde y **107 pruebas puras en
    verde** (25 + 40 + 7 + 35, recuento leído de los XML de resultados, no del mensaje de Gradle).
    `MEMORY.md` baja a 42 líneas.
  - **`HtmlExportMediaTest > legacy item markup migrates to checklist html` falla, y es preexistente**:
    es el `HtmlExportMediaTest.kt:117` que ya arrastraba la suite y no lo toca esta entrega
    (comprobado contra `HEAD`).
  - **`git diff -- app/src/main/res` NO está vacío**, y no por esta spec: son las tres claves de la
    007 (`table_row_actions`, `table_column_actions`, `table_move_handle`) sin commitear en el árbol.
    El criterio de RNF-21 se cumple igual: **esta entrega no añade ninguna cadena**.
# Tasks — Spec 007 · Tiradores del bloque de tabla

Plan: `specs/007-tiradores-tabla/plan.md` · Suelo: `specs/005-tabla-seleccion/`

8 tareas, en orden. Cada «Hecho cuando» es un comando o un resultado observable. Tocar las
cadenas antes del código evita tener el árbol roto mientras compila.

---

- [x] **T1. Las tres claves en los nueve locales.** RF-43, RNF-17
  - `table_row_actions` = «Row actions», `table_column_actions` = «Column actions»,
    `table_move_handle` = «Move table», junto al bloque de tabla de cada `strings.xml`.
  - Traducir a los ocho locales restantes: `values-es-rVE`, `values-es-rES`, `values-b+es+419`,
    `values-pt-rBR`, `values-pt-rPT`, `values-fr`, `values-it`, `values-en-rGB`.
  - Ninguna de las tres lleva apóstrofo; si hiciera falta, se escribe `\'`.
  - Hecho cuando: `grep -rc "table_row_actions\|table_column_actions\|table_move_handle" app/src/main/res/values*/strings.xml`
    da 3 en cada uno de los nueve ficheros, y `./gradlew :app:mergeDebugResources` termina en verde.

- [x] **T2. `NotionSelectionHandle` pasa a `IconButton` de 48×48.** RF-42, RNF-16
  - Fuera el `Box` con `size` + `border` + `clip` + `clickable` y el `Text` de «⋮⋮»/«⋯».
  - Dentro, un `IconButton` con `Icons.Filled.DragHandle`, `Modifier.rotate(90f)` si `isVertical`.
  - Sin `Modifier.size` y sin `LocalMinimumInteractiveComponentSize`.
  - `selected` pasa del borde al tinte del icono con `IconButtonDefaults.iconButtonColors`.
  - Nuevo parámetro `contentDescription: String`.
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` termina sin errores y
    `grep -rn "LocalMinimumInteractiveComponent" app/src/main/java/com/example/ui/` no devuelve nada.

- [x] **T3. Constantes a 48 dp, y el carril con ellas.** RF-44
  - `HandleWidth`, `HandleHeight` y `ColumnHandleHeight` a `48.dp`.
  - `TableRowGutter` sigue siendo `HandleWidth`, así que el carril pasa a 48 dp sin tocarlo.
  - Hecho cuando: `grep -n "HandleWidth = \|HandleHeight = \|ColumnHandleHeight = \|TableRowGutter = " app/src/main/java/com/example/ui/EditableTableBlock.kt`
    muestra `48.dp` en las tres primeras y `TableRowGutter = HandleWidth` en la cuarta.

- [x] **T4. Descripción propia en los tres tiradores.** RF-43
  - `RowHandle` → `R.string.table_row_actions`.
  - `HeaderColumnHandle` → `R.string.table_column_actions`.
  - Tirador de arrastre dentro de `FloatingToolbar` → `R.string.table_move_handle`.
  - Hecho cuando: el diff muestra tres llamadas a `contentDescription = ` con tres claves
    distintas, y ninguna es `null` ni vacía.

- [x] **T5. Se conservan pulsación y arrastre.** RF-45
  - Los tres call sites siguen aplicando `onDragModifier` con `detectDragGesturesAfterLongPress`.
  - El `onClick` de fila y de columna sigue abriendo su `DropdownMenu`; el de la barra sigue sin
    abrir nada, porque lo único que hace es arrastrar el bloque.
  - Hecho cuando: `grep -c "detectDragGesturesAfterLongPress" app/src/main/java/com/example/ui/EditableTableBlock.kt`
    devuelve 4 (los tres tiradores más el de la barra) y `grep -c "DropdownMenu("` sigue devolviendo 2.

- [x] **T6. La suite pura sigue verde sin tocarla.** RF-44
  - `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'` con `--rerun`.
  - **No se edita** `TableSelectionGeometryTest.kt`: sus constantes `gutter = 24f` y
    `handleWidth = 24f` son locales y no dependen del carril de producción.
  - Hecho cuando: la suite termina en verde y `git diff --stat app/src/test` está vacío.

- [x] **T7. Build de la app.** — cierre técnico
  - `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug`.
  - Hecho cuando: los dos terminan correctamente y `git diff -- app/src/main/java/com/example/data/model`
    está vacío (el modelo de datos no se toca).

- [x] **T8. Memoria del proyecto y verificación pendiente en dispositivo.** RF-20
  - `MEMORY.md`: estado de la 007, la decisión de 48 dp con su coste (24 dp menos de tabla,
    carril de 48 dp) y los errores a evitar.
  - Registrar como **defectos del vídeo del 2026-10-07 que quedan sin corregir**: la 4.ª columna
    que se sale de la tarjeta sin scroll; la celda que pierde texto por la izquierda; el recorte
    sin puntos suspensivos; los dos menús que se salen o tapan columnas; la «gota» de acento bajo
    la cabecera; y la separación de 4 px de la barra.
  - **✅ Cerrado (2026-10-16)**: verificado en dispositivo por quien Decide, «todo se ve bien». El tirador de 48 dp no tapa el título de
    la cabecera, el carril de 48 dp no deja demasiado espacio muerto, y la barra no crece en alto.
  - **T6 y T7 comprobados el 2026-10-16**: `git diff` de `data/model` **vacío** (el modelo no se toca) y
    `:app:testDebugUnitTest --tests '*TableSelectionGeometry*' --rerun` en **verde**.
  - Hecho cuando: `MEMORY.md` está en 50 líneas o menos y nombra los siete defectos pendientes.
  - **Lo único de esta tarea que NO se cumple**: los siete defectos del vídeo del 2026-10-07 no llegaron a
    `MEMORY.md`, porque la memoria se comprime y ese listado no sobrevivió. No se dan por **arreglados** uno
    por uno, ya que no hay prueba de cada uno: lo que los sustituye es la verificación del 2026-10-16 sobre la
    app entera. Si alguno sigue vivo, saldrá al volver a mirar la tabla.
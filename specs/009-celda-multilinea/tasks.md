# Tasks — Spec 009 · Celda multilínea y geometría por fila

Plan: `specs/009-celda-multilinea/plan.md` · Suelo: `specs/008-cabecera-como-fila/`

4 tareas, en orden de dependencia. Cada «Hecho cuando» es un comando o un resultado observable, y
cada tarea empieza por su prueba en rojo.

---

- [x] **T1. `TableRowHeights` en rojo y en verde.** RF-56, RNF-22
  - Pruebas puras nuevas en `app/src/test/java/com/example/TableRowHeightsTest.kt`, sin Robolectric.
  - Casos: añadir al final sin mover los anteriores, quitar del medio, mover, y que un índice fuera
    de rango no lance ni cambie nada.
  - `ui/table/TableRowHeights.kt` con `resized`, `without` y `moved`, **sin un solo import de
    composición**.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests '*TableRowHeights*' --rerun` está en
    verde y `grep -c '^import androidx.compose' app/src/main/java/com/example/ui/table/TableRowHeights.kt`
    devuelve 0.

- [x] **T2. La geometría con filas de alto distinto.** RF-57
  - Un caso nuevo en `TableSelectionGeometryTest`: el borde superior de una celda de la fila `n` es la
    suma de los altos reales de las filas anteriores, con filas desiguales.
  - Un tirador por fila, cada uno dentro de la suya, con filas desiguales.
  - Hecho cuando: la suite está en verde **sin tocar `TableSelectionGeometry.kt`**. Es la
    comprobación de que la geometría ya soportaba esto y lo que no la alimentaba eran los datos.

- [x] **T3. La tabla mide cada fila y deja ver el texto.** RF-55, RF-56, RF-57, RF-58, RF-59
  - `TableBlockState.rowHeightPx` deja de ser un alto único y pasa a una lista con una entrada por
    fila; `insertRowAbove`, `insertRowBelow`, `deleteRow` y `moveRow` la mantienen, y `reloadFrom` la
    reconstruye.
  - Las medidas de la tabla reciben la lista, no un alto repartido.
  - Fuera `singleLine` de la celda. Se queda el alto mínimo, **sin máximo**.
  - Cada celda toma el alto de su fila como suelo con `heightIn(min = …)`, nunca como alto exacto.
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` termina sin errores,
    `grep -rn "singleLine" app/src/main/java/com/example/ui/EditableTableBlock.kt` no devuelve nada y
    `grep -n "rowHeightPx" app/src/main/java/com/example/ui/EditableTableBlock.kt` no muestra ningún
    alto único para toda la tabla.

- [x] **T4. Cierre técnico y memoria.** RNF-22
  - La suite pura de la tabla entera en verde: `*TableRowHeights*`, `*TableSelectionGeometry*`,
    `*TableData*`, `*PlainTextExport*`.
  - `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug` sin errores.
  - `specs/008-cabecera-como-fila/spec.md`: anotar que su RF-54 queda enmendado por la 009.
  - `MEMORY.md`: la regla de que una tabla no oculta texto, y que una geometría que reparte un solo
    alto a todas las filas miente en cuanto una crece.
  - Hecho cuando: los comandos terminan correctamente y `MEMORY.md` nombra las dos reglas.
  - **Pendiente que no se puede cerrar aquí**: en dispositivo, que una celda larga se lea entera, que
    el tinte de cabecera cubra la fila entera y que el contorno caiga sobre la celda enfocada en una
    fila más alta que las de arriba.
# Tasks — Spec 005 · Selección y foco del bloque de tabla

Plan: `specs/005-tabla-seleccion/plan.md` · Madre: `specs/001-limites-constitucion/`

9 tareas, en orden de dependencia. Ninguna depende de otra spec.

Cada «Hecho cuando» es un comando o un resultado observable.

**T1 a T4 son geometría pura**: no tocan la interfaz y se pueden comprobar enteras antes de que nada
se vea en pantalla. **T5 a T8** tocan la misma región del archivo, así que van seguidas y en ese
orden.

---

## Fase 1 — Geometría pura

- [x] **T1. Conjunto de celdas enfocadas y condición de señalada.** RF-22, RF-23, RF-30, RF-31
  - Archivo nuevo `ui/table/TableSelectionGeometry.kt`: `CellKey`, `SelectionState`,
    `nextFocusSet`, `isSignaled`.
  - Suite nueva `TableSelectionGeometryTest.kt`: los dos órdenes de llegada dan el mismo conjunto y
    ninguno pasa por vacío; quitar la última celda lo deja vacío; `isSignaled` con `active` sola,
    con `hovered` sola, con ambas y con ninguna.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'` termina en
    verde, y `grep -c "^import androidx.compose" app/src/main/java/com/example/ui/table/TableSelectionGeometry.kt`
    devuelve 0.

- [x] **T2. Contorno de selección continuo.** RF-26, RF-27, RF-32, RF-23
  - `OutlineRect`, `TableMetrics`, `selectionOutline` en el mismo archivo.
  - Devuelve **una** región o `null`, nunca una lista. Prioridad: celda enfocada sobre fila
    seleccionada. La celda de cabecera entra por `row == -1` con `headerHeight` en el origen.
  - Pruebas: celda enfocada produce exactamente su región; fila seleccionada produce una sola región
    que abarca las celdas de la fila; con ambas a la vez solo hay una región, la de la celda;
    sin selección devuelve `null`; una celda de una fila que ya no existe devuelve `null` y no un
    rectángulo fuera de la tabla.
  - Hecho cuando: la suite sigue en verde y `grep -n "List<OutlineRect>\|Set<OutlineRect>"` en el
    archivo de geometría no devuelve ninguna coincidencia.

- [x] **T3. Posición de los manejadores.** RF-28, RF-29
  - `HEADER_ROW`, `rowHandleCenters`, `columnHandleCenter`.
  - `rowHandleCenters` devuelve un centro por fila contando la cabecera, y con la cabecera
    desactivada no deja hueco arriba.
  - Pruebas: con cabecera desactivada solo devuelve los centros de las de datos; con alturas
    desiguales cada centro cae dentro de su fila; añadir o quitar filas devuelve un centro por fila;
    el centro de columna queda dentro del ancho de su columna.
  - Hecho cuando: la suite sigue en verde y existe un caso que falle si `rowHandleCenters` deja el
    hueco de la cabecera cuando `showHeader` es falso.

- [x] **T4. Colocación de la barra superpuesta.** RF-24
  - `toolbarTopOffset`, con la garantía de que `offset + toolbarHeight <= 0`.
  - Pruebas: con hueco suficiente la barra queda por encima de la tabla; con hueco insuficiente
    también, superponiéndose al bloque de arriba y nunca a la primera celda.
  - Hecho cuando: la suite sigue en verde y los dos casos devuelven `offset + toolbarHeight <= 0f`.

---

## Fase 2 — Estado

- [x] **T5. Cablear el conjunto de foco y quitar el detector de foco.** RF-21, RF-22, RF-23, RF-30, RF-31
  - `TableBlockState`: `focusedCells: Set<CellKey>`, `hovered`, `active`, `signaled`, y `lastFocused`
    solo para las acciones contextuales de la hoja.
  - `selectCell` y `selectHeaderCell` llaman a `nextFocusSet`. El puntero sobre la tabla cambia
    `hovered`.
  - **Se elimina `Modifier.onFocusChanged` del contenedor de la tabla.** Es el defecto 1. Las
    celdas lo conservan: cada celda es un destino de foco legítimo y su aviso sí es fiable.
  - `activeRow` y `activeCol` se quedan como `Int` con reserva a 0, para que la hoja de la tabla
    nunca ofrezca una acción sin destino.
  - Hecho cuando: `grep -rn "setTableFocus" app/src/main/java/com/example/ui/EditableTableBlock.kt`
    no devuelve ninguna coincidencia, y `./gradlew :app:assembleDebug` termina correctamente.

---

## Fase 3 — Dibujado

- [x] **T6. Capa superpuesta del contorno y retirada de los bordes por celda.** RF-25, RF-26, RF-27
  - Archivo nuevo `ui/table/TableSelectionOverlay.kt`, con el contorno dibujado encima y sin
    participar en la medición. Los manejadores los coloca T8, en esa misma capa, para que las dos
    cosas que se superponen se hagan de una vez.
  - `TableCell` y `TableHeaderRow` pierden el `border` de acento, el fondo de resaltado y el
    `zIndex`; conservan solo la línea de 0.5 dp de la rejilla.
  - Se borra `inActiveCol`, que era la causa de que las cuatro celdas de la columna se pintaran.
  - Hecho cuando: `grep -n "inActiveCol\|zIndex" app/src/main/java/com/example/ui/EditableTableBlock.kt`
    no devuelve ninguna coincidencia, y `./gradlew :app:assembleDebug` termina correctamente.

- [x] **T7. Barra superpuesta y retirada de la franja y del gutter.** RF-24, RF-25
  - La barra se dibuja en una capa superpuesta con `toolbarTopOffset`, sin `AnimatedVisibility` y sin
    franja reservada.
  - Se borra `padding(start = TableGutterWidth)` de la columna desplazable.
  - Hecho cuando: `grep -n "TableGutterWidth" app/src/main/java/com/example/ui/EditableTableBlock.kt`
    no devuelve ninguna aparición dentro de un modificador de relleno o de tamaño, y
    `./gradlew :app:assembleDebug` termina correctamente.

- [x] **T8. Manejador de la fila de cabecera.** RF-29, RF-28
  - Un manejador para la cabecera con las mismas acciones que el de una fila de datos, colocado con
    `rowHandleCenters` en la capa superpuesta.
  - Se borra el `Column` de dentro de la celda de cabecera que alojaba el manejador de columna, que
    era lo que hacía crecer la fila.
  - Hecho cuando: con la tabla señalada hay un manejador por fila contando la cabecera, el manejador
    de columna sale de la capa superpuesta, y la altura de la cabecera no depende de si los
    manejadores se están mostrando.

---

## Fase 4 — Cierre

- [x] **T9. Comprobaciones de cierre y memoria.** RF-20
  - `./gradlew :app:compileDebugUnitTestKotlin` sin errores.
  - `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'` en verde.
  - `git diff -- app/src/main/java/com/example/data/model` vacío: el modelo de datos no cambia.
  - `git diff --numstat app/src/test` no elimina ninguna anotación de caso de prueba.
  - `MEMORY.md` en 50 líneas como máximo, con el estado, la decisión de superponer la barra y el
    error del detector de foco.
  - Hecho cuando: los cinco puntos se cumplen.

---

## Comprobaciones de cierre de la spec

Ejecutadas el 2026-10-04, todas en verde.

- [x] `./gradlew :app:compileDebugUnitTestKotlin` termina sin errores (0 errores)
- [x] `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'` en verde (**34 casos**)
- [x] `grep -rn "setTableFocus" …EditableTableBlock.kt` → vacío
- [x] La geometría no se calcula dentro de una función que dibuja: `TableSelectionGeometry.kt` tiene
      **0** imports de `androidx.compose` y `TableSelectionOverlay.kt` solo llama a lo ya comprobado
- [x] `grep -rn "TableGutterWidth" …` → vacío: el gutter no reserva ancho
- [x] `grep -n "List<OutlineRect>\|Set<OutlineRect>"` → vacío: la función sigue devolviendo una región
- [x] `grep -n "inActiveCol"` → vacío
- [x] `git diff -- app/src/main/java/com/example/data` → vacío: el modelo de datos no cambia
- [x] `git diff --numstat app/src/test` no elimina casos (el archivo es nuevo; hay 34 casos)
- [x] `./gradlew :app:assembleDebug` correcto

### Mutaciones hechas para comprobar que las pruebas muerden

Una prueba que no se ha visto fallar no comprueba nada. Cada mutación de la tabla se revirtió después.

| Mutación | Casos que saltaron |
|---|---|
| Invertir la prioridad celda/fila en `selectionOutline` | `focused cell wins…`, `a stale focused cell…` |
| `rowTop` ignorando `showHeader` | `the first row starts at zero…`, `a hidden header leaves no gap…` |
| `toolbarTopOffset` apoyando la barra en la tabla | 3 casos de RF-24 (**función retirada después**) |
| `rowHandleCenters` sin contar la cabecera | 4 casos de manejadores |

Cada mutación se revirtió y se volvió a dejar la suite en verde.

## Pendiente de otra entrega

- La suite completa del proyecto sigue bloqueada por la plataforma (spec 002, T4 y T5). Esta entrega
  **no** la exige y **no** la desbloquea: sus pruebas son puras y se ejecutan.
- **Sin verificación visual**: nada de esta entrega se ha visto en un dispositivo. En concreto, el
  margen de 60 dp por tabla puede resultar excesivo, y el caso límite 10 (tabla como primer bloque)
  no se ha podido mirar.
- La previsualización y el texto hablado siguen produciendo una tabla vacía: es el defecto de
  `toRenderingBlock`, en su propia entrega.
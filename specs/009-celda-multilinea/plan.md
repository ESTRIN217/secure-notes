# Plan — Spec 009 · Celda multilínea y geometría por fila

Spec: `specs/009-celda-multilinea/spec.md` · Suelo: `specs/008-cabecera-como-fila/` · Madre:
`specs/001-limites-constitucion/`

Cubre RF-55 a RF-59 y RNF-22, más los heredados RF-3, RF-19, RF-20, RNF-4, RNF-5 y RNF-6.

## 1. Archivos y responsabilidades

| Archivo | Cambio |
|---|---|
| `ui/table/TableRowHeights.kt` | **Nuevo.** Funciones puras que mantienen la lista de altos al añadir, quitar y mover filas. Sin imports de composición (RNF-22). |
| `test/…/TableRowHeightsTest.kt` | **Nuevo.** Pruebas puras de las tres operaciones y de la medición. |
| `ui/EditableTableBlock.kt` | `rowHeightPx` pasa a lista; `TableCell` multilínea y con el alto de su fila como suelo; `TableRow` recibe su alto. |
| `ui/table/TableSelectionGeometry.kt` | **No cambia.** Ya suma los altos reales; hay prueba que lo fija. |
| `test/…/TableSelectionGeometryTest.kt` | Caso nuevo: contorno en una fila más alta que las de arriba. |
| `specs/008-cabecera-como-fila/spec.md` | Anotado que su RF-54 queda enmendado por la 009. |
| `MEMORY.md` | La regla permanente. |

## 2. Funciones puras (RNF-22)

### 2.1 Mantener la lista de altos (RF-56)

Hoy el estado tiene `var rowHeightPx: MutableState<Float>` y `metrics` la reparte a todas las filas:
`List(state.rows.size) { state.rowHeightPx }`. Con multilínea eso es una mentira. Se sustituye por
una lista con una entrada por fila, y estas funciones la mantienen al día:

```
fun resized(previous: List<Float>, count: Int, initial: Float): List<Float> =
    List(count) { index -> previous.getOrElse(index) { initial } }

/** Quita la entrada de una fila. Si el índice no existe, no cambia nada. */
fun without(previous: List<Float>, index: Int): List<Float> =
    if (index !in previous.indices) previous
    else previous.toMutableList().apply { removeAt(index) }

/** Mueve una entrada de sitio, como la fila que representa. */
fun moved(previous: List<Float>, from: Int, to: Int): List<Float> =
    if (from !in previous.indices || to !in previous.indices || from == to) previous
    else previous.toMutableList().apply { add(to, removeAt(from)) }
```

**Por qué `resized` conserva los anteriores** y no rellena de cero: al añadir una fila al final, los
altos de las que ya estaban no deben cambiar, o el contorno saltaría al insertar. Y al quitar una fila
del medio, las posteriores se desplazan de posición pero conservan su alto: es la fila que se ha
ido, no su medida, lo que cambia.

### 2.2 Pseudocódigo del alto de una celda (RF-58, RF-59)

```
TableRow(..., altoDeLaFila: Float)
    for each celda:
        TableCell(modifier = Modifier.width(ancho).heightIn(min = altoDeLaFila))
```

`heightIn(min = …)` y **no** `height(…)`: con un alto exacto, una celda cuyo texto crece después de
la medida quedaría recortada. Es la diferencia entre RF-58 y RF-59.

El suelo se realimenta desde la fila ya medida (`onGloballyPositioned`), así que antes de medir
vale el mínimo de Material. El coste es **una recomposición por fila** tras la medición: es lo que
compensa no depender de la altura intrínseca del campo de texto.

## 3. Orden de los cambios

1. **`TableRowHeightsTest` en rojo** y luego `TableRowHeights.kt` (RF-56, RNF-22).
2. **Geometría**: caso de prueba nuevo para una fila alta (RF-57). El fichero no cambia.
3. **`EditableTableBlock.kt`**: lista de altos en el estado (RF-56), `metrics` con ella (RF-57),
   multilínea (RF-55) y suelo por celda (RF-58, RF-59).
4. **Suite pura y build** (RNF-22).

El estado va antes que la celda porque el suelo de la celda sale de la misma lista.

## 4. Estrategia de tests

| Prueba | Qué fija |
|---|---|
| `TableRowHeightsTest` — añadir al final | los altos anteriores no se mueven (RF-56) |
| `TableRowHeightsTest` — quitar del medio | se pierde su entrada y las demás conservan su valor (RF-56) |
| `TableRowHeightsTest` — mover | el alto viaja con la fila (RF-56) |
| `TableRowHeightsTest` — índices fuera de rango | no lanza ni cambia la lista (caso 4) |
| `TableSelectionGeometryTest` — contorno en una fila alta | el borde superior es la suma de los altos reales (RF-57) |
| `TableSelectionGeometryTest` — tiradores con filas desiguales | un centro por fila, cada uno en la suya (RF-57) |

**Lo que las pruebas NO pueden comprobar**: que las celdas alcancen de verdad esos altos, que una
fila envuelva y que el contorno caiga justo. Eso es comportamiento de pantalla y aquí no hay
dispositivo. Queda como verificación pendiente, no como comprobado.

## 5. Decisiones y alternativas descartadas

**`height(IntrinsicSize.Min)` en la fila con `fillMaxHeight()` en las celdas.** Es la respuesta
idiomática de composición y la más barata, pero depende de que el campo de texto reporte altura
intrínseca: `BasicTextField` construye sobre un layout compuesto, y si no la reporta la fila se
colapsa a cero. Sin pruebas de interfaz en la plataforma no hay forma de descartarlo, y el modo de
fallo es Catastrófico. Descartado por la decisión 2 de la spec.

**Alto exacto por celda en vez de suelo.** Descartado: obliga a recortar cuando el texto crece después
de medir, que es el defecto que viene a arreglarse (RF-59).

**Arreglarlo en `TableSelectionOverlay`.** Descartado por la decisión 3 de la spec: la geometría ya
suma altos reales por fila y `selectionOutline` no distingue filas. Lo que mentía era el que
construía las medidas, que repartía un único alto a todas las filas.

**Multilínea solo en la fila de cabecera, como antes de la 008.** Descartado: es exactamente la
asimetría que la 008 vino a cerrar, y dejaría dos clases de celda.
# Plan — Spec 005 · Selección y foco del bloque de tabla

Spec: `specs/005-tabla-seleccion/spec.md` · Madre: `specs/001-limites-constitucion/`

Cubre RF-21 a RF-32 y RNF-7 a RNF-9, más los heredados RF-3, RF-19, RF-20, RNF-4, RNF-5 y RNF-6.

---

## 1. Archivos y responsabilidades

| Archivo | Responsabilidad | Estado |
|---|---|---|
| `app/src/main/java/com/example/ui/table/TableSelectionGeometry.kt` | **Nuevo.** Funciones puras: conjunto de foco, contorno de selección, posiciones de manejadores y colocación de la barra. Sin Compose. | Nuevo |
| `app/src/test/java/com/example/TableSelectionGeometryTest.kt` | **Nuevo.** Ejercita el archivo anterior sin levantar interfaz. | Nuevo |
| `app/src/main/java/com/example/ui/EditableTableBlock.kt` | Estado de la tabla y dibujado. Deja de decidir geometría; pasa a **leerla** del archivo anterior. | Modifica |
| `app/src/main/java/com/example/ui/table/TableSelectionOverlay.kt` | **Nuevo.** Capa que dibuja contorno y manejadores superpuestos, sin ocupar sitio en la maqueta. | Nuevo |

### Por qué un archivo aparte

RNF-7 pide que la geometría sea comprobable sin pantalla. Eso solo es cierto si la geometría **no
convive con el dibujado** en el mismo archivo. La comprobación es mecánica: el archivo de geometría no
puede importar nada de composición, y su prueba se ejecuta sin el entorno de pruebas. Si mañana
alguien mete un `Modifier` ahí dentro, la prueba sigue corriendo y la garantía se rompe en silencio,
así que el criterio de cierre incluye el `grep` del imports.

Alternativa descartada: dejarlo todo en `EditableTableBlock.kt`, que ya tiene 1300 líneas. Se
descartó porque el criterio de cierre «la geometría no se calcula dentro de una función que dibuja»
sería entonces imposible de comprobar con un comando, y RNF-4 no admite comprobaciones que solo
puedan hacerse mirando.

---

## 2. Funciones puras

Todas reciben la situación explícita como parámetro. Ninguna lee estado, ninguna dibuja.

### 2.1 Tipos

```kotlin
/** Celda del bloque de tabla. `row` -1 identifica la fila de cabecera. */
data class CellKey(val row: Int, val col: Int)

/** Región rectangular en píxeles, con el origen en la esquina superior izquierda de la tabla. */
data class OutlineRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Medidas de la tabla en píxeles. `columnWidths` no incluye el gutter de los manejadores. */
data class TableMetrics(
    val columnWidths: List<Float>,
    val rowHeights: List<Float>,
    val headerHeight: Float,
    val showHeader: Boolean,
)

/** Situación que decide la geometría. */
data class SelectionState(
    val focused: CellKey?,
    val selectedRow: Int?,
    val hovered: Boolean,
    val active: Boolean,
)
```

`SelectionState` lleva `hovered` y `active` porque la condición de señalada es
`active || hovered`, y esa condición aparece en tres sitios distintos (barra, manejadores de fila,
limpieza al salir). Concentrarla aquí evita que los tres se desincronicen.

### 2.2 Conjunto de foco — RF-22

```kotlin
fun nextFocusSet(current: Set<CellKey>, cell: CellKey, focused: Boolean): Set<CellKey> =
    if (focused) current + cell else current - cell

fun isSignaled(state: SelectionState): Boolean = state.active || state.hovered
```

Por qué un conjunto y no una celda única: es lo que hace el resultado **independiente del orden**.
Con una celda única, si el aviso de que A pierde el foco llega después del de que B lo gana, A borra
a B y la tabla se marca inactiva un fotograma. Con un conjunto, `current + B` y luego `current - A`
terminan en `{B}` en cualquiera de los dos órdenes, y nunca pasan por vacío al moverse dentro de la
tabla. **RF-22 es exactamente esta propiedad**, así que el caso de prueba la ejercita en los dos
órdenes.

### 2.3 Contorno de selección — RF-26, RF-27, RF-32

```kotlin
fun selectionOutline(metrics: TableMetrics, state: SelectionState): OutlineRect? {
    val focused = state.focused
    if (focused != null && focused.row >= 0 && focused.row < metrics.rowHeights.size)
        return cellRect(metrics, focused)          // RF-26
    if (focused != null && focused.row == HEADER_ROW && metrics.showHeader)
        return headerCellRect(metrics, focused.col) // RF-28
    val row = state.selectedRow
    if (row != null && row in metrics.rowHeights.indices)
        return rowRect(metrics, row)                // RF-27
    return null
}
```

Devuelve **una sola región o ninguna**, nunca una lista. Es lo que hace exigible «continuo»: si
devolviera una región por celda, el dibujado no tendría forma de saber si tiene que unirlas o
dibujarlas sueltas. La prioridad es celda enfocada sobre fila seleccionada, que es **RF-32**:
devolver ambas sería ambiguo.

La altura de la fila sale de la suma de `rowHeights[0..row]` más `headerHeight`, de modo que una
fila de altura variable con tipografía grande sigue encerrada exacta (**caso límite 4**).

`headerHeight` entra en el cálculo aunque la región sea una celda de cabecera, porque el origen de
coordenadas es el borde superior de la tabla, no el de la primera fila de datos.

### 2.4 Posición de los manejadores — RF-28, RF-29, casos 1, 6, 7

```kotlin
const val HEADER_ROW = -1

fun rowHandleCenters(metrics: TableMetrics): List<Float> {
    val result = mutableListOf<Float>()
    if (metrics.showHeader) result += metrics.headerHeight / 2f          // RF-29
    var y = if (metrics.showHeader) metrics.headerHeight else 0f
    metrics.rowHeights.forEach { h ->
        y += h / 2f
        result += y
        y += h / 2f
    }
    return result
}

fun columnHandleLeft(metrics: TableMetrics, col: Int, handleWidth: Float): Float? {
    val width = metrics.columnWidths.getOrNull(col) ?: return null
    return metrics.columnLeft(col) + width / 2f - handleWidth / 2f
}
```

`rowHandleCenters` devuelve un punto por fila **contando la cabecera**, en el mismo orden en que se
recorren las filas, para que el manejador `i` sea siempre el de la fila `i`. Con la cabecera
desactivada devuelve solo los de datos y no deja hueco arriba (**caso límite 1**). El total se deriva
de las medidas y no de un contador guardado, así que al añadir, quitar o reordenar filas los
manejadores se recolocan solos y no hay ninguna posición que pueda quedarse desfasada (**casos 6 y
7**). El centro de cada fila es `rowTop(row) + alto / 2`, que sigue valiendo con filas de distinta
altura por tipografía grande (**caso 4**).

`columnHandleLeft` devuelve el **borde izquierdo**, no el centro, porque lo que se dibuja es el
borde: quien lo coloca no tiene que volver a recentrar. Null si la columna ya no existe, en vez de
recortar al último índice, que pondría un manejador encima de una columna que no es. Va en una capa
superpuesta, así que no empuja la cabecera (**RF-28**).

### 2.5 Colocación de la barra — RF-24

```kotlin
fun toolbarTopOffset(toolbarHeight: Float, spaceAbove: Float): Float {
    if (toolbarHeight <= spaceAbove) return -spaceAbove
    return -(toolbarHeight + TOOLBAR_TABLE_GAP)
}
```

Devuelve el borde superior de la barra **relativo al borde superior de la tabla**, con la tabla en el
cero, y las dos ramas cumplen por construcción `desplazamiento + altura <= 0`: con hueco suficiente la
barra se apoya al fondo del hueco, y si el hueco no da sube por encima de él.

El borrador de esta función estaba mal escrito: tomaba `min(wanted, lowest)` y eso colocaba la barra
**al fondo del hueco grande**, que es lo contrario de lo debido. El mínimo de dos candidatos no
garantiza nada útil, y con hueco amplio ganaba el candidato que más se aleja de la tabla. La versión
correcta tiene dos ramas y cada una cumple la garantía por su cuenta.

La preferencia entre las dos ramas es deliberada y es lo que evita el caso límite 8: cuando el hueco no
da, la barra se superpone al bloque de arriba en lugar de bajar a tapar la primera fila, porque el
bloque de arriba no tiene nada seleccionado y la primera fila sí.

`spaceAbove` lo mide el dibujado, no esta función: es un dato de la maqueta del editor, no una regla.

---

## 3. Interfaz

### 3.1 Dónde vive cada cosa

```
EditableTableBlock
└── Column                                  ← sin onFocusChanged: se elimina (defecto 1)
    ├── Box                                  ← capa superpuesta de la barra
    │   └── FloatingToolbar                  ← alineada arriba a la derecha, con offset de §2.5
    └── Card
        └── Box                              ← una sola capa para contorno y manejadores
            ├── Column (scroll horizontal)   ← sin padding de gutter
            │   ├── TableHeaderRow           ← celdas, sin manejador dentro
            │   └── TableRow × n             ← celdas, sin manejador dentro
            └── TableSelectionOverlay        ← contorno + manejadores, todo superpuesto
```

`TableSelectionOverlay` es un `Box` que no tiene contenido propio: dibuja con `drawBehind` y coloca
los manejadores con `offset`. Al no participar de la medición, **no puede alterar la maqueta**, que es
**RF-25** por construcción y no por medición previa y posterior.

### 3.2 Qué se borra de las celdas

Cada celdaToday lleva `border(1.5.dp, primary)` cuando está enfocada o en su columna, más
`zIndex`. Eso es lo que produce la escalera. Se retira **entero**: la celda conserva solo la línea de
0.5 dp en `outlineVariant` que dibuja la rejilla. El estado activo ya no cambia el aspecto de la
celda, y quien lo dibuja es la capa superpuesta.

Con ello desaparece también el `inActiveCol`, que era la causa directa de que las 4 celdas de la
columna se pintaran.

### 3.3 Qué se borra del orquestador

- `Modifier.onFocusChanged` sobre el `Column` de la tabla.
- `AnimatedVisibility` con franja reservada.
- `padding(start = TableGutterWidth)` sobre la columna desplazable.
- El `Column` de dentro de la celda de cabecera que alojaba el manejador de columna.
- La medición de `headerHeightPx` **como reserva de espacio**: pasa a ser solo entrada de la geometría.

### 3.4 Estado

`TableBlockState` pasa de `focusedCell: FocusedCell?` a:

```kotlin
var focusedCells by mutableStateOf(emptySet<CellKey>())
var hovered by mutableStateOf(false)
var selectedRow by mutableStateOf<Int?>(null)
private var lastFocused by mutableStateOf<CellKey?>(null)   // solo alimenta el menú contextual

val active: Boolean get() = focusedCells.isNotEmpty()
val signaled: Boolean get() = isSignaled(SelectionState(hovered = hovered, active = active))
```

`lastFocused` no se limpia nunca, porque es memoria de «la última celda tocada» para las acciones
contextuales de la hoja: con los `+` fuera, esa hoja es la única vía para añadir o quitar filas y
columnas, y sus acciones no pueden quedarse sin destino. `focusedCells` es el que responde a las
condiciones de presencia.

`activeRow` y `activeCol` pasan a ser `Int` con reserva a 0, como ya están.

---

## 4. Estrategia de tests

### Por qué pruebas puras y no de imagen

Roborazzi daría capturas de referencia del bloque entero, pero no puede distinguir «contorno continuo»
de «escalera» sin que la diferencia sea visible en el golden, y un golden que hay que regenerar a
mano no es una comprobación. Además, la suite con entorno de pruebas **no se ejecuta en la
plataforma de desarrollo** (bloqueo documentado en la spec 002: sin runtime nativo para aarch64), así
que una prueba de imagen sería aquí inc comprobable.

Las funciones puras sí se ejecutan: comprobado en esta máquina antes de escribir el plan
(`RichTextParserTest`, 35 casos, 0 fallos). Por eso **RNF-8** se puede exigir de verdad.

### Qué cubre cada prueba

| Prueba | RF | Qué demuestra |
|---|---|---|
| `focus set, dos órdenes` | RF-22 | `nextFocusSet` da el mismo conjunto con los dos órdenes, y no pasa por vacío |
| `focus set, vaciar` | RF-23 | Quitar la última celda deja el conjunto vacío |
| `contorno, celda enfocada` | RF-26 | Una región, exactamente la de esa celda |
| `contorno, fila seleccionada` | RF-27 | Una región que abarca toda la fila, de una vez |
| `contorno, prioridad` | RF-32 | Con celda enfocada y fila seleccionada, una sola región: la de la celda |
| `contorno, cabecera` | RF-28 | La celda de cabecera devuelve la región del título, con `headerHeight` en el origen |
| `contorno, nada seleccionado` | RF-23 | `null` |
| `contorno, foco fuera de rango` | casos 3, 6 | Una celda de una fila que ya no existe devuelve `null` en vez de un rectángulo fuera de la tabla |
| `manejadores, con cabecera` | RF-29 | Un centro por fila contando la cabecera |
| `manejadores, sin cabecera` | caso 1 | Solo los de datos, sin hueco arriba |
| `manejadores, alturas desiguales` | casos 4, 6, 7 | Cada centro cae dentro de su fila con filas de distinta altura, y con filas añadidas o quitadas |
| `barra, hueco suficiente` | RF-24 | `offset + altura <= 0` |
| `barra, hueco insuficiente` | casos 8, 10 | `offset + altura <= 0` también, superponiéndose al bloque de arriba |
| `señalada, foco o puntero` | RF-30, RF-31 | `active` sola, `hovered` sola, ambas, y ninguna |

### Comandos

```bash
./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'
./gradlew :app:compileDebugUnitTestKotlin
./gradlew :app:assembleDebug
```

La suite completa **no** es criterio de cierre: sigue bloqueada por la plataforma en la forma que
documenta la spec 002 en T4 y T5. Exigirla sería exigir algo que nadie puede comprobar aquí, y
**RF-16** de la madre ya prevé ese caso.

### Cobertura de código nuevo

Todo el archivo de geometría queda cubierto por pruebas; lo que queda en `EditableTableBlock` son
llamadas a esas funciones y el dibujado, que es lo que la spec 002 demuestra no poder comprobar en
esta plataforma.

---

## 5. Decisiones y alternativas descartadas

**Conjunto de celdas enfocadas en vez de celda enfocada única.**
Descartado lo primero: es lo que ya había y es la causa de RF-22. La celda única parece suficiente
porque solo hay un foco, pero el estado que la aplicación necesita no es «qué celda», sino «hay
alguna celda con foco», y ese es un dato de conjunto. Además `lastFocused` sigue resolviendo el «qué
celda» para el menú.

**Capa superpuesta para contorno y manejadores en vez de bordes por celda.**
Descartado el borde por celda: produce la escalera y no se puede distinguir de un contorno continuo
sin lógica adicional en el dibujado. Se podría haber resuelto pintando solo el borde superior y el inferior de
cada celda y dejando los laterales, pero eso se rompe con el desplazamiento horizontal, con filas de
altura variable y con la fila de cabecera; la capa superpuesta no depende de ninguno.

**Reservar hueco para los manejadores en vez de superponerlos.**
Descartado por RF-25: reservar hueco cambia el ancho de columna, y el ancho de columna es lo que
cambia la posición de todo lo demás. La referencia superpone el manejador sobre el primer borde.

**`onFocusChanged` en un contenedor con `focusable()` para arreglar el defecto 1.**
Descartado: haría el contenedor un destino de foco, con lo que entraría en el orden de tabulación y
robaría el foco a las celdas. El conjunto de celdas hace lo mismo sin tocar el sistema de foco.

**Dejar el gutter de 32 dp pero sin los `+`.**
Es lo que hay hoy y es el defecto visible: el hueco se reserva aunque los manejadores no se pinten.
La superposición lo elimina.

**Contorno de columna completa como en la referencia.**
Fuera de alcance por decisión explícita. La columna activa se señala con su manejador. Añadirla sería
una interacción nueva, no una corrección.

---

## 6. Orden de las tareas

1. Geometría pura del foco (**RF-22**), porque de ella dependen la presencia de la barra y todo lo demás.
2. Geometría del contorno (**RF-26, RF-27, RF-32**), que es el motivo visual de la entrega.
3. Geometría de los manejadores (**RF-28, RF-29**).
4. Colocación de la barra (**RF-24**).
5. Cablear el estado y quitar el detector de foco (**RF-21, RF-22, RF-23, RF-30, RF-31**).
6. Capa de dibujado superpuesta y retirada de los bordes por celda (**RF-26, RF-27, RF-25**).
7. Barra superpuesta y retirada de la franja y del gutter (**RF-24, RF-25**).
8. Manejador de cabecera (**RF-29**).
9. Cierre: comprobaciones y memoria.

Las cuatro primeras son de geometría pura y no tocan la interfaz: se pueden hacer y comprobar enteras
antes de que nada se vea en pantalla. Las cinco siguientes tocan la misma región del archivo, así que
van seguidas y en ese orden.
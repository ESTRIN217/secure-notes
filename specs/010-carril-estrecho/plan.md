# Plan — Spec 010 · El carril del tirador de fila se estrecha

Spec: `specs/010-carril-estrecho/spec.md` · Suelo: `specs/005-tabla-seleccion/`, `007-`, `008-`,
`009-` · Madre: `specs/001-limites-constitucion/`

Cubre RF-60, RF-61, RF-62 y RNF-23, RNF-24, más los heredados RF-3, RF-19, RF-20, RNF-4, RNF-5 y
RNF-6.

## 1. Archivos y responsabilidades

| Archivo | Cambio |
|---|---|
| `ui/EditableTableBlock.kt` | **Único archivo de producción.** `TableRowGutter` pasa a 24 dp y nace `HandleOverhang`; el tirador de fila se alinea por su borde derecho; la capa de tiradores se ensancha a la izquierda y el tirador de columna compensa el desfase. |
| `ui/table/` | **No se toca** (RNF-24). `gutterWidth` ya es un parámetro de `TableMetrics` y baja solo. |
| `MEMORY.md` | La regla del alineamiento. |

## 2. Geometría del carril

Con el carril estrecho, el tirador de 48 dp no cabe dentro. La cuenta que lo resuelve:

```
ancho de tarjeta
├── 0 … 24 dp ──── el carril, dentro de la tarjeta, sin texto
├── 24 … 36 dp ─── el relleno horizontal de la celda (12 dp), sin texto
└── 36 dp ──────── aquí empieza el texto de la primera columna
     ↑
     el borde derecho del tirador se para aquí
```

El tirador de 48 dp ocupa de **−12 dp a +36 dp**: 24 del carril, 12 del relleno, y **0 del texto**.
De sus 48 dp, solo **12 dp sobresalen por fuera de la tarjeta** (decisión 1 de la spec).

`HandleWidth` no cambia: el objetivo táctil sigue siendo de 48 dp (RF-61). Lo que cambia es de
dónde se coloca.

## 3. Los tres cambios concretos

### 3.1 Constantes (RF-60)

```
private val HandleWidth = 48.dp        // sin cambio: el objetivo táctil
private val HandleHeight = 48.dp
private val ColumnHandleHeight = 48.dp
private val CellHorizontalPadding = 12.dp   // nuevo, mirrors TableCell
private val TableRowGutter = 24.dp     // era HandleWidth
private val HandleOverhang = 48.dp - TableRowGutter   // nuevo: 12.dp
```

El desborde se **deriva** de la resta, no se escribe a mano: si algún día el carril se ensancha, el
desborde se ajusta solo y no puede quedar negativo por descuido.

### 3.2 Posición del tirador de fila (RF-60)

Hoy:

```
offset { IntOffset(0, (centerY - handleHeightPx / 2f).roundToInt()) }
```

Con el carril estrecho, el borde izquierdo pasa a ser `gutter − 48`:

```
offset { IntOffset((gutterWidthPx - handleWidthPx).roundToInt(), y) }
```

El eje vertical no se toca: el tirador sigue centrado en su fila.

### 3.3 Que el desborde reciba el toque (RF-61, decisión 2)

El tirador se dibuja en una capa con `matchParentSize()`, que abarca la tarjeta. Con el desborde, el
tirador quedaría medio **fuera de los límites de su padre**, y un punto del dedo ahí puede no llegar
a él. La capa se **desplaza hacia la izquierda** lo mismo que el desborde:

```
Box(modifier = modifier.matchParentSize().offset(x = (-HandleOverhangPx).toDp()))
```

Con eso los límites de la capa contienen el tirador completo, y el toque llega a los 48 dp. Como la
capa se mueve, el tirador de columna —que se coloca con `columnHandleLeft`— hay que desplazarlo en la
misma medida para que siga centrado en su columna:

```
leftPx = columnHandleLeft(metrics, col, handleWidthPx) + overhangPx
```

## 4. Qué NO cambia

- `minUnitWidth = 100.dp`: decidido expresamente en la decisión 3 de la spec.
- La pista de desplazamiento: decisión 4.
- `ui/table/`: `gutterWidth` es un parámetro, así que el contorno y la región de fila se recolocan
  solos con el nuevo valor (RNF-24).
- El ancho de las columnas no depende de que el tirador esté visible (RF-62): el `padding` y la resta
  del carril son incondicionales, y no se tocan.

## 5. Estrategia de tests

La geometría **no cambia**, así que su suite debe seguir verde **sin editar el fichero**: su
`gutter = 24f` es una constante local del test y no lee nada de producción. Eso es en sí una
comprobación de que el carril no está atado en ningún sitio.

| Prueba | Qué fija |
|---|---|
| `TableSelectionGeometryTest` sin editar, en verde | que el carril es un parámetro y no una constante cableada (RNF-24) |
| Caso nuevo: la primera columna empieza en el ancho del carril | que bajar el carril desplaza el contorno (RF-60) |
| `TableRowHeightsTest` sin tocar | que el alto por fila no depende del carril |

**Lo que no se puede comprobar aquí**: que el desborde de 12 dp reciba el toque, y que el tirador se
vea centrado donde toca. Es comportamiento de puntero y de pantalla, y no hay pruebas de interfaz en
la plataforma. Queda como verificación pendiente con plan alternative declarado en la spec.

## 6. Decisiones y alternativas descartadas

**Centrar el tirador en un carril estrecho.** Descartado por la decisión 1: la mitad derecha caería
sobre el relleno y sobre el texto de la primera celda, que es el defecto que el carril vino a
cerrar.

**Dejar el desborde fuera de los límites de su padre.** Descartado por la decisión 2: el tirador se
vería de 48 dp y solo respondería en la mitad que queda dentro.

**Hacer el carril condicional, reservándolo solo con la tabla señalada.** Descartado: la tabla
saltaría 48 dp a la derecha en cuanto se la tocara, y el RF-25 de la spec 005 prohíbe justamente eso.
Recuperar el ancho sin salto es lo que obliga a que el carril siga reservándose siempre.

**Bajar también el mínimo de columna para que quepan cuatro columnas.** Descartado por decisión de
la persona usuaria: es una decisión aparte sobre cómo se lee cualquier tabla.
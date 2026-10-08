# Plan — Spec 008 · La fila de cabecera es una fila más

Spec: `specs/008-cabecera-como-fila/spec.md` · Suelo: `specs/005-tabla-seleccion/` y
`specs/007-tiradores-tabla/` · Madre: `specs/001-limites-constitucion/`

Cubre RF-46 a RF-54 y RNF-19 a RNF-21, más los heredados RF-3, RF-19, RF-20, RNF-4, RNF-5 y RNF-6.

## 1. Archivos y responsabilidades

| Archivo | Cambio |
|---|---|
| `data/model/DataBlock.kt` | `TableData` pierde `headers`; `rows` es la única fuente. `fromJson` acepta las dos formas y antepone los títulos antiguos; `toHtml` emite cada fila dentro de su `<tr>` y usa `<th>` solo en la primera y con la opción activa; `fromLegacyHtml` idem; `default3x3` pasa a 4 filas. |
| `ui/table/TableSelectionGeometry.kt` | Fuera `HEADER_ROW`, `TableMetrics.headerHeight` y `TableMetrics.showHeader`. `rowTop` empieza en 0 y `cellRect` pierde la rama de cabecera. |
| `ui/EditableTableBlock.kt` | Fuera `TableBlockState.headers`, `headerHeightPx` y `setHeader`. `TableHeaderRow` se borra: la primera fila la pinta `TableRow` con `isHeaderRow`. `TableCell` recibe `isHeaderRow`, un sitio para la barra de ancho y el mismo alto mínimo para todos. Fuera `headerSlot`, `dataRowCount` y los parámetros muertos de `TableRow`. |
| `util/RichTextConverter.kt` | Los tres recorridos de tabla (texto plano, Markdown, HTML) pasan a emitir filas. Markdown marca la primera en negrita cuando la opción está activa. |
| `util/export/PdfBlockRenderer.kt` | `drawTable` recorre filas; la primera usa el pintado de cabecera si la opción está activa. |
| `util/NoteEditorUtils.kt` | `parsePipeTableToHtml` emite los títulos dentro de un `<tr>`. |
| `ui/NoteEditorScreen.kt` | El diálogo de insertar tabla crea la primera fila solo si la opción está marcada, y guarda `showHeader`. |
| `test/…/TableDataTest.kt` | **Nuevo.** Pruebas puras del modelo. |
| `test/…/TableSelectionGeometryTest.kt` | Reescritura: sin fila de cabecera ni alto de cabecera. |
| `test/…/PlainTextExportTest.kt`, `HtmlExportMediaTest.kt`, `PdfExporterTest.kt` | Se actualiza la construcción de `TableData`; las aserciones no cambian. |
| `MEMORY.md` | T9. |

**No se toca** `data/model/NoteContentBlock.kt` ni `ui/NoteContentBlockRenderer.kt`: son el segundo
modelo de tabla, el de la vista de lectura, y quedan fuera de alcance (ver «Fuera de alcance» de la
spec). Sus pruebas (`RichTextParserTest`) siguen verdes porque no construyen `TableData`.

## 2. Funciones puras

Tres funciones puras, todas sin imports de composición, que es lo que permite ejercitarlas aquí
(RNF-20).

### 2.1 `TableData.fromJson` — la lectura de las dos formas (RF-46, RNF-19)

```
fun fromJson(json: String?): TableData?:
    obj = parse(json) o devolver null
    titulosViejos = obj["headers"] como lista de cadenas, o lista vacía
    filasGuardadas = obj["rows"] como lista de listas de cadenas, o lista vacía
    filas = si titulosViejos está vacía
                filasGuardadas
            si no
                [titulosViejos] + filasGuardadas
    pesos = obj["columnWeights"] o lista vacía
    fondo = obj["bgColorHex"] no vacío, o null
    cabecera = obj.optBoolean("showHeader", true)
    columnaCabecera = obj.optBoolean("showHeaderColumn", false)
    devolver TableData(filas, pesos, fondo, cabecera, columnaCabecera)
```

**Por qué anteponer siempre** (decisión 3 de la spec): una lista de títulos no vacía significa que la
tabla **tenía** una fila de cabecera visible. Si anteponerla solo cuando `showHeader` era `true`, una
tabla con la cabecera apagada perdería ese texto en la siguiente escritura, y la constitución no
permite descartar texto del usuario. El caso 2 de la spec —nota antigua sin títulos, `headers` vacío—
no antepone nada, así que la fila fantasma desaparece sola.

### 2.2 `TableData.toHtml` — cada fila dentro de su fila (RF-50, RF-51)

```
fun toHtml(): String:
    esc = escapar & < > "
    partes = ["<table>"]
    si pesos normalizados tienen una entrada por columna: partes += "<colgroup>" + <col> por peso
    filas.forEachIndexed { indice, fila ->
        etiqueta = si (indice == 0 y showHeader) "th" si no "td"
        partes += filaAHtml(fila, etiqueta, esc)
    }
    partes += "</table>"

fun filaAHtml(fila: List<String>, etiqueta: String, esc): String:
    si fila está vacía: devolver "<tr></tr>"
    devolver "<tr><etiqueta>" + fila.joinToString("</etiqueta><etiqueta>") { esc(it) }
              + "</etiqueta></tr>"
```

**Por qué no se rellenan las filas cortas** (caso 5): la versión anterior emitía tantas celdas como
tenía la fila, y rellenar cambia la salida de todas las tablas exportadas. Queda fuera del objetivo.

### 2.3 `TableMetrics` — una sola numeración de filas (RF-53)

```
data class TableMetrics(
    columnWidths: List<Float>,
    rowHeights: List<Float>,     // una por fila, la primera incluida
    gutterWidth: Float = 0f,
)

fun rowTop(fila): Float = suma de rowHeights[0..<fila]
fun rowHandleCenters(metrics): List<Float> =
    rowHeights.mapIndexed { fila, alto -> rowTop(fila) + alto / 2 }
```

`HEADER_ROW` desaparece: no hay índice de fila negativo, así que ni `cellRect` ni `moveFocus` ni el
tirador de columna necesitan el caso especial. `cellRect` se reduce a «columna que no existe → null» y
«fila que no existe → null».

## 3. El orquestador (`EditableTableBlock.kt`)

### 3.1 El estado (RF-46, RF-52, RF-49)

| Se va | En su lugar |
|---|---|
| `var headers` | nada: la cabecera es `rows[0]` |
| `var headerHeightPx` | nada: la primera fila se mide como las demás |
| `fun setHeader(col, valor)` | `setCellValue(0, col, valor)`, que ya crece la fila si hace falta |
| `columnCount = headers.size.coerceAtLeast(anchura mayor)` | `columnCount = rows.maxOfOrNull { it.size } ?: 0` |
| `focusedRow` con `takeIf { it.row >= 0 }` | `focusedRow = lastFocused?.row` |
| `moveFocus` con `cur.row < 0` | `newRow = (cur.row + dRow).coerceIn(0, rows.lastIndex)` |
| `moveColumn` con `if (headers.isEmpty()) return` | sin guardia: mueve celdas de todas las filas |

`fitToContent` mide la primera fila cuando la opción de cabecera está activa, y todas si no.

### 3.2 Una fila, un composable (RF-47, RF-54)

```
TableRow(fila, indiceFila, …, esFilaCabecera = showHeader y indiceFila == 0)
    fondo de fila = cebra y fila activa, como hoy
    por cada columna:
        TableCell(
            celda = fila[columna] o "",
            esCabecera = esFilaCabecera o (showHeaderColumn y columna == 0),
            barraAncho = si indiceFila == 0 y columna < ultima, la barra de arrastre, si no null
        )
```

`TableCell` gana un parámetro `esCabecera` que sustituye a `isHeaderCol`, y un hueco opcional para la
barra de ancho, que hasta ahora vivía dentro de la celda de cabecera. La barra se dibuja en la
primera fila **siempre**, esté la opción activada o no, porque es un tirador de columna y la primera
fila es la que hace de cabecera de columnas. Con eso, redimensionar una columna y abrir su menú
siguen funcionando con la opción de fila de cabecera apagada, que antes no lo hacían.

`TableCell` deja de distinguirse en nada más: mismo `heightIn(min = 44.dp)`, mismo `singleLine`, mismo
relleno, mismo reconocimiento de enlaces. Solo cambian la negrita y el tinte (RF-54).

### 3.3 Los tiradores (RF-49, RF-53, enmienda a RF-28/RF-29)

```
centros = rowHandleCenters(metrics)              // uno por fila, sin hueco de cabecera
filaActiva = selectedRow ?: focused?.row
para cada (indiceFila, centroY) con indiceFila == filaActiva:
    RowHandle(indiceFila, …, canDeleteRow = filas.size > 1,
              rowHeightPx = metrics.rowHeights[indiceFila],
              rowCount = metrics.rowHeights.size)

tirador de columna: si focused?.row == 0 y la columna existe
```

Consecuencias: la primera fila ya no tiene índice negativo, así que su tirador **sí** ofrece eliminar
(RF-49), y arrastrarlo **sí** mueve la fila. El tirador de columna aparece al enfocar una celda de la
primera fila, con la opción activada o no (enmienda a RF-28).

## 4. Los exportadores (RF-51)

Los cuatro pasan a recorrer **filas**, y cada uno decide por su cuenta cómo se ve la primera:

| Exportador | Con la opción activa | Con la opción desactivada |
|---|---|---|
| HTML (`DataBlock.toHtml`) | primera fila con `<th>` | primera fila con `<td>` |
| Markdown (`RichTextConverter`) | primera fila en negrita, luego su línea de `---` | primera fila normal, luego su línea de `---` |
| Texto plano (`RichTextConverter`) | indistinguible: el formato no tiene negrita | indistinguible |
| PDF (`PdfBlockRenderer`) | primera fila con el pintado de cabecera | primera fila normal |

**Por qué el texto plano no cambia** (caso 8 de la spec): no hay forma de expresar «cabecera» en texto
plano. Antes esto era un bug —el texto plano emitía la lista de títulos aunque la opción estuviera
apagada—; con la cabecera convertida en fila pasa a ser correcto por construcción, porque la fila se
emite **porque es una fila**, no porque haya una lista de títulos.

**Por qué la línea de `---` va siempre** tras la primera fila: el formato de tabla de Markdown la
exige, y sin ella no hay tabla. Es estructura, no estilo.

## 5. Orden de los cambios y cómo se comprueba cada uno

El orden es el de dependencia: primero el modelo, porque el editor y los exportadores compilan contra
él.

1. **`TableDataTest` en rojo** (`TableDataTest.kt` nuevo), luego `TableData` (RF-46, RF-50, RF-51,
   RNF-19). Se comprueba con `./gradlew :app:testDebugUnitTest --tests '*TableData*' --rerun`.
2. **`TableSelectionGeometryTest` en rojo** (reescrito), luego `TableSelectionGeometry.kt` (RF-53).
   Mismo comando.
3. **`EditableTableBlock.kt`** (RF-47, RF-49, RF-52, RF-54). `./gradlew :app:compileDebugKotlin`.
4. **Exportadores** (RF-51) y `NoteEditorUtils`, `NoteEditorScreen`. Pruebas de texto plano y HTML.
5. **Las tres pruebas que construyen `TableData`** se actualizan; sus aserciones no cambian, lo que
   es la comprobación de que la exportación no se ha movido.
6. **Suite pura completa y build** (RNF-20). `./gradlew :app:testDebugUnitTest --tests
   '*TableData*' --tests '*TableSelectionGeometry*' --tests '*PlainTextExport*' --tests
   '*HtmlExportMedia*' --tests '*RichTextParser*'` y `./gradlew :app:assembleDebug`.

## 6. Estrategia de tests

Todo lo comprobable es **puro y JUnit sin entorno de pruebas**, porque en esta plataforma los tests
que necesitan Robolectric no corren (ver «Bloqueo de plataforma» en `MEMORY.md`). Por eso la lógica
que se decide va en `TableData` y en `TableSelectionGeometry.kt`, no en los composables.

| Prueba | Qué fija (rojo antes del código) |
|---|---|
| `TableDataTest` — lectura antigua | una tabla con `headers` y `rows` se lee con los títulos como primera fila y las mismas filas de datos detrás (RNF-19) |
| `TableDataTest` — lectura nueva | una tabla ya escrita con la primera fila dentro de `rows` se lee igual, sin duplicar la fila (RF-46) |
| `TableDataTest` — ida y vuelta | escribir, serializar y volver a leer devuelve la misma tabla (RF-46) |
| `TableDataTest` — cabecera en `th` | con la opción activa la salida contiene `<tr><th>` y termina en `</th></tr>` (RF-50) |
| `TableDataTest` — sin `<th>` suelto | no hay ninguna secuencia `<th>` fuera de un `<tr>` (RF-50) |
| `TableDataTest` — opción apagada | la primera fila sale con `<td>` y sigue saliendo (RF-48, RF-51) |
| `TableDataTest` — filas desiguales | el número de columnas sale de la fila más ancha (RF-52) |
| `TableSelectionGeometryTest` — sin caso de cabecera | las coordenadas empiezan en 0 y la celda de la primera fila se resuelve con `OutlineRect` como las demás (RF-53) |
| `TableSelectionGeometryTest` — un tirador por fila | el número de centros coincide con el de filas, sin hueco (RF-53) |
| `PlainTextExportTest` — sin cambio | la aserción existente sigue verde sin editarla: es la prueba de que el texto plano no se ha movido (RF-51) |
| `HtmlExportMediaTest` — sin cambio | la aserción de escapado sigue verde sin editarla (RF-50) |

`PdfExporterTest` es Robolectric y **no se puede ejecutar aquí**: se actualiza su construcción de
`TableData` para que compile, y se registra como verificación pendiente, no como comprobada.

## 7. Decisiones y alternativas descartadas

**Guardar en qué fila está la cabecera, en vez de que sea siempre la primera.** Descartado: obliga a
la geometría a seguir distinguiendo una fila del resto —el caso especial que se viene a cerrar—, y a
migrar un índice en notas ya escritas. Con la fila 0, desactivar la cabecera es quitar un pintado, que
es exactamente lo que ya hace la columna.

**Esconder la primera fila cuando la opción está apagada.** Descartado: es lo que hace hoy el
editor, y rompe la paridad con la columna, que al desactivarse no desaparece. Además obligaría a que
las cuatro exportaciones tuvieran la excepción «si no hay cabecera, no emitas la fila 0», que es
justo la duplicación que se elimina.

**Conservar `headers` como campo y solo arreglar los síntomas.** Descartado por ser la causa, no un
síntoma: los cuatro fallos de la lista 2 de la spec —escritura que se pierde, columnas que no se
pueden reordenar, tirador que no mueve nada, cabecera fuera de la tabla en el HTML— son consecuencia
directa de la lista paralela. Arreglarlos encima dejaría la asimetría intacta y con el mismo coste de
código.

**Unificar también el modelo de la vista de lectura en esta entrega.** Descartado por tamaño: son
~40 líneas y dos pruebas más, pero pertenece a otra capa (lectura, texto a voz y previsualización) y
su propio defecto conocido. Se registra en «Fuera de alcance» de la spec y en `MEMORY.md`.
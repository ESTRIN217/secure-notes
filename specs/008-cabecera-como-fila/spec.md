# Spec 008 — La fila de cabecera es una fila más

Estado: implementada (2026-10-08), T1-T9 cerradas; **sin verificación visual**

> **Enmienda posterior.** Su **RF-54** («una sola línea») queda **enmendado** por
> `specs/009-celda-multilinea/spec.md`: una tabla no puede ocultar texto. Ver la nota de
> procedencia de la 009.

> **Nota de procedencia.** Hija de `specs/001-limites-constitucion/spec.md` (**aprobada**), que
> sigue siendo la ley. Hereda sin modificar **RF-3, RF-19, RF-20** y **RNF-4, RNF-5, RNF-6**.
> Encadena con `specs/005-tabla-seleccion/` y `specs/007-tiradores-tabla/`, **cerradas**: su carril
> de 48 dp, su único tirador de fila, su tirador de columna superpuesto y su barra flotante son el
> suelo que aquí no se toca. `RF-46`…`RF-54` y `RNF-19`…`RNF-21` son nuevos: `RF-45` y `RNF-18` son
> los más altos en uso (spec 007).

## Enmiendas a specs anteriores

Esta spec **enmienda dos requisitos** de `specs/005-tabla-seleccion/spec.md`, que sigue cerrada:

- **RF-29 pasa a ser**: EL SISTEMA ofrece un tirador para **cada** fila, la primera incluida, con las
  mismas acciones que el de cualquier fila de datos —**eliminar incluida**— y sin alterar el alto de
  la fila. Antes la fila de cabecera no tenía acción de eliminar, porque no era una fila.
- **RF-28 pasa a ser**: MIENTRAS una celda de la **primera fila** tenga el foco, ENTONCES EL SISTEMA
  muestra el tirador de esa columna y lo dibuja centrado sobre el borde superior de la columna, sin
  alterar el alto de la fila. La condición ya **no** depende de que la opción de cabecera esté
  activada, porque la primera fila existe siempre.

El resto de la 005 y de la 007 sigue vigente sin cambio.

## Contexto y objetivo

El bloque de tabla tiene **dos** maneras de marcar una cabecera y **no se parecen en nada**:

- La **columna** de cabecera no existe como dato. Es la columna 0 de cada fila, a la que solo se le
  pone negrita y un tinte. Se activa y se desactiva sin que pase nada más.
- La **fila** de cabecera **sí** existe como dato, pero **fuera** de la tabla: una lista paralela de
  títulos, con su propia casilla de «activada», su propia altura y su propia fila numerada aparte.

De ahí sale toda una familia de rarezas que se fueron tapando una a una y nunca se cerraron:

1. **Escribir en la fila de cabecera puede no guardarse.** La casilla de "activada" solo acepta
   columnas que ya están en la lista de títulos; si la tabla tiene una fila más ancha que esa lista,
   las celdas de más se **pintan y reciben el foco pero su texto se descarta al salir**.
2. **No se pueden reordenar las columnas de una tabla sin cabecera.** La operación de reordenar
   columnas se niega a ejecutarse si la lista de títulos está vacía, aunque las filas tengan celdas.
3. **El tirador de la fila de cabecera se puede arrastrar y no mueve nada.** Marca una fila de destino
   y al soltar no ocurre nada.
4. **La cabecera sale de la tabla en el HTML exportado.** Los títulos se emiten sin la fila que los
   contiene, así que solo un parser HTML5 los salva.
5. **Cada exportador decide por su cuenta qué es una cabecera**, y no coinciden entre sí.
6. **La celda de cabecera no es una celda**: no corta a una línea, no tiene el alto mínimo de las
   demás, no reconoce enlaces y no recibe el fondo alterno de las filas.
7. **La numeración de filas tiene un caso especial** —un número de fila negativo— que obliga a la
   geometría a llevar la altura de la cabecera aparte.

El objetivo es **una sola regla**: la cabecera es *la primera fila*, y «activar fila de cabecera» es
*activar un estilo sobre esa fila*, exactamente igual que «activar columna de cabecera» es activar un
estilo sobre esa columna. No hay dos cosas que sean cabeceras: hay una fila y una columna.

## Usuarios

- Quien escribe tablas: las dos cabeceras se comportan igual y ninguna se traga lo que escribe.
- Quien exporta: el HTML exportado es HTML válido y las cuatro exportaciones dicen lo mismo.
- Quien mantiene el editor: una fila, una columna, un solo camino de pintado y de geometría.

## Historias de usuario

- **HU-1.** Como quien escribe una tabla, quiero que escribir en la primera fila se guarde siempre,
  para no perder un título por haber pegado una tabla con filas de distinta longitud.
- **HU-2.** Como quien escribe una tabla, quiero que «Fila de encabezado» y «Columna de encabezado» me
  hagan lo mismo, porque hoy una apaga un estilo y la otra esconde datos.
- **HU-3.** Como quien exporta, quiero que la cabecera de la tabla sea una fila de verdad en el HTML,
  porque si no se pierde al abrirlo con cualquier herramienta que no sea un navegador.
- **HU-4.** Como quien mantiene el editor, quiero una sola numeración de filas, porque la fila
  negativa obliga a un caso especial en cada capa.

## Definiciones

- **Fila de cabecera**: la primera fila de la tabla mientras la opción «Fila de encabezado» esté
  activada. No es una fila especial: es la primera.
- **Columna de cabecera**: la primera columna de la tabla mientras la opción «Columna de encabezado»
  esté activada. No es una columna especial: es la primera.
- **Cabecera**: el pintado —negrita y tinte— que distingue a una fila o a una columna. Es lo único
  que las dos opciones hacen.
- **Desactivar** una de las dos opciones: quitar el pintado. No borra la fila ni la columna.

## Requisitos funcionales

Heredados sin modificar: **RF-3**, **RF-19**, **RF-20**.

- **RF-46**: EL SISTEMA trata la fila de cabecera como **la primera fila de la tabla**, sin ninguna
  lista de títulos aparte. Verificable: el dato de la tabla no declara ningún campo `headers`, y el
  editor pinta y guarda la primera fila por el mismo camino que las demás.
- **RF-47**: EL SISTEMA hace que «Fila de encabezado» y «Columna de encabezado» sean **la misma clase
  de cosa**: una fila y una columna a las que se les aplica el mismo pintado. Verificable: con la
  opción activada, la celda afectada sale en negrita y con tinte en los dos casos; y con la opción
  desactivada, en los dos casos lo único que cambia es ese pintado.
- **RF-48**: SI la persona usuaria desactiva «Fila de encabezado», ENTONCES EL SISTEMA **conserva la
  primera fila**: sigue visible, sigue siendo editable y sale en las cuatro exportaciones, ya sin
  pintado de cabecera. Verificable: el número de filas que aparece en pantalla y en cada exportación
  es el mismo con la opción activada que con ella desactivada.
- **RF-49**: EL SISTEMA deja **borrar, mover e insertar** la primera fila igual que cualquier otra
  fila, y la fila que pase a ser la primera hereda el papel de cabecera. Verificable: el tirador de
  la primera fila ofrece las mismas acciones que el de cualquier otra fila, eliminar incluida, y tras
  eliminarla la nueva primera fila aparece con el pintado de cabecera si la opción está activada.
- **RF-50**: CUANDO EL SISTEMA emite una tabla en HTML, ENTONCES emite **cada fila dentro de su
  `<tr>`**, incluidos los títulos de la cabecera. Verificable: la exportación de una tabla con
  cabecera contiene `<tr><th>` y no contiene ninguna secuencia `<th>` fuera de un `<tr>`.
- **RF-51**: CUANDO EL SISTEMA exporta la tabla en HTML, Markdown, texto plano o PDF, ENTONCES todas
  las exportaciones contienen **las mismas filas**. Verificable: dar de alta una tabla con tres filas y
  exportarla por los cuatro caminos produce el mismo número de filas en los cuatro resultados.
- **RF-52**: CUANDO se escribe en una celda de la primera fila, ENTONCES EL SISTEMA guarda el texto
  siempre, incluso si esa fila es más corta que las de abajo. Verificable: escribir en la última
  columna de una tabla cuya primera fila no llega a esa columna, salir del bloque y volver a entrar,
  deja el texto escrito.
- **RF-53**: EL SISTEMA numera **las filas una sola vez**, sin una fila de cabecera aparte: no
  existe ningún índice de fila negativo y la altura de la primera fila se mide como la de cualquier
  otra. Verificable: la geometría de la tabla no declara ninguna constante de fila de cabecera ni un
  alto de cabecera, y el número de centros de tirador coincide con el de filas.
- **RF-54**: EL SISTEMA hace que una celda de la primera fila **se comporte como cualquier otra
  celda**: una sola línea, el mismo alto mínimo, el mismo relleno y el mismo tratamiento de los
  enlaces, y solo cambia el pintado. Verificable: la celda de la tabla se construye con el mismo alto
  mínimo y el mismo límite de líneas esté o no en la primera fila.

## Requisitos no funcionales

Heredados: **RNF-4**, **RNF-5**, **RNF-6**.

- **RNF-19 — Ninguna nota pierde texto al leerse.** Una nota escrita con el formato anterior, en el
  que los títulos de la cabecera vivían aparte, se lee con esos títulos presentes y con las mismas
  filas de datos que tenía. Verificable: una prueba toma una tabla con la lista de títulos antigua y
  comprueba que al leerla la primera fila contiene esos títulos y que ninguna fila de datos se ha
  perdido ni cambiado de sitio.
- **RNF-20 — La lógica comprobable no depende de la pantalla.** La geometría y el modelo de la tabla
  se prueban sin interfaz. Verificable: los dos archivos no importan nada de Compose
  (`grep -c '^import androidx.compose'` → 0) y sus pruebas son JUnit puro sin entorno de pruebas.
- **RNF-21 — Sin coste nuevo.** No se añade ninguna dependencia ni ninguna cadena de texto: las dos
  opciones de cabecera ya tienen sus etiquetas en los nueve idiomas. Verificable: el conjunto de
  dependencias no cambia y `git diff -- app/src/main/res` está vacío.

## Casos límite

1. **Nota antigua con la cabecera apagada.** Sus títulos estaban guardados pero invisibles, y ni
   Markdown ni texto plano los respetaban. Al leerla, esos títulos pasan a ser la primera fila, a la
   vista y como dato. **Es un cambio visible y es la decisión consciente**: lo que no se hace es
   borrar texto del usuario.
2. **Nota antigua sin fila de cabecera.** Si nunca tuvo títulos, no aparece una fila fantasma: la
   tabla empieza por sus datos. Las celdas que se pintaban vacías y no admitían escritura desaparecen,
   y no se pierde nada porque estaban vacías.
3. **Tabla de una sola fila.** Sigue sin poder eliminarse, como antes: la protección de no dejar la
   tabla sin filas se mantiene.
4. **Tabla sin filas.** El número de columnas es cero y no se pinta nada, como antes.
5. **Filas de distinta longitud.** La geometría sigue usando un ancho por columna, así que una fila
   más corta que otra no desplaza nada.
6. **Insertar una fila por encima de la primera.** La fila nueva se convierte en la cabecera, porque
   pasa a ser la primera. Es la consecuencia directa de RF-49 y no es un fallo.
7. **Reordenar columnas.** Cada título viaja con su columna, porque es la celda de la primera fila de
   esa columna. Mismo resultado que antes, con un solo camino.
8. **Exportar a texto plano con la cabecera apagada.** El texto plano no tiene forma de expresar
   negrita, así que no puede distinguir la cabecera; sí emite la fila y su línea de separación, que el
   formato de tabla exige. El pintado sí se ve en HTML y en PDF.

## Decisiones tomadas

1. **La cabecera es la primera fila, siempre, y no se guarda su posición.** La alternativa
   descartada era guardar en qué fila está la cabecera para que esta la siguiera al reordenar: eso
   obliga a la geometría a seguir distinguiendo una fila del resto —que es exactamente el problema
   que se viene a cerrar— y a migrar un índice en las notas ya escritas. Con la fila 0, desactivar
   la cabecera es solo quitar un pintado, que es lo mismo que hace ya la columna.
2. **Desactivar la fila de cabecera la convierte en dato, no la borra.** La alternativa descartada
   era esconderla, que es lo que hace hoy el editor: rompería la paridad con la columna —que al
   desactivarse tampoco desaparece— y obligaría a que las cuatro exportaciones tuvieran excepciones.
3. **Al leer una nota antigua, los títulos antiguos se anteponen siempre.** La alternativa
   descartada era anteponerlos solo si la cabecera estaba activada: reproduce exactamente lo que se
   veía, pero las cabeceras apagadas se perderían de forma permanente en la siguiente escritura de
   la nota, y la constitución no permite decidir que el texto del usuario se descarta.
4. **El texto plano no puede expresar la cabecera, y no se lo fuerza.** Se documenta en el caso
   límite 8 en vez de inventar un formato: la línea de separación la exige el formato de tabla, así
   que va siempre tras la primera fila.

## Fuera de alcance

Se registran para que consten. **No se hacen en esta entrega.**

- El segundo modelo de tabla de la vista de lectura, el que también guarda los títulos aparte y lo
  usan la lectura en pantalla sin edición, el texto a voz y la previsualización. Unificarlo con este
  es un trabajo aparte; se registra porque su forma es la misma asimetría que aquí se cierra en el
  editor, y porque al unificar se arreglaría de paso el defecto conocido de la previsualización que
  muestra la tabla vacía.
- Los siete defectos de la grabación del 2026-10-07 que la spec 007 dejó registrados sin corregir.
- Los otros defectos conocidos de `MEMORY.md`: la selección compartida entre bloques del editor y
  la información de selección del bloque de lista de verificación.
- La comprobación en dispositivo: no hay ni la puede haber en la plataforma de desarrollo.
- El ancho de columna, el carril de los tiradores y el ritmo vertical del editor.

## Criterios de finalización

- [x] `./gradlew :app:testDebugUnitTest --tests '*TableData*' --tests '*TableSelectionGeometry*' --tests
      '*PlainTextExport*' --tests '*RichTextParser*'` termina en verde con `--rerun`: **107 pruebas**
      (25 de `TableDataTest`, 40 de geometría, 7 de texto plano, 35 del parser).
- [x] `./gradlew :app:compileDebugUnitTestKotlin` termina sin errores.
- [x] `./gradlew :app:assembleDebug` termina correctamente.
- [x] `grep -rn "headers" app/src/main/java/com/example/data/model/DataBlock.kt app/src/main/java/com/example/ui/EditableTableBlock.kt`
      no devuelve ninguna declaración de lista de títulos. Solo queda la **lectura** de la clave
      antigua, que es RNF-19 y no una declaración.
- [x] `grep -rn "HEADER_ROW\|headerHeight" app/src/main/java/com/example/` no devuelve nada.
- [x] La exportación HTML de una tabla con cabecera contiene `<tr><th>` y ningún `<th>` suelto.
- [x] Esta entrega **no añade ninguna cadena**: `git diff -- app/src/main/res` solo muestra las tres
      claves de la spec 007, que estaban sin commitear en el árbol.
- [x] `MEMORY.md` registra la regla permanente «la cabecera es la primera fila / la primera columna:
  es un pintado, no un dato» y los errores a evitar de esta entrega.

Dos cosas quedan **fuera de alcance y no se hacen aquí**, declaradas para que consten: la
comprobación visual, que no hay dispositivo en esta plataforma, y el segundo modelo de tabla de la
vista de lectura. `HtmlExportMediaTest > legacy item markup migrates to checklist html` falla, y es
el fallo preexistente que ya arrastraba la suite (`HtmlExportMediaTest.kt:117`, comprobado contra
`HEAD`).

## Dudas abiertas

Ninguna. Las tres decisiones de diseño —qué fila es la cabecera, qué pasa al desactivarla y qué se
hace con las notas antiguas— se cerraron con la persona usuaria antes de escribir esta spec.
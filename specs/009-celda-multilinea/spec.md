# Spec 009 — Celda multilínea y geometría por fila

Estado: implementada (2026-10-09), T1-T4 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**: «todo se ve bien».

> **Nota de procedencia.** Hija de `specs/001-limites-constitucion/spec.md` (**aprobada**), que
> sigue siendo la ley. Hereda sin modificar **RF-3, RF-19, RF-20** y **RNF-4, RNF-5, RNF-6**.
> Encadena con `specs/008-cabecera-como-fila/`, **cerrada**: la cabecera es la primera fila y las dos
> cabeceras son el mismo pintado. `RF-55`…`RF-59` y `RNF-22` son nuevos: `RF-54` y `RNF-21` son los
> más altos en uso (spec 008).

## Enmiendas a specs anteriores

Esta spec **enmienda el RF-54** de `specs/008-cabecera-como-fila/spec.md`:

- **RF-54 pasa a ser**: EL SISTEMA hace que una celda de la primera fila se comporte como cualquier
  otra celda —mismo relleno, mismo tratamiento de los enlaces, mismo alto mínimo— y solo cambia el
  pintado de cabecera. Antes decía «una sola línea», y eso **ocultaba texto**.

## Contexto y objetivo

La spec 008 dejó las celdas con una sola línea y un alto mínimo fijo, para que la celda de cabecera
dejara de ser un caso especial. El efecto buscado se consiguió, pero aparece uno contrario: **una tabla
no puede ocultar texto**. Con una sola línea y sin alto máximo, una celda más ancha que su columna
corta el texto a media palabra y no hay forma de leerlo.

Ese ajuste además **tapaba un defecto anterior**. El bloque medía **una sola altura para todas las
filas**, y el valor que guardaba era el de la última fila medida. Con todas las filas a la misma altura el
error es invisible; en cuanto una fila crece, el contorno de selección y los tiradores se colocan
sumando alturas que no son las reales, y caen sobre filas que no son la suya.

Con la celda multilínea las dos cosas quedan a la vista: el texto se leía entero, y la geometría
miente. Esta spec arregla las dos.

## Usuarios

- Quien escribe tablas: ninguna celda esconde texto, ni la más larga ni la de una fila alta.
- Quien edita: el contorno señala la celda de verdad, esté en la fila que esté.
- Quien mantiene el editor: una fila mide lo que miden sus celdas, y se sabe cuánto mide cada una.

## Historias de usuario

- **HU-1.** Como quien escribe una tabla, quiero que una celda larga se lea entera, porque una tabla
  cuya última palabra no se ve no sirve.
- **HU-2.** Como quien edita, quiero que el contorno caiga sobre la celda enfocada aunque su fila sea
  más alta que las de arriba, porque si no señala otra cosa.
- **HU-3.** Como quien escribe una tabla, quiero que el tinte de la cabecera cubra toda la fila, y no
  solo lo que ocupa su texto.

## Definiciones

- **Alto de una fila**: la altura que la fila ocupa, que es la de su celda más alta.
- **Celda multilínea**: celda cuyo texto envuelve en tantas líneas como necesite y cuya altura crece
  con ellas.

## Requisitos funcionales

Heredados sin modificar: **RF-3**, **RF-19**, **RF-20**.

- **RF-55**: EL SISTEMA **no oculta nunca el texto de una celda**: envuelve en tantas líneas como
  necesite y crece con ellas. Verificable: la celda no fija ni un máximo de líneas ni un alto máximo,
  y su campo de texto no es de una sola línea.
- **RF-56**: EL SISTEMA mide **el alto de cada fila por separado** y lo mantiene aunque las filas se
  añadan, se quiten o se reordenen. Verificable: al quitar una fila, la lista de altos pierde su
  entrada; al mover una fila, su alto viaja con ella; y añadir una fila no cambia los altos de las
  demás.
- **RF-57**: EL SISTEMA apoya el contorno de selección, la región de fila y los tiradores **en el alto
  real de cada fila**, no en un alto común. Verificable: el borde superior de una celda de la fila `n`
  es la suma de los altos de las filas `0..n-1`, con los altos reales de cada una.
- **RF-58**: MIENTRAS una fila contenga una celda más alta que sus hermanas, ENTONCES EL SISTEMA hace
  que **todas sus celdas ocupen el alto de la fila**, de modo que el borde, el tinte de cabecera y la
  sombra de un enlace cubren la fila entera. Verificable: ninguna celda fija su alto al de su texto,
  y el suelo común es un mínimo, nunca un tope.
- **RF-59**: SI el texto de una celda necesita más alto del que su fila ocupa tras medirla, ENTONCES
  EL SISTEMA deja que la celda crezca y **no recorta el texto**. Verificable: el alto que se impone a
  una celda es un mínimo, no un alto exacto.

## Requisitos no funcionales

Heredados: **RNF-4**, **RNF-5**, **RNF-6**.

- **RNF-22 — Sin coste nuevo.** No se añade ninguna dependencia ni ninguna cadena de texto. La lógica
  de los altos por fila vive en una capa sin composición, como la geometría. Verificable: el fichero
  nuevo no importa nada de Compose (`grep -c '^import androidx.compose'` → 0) y `git diff --
  app/src/main/res` no muestra nada de esta entrega.

## Casos límite

1. **Fila con una sola celda larga.** La fila crece y todas sus celdas, las vacías incluidas, ocupan
   el alto entero.
2. **Fila con todas las celdas cortas.** El suelo común sigue siendo el alto mínimo, así que la fila
   no se aplana.
3. **Fila que se encoge al editar.** Se vuelve a medir y el alto baja; el alto viejo no se queda pegado.
4. **Fila eliminada mientras su celda tiene el foco.** La celda ya no existe y ni el contorno ni el
   tirador se dibujan.
5. **Tabla vacía.** No hay filas que medir y no hay nada que dibujar.
6. **Fila movida.** El alto viaja con la fila: si la fila alta pasa a ser la última, el contorno de la
   tercera sigue cayendo en la tercera.
7. **Tipografía grande.** Una fila que pasa a ocupar tres líneas se mide a tres líneas y todo lo demás
   se recoloca con ella.

## Decisiones tomadas

1. **El alto que se impone a una celda es un mínimo, no un alto exacto.** Es la diferencia entre RF-58
   y RF-59, y depende de cuál se elija: con un alto exacto, una celda cuyo texto crece después de la
   medida quedaría recortada, que es justo lo que esta spec viene a quitar. Se descarta por completo el
   alto exacto.
2. **El alto de cada celda se realimenta desde el alto de su fila, ya medido.** La alternativa
   descartada es `height(IntrinsicSize.Min)` en la fila con `fillMaxHeight()` en las celdas, que es la
   respuesta idiomática de composición pero depende de que el campo de texto reporte altura intrínseca:
   si no la reporta, la fila se colapsa a cero. No hay pruebas de interfaz en la plataforma de
   desarrollo para descartarlo, así que se elige la vía determinista, que además no puede recortar.
3. **La capa superpuesta del contorno no cambia.** La geometría ya suma los altos reales por fila, y
   hay una prueba que lo fija; lo que estaba mal era quién le daba los datos. Se descarta tocar el
   dibujado: el defecto estaba en el que las medía, no en el que las dibuja.
4. **El desplazamiento horizontal sin pista visible se deja fuera.** Ya está registrado como defecto
   conocido en las specs 005 y 007, y la multilínea no lo empeora de forma distinta: la tabla crece
   hacia abajo, no hacia un lado nuevo.

## Fuera de alcance

Se registran para que consten. **No se hacen en esta entrega.**

- La pista de que la tabla se desplaza en horizontal, con la cuarta columna saliéndose de la tarjeta.
  Es un defecto ya registrado en las specs 005 y 007 y merece su propia decisión: desplazarse o
  comprimir.
- Los siete defectos de la grabación del 2026-10-07 que la spec 007 dejó registrados sin corregir.
- La barra flotante pegada al borde derecho de la pantalla en vez de al de la tarjeta.
- El segundo modelo de tabla de la vista de lectura, el que también guarda los títulos aparte.
- La comprobación en dispositivo: no la hay ni se puede hacer en la plataforma de desarrollo.

## Criterios de finalización

- [ ] `./gradlew :app:testDebugUnitTest --tests '*TableRowHeights*' --tests '*TableSelectionGeometry*' --rerun`
      termina en verde.
- [ ] `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug` sin errores.
- [ ] `grep -rn "singleLine" app/src/main/java/com/example/ui/EditableTableBlock.kt` no devuelve nada.
- [ ] `grep -n "rowHeightPx" app/src/main/java/com/example/ui/EditableTableBlock.kt` no devuelve ningún
      alto único por tabla: o no existe o es por fila.
- [ ] `git diff -- app/src/main/res` no muestra nada de esta entrega.
- [ ] `MEMORY.md` registra que una tabla no oculta texto y que una geometría que asume filas del
      mismo alto miente en cuanto una crece.

## Dudas abiertas

Ninguna. La decisión de multilínea y de no ocultar texto es de la persona usuaria, y quedó cerrada al
señalar que en una tabla el texto no debe desaparecer; la elección entre realimentar el alto medido y
usar altura intrínseca se resolvió por la regla de no recortar, y está argumentada en la decisión 2.
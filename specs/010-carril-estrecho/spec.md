# Spec 010 — El carril del tirador de fila se estrecha

Estado: implementada (2026-10-09), T1-T3 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**: «todo se ve bien».

> **Enmienda posterior.** Su **decisión 2** (ensanchar la capa de tiradores para que el desborde sea
> pulsable) y su **RF-61** quedan **enmendados** por `specs/011-sin-desborde/spec.md`: en dispositivo
> la parte del tirador fuera de la tarjeta **no recibe el toque**, y el carril baja a 36 dp para que
> el tirador quepa entero dentro. Ver la nota de procedencia de la 011.

> **Nota de procedencia.** Hija de `specs/001-limites-constitucion/spec.md` (**aprobada**), que
> sigue siendo la ley. Hereda sin modificar **RF-3, RF-19, RF-20** y **RNF-4, RNF-5, RNF-6**.
> Encadena con `specs/005-tabla-seleccion/`, `007-tiradores-tabla/`, `008-cabecera-como-fila/` y
> `009-celda-multilinea/`, todas cerradas. `RF-60`…`RF-62` y `RNF-23` son nuevos: `RF-59` y `RNF-22`
> son los más altos en uso (spec 009).

## Contexto y objetivo

El tirador de fila vive en un carril de **48 dp que se reserva siempre**, esté o no visible, y ese
carril se descuenta del ancho de las columnas. Es el precio del arreglo que evitó que el tirador
tapara la primera letra de cada fila: sin carril, los tiradores se montaban encima de la celda y
tapaban texto. Pero el precio es un impuesto permanente: **en una pantalla de 360 dp, con 16 dp de
margen a cada lado, quedan 280 dp para las columnas**, y el tirador se lleva 48 dp aunque no haya
ninguno en pantalla.

El tirador de columna no paga nada: vive en una capa superpuesta y solo desborda hacia arriba. La
asimetría es real y la nota quien escribe tablas en pantallas estrechas.

El objetivo es recuperar parte de ese ancho **sin que el tirador vuelva a tocar texto**, sin
perder el objetivo táctil de 48 dp y sin que la tabla se mueva al tocarla.

## Usuarios

- Quien escribe tablas en pantallas estrechas: más ancho para las columnas sin que el tirador invada
  el texto.
- Quien toca la tabla con el dedo: el tirador sigue siendo igual de fácil de pulsar.

## Definiciones

- **Carril**: la franja reservada a la izquierda de la tabla, dentro de la tarjeta, donde no hay
  texto porque el tirador de fila se coloca encima.
- **Desborde**: la parte del tirador que sobresale por fuera de la tarjeta, a la izquierda.

## Requisitos funcionales

Heredados sin modificar: **RF-3**, **RF-19**, **RF-20**.

- **RF-60**: EL SISTEMA reserva un carril **más estrecho que el tirador** y coloca el tirador de fila
  de modo que su borde derecho alcance el final del carril, de forma que **el texto de la primera
  columna no queda nunca debajo del tirador**. Verificable: el carril y el relleno horizontal de la
  primera celda suman la posición donde empieza su texto, y el borde derecho del tirador llega
  exactamente hasta ahí.
- **RF-61**: EL SISTEMA conserva el **objetivo táctil completo de 48 dp** del tirador de fila aunque
  el carril sea más estrecho que él. Verificable: el tirador sigue midiendo 48 dp, y lo que sobresale
  por fuera de la tarjeta también forma parte de su zona pulsable.
- **RF-62**: MIENTRAS haya una celda enfocada o un puntero encima, ENTONCES EL SISTEMA **no cambia el
  ancho disponible para las columnas**: el carril se reserve siempre igual, con el tirador visible o
  no. Verificable: el ancho de las columnas es el mismo con la tabla señalada que sin ella.
  **Nogliada**: es el RF-25 de la spec 005, que se respeta y no se enmenda.

## Requisitos no funcionales

Heredados: **RNF-4**, **RNF-5**, **RNF-6**.

- **RNF-23 — Sin coste nuevo.** No se añade ninguna dependencia ni ninguna cadena de texto, y no
  cambia el tamaño del tirador. Verificable: `git diff -- app/src/main/res` no muestra nada y
  `HandleWidth` sigue valiendo 48 dp.
- **RNF-24 — La geometría sigue sin composición.** El cambio no toca
  `ui/table/TableSelectionGeometry.kt` ni `ui/table/TableRowHeights.kt`. Verificable:
  `git diff -- app/src/main/java/com/example/ui/table/` está vacío y la suite de geometría sigue
  verde **sin editar el fichero de pruebas**, porque su `gutter` es una constante local.

## Casos límite

1. **Tabla de una sola columna.** El carril se descuenta igual; el tirador cae sobre el relleno, no
   sobre el texto.
2. **Tabla que ya se desplaza.** El carril se descuenta del ancho disponible igual que antes; lo que
   cambia es cuánto se descuenta.
3. **Tabla señalada y no señalada.** El ancho de las columnas es el mismo en los dos casos: el carril
   no aparece ni desaparece.
4. **El desborde queda fuera de la tarjeta.** Sobre el margen del editor, no sobre el contenido de
   otro bloque.
5. **Foco en una celda de la primera fila.** El tirador de columna sigue centrado en su columna
   aunque la capa de tiradores se haya desplazado para contener el desborde del de fila.
6. **Tipografía grande.** El texto de la primera celda sigue empezando en el mismo sitio: el carril no
   depende del tamaño de letra.

## Decisiones tomadas

1. **El tirador se alinea por su borde derecho al final del carril.** La alternativa descartada era
   centrarlo en un carril estrecho: entonces la mitad derecha caería sobre el relleno y el texto de
   la primera celda, que es exactamente el defecto que el arreglo del carril vino a cerrar. Con el
   borde derecho pegado al final, el tirador ocupa el carril y el relleno, y se detiene donde
   empieza el texto.
2. **Se ensancha la capa de tiradores para que contenga el desborde.** La alternativa descartada era
   dejar el tirador medio fuera de los límites de su padre: si el padre no contiene el punto del
   dedo, el toque puede no llegar. Desplazar la capa es lo que garantiza que el tirador completo
   siga siendo pulsable.
3. **El mínimo de columna no se toca en esta entrega.** Se decidió no bajarlo: no es este cambio el
   que decide si una tabla con cuatro columnas cabe en una pantalla. Sigue registrado.
4. **La pista de desplazamiento no se toca.** Está registrada sin corregir en las specs 005 y 007 y
   merece su propia decisión sobre si la tabla se desplaza o se comprime.

## Fuera de alcance

Se registran para que consten. **No se hacen en esta entrega.**

- El ancho mínimo por columna, que es lo que saca la cuarta columna de la tarjeta en pantallas
  estrechas. Decidido expresamente.
- La pista visual de que la tabla se desplaza en horizontal. Decidido expresamente.
- Los siete defectos de la grabación del 2026-10-07 que la spec 007 dejó registrados sin corregir.
- La barra flotante pegada al borde derecho de la pantalla en vez de al de la tarjeta.
- El segundo modelo de tabla de la vista de lectura.

## Criterios de finalización

- [x] `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*' --tests '*TableRowHeights*' --rerun`
      termina en verde **sin editar** `TableSelectionGeometryTest.kt`.
- [x] `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug` sin errores.
- [x] `grep -n "TableRowGutter = \|HandleWidth = \|HandleOverhang = " app/src/main/java/com/example/ui/EditableTableBlock.kt`
      muestra el carril más pequeño que el tirador, y el tirador sigue en 48 dp.
- [x] `git diff -- app/src/main/java/com/example/ui/table/` está vacío.
- [x] `git diff -- app/src/main/res` está vacío.
- [x] `MEMORY.md` registra que un carril más estrecho que el tirador obliga a alinearlo por su borde
      derecho, no centrado.

## Dudas abiertas

Ninguna. El ancho del carril, el alineamiento del tirador y el mantenimiento del objetivo táctil
están decididos. Lo único que **no** se puede comprobar aquí es si el desborde recibe el toque, y
queda declarado como verificación pendiente con su plan alternative.
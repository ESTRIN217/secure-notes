# Spec 011 — El desborde del tirador no es pulsable

Estado: implementada (2026-10-09), T1-T2 cerradas; **verificado en dispositivo** para el tirador

> **Nota de procedencia.** Hija de `specs/001-limites-constitucion/spec.md` (**aprobada**), que
> sigue siendo la ley. Hereda sin modificar **RF-3, RF-19, RF-20** y **RNF-4, RNF-5, RNF-6**.
> Encadena con `specs/010-carril-estrecho/`, **cerrada**, que queda **enmendada** por esta.
> `RF-63` y `RNF-25` son nuevos: `RF-62` y `RNF-24` son los más altos en uso (spec 010).

## Enmiendas a specs anteriores

Esta spec **enmienda la decisión 2 y el RF-61** de `specs/010-carril-estrecho/spec.md`:

- La **decisión 2** («se ensancha la capa de tiradores para que contenga el desborde») queda
  **descartada**: en dispositivo, la parte del tirador que sobresale de la tarjeta **no recibe el
  toque**.
- El **RF-61** se cumple por otra vía: el tirador conserva los **48 dp enteros dentro** de la tarjeta.

El resto de la 010 se mantiene: el carril sigue siendo más estrecho que el tirador y el tirador
sigue alineado por su borde derecho para no tocar el texto.

## Contexto y objetivo

La spec 010 apostó por un carril de 24 dp para que el tirador de 48 dp saliera 12 dp por fuera de la
tarjeta.
Se comprobó en dispositivo y **esa parte no responde al toque**: un tirador que se ve de 48 dp y solo
acepta la mitad dentro es peor que un tirador más pequeño que sí funciona.

Encima, el cambio tenía un **error de aritmética**: la capa de tiradores se desplazó 12 dp a la
izquierda y el tirador de fila se colocó a `carril − 48` **relativo a esa capa ya desplazada**, con lo
que el desborde se contaba dos veces y el tirador acababa 36 dp fuera en vez de 12. Al tirador de
columna sí se le devolvió el desfase y al de fila no.

El objetivo es cerrar la puerta a un desborde que no funciona: el tirador entero dentro de la tarjeta,
y el ancho recuperado que eso permita.

## Usuarios

- Quien escribe tablas en pantallas estrechas: recupera ancho sin encontrar un tirador que a veces no
  responde.
- Quien toca la tabla con el dedo: el tirador responde en toda su superficie.

## Definiciones

- **Desborde**: la parte de un tirador que queda fuera de la tarjeta. Esta spec lo hace **cero**: es
  un término que se conserva solo para decir que ya no existe.

## Requisitos funcionales

Heredados sin modificar: **RF-3**, **RF-19**, **RF-20**.

- **RF-63**: EL SISTEMA coloca el tirador de fila **entero dentro de la tarjeta**, sin que ninguna
  parte suya quede fuera. Verificable: el carril más el relleno horizontal de la primera celda miden
  lo mismo que el tirador, así que su borde derecho coincide con el punto donde empieza el texto, y
  no hay ningún desplazamiento aplicado a la capa de tiradores.
- **RF-61 (enmendado, de la 010)**: EL SISTEMA conserva el objetivo táctil completo de **48 dp** del
  tirador de fila, y **toda** esa superficie recibe el toque. Verificable: el tirador mide 48 dp, vive
  dentro de los límites de la tarjeta, y no hay ningún modificador que lo saque de ellos.

## Requisitos no funcionales

Heredados: **RNF-4**, **RNF-5**, **RNF-6**.

- **RNF-25 — Menos código que antes.** Al no haber desborde, desaparecen el desplazamiento de la
  capa de tiradores y la compensación del tirador de columna. Verificable: el diff respecto de la
  versión anterior **elimina** líneas además de cambiarlas, y no queda ninguna referencia a un
  desborde en el fichero.
- **RNF-23 (de la 010)** — Sin cadenas nuevas ni dependencias, y `ui/table/` sin tocar: el carril
  sigue siendo un parámetro de las medidas.

## Casos límite

1. **Tabla de una sola columna.** El carril se descuenta igual y el tirador cae sobre el relleno.
2. **Tabla señalada y no señalada.** El ancho de las columnas es el mismo: el carril se reserva
   siempre (RF-62 de la 010, sin cambios).
3. **Tipografía grande.** El texto de la primera columna sigue empezando en el mismo sitio: el carril
   no depende del tamaño de letra.
4. **El tirador cubre el borde izquierdo de la primera celda.** Es inevitable: el tirador ocupa 48 dp
   desde el borde y la celda empieza a 36 dp. Cubre 12 dp de borde y relleno, nunca texto.

## Decisiones tomadas

1. **Carril de 36 dp, y el tirador entero dentro.** La cuenta se cierra sola: 36 de carril más 12 de
   relleno son exactamente los 48 dp del tirador, así que su borde derecho cae justo donde empieza el
   texto. Se recuperan **12 dp** de ancho, no los 24 que se buscaban. La alternativa descartada era
   mantener los 24 dp y aceptar que una parte del tirador no responda, que es el defecto que la 010
   introdujo y que se ha comprobado en dispositivo.
2. **Corregir la aritmética y volver a probar los 24 dp antes de renunciar a ellos.** Se descartó:
   aunque el error esté corregido, la capa de tiradores quedaría medio fuera de la tarjeta y el
   mismo problema subiría un nivel. Y no hay forma de comprobarlo sin un dispositivo, que es lo que
   más cuesta. Se recupera lo que es seguro y se deja escrito por qué.
3. **El tirador de columna vuelve a su sitio.** Como la capa ya no se desplaza, el tirador de columna
   se coloca donde siempre, sin compensación. ElRF-28 enmendado de la 008 sigue cumpliéndose igual.

## Fuera de alcance

Se registran para que consten. **No se hacen en esta entrega.**

- El ancho mínimo por columna, que es lo que saca la cuarta columna de la tarjeta en pantallas
  estrechas.
- La pista visual de que la tabla se desplaza en horizontal.
- Si el mínimo de columna y el carril pueden negociar el ancho entre sí, que es la pregunta de fondo
  que estas tres specs van sorteando de uno en uno.
- Los siete defectos de la grabación del 2026-10-07 que la spec 007 dejó registrados sin corregir.

## Criterios de finalización

- [x] `grep -n "TableRowGutter = \|HandleWidth = \|HandleOverhang" app/src/main/java/com/example/ui/EditableTableBlock.kt`
      muestra el carril a 36 dp, el tirador en 48 dp y **ninguna** referencia a un desborde.
- [x] `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*' --tests '*TableRowHeights*' --rerun`
      en verde, **sin editar** `TableSelectionGeometryTest.kt`.
- [x] `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug` sin errores.
- [x] `git diff -- app/src/main/java/com/example/ui/table/` y `git diff -- app/src/main/res` vacíos.
- [ ] `MEMORY.md` registra que un tirador más ancho que el carril solo funciona entero dentro.

## Dudas abiertas

Ninguna. La elección entre rendirse a 12 dp o reintentar los 24 dp la decidió la persona usuaria, y
está argumentada en las decisiones 1 y 2.
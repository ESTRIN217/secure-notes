# Spec 007 — Tiradores del bloque de tabla con objetivo táctil real

Estado: **implementada** (2026-10-07), T1-T8 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**: «todo se ve bien».

> **Nota de procedencia.** Hija de `specs/001-limites-constitucion/spec.md` (**aprobada**), que
> sigue siendo la ley. Hereda sin modificar **RF-3, RF-19, RF-20** y **RNF-4, RNF-5, RNF-6**.
> Encadena con `specs/005-tabla-seleccion/`, **cerrada y commiteada**: su carril de 24 dp, su
> único tirador de fila, su tirador de columna superpuesto y su barra flotante son el suelo que
> aquí no se toca. `RF-42`…`RF-45` y `RNF-16` son nuevos: `RF-41` y `RNF-15` son los más altos
> en uso (spec 006).

## Contexto y objetivo

`NotionSelectionHandle` dibuja los tres tiradores de la tabla: el de fila, el de columna y el de
arrastre de la barra flotante. Es una `Box` de 16×24 dp (o 24×16 dp en horizontal) con un borde y
un `Text` de «⋮⋮» o «⋯» dentro, con `clickable` encima.

Sobre un dispositivo tactil eso son **16 dp de ancho** para el de fila y el de la barra, y el
objetivo táctil mínimo de Material es de **48 dp**: se queda en **un tercio del ancho**, y la
mitad en alto. El de columna tiene 24 dp de ancho. Además el arrastre se pide con
`detectDragGesturesAfterLongPress`, es decir pulsación larga sobre un blanco de 16×24 dp.

El objetivo es que los tres sean botones de icono de **48×48 dp reales**, sin trucos, con
descripción accesible propia y acorde a su función, y que el carril de la primera columna pase a
48 dp para que el tirador de fila quepa dentro sin tocar texto.

## Alcance

Los tres usos de `NotionSelectionHandle`: `HeaderColumnHandle`, `RowHandle` y el tirador de
arrastre dentro de `FloatingToolbar`, más el carril (`TableRowGutter`) que aloja al de fila.

## Fuera de alcance

Se registran para que consten. **No se hacen en esta entrega.**

- Los siete defectos de la grabación del 2026-10-07: la 4.ª columna que se sale de la tarjeta
  sin scroll ni indicador; la celda que pierde caracteres por la izquierda; el recorte de
  texto sin puntos suspensivos; el menú de fila que sobresale por la izquierda de la tarjeta; el
  menú de columna que tapa las columnas 1 y 2; la «gota» de color de acento bajo la cabecera; y
  la separación de 4 px entre la barra y la tabla.
- Las cuatro decisiones que la persona usuaria descartó sobre ellos: comprimir en vez de
  desplazarse, elipsis en las celdas, colocación de los menús y más separación en la barra.
- El modelo de datos: ni `DataBlock`, ni `TableData`, ni la serialización en `meta`, ni la
  versión de Room. Ninguna nota existente cambia de aspecto más allá del ancho de columna.
- La capa `ui/table/TableSelectionGeometry.kt` y sus pruebas: la geometría no cambia, solo el
  valor del carril que le pasa el orquestador.
- El ritmo vertical del editor y el resto de bloques.

## Requisitos funcionales

Heredados sin modificar: **RF-3**, **RF-19**, **RF-20**.

- **RF-42**: EL SISTEMA dibuja los tres tiradores del bloque de tabla como un botón de icono de
  **48×48 dp**, sin reducir su tamaño con `Modifier.size` ni anulando el mínimo interactivo.
  Verificable: el composable del tirador construye un `IconButton` sin `LocalMinimumInteractiveComponentSize`,
  y `grep -n "HandleWidth = " app/src/main/java/com/example/ui/EditableTableBlock.kt` devuelve
  `48.dp`; los tres usos pasan por ese composable y no hay ningún otro tirador de la tabla.
- **RF-43**: CADA UNO de los tres tiradores expone una descripción accesible **distinta y acorde
  a su función** —acciones de fila, acciones de columna, mover la tabla—, resuelta con
  `stringResource`. Verificable: los tres call sites pasan tres claves distintas entre sí, y
  ninguna de las tres descripciones es nula ni vacía.
- **RF-44**: EL SISTEMA reserva un **carril de 48 dp** a la izquierda de la primera columna para
  el tirador de fila, con el mismo ancho que el tirador, de modo que **ningún tirador cubre
  texto**. Verificable: `TableRowGutter` vale lo mismo que `HandleWidth`, y el ancho disponible
  para columnas que calcula el orquestador descuenta el carril completo.
- **RF-45**: AL pulsar un tirador, EL SISTEMA conserva la acción que ya tenía —abrir el menú de
  fila, abrir el menú de columna o arrastrar el bloque— y el tirador **sigue permitiendo el
  arrastre por pulsación larga**. Verificable: los tres call sites conservan sus modificadores
  `onDragModifier` con `detectDragGesturesAfterLongPress`, y el `onClick` de fila y columna sigue
  abriendo su `DropdownMenu`.

## Requisitos no funcionales

Heredados: **RNF-4**, **RNF-5**, **RNF-6**.

- **RNF-16 — Sin trampas de tamaño.** La entrega no usa
  `LocalMinimumInteractiveComponentSize`, ni `Modifier.size` para encoger el tirador, ni
  `LocalMinimumInteractiveComponentEnforcement`. Verificable: `grep -rn "LocalMinimumInteractiveComponent\|LocalMinimumInteractiveComponentEnforcement"`
  en `EditableTableBlock.kt` no devuelve nada.
- **RNF-17 — Texto en los nueve locales a la vez.** Las tres claves nuevas existen en los nueve
  `strings.xml`. Verificable: un `grep -l` por clave devuelve nueve ficheros, y
  `./gradlew :app:mergeDebugResources` termina en verde.
- **RNF-18 — Sin coste nuevo.** No se añade ninguna dependencia: el icono `DragHandle` está en
  `material-icons-extended`, que el proyecto ya declara. Verificable: el conjunto de
  dependencias no cambia.

## Casos límite

1. **Tabla con muchas filas.** El carril crece solo en ancho; las filas no cambian de alto ni se
   mueven, porque el tirador vive en una capa superpuesta.
2. **Tabla estrecha.** Con un carril de 48 dp, una tabla de 4 columnas deja menos ancho útil que
   antes. Es el coste declarado en la decisión 1.
3. **Cabecera desactivada.** El carril sigue reservándose igual y el tirador de fila no cambia de
   sitio: `rowHandleCenters` no depende del ancho del carril.
4. **Tabla con scroll horizontal.** El carril no pertenece a la zona desplazable, así que no se
   desplaza con las columnas.
5. **Tipografía grande.** El tirador se centra en su fila con la geometría existente, que sigue
   Midiendo la fila por su alto real.
6. **El tirador de la barra.** Al medir 48 dp, la barra crece **en ancho**, no en alto: ya
   contenía un `IconButton` de 48×48 para el menú «⋮», así que el alto no cambia.
7. **Pulsación larga cerca del borde.** El tirador de columna, ahora de 48×48, sobresale más
   hacia arriba sobre la cabecera. Al ser superpuesto y no ocupar alto, RF-28 y RF-29-bis de la
   005 siguen cumpliéndose.
8. **Contenido descrito en los tres tiradores.** Un lector de pantalla anuncia «Row actions»,
   «Column actions» o «Move table», que dicen qué hace el tirador sin decir «⋮⋮».

## Decisiones tomadas

1. **48×48 dp reales, sin reducción.** Es un intercambio consciente: **la tabla pierde 24 dp de
   ancho útil y el carril pasa de 24 a 48 dp**. A cambio, el objetivo táctil es el de Material y
   el arrastre deja de pedir pulsación larga sobre 16 dp. La alternativa descartada a propósito
   era conservar 24 dp con `CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp)`:
   habría mantenido el ancho, pero deja el tirador por debajo del mínimo que el propio tema
   impone y es exactamente el tipo de rodeo que esta capa no debe hacer.
2. **`IconButton`, no `VerticalDragHandle`.** Existe en material3 1.5.0-alpha28 (4×48 dp, 12×52 al
   pulsar), pero no se usa: es un tirador especializado que no admite descripción propia por
    función y que dibujaría una forma distinta a la que el resto de la tabla ya muestra.
3. **`Icons.Filled.DragHandle`, rotado 90° para el tirador vertical.** El nombre automirror no
   existe; `DragHandle` y `DragIndicator` están solo en `material-icons-extended`, que ya está
   declarado, así que no hay dependencia nueva.
4. **Tres descripciones distintas, no una compartida.** Una sola clave habría hecho que los tres
   tiradores se anunciaran igual, y el de la barra no abre un menú: mueve la tabla.
5. **No se toca el carril más allá del ancho.** La 005 decidió que el carril existe para que el
   tirador no coma texto; aquí solo se ensancha para que quepa el botón.

## Criterios de finalización

- [ ] `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'` termina en verde
      **sin editar** `TableSelectionGeometryTest.kt`: sus constantes `gutter` y `handleWidth` son
      locales y no dependen del carril de producción.
- [ ] `./gradlew :app:compileDebugUnitTestKotlin` termina sin errores.
- [ ] `grep -c "clickable" ` sobre el cuerpo de `NotionSelectionHandle` devuelve 0.
- [ ] `grep -rn "LocalMinimumInteractiveComponent" app/src/main/java/com/example/ui/` no devuelve nada.
- [ ] Las tres claves `table_row_actions`, `table_column_actions` y `table_move_handle` aparecen
      en los nueve `strings.xml`.
- [ ] `./gradlew :app:mergeDebugResources` termina en verde (detecta el apóstrofo crudo).
- [ ] `./gradlew :app:assembleDebug` termina correctamente.
- [ ] `git diff -- app/src/main/java/com/example/data/model` está vacío.
- [ ] La memoria del proyecto registra los siete defectos del vídeo que quedan **sin corregir**.
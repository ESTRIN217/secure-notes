# Plan — Spec 007 · Tiradores del bloque de tabla

Spec: `specs/007-tiradores-tabla/spec.md` · Suelo: `specs/005-tabla-seleccion/` · Madre: `specs/001-limites-constitucion/`

Cubre RF-42 a RF-45 y RNF-16 a RNF-18, más los heredados RF-3, RF-19, RF-20, RNF-4, RNF-5 y RNF-6.

## 1. Archivos y responsabilidades

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/example/ui/EditableTableBlock.kt` | **Único archivo de producción.** `NotionSelectionHandle` pasa a ser un `IconButton` de 48×48 dp; los tres call sites pasan descripción; las constantes pasan a 48 dp. |
| `app/src/main/res/values*/strings.xml` (9) | Tres claves nuevas. |
| `app/src/test/java/com/example/TableSelectionGeometryTest.kt` | **No se toca.** Verificado: sus constantes `gutter = 24f` (línea 202) y `handleWidth = 24f` (línea 241) son locales y no leen nada de producción, así que el carril puede pasar a 48 dp sin tocar la suite. |
| `MEMORY.md` | T8. |

No hay archivo nuevo y no hay lógica pura nueva que extraer: el cambio es declarativo (un
composable por otro) y las medidas las sigue aportando `ui/table/TableSelectionGeometry.kt`, que
no cambia. Añadir una función pura aquí sería inventarse trabajo, no cubrir un requisito.

## 2. El composable del tirador

`NotionSelectionHandle` conserva su nombre, su firma pública y su parámetro `isVertical`, para no
tocar los tres call sites más de lo necesario. Cambia el cuerpo:

- Se elimina el `Box` con `size`, `background`, `border`, `clip`, `clickable` y el `Text` de «⋮⋮».
- Se construye un `IconButton` cuyo `Icon` es `Icons.Filled.DragHandle`, con `Modifier.rotate(90f)`
  cuando `isVertical`, porque en esta versión el icono es horizontal y los tiradores de fila y de
  barra son verticales.
- **Sin `Modifier.size`**: verificado en el bytecode de material3 1.5.0-alpha28, un `size` externo
  lo anula `minimumInteractiveComponentSize()` desde dentro y el `IconButton` mide 48×48 igual.
- `selected` deja de pintar el borde y pasa al tinte del icono (`IconButtonDefaults.iconButtonColors`),
  que es lo que Material usa para el estado seleccionado.
- Se añade `contentDescription: String`, que el composable resuelve con `stringResource` en los
  tres call sites y le pasa ya resuelta.

`HandleWidth` y `HandleHeight` pasan a 48 dp, `ColumnHandleHeight` también a 48 dp (el tirador de
columna es ahora cuadrado) y `TableRowGutter` sigue siendo `HandleWidth`, así que el carril queda
en 48 dp sin tocar una línea más.

## 3. Orden de los cambios y cómo se comprueba cada uno

1. **Las tres claves en los nueve locales** (`RNF-17`). Se comprueba con
   `grep -rc "table_row_actions" app/src/main/res/values*/strings.xml` → 1 en los nueve, y
   `./gradlew :app:mergeDebugResources` en verde, que es lo que detecta el apóstrofo crudo.
2. **El composable y las constantes** (`RF-42`, `RNF-16`). Se comprueba con `./gradlew :app:compileDebugKotlin`
   y con `grep -rn "LocalMinimumInteractiveComponent" app/src/main/java/com/example/ui/` vacío.
3. **Los tres call sites** (`RF-43`, `RF-45`). Se comprueba leyendo el diff: las tres
   descripciones son distintas entre sí, los tres `onDragModifier` siguen ahí y los dos
   `onClick` siguen abriendo su `DropdownMenu`.
4. **Suite pura y build** (`RF-44`). `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'`
   en verde **sin editar la prueba** — es la comprobación de que el carril no estaba atado a 24 dp
   en ningún sitio — y `./gradlew :app:assembleDebug` en verde.

El orden importa poco porque todo vive en el mismo archivo, pero poner las claves primero evita
tener el código roto mientras se compila.

## 4. Decisiones y alternativas descartadas

**Reescribir los tres tiradores por separado, en vez de tocar el composable compartido.**
Descartado: los tres miden hoy lo mismo, se colocan con la misma geometría y arrastran igual.
Copiar el código tres veces sería tres sitios que se desincronizan.

**Dejar `NotionSelectionHandle` con el `Text` de «⋮⋮» y solo agrandarlo.** Descartado: agrandar la
caja sin cambiar el táctil deja 48 dp de hueco con 24 dp de botón, que es el problema disfrazado.

**Bajar el carril a menos de 48 dp para no perder ancho.** Descartado: el carril existe justamente
para que el tirador no toque texto (RF-29-bis de la 005); con un tirador de 48 dp, un carril
menor lo devolvería al borde del texto.

**Reutilizar `drag_to_reorder`.** Descartado: es la descripción de los bloques del editor, no de
la tabla, y no dice qué hace el tirador.

**Bajar el botón de la barra a 24 dp con el escape hatch, para que no crezca en ancho.** Descartado
por decisión de la persona usuaria: 48 dp de verdad en los tres. El crecimiento en ancho de la
barra es el precio, y no hay coste en alto porque la barra ya contenía un `IconButton` de 48×48.
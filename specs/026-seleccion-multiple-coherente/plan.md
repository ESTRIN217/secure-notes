# Spec 026 — Plan

> Adjunto a `spec.md` (spec 026, **aprobada**). Los RF/RNF se citan tal cual. El plan describe **cómo**, con su
> alternativa descartada; los nombres de fichero son los reales del repo.

## Alcance de ficheros

### `:app` — nuevo (puro)

- **`app/src/main/java/com/example/ui/NoteSelection.kt`** — **puro**: no importa `android` ni `androidx`, solo
  `kotlin.collections`. Es la pieza única que decide la selección (RNF-107). Precedente de «lógica de interfaz
  pura dentro de `ui`»: `ui/editor/prepareNoteBlockMove` (`NoteBlockMovePlanTest`).
  - `fun isActive(selected: Set<Int>): Boolean` → `selected.isNotEmpty()`. Es la definición **única** de
    «selección activa» (RF-179) que comparten la barra, el toque, la pulsación larga y el atrás.
  - `fun toggle(selected: Set<Int>, noteId: Int): Set<Int>` → `if (noteId in selected) selected - noteId else
    selected + noteId`. Es la alternancia que usan toque (RF-173) y pulsación larga (RF-175).
  - **Alternativa descartada:** dejar las dos comprobaciones escritas en cada lambda de `MainListScreen`. Es lo
    que hay hoy; produce el defecto que motiva la spec (toque y pulsación larga con sentidos opuestos) y deja
    la decisión sin prueba.

### `:app` — modificado

- **`app/src/main/java/com/example/ui/MainListScreen.kt`**:
  - **`onClick` de las dos llamadas a `NoteCardItem`** (`:1092` en cuadrícula y `:1159` en lista): pasan de
    `if (selección no vacía) vaciar` a `if (NoteSelection.isActive(...)) NoteSelection.toggle(...)`, con
    `else if` para el resto (RF-173, RF-174). Un solo nivel de `if/else` y guard clauses, como pide la
    convención.
  - **`onLongClick` de las dos llamadas** (`:1104` y `:1171`): el `if/else` anidado que hoy alterna a mano se
    sustituye por `NoteSelection.toggle`, sin cambiar el comportamiento (RF-175, RNF-111).
  - **`BackHandler`** (`:165`): su condición `enabled` suma `NoteSelection.isActive(selectedNoteIds)` y su
    cuerpo empieza por cerrar el modo cuando lo está; si no, hace lo de siempre (RF-177, RF-178).
  - **Barra superior** (`:376`, `:464`, `:494`) y **visibilidad del botón flotante** (`:666`): la comprobación
    literal deja de escribirse y se usa `NoteSelection.isActive(...)` (RF-179).
  - **No se toca**: la X de cerrar (`:466`), las acciones por lotes (`:494`–`:660`), el `AlertDialog` de borrado,
    la hoja de compartir, la etiqueta por lotes, ni el modo de reordenación. Ya limpian la selección al
    terminar.
- **`app/src/test/java/com/example/NoteSelectionTest.kt`** — nuevo; pruebas **puras** de la pieza (RNF-107).
- **`app/src/test/java/com/example/NoteSelectionInvariantsTest.kt`** — nuevo; invariantes **puros** por lectura
  de fuente (patrón de `StorageEditorFilesInvariantsTest`). Cubren el **cableado**, que las pruebas puras de la
  decisión no ven: la pantalla usa la pieza en los cuatro sitios y no vuelve a escribir la comprobación
  (RF-179), y el atrás cierra el modo (RF-177).

**No se toca la interfaz.** Ni un icono, ni un orden de acciones, ni un texto: la X ya existe (RNF-109) y la
barra superior se pinta igual. No se toca ningún `strings.xml` (RNF-109) ni ningún `build.gradle.kts` (RNF-108).

## Funciones puras (con `hoy` como parámetro solo donde toca)

| Función | Módulo | Entrada | Salida | RF/RNF |
|---|---|---|---|---|
| `NoteSelection.isActive` | `:app` | `Set<Int>` de ids seleccionados | `Boolean` | RF-179 |
| `NoteSelection.toggle` | `:app` | `Set<Int>` y el id de una nota | `Set<Int>` nuevo | RF-173, RF-175, RF-176 |

El `BackHandler` y los `onClick`/`onLongClick` **no** son puros (son lambdas de una `@Composable`), así que no
llevan prueba unitaria; su decisión está extraída a las funciones puras de la tabla y su **cableado** se
comprueba con invariantes.

## Algoritmo en pseudocódigo

```
# RF-179 — una sola definición de «selección activa»
isActive(seleccion):  return seleccion.isNotEmpty()

# RF-173/175/176 — alternar es la misma acción en las dos formas
toggle(seleccion, id):
    if id in seleccion: return seleccion - id        # quitarla (y si era la única, el modo termina)
    else:               return seleccion + id        # añadirla

# RF-173/174 — el toque: en modo selección alterna; fuera, hace lo de siempre
alTocar(nota):
    if isActive(seleccionada):  seleccionada = toggle(seleccionada, nota.id)   # NO abre
    elif seccion != PAPELERA:   abrirEditor(nota.id)
    else:                       restaurarDePapelera(nota)

# RF-175/RNF-111 — la pulsación larga: alterna; en la papelera, nada
alMantenerPulsado(nota):
    if seccion != PAPELERA: seleccionada = toggle(seleccionada, nota.id)

# RF-177/178 — el atrás: el modo se cierra antes que la navegación
alPulsarAtras():
    if isActive(seleccionada): seleccionada = vacío            # ni sección, ni cajón, ni salir
    else:                      comportamiento de siempre (volver a inicio, cerrar cajón)
```

## Decisiones justificadas (con su alternativa descartada)

1. **El toque alterna en vez de vaciar.** Alternativa descartada: mantener «tocar cualquier nota sale del
   modo» (lo de hoy). Es el defecto reportado: destruye la selección y obliga a dos toques para leer una nota;
   además contradice a la pulsación larga de la misma pantalla.
2. **Una pieza pura para las dos decisiones.** Alternativa descartada: escribir la alternancia en cada lambda.
   Con la regla duplicada en cuatro sitios (dos `onClick`, dos `onLongClick`), el toque y la pulsación larga
   ya divergieron una vez; con la pieza compartida (RF-179) no pueden divergir y la regla tiene prueba.
3. **Atrás cierra el modo antes que la sección (RF-178).** Alternativa descartada: cerrar el modo y navegar
   en el mismo atrás. Dejaría al usuario en otra sección sin haber pedido salir, y rompería el orden natural
   de «primero la capa transitoria».
4. **La papelera no gana modo selección (RNF-111).** Alternativa descartada: habilitar la pulsación larga
   allí, porque la barra y el `AlertDialog` ya contemplan el borrado permanente. Es alcance nuevo y la
   petición no lo pide; la rama de la papelera se conserva intacta.
5. **La comprobación literal `selectedNoteIds.isNotEmpty()` desaparece de la pantalla.** Alternativa
   descartada: usarla solo en las rutas nuevas y dejar el resto. Entonces «selección activa» tendría dos
   definiciones (una con nombre y otra literal) y el invariante no podría afirmar nada.

## Estrategia de tests (`./gradlew test`)

### 1. Pruebas **puras** de la decisión (`NoteSelectionTest`, sí corren aquí)

- `isActive(emptySet())` → `false`; `isActive(setOf(1))` → `true`.
- `toggle(emptySet(), 7)` → `{7}` (entrar en el modo con una sola nota).
- `toggle({1}, 2)` → `{1, 2}` (añadir la segunda — HU-1).
- `toggle({1, 2}, 1)` → `{2}` (quitar una habiendo otras — HU-2).
- `toggle({1}, 1)` → `emptySet()` (quitar la única → el modo termina — RF-176).
- `toggle` es su propio inverso: aplicarlo dos veces con el mismo id devuelve el conjunto original.
- `toggle` **no** muta el conjunto de entrada (se compara el original tras la llamada).
- Alternar un id ajeno a la selección no toca los demás ids.

### 2. Invariantes de `:app` (`NoteSelectionInvariantsTest`, puros por lectura de fuente)

Con las dos reglas del proyecto: cada recorrido **quita comentarios** antes de buscar y **se autocomprueba**
(lee el fichero y exige un mínimo de líneas). Cubren el **cableado**:

- **RNF-107** — `NoteSelection.kt` **no** importa `android` ni `androidx`.
- **RF-179** — `MainListScreen.kt` **no** contiene `selectedNoteIds.isNotEmpty()` ni
  `selectedNoteIds.isEmpty()`: todas las decisiones pasan por la pieza.
- **RF-173** — en el tramo de las llamadas a `NoteCardItem` (desde la primera llamada hasta `fun
  NoteCardItem(`), la alternancia aparece en los **dos** `onClick` y en los **dos** `onLongClick` (cuatro
  usos de `NoteSelection.toggle`), y el comentario «Exit selection mode when tapping any note card» ya no
  está.
- **RF-177** — el `BackHandler` menciona `NoteSelection.isActive(selectedNoteIds)` en su condición y en su
  cuerpo, y el cuerpo limpia la selección (`selectedNoteIds = emptySet()`).

### 3. Honestidad sobre lo que **no** se prueba aquí

- Que el gesto real (toque, pulsación larga, atrás) se comporte así **en pantalla** no se puede probar sin
  Robolectric, que no corre en esta plataforma: va al guion de dispositivo.
- Cero claves nuevas (RNF-109) y cero `build.gradle.kts` tocados (RNF-108) se comprueban con `git diff` en las
  tareas, no con una prueba (una prueba que fijara el número de claves mentiría en cuanto otra spec añada una).

### 4. Compilación y suite

`./gradlew :app:compileDebugKotlin`, `./gradlew :app:test --continue` contra la línea base **786/41**: las
mismas clases rojas de siempre, ninguna nueva (RNF-110).

## Guion de verificación en dispositivo (RF-173…RF-179)

1. En la sección de inicio, **mantener pulsada** una nota: se activa la barra con «1» y la X.
2. **Tocar otra nota**: la barra pasa a «2» y la segunda nota se marca; el editor **no** se abre.
3. **Tocar la primera** (ya marcada): la barra vuelve a «1». Tocar de nuevo la que queda: la barra vuelve a
   ser la de la sección (el modo se cierra solo).
4. Repetir con **pulsación larga** sobre la segunda nota: el resultado es el mismo que con el toque.
5. Repetir la selección y pulsar **atrás**: la barra vuelve a la de la sección y la app **no** se cierra ni
   cambia de sección.
6. Ir a Favoritos, mantener pulsada una nota y pulsar **atrás**: primero se cierra el modo; un segundo atrás
   vuelve a Inicio.
7. Sin ninguna nota seleccionada: tocar una nota **abre** el editor; en la papelera, tocar una nota la
   **restaura**; mantener pulsada una nota en la papelera **no** hace nada.
8. Seleccionar dos notas y usar Fijar/Favorito/Archivar/Compartir/Borrar: la acción se aplica a las dos y el
   modo se cierra.

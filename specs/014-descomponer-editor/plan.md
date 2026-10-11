# Plan — Spec 014: El editor de notas, partido en capas

Spec: `specs/014-descomponer-editor/spec.md` · Estado: aprobada

RF que cubre cada parte: **RF-84** (estado muerto), **RF-85** (un guardado), **RF-86** (no calcular
lo que no se pinta), **RF-87** (lógica comprobable fuera de la interfaz), **RF-88** (tamaño como
invariante), **RF-89** (los cinco insertadores, una decisión).

## 1. Línea base, medida antes de tocar nada

| Medida | Valor |
|---|---|
| `NoteEditorScreen.kt` | 3504 líneas |
| función `NoteEditorScreen` | líneas 116-3496 (**3380**) |
| declaraciones `remember` | 95 |
| funciones locales | 49 |
| `LaunchedEffect` / `DisposableEffect` | 8 / 3 |
| estado muerto | 4 declaraciones que nadie leen + 3 que nunca se asignan |
| llamadas al guardado completo | **8** — 7 de la nota actual, 1 de otra nota |
| insertadores de bloque con el mismo preámbulo | 5 |
| funciones locales de más de 20 líneas | 10 |
| `:app:compileDebugKotlin` | verde, **sin avisos** |
| pruebas comprobables (las que corren aquí) | **33 clases, 411 tests, 3 fallos preexistentes** |
| pruebas de emulador (no corren aquí) | 9 clases, 40 tests, 38 fallos por falta de runtime nativo |
| total `:app` | 42 clases, 451 tests, 41 fallos |

Los **3** fallos de las comprobables son preexistentes y ajenos a esta entrega, y su nombre queda
fijado para que una regresión nueva no se esconda detrás de ellos:
`HtmlExportMediaTest.legacy item markup migrates to checklist html`,
`OnDeviceModelSourceTest.every catalog model exposes a hugging face source` y
`OnDeviceModelSourceTest.source label drops the scheme and the file`.

El recuento de pruebas comprobables se mide en **T1** y se fija ahí, porque la suite tiene dos mitades
y solo una corre en esta plataforma.

## 2. Archivos y responsabilidades

Paquete nuevo `com.example.ui.editor`, siguiendo el precedente de `ui/chat/` y `ui/table/`: la
lógica comprobable **no importa nada de composición**, a propósito, porque es la única forma de
ejercitarla sin emulador.

### 2.1 Lógica comprobable (cero `androidx.compose`) — RF-87

| Fichero | Qué lleva | Viene de |
|---|---|---|
| `SegmentTagTransforms.kt` | `styleApplies`, `applyTransformForTag`, `removeTransformForTag` | 436-480 |
| `ToolbarStyleState.kt` | `computeToolbarStyle(...)` | 368-434 |
| `NoteSearchMatches.kt` | `buildMatchRanges(...)` | 302-356 (el núcleo) |
| `BlockFileKinds.kt` | `isTextLikeFile(mime, ext)` | 1424-1431 |
| `BlockInsertPlan.kt` | `insertPlanFor(...)` | el preámbulo repetido 5 veces |

Las cinco están hechas (T2). `BlockInsertPlan` se cuenta como parte de T2 aunque la usen los
insertadores de T4: escribirla con su prueba antes de tocar los cinco era la mitad del trabajo.

### 2.2 Mandos

**Corrección de alcance, hecha al empezar T4.** El plan pedía sacar a ficheros propios
`insertBlockAtCursor`, mover/borrar/convertir, `handleSlashSelect` y `moveBlockToAnotherNote`. Al leer
el código se vio que los tres últimos **no son separables** sin convertir el refactor en otra cosa:
`handleSlashSelect` cierra sobre 14 variables de la pantalla y `moveBlockToAnotherNote` sobre 17
(bloques, historial, nota destino, permisos de adjuntos, alcance del coroutine). Sacarlos obligaría
a inventar un contenedor de estado —que es exactamente la spec 015— o a pasar veinte argumentos por
función, que es peor que lo que hay ahora.

Lo que **sí** es separable es el núcleo puro de cada uno: mueve en la lista, quita, convierte. Eso
tiene frontera con nombre, se prueba sin pantalla y acorta a quien lo llama.

| Fichero | Qué lleva | Viene de |
|---|---|---|
| `BlockCommands.kt` | `headingTypeForTag`, `moveBlockBy`, `deleteBlockAt`, `convertBlockAt` — solo listas | 375-388, 721-761 |
| `SegmentTagApplier.kt` | `applyTag`, `applyTagWithVal` (partidas en ≤20 líneas) | 1650-1769 |
| `SlashCommandRouter.kt` | `handleBlockCommand` | 1465-1573 |
| `NoteFileImport.kt` | `handlePickedFile`, `openExternalFile` | 1363-1463 |

Y **dentro** del composable, que es donde puede vivir sin inventar nada: `insertBlockAtCursor(block)`
sustituye a los cinco insertadores, que eran **byte a byte idénticos** salvo la construcción del
bloque. Se queda local por lo mismo que `persistNote`: cierra sobre el estado, y sacarlo es la 015.

### 2.3 Superficie

| Fichero | Qué lleva | Viene de |
|---|---|---|
| `NoteEditorDialogs.kt` | los seis diálogos (medio ×2, URL, marcador, tabla, acción de enlace) | 2188-2598 |
| `NoteEditorSheets.kt` | paleta de color y hoja de voz/archivo | 2744-2891, 2917-3253 |
| `NoteEditorAiSurfaces.kt` | panel flotante, hoja de contexto y diálogo de streaming | 2651-2742, 3255-3339 |

## 3. Funciones puras, con «hoy» como parámetro

Las cuatro tienen la misma forma: **hoy** viven dentro del composable y se calculan en cada
recomposición; **después** reciben lo que necesitan y devuelven el resultado.

### 3.1 `computeToolbarStyle` — RF-86

```
computeToolbarStyle(
    blockType: BlockType?,      // el tipo del bloque activo
    segments: List<TextSegment>,// sus tramos
    cursor: Int,                // dónde está el cursor
    collapsed: Boolean,         // ¿selección vacía?
    pending: TextSegment?       // estilo de lo que se está escribiendo
): Set<String>
```

Devuelve **solo** los estilos activos. Hoy además calcula dos colores que **nadie lee**
(`activeFontColor`, `activeBgColor`), y lo hace con cuatro llamadas a la conversión de color en cada
recomposición; se borran (decisión 2).

### 3.2 `buildMatchRanges` — RF-87

```
buildMatchRanges(
    cleanText: String,
    query: String,
    fullWord: Boolean,
    caseSensitive: Boolean,
    toRawIndex: (Int) -> Int    // traduce índice de texto limpio a índice del original
): List<IntRange>
```

Devuelve `IntRange`, **no** el tipo de rango de texto de la interfaz: así el fichero no importa
composición. La reentrancia que hoy tiene el efecto (se relanza porque su propia salida cambia su
propia clave) se conserva tal cual; se documenta, no se arregla (decisión 5).

### 3.3 `insertPlanFor` — RF-89

```
insertPlanFor(activeIndex: Int, activeBlock: DataBlock?, blockCount: Int): InsertPlan
InsertPlan(replaceInPlace: Boolean, index: Int)
```

Es el preámbulo que hoy se copia en los cinco insertadores: si el bloque activo está vacío, se
sustituye; si no, se inserta detrás. **Dos desenlaces, una función**, con prueba para los dos.

### 3.4 `persistNote` — RF-85

Un único camino de guardado completo. Detalle que decide que no haya cambio de comportamiento
(decisión 4):

| Sitio | Hoy | Después |
|---|---|---|
| 975, 1015 (IA) | `saveNote(… saveContent …)` | `persistNote(contentForSave())` |
| 1396, 1583, 3194 | `saveNote(… contentForSave() …)` | `persistNote(contentForSave())` |
| 1336 (ON_STOP) | `saveCurrentNote()` con guarda | igual |
| 1601 (salir) | guarda propia equivalente a `hasContentToSave()` | `saveCurrentNote()` |
| **916** (`moveBlockToAnotherNote`) | `saveNote` de **otra nota**, con sus campos | **se queda**: no es un duplicado |

Las 5 primeras **no** ganan guarda nueva: `persistNote` no lleva guarda, y quien la tenía la conserva.
Las dos últimas ya la tenían y es la misma. **916 no se toca**, y por eso el invariante de RF-85 baja
de 8 a **2** y no a 1: una es el camino único de la nota actual y la otra es la nota destino del
bloque movido, que son cosas distintas por diseño.

## 4. Algoritmo — el preámbulo que se repite cinco veces

Lo que hacen hoy `insertAudioBlock`, `insertFileBlock`, `insertVideoBlock`, `insertImageBlock` y
`insertDrawingBlock` (614-827), escrito una vez:

```
fun insertBlockAtCursor(type, content, meta) =
    plan   = insertPlanFor(índice activo, bloque activo, tamaño)
    bloque = DataBlock(type, content, meta)
    lista  = bloques.toMutableList()
    lista[plan.index] = if (plan.replaceInPlace) bloque else { lista.add(plan.index, bloque); lista }
    aplicarCambio(lista)
    sincronizarCursorCon(plan.index)
```

## 5. Interfaz

- `persistNote(content: String)` y `saveCurrentNote()` son **locales** todavía: cierran sobre las
  nueve variables de estado, y hacerlas upheaval público obligaría a pasar las nueve. Se convierten
  en funciones de nivel superior cuando el estado agrupado (spec 015) les dé un hogar.
- Los diálogos y las hojas reciben **parámetros con nombre y devuelven eventos**, nunca estado: es
  la línea que ya trazó `ui/chat/`.
- La hoja de voz/archivo es la que más parámetros pide (unos veinte). Para no repartir veinte
  argumentos, se le pasa **un** receptor de operaciones con las acciones que necesita, que es el
  patrón de la tabla del §3 de `GUIDELINES.md` (ISP: callbacks de un solo método, no interfaces anchas).

## 6. Decisiones y alternativas descartadas

1. **Paquete `ui/editor/` y no más archivos sueltos en `ui/`.** `ui/` ya tiene 60 ficheros y el
   editor son 20 de ellos. Un paquete lo hace navegable. Alternativa descartada: dejarlo en `ui/`,
   que es donde crecería la lista.
2. **Borrar el cálculo de colores de la barra en vez de terminarlo.** Decidido por quien decide: es
   código muerto. Se comprueba primero que no lo lee nadie — y no lo lee. La botón de color
   terminado, si hace falta, es otra spec.
3. **`Set<String>` y no un tipo con nombre** para el estado de la barra. Con dos campos muertos fuera
   queda un valor; envolverlo en una clase sería una abstracción sin segundo consumidor
   (`GUIDELINES.md` §6, YAGNI).
4. **`persistNote` sin guarda.** Añadir la guarda de «¿hay algo que guardar?» a los cinco sitios que
   hoy no la tienen sería un cambio de comportamiento disfrazado de refactor (RNF-39). La guarda se
   queda donde ya estaba.
5. **No tocar el agrupamiento de estado ni las barras.** Es lo que queda del composable, pero el
   estado se comparte **por identidad** con el editor de bloques (`pendingInsert`, `pendingSelection`,
   `pendingTagInsert` se le pasan como objetos `MutableState`, y `toolbarParseResult` lo escribe el
   hijo y lo lee la barra). Ese cruce no se toca hasta que el resto esté quieto y en verde.
6. **El invariante de tamaño nace en rojo.** Se pone el umbral un archivo por encima del tamaño real
   y se ve fallar; solo después se baja al valor true. Un invariante que nunca falla no está probado
   (`MEMORY.md`: «una prueba que no se ha visto fallar no está probada»).
7. **Los `remember` de dentro de `if (showVoiceFileSheet)` se quedan dentro del `if`.** Al extraer la
   hoja a un composable propio, el `if` **no** se mueve dentro del hijo: si el hijo se autocondiciona,
   la grabación en curso sobrevive al cierre de la hoja, que es un cambio de comportamiento.

## 7. Estrategia de pruebas

`./gradlew test` **completo no está verde ni puede estarlo aquí**: Robolectric no tiene runtime nativo
para `linux/aarch64` y falla en `DefaultNativeRuntimeLoader`. La línea base que se toma es **la de las
pruebas que sí corren**, y se declara explícitamente para que nadie la lea como «la suite completa
está verde».

| Tarea | Pruebas | Dónde |
|---|---|---|
| T1 | invariante de tamaño + búsqueda de estado muerto | `:app`, puro |
| T2 | `computeToolbarStyle`, las 3 transformaciones, `buildMatchRanges`, `isTextLikeFile` | `:app`, puro |
| T3 | — (borrado de código muerto, verificado por el invariante de T1) | — |
| T4 | `insertPlanFor`: los dos desenlaces | `:app`, puro |
| T5 | — (verificado por compilación y por el invariante) | — |
| T6-T7 | — (superficie; aquí no hay nada que ejercitar sin emulador) | — |

Los ficheros de lógica comprobable se comprueban con `grep -c '^import androidx.compose'` → **0**.

## 8. Riesgos y trampas conocidas

| Riesgo | Mitigación |
|---|---|
| Kotlin **no** admite referencias adelantadas a funciones locales | al mover, colocar **después** de lo que llama. Ya se rompió una vez (`MEMORY.md`: `Unresolved reference 'activeSegments'`) |
| `activeSegments` se invoca como **clave** de `remember` (374) | la clave se calcula antes del `remember`; al extraerse, la clave pasa a ser el valor ya calculado |
| `LaunchedEffect(1137)` termina en un `.collect` sin fin | nada posterior a ese punto se ejecuta: no envolverlo en `try/finally` |
| El efecto de búsqueda **se relanza a sí mismo** | se conserva; documentado, no arreglado |
| Las hojas viven **fuera** del `Box` con `padding(innerPadding)` | no moverlas dentro al extraerlas |
| Indentación de 8 espacios de más en 1618-1769 | se corrige **al** extraerse, y el diff se lee para no confundirlo con lógica |
| Lambdas `val x = { }` **no recordadas** (11) | se recuerdan al moverlas; sin eso el padre recompone más, que es empeorar |
| Un movimiento puede compilar en un orden y no en otro | Tarea por tarea, con `compileDebugKotlin` verde entre una y otra |
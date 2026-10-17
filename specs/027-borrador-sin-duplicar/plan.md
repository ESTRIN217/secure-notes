# Spec 027 — Plan

> Adjunto a `spec.md` (spec 027, **aprobada**). Los RF/RNF se citan tal cual. El plan describe **cómo**, con su
> alternativa descartada; los nombres de fichero son los reales del repo.

## Alcance de ficheros

### `:app` — nuevo

- **`app/src/main/java/com/example/ui/DraftWrite.kt`** — la pieza de la escritura de un borrador (RNF-113). Tres
  cosas y ninguna otra:
  - `const val WRITE_IN_FLIGHT = -1` — la escritura está lanzada y esperando el id de la fila.
  - `fun draftWriteId(noteId: Int, writeState: Int): Int` → con qué id se escribe (RF-180).
  - `fun isDraftUnsaved(noteId: Int, writeState: Int, hasContent: Boolean): Boolean` → si hay algo que descartar
    (RF-182, RF-183).
  - `class DraftWriteGate(noteId: Int)` — quien **recuerda** el id, **serializa** las escrituras (RF-181) y marca
    «en vuelo». Sus dos reglas son las de arriba; lo único que añade es el `Mutex` y el estado observable
    (`mutableIntStateOf`), porque la bandera del aviso tiene que **reaccionar** a que la inserción termine.
    Precedente de un estado observable con sus reglas al lado: `NoteEditorInteractionState`.
  - **Alternativa descartada:** dejar la regla dentro del composable (es lo de hoy, en `NoteEditorScreen.kt:1090`).
    Es donde se escribió el defecto: `noteId == 0` parece una verdad sobre la nota y en realidad son dos hechos
    distintos, y sin pieza propia no hay ni dónde probar la regla ni un sitio donde se lea dos veces igual.

### `:app` — modificado

- **`app/src/main/java/com/example/ui/NoteEditorScreen.kt`**:
  - **`val draftWrite = remember(noteId) { DraftWriteGate(noteId) }`**, junto a los demás `remember` del principio.
    La clave es `noteId` porque al promoverse el borrador la pantalla se recrea con el id real (`key(tab.noteId)`,
    `NoteEditorTabsHost.kt:158`) y nace una puerta nueva, ya con fila.
  - **`hasContentToSave`** sube junto a `contentForSave` (`:752`): las dos son funciones derivadas del contenido, y
    `persistNote` la necesita. Kotlin no admite referencias adelantadas a **funciones** locales; el motivo ya está
    escrito en el comentario de `:748`.
  - **`persistNote(content, onWritten = {})`** — sigue siendo el único camino de guardado del editor (RF-85), pero
    **no recibe el id**: llama a `draftWrite.launch()`, recalcula la bandera del aviso en el acto (RF-182) y lanza
    `draftWrite.write { destino -> guardarConId(destino, contenido) }` en el ámbito del `ViewModel` (RNF-119).
  - **`guardarConId(destino, content)`** — la lista de campos de la nota, con el id que le toque. Es el bloque de
    14 líneas que hoy está dentro de `persistNote`.
  - **`draftGuard?.saveAction`** pasa de 17 líneas con su propia llamada a `saveNoteAndGetId(id = 0, …)` a **una**:
    `persistNote(contentForSave(), onSaved)`. Es el duplicado que produce la nota #2 (RF-184), y también 15 líneas
    menos en el monolito (RNF-116).
  - **`LaunchedEffect` de la bandera** (`:1089`) usa `isDraftUnsaved(noteId, draftWrite.state, hasContentToSave())`
    y añade `draftWrite.state` a sus claves, para que se recalcule cuando la inserción devuelva el id.
  - **Autoguardado** (`:1283`) y **segundo plano** (`:1120`) pasan de `noteId != 0` a
    `draftWriteId(noteId, draftWrite.state) != 0`: una nota nueva que ya tiene fila se actualiza como cualquier otra
    (RF-185), y una que no la tiene, sigue sin insertarse sola.
  - **No se toca**: `moveBlockToAnotherNote` (guarda **otra** nota, RNF-117), la carga y las migraciones de
    `LaunchedEffect(noteId)` (`:954`, `:994`), el guardado de la nota del comando «/» (`:1229`), el diálogo de
    barras, el historial o la selección.

### `:app` — pruebas

- **`app/src/test/java/com/example/DraftWriteTest.kt`** — nuevo; pruebas **puras** de las dos reglas y del candado
  (RNF-113). La del doble `insert` (RF-181) es la que reproduce el defecto reportado.
- **`app/src/test/java/com/example/EditorInvariantsTest.kt`** — modificado: `MAX_GUARDADOS` baja de 2 a 1 con su
  motivo nuevo (RNF-117), `MAX_LINEAS` baja al número real que quede (RNF-116), y un invariante nuevo vigila el
  **cableado**: que el editor escriba por `draftWrite`, que la bandera use `isDraftUnsaved` y que el autoguardado
  use `draftWriteId` (RF-186).

**No se toca la interfaz.** Ni un icono, ni un texto, ni un `strings.xml` (RNF-115), ni un `build.gradle.kts`
(RNF-114).

## Funciones puras

| Función | Módulo | Entrada | Salida | RF/RNF |
|---|---|---|---|---|
| `draftWriteId` | `:app` | id de la pestaña y estado de la escritura | id con el que escribir | RF-180 |
| `isDraftUnsaved` | `:app` | id de la pestaña, estado de la escritura, hay contenido | `Boolean` | RF-182, RF-183 |
| `DraftWriteGate.write` | `:app` | lambda `suspend (Int) -> Int` | id devuelto por el guardado | RF-180, RF-181 |

`DraftWriteGate` usa `mutableIntStateOf` y `Mutex`, así que **no** es pura y se prueba con `runBlocking` en la
misma clase de prueba: sin `Context`, sin `android` y sin Robolectric. El composable no lleva prueba unitaria; su
decisión está en las tres funciones de arriba y su **cableado** se comprueba con invariantes.

## Algoritmo en pseudocódigo

```
# RF-180 — con qué id se escribe. La pestaña manda; si es borrador, manda la fila si ya existe.
draftWriteId(noteId, estado):  return noteId != 0 ? noteId : (estado > 0 ? estado : 0)

# RF-182/RF-183 — ¿hay algo que descartar? Solo mientras no se haya lanzado ninguna escritura.
isDraftUnsaved(noteId, estado, hayContenido):
    return noteId == 0 && estado == 0 && hayContenido

# RF-180/RF-181 — la puerta: recuerda el id y escribe de una en una
persistNote(contenido):
    draftWrite.launch()                                    # estado = EN_VUELO (RF-182)
    banderaAviso = isDraftUnsaved(noteId, draftWrite.estado, hayContenido)
    viewModel.launchInViewModelScope {
        onWritten(draftWrite.write { destino ->            # candado: una escritura a la vez
            nuevo = guardarConId(destino, contenido)       # 0 = insertar; >0 = actualizar
            si destino == 0 && nuevo > 0: draftWrite.estado = nuevo
            nuevo
        })
    }

# RF-185 — el autoguardado, antes y después
autoguardado(dispara):
    si hay texto y draftWriteId(noteId, draftWrite.estado) != 0:  persistNote(contenidoActual())

# RF-184 — el «Guardar» del aviso: el mismo camino, y entrega el id para promover la pestaña
alPulsarGuardar(alTerminar):
    persistNote(contenidoActual()) { nuevo -> alTerminar(nuevo) }   # tabsManager.promoteDraft(nuevo)
```

## Decisiones justificadas (con su alternativa descartada)

1. **El editor recuerda el id de su propia escritura** en lugar de que la barra de pestañas se lo comunique.
   Alternativa descartada: promover la pestaña en cuanto hay fila (es decir, llamar a `promoteDraft` desde el
   editor). Es coherente, pero **recrea el editor a mitad de uso** —`key(tab.noteId)` cambia de `0` al id real— y
   con ello se pierden el cursor y el historial de deshacer. Recordar el id no cambia nada de la pantalla.
2. **Un `Mutex` en la puerta** y no «leer el id otra vez antes de escribir». Alternativa descartada: leer el estado
   fuera del candado, sin candado. Es la carrera que repro el reporte: `onAttachRecording` persiste y, un instante
   después, `handleSaveAndExit` persiste otra vez; las dos leen «no hay fila» e insertan. Con el candado, la
   segunda ve el id que dejó la primera.
3. **La bandera del aviso se recalcula dentro de `persistNote`, no solo en el `LaunchedEffect`.** Alternativa
   descartada: fiarse del efecto. El efecto llega un fotograma después, y `handleSaveAndExit` lee `hasUnsaved` en
   la línea siguiente: el diálogo saldría igual. Es el mismo patrón que el `BackHandler` de la spec 026, que
   obliga a declarar el estado por encima de su primer consumidor.
4. **Atrás guarda y sale, sin preguntar** (RF-182, decidido con quien Decide). Alternativa descartada: que el
   atrás **siempre** pregunte y no guarde antes. Habría que quitar el guardado actual de adjuntar ficheros y de la
   IA, y el aviso solo tiene sentido donde el contenido se pierde de verdad: cerrar la pestaña del borrador.
4. **Autoguardar en cuanto la nota tiene fila** (RF-185). Alternativa descartada: mantener la condición
   `noteId != 0` y arreglar solo el duplicado. Entonces, tras adjuntar algo a una nota nueva, seguir escribiendo no
   guarda hasta salir: el mismo defecto que se reporta, con otra forma.
5. **`isDraftUnsaved` incluye «hay contenido».** Sin ese término, una escritura lanzada sobre un borrador ya vacío
   dejaría el aviso puesto para siempre. Con él, vaciar la nota apaga el aviso, como hoy.
6. **El autoguardado sigue sin insertar borradores.** Alternativa descartada: insertar al dejar de escribir. Abrir
   el editor de una nota nueva y salir sin escribir crearía una nota vacía por sesión; el autoguardado insertar es
   alcance nuevo (ver RF-185 y dudas abiertas de la spec).

## Estrategia de tests (`./gradlew test`)

### 1. Pruebas **puras** de las dos reglas (`DraftWriteTest`, sí corren aquí)

- `draftWriteId(0, 0)` → `0`: un borrador sin fila inserta.
- `draftWriteId(0, WRITE_IN_FLIGHT)` → `0`: en vuelo sigue siendo un borrador.
- `draftWriteId(0, 7)` → `7`: con fila, actualiza esa fila (RF-180).
- `draftWriteId(5, 0)` → `5` y `draftWriteId(5, 7)` → `5`: la pestaña manda siempre.
- `isDraftUnsaved(0, 0, true)` → `true` (RF-183).
- `isDraftUnsaved(0, 0, false)` → `false`: un borrador vacío no pregunta.
- `isDraftUnsaved(0, WRITE_IN_FLIGHT, true)` → `false` (RF-182): lo que se está escribiendo no se descarta.
- `isDraftUnsaved(0, 7, true)` → `false`: ya está en la base de datos.
- `isDraftUnsaved(5, 0, true)` → `false`: una nota que ya existía no es un borrador.

### 2. Pruebas del candado (`DraftWriteTest`, sin Robolectric, con `runBlocking`)

- **El defecto reportado, con nombre**: dos escrituras seguidas de un borrador dejan **un** `insert` y un
  `update` sobre el id devuelto (RF-180, RF-181).
- Dos escrituras **concurrentes** (`async` + `awaitAll`): un solo `insert`.
- La escritura que falla (`0`) deja el borrador como estaba: el siguiente intento vuelve a insertar.
- Una nota con `noteId = 5` nunca inserta: las dos escrituras van a `5`.
- `launch()` pone `WRITE_IN_FLIGHT` y `isDraftUnsaved` se apaga **antes** de que la escritura termine (RF-182).

### 3. Invariantes de `:app` (`EditorInvariantsTest`, puros por lectura de fuente)

Con las dos reglas del proyecto: cada recorrido **quita comentarios** antes de buscar y **se autocomprueba**.
Cubren el **cableado**, que las pruebas puras de la decisión no ven:

- **RNF-113** — `DraftWrite.kt` **no** importa `android` ni `androidx.compose` salvo `runtime` (el estado
  observable), y las dos reglas viven ahí y no en la pantalla.
- **RF-186** — `NoteEditorScreen.kt` **no** vuelve a escribir `noteId == 0 &&` para la bandera del aviso, y usa
  `isDraftUnsaved(noteId, draftWrite.state, hasContentToSave())` en el efecto **y** en `persistNote`.
- **RF-184** — `draftGuard?.saveAction` es una línea que llama a `persistNote(contentForSave(), onSaved)` y **no**
  vuelve a escribir la lista de campos ni un `id = 0`.
- **RF-185** — el autoguardado y el `ON_STOP` usan `draftWriteId(noteId, draftWrite.state) != 0`.
- **RNF-117** — queda **una** llamada `viewModel.saveNote(`, la de `moveBlockToAnotherNote`.

### 4. Honestidad sobre lo que **no** se prueba aquí

- Que el editor abra el aviso y que el botón de atrás no lo haga **en pantalla** no se puede probar sin
  Robolectric, que no corre en esta plataforma: va al guion de dispositivo.
- Cero claves nuevas (RNF-115) y cero `build.gradle.kts` tocados (RNF-114) se comprueban con `git diff` en las
  tareas, no con una prueba.

### 5. Compilación y suite

`./gradlew :app:compileDebugKotlin`, `./gradlew :app:test --continue` contra la línea base **799/41**: las mismas
clases rojas de siempre, ninguna nueva (RNF-118). Los dos invariantes con número exacto (`MAX_LINEAS`,
`MAX_GUARDADOS`) se ajustan **al valor real que queda**, y el ajuste va escrito con su motivo.

## Guion de verificación en dispositivo (RF-180…RF-186)

El de la spec, copiado al final de `spec.md`: ocho pasos, con nota nueva + atrás, con barra de pestañas, con
adjunto, con IA, y con una nota que ya existía.
# Spec 027 — Tareas

> Orden de dependencia. Una tarea por sesión de trabajo (≤ 30 min). Los tests van **primero**.

- [x] **T1. Pruebas puras de las dos reglas, en rojo.** RNF-113, RF-180, RF-182, RF-183
  - Hecho cuando: existe `app/src/test/java/com/example/DraftWriteTest.kt` con los nueve casos de `plan.md` §
    Estrategia 1 (`draftWriteId` con los cinco casos e `isDraftUnsaved` con los cuatro) y
    `./gradlew :app:compileDebugUnitTestKotlin` **falla** porque `com.example.ui.DraftWrite` todavía no existe.
  - Hecho cuando además: el fallo es **solo** por la referencia nueva, para que la regresión ya esté medida antes
    de tocar la implementación.
  - **Hecho.** Rojo por las **13 referencias** nuevas (`draftWriteId`, `isDraftUnsaved`, `WRITE_IN_FLIGHT`) y por
    nada más. **9 casos.**

- [x] **T2. La pieza pura.** RNF-113, RF-180, RF-182, RF-183, RF-186
  - Hecho cuando: `app/src/main/java/com/example/ui/DraftWrite.kt` existe con `WRITE_IN_FLIGHT`, `draftWriteId`,
    `isDraftUnsaved` y `DraftWriteGate` (`launch`, `write`, `state`), **sin** importar `android`, y
    `./gradlew :app:testDebugUnitTest --tests '*DraftWriteTest'` pasa en verde.
  - **Hecho.** `9/0` en verde. La puerta entró ya en este paso; T4 la revisa con sus pruebas.

- [x] **T3. Pruebas del candado, en rojo.** RF-180, RF-181, RF-182
  - Hecho cuando: `DraftWriteTest` tiene los cinco casos de `plan.md` § Estrategia 2 —**el doble `insert` con
    nombre**, las dos escrituras concurrentes, la escritura que falla, la nota que ya existía y `launch()` antes de
    terminar— y los primeros están en rojo porque `DraftWriteGate.write` todavía no existe o no serializa.
  - Hecho cuando además: el caso que reproduce el defecto está escrito como el primero, para que se lea antes que
    el resto.
  - **Hecho.** El caso del defecto es el primero, con el nombre puesto: «dos escrituras seguidas de un borrador
    dejan una sola fila». Los cinco en rojo por `DraftWriteGate` inexistente.

- [x] **T4. La puerta.** RF-180, RF-181, RF-182, RF-184
  - Hecho cuando: `DraftWriteGate.write` serializa con `Mutex`, lee el id **dentro** del candado, guarda el id
    devuelto solo cuando insertó (`destino == 0 && nuevo > 0`) y devuelve siempre el id del guardado;
    `launch()` pone `WRITE_IN_FLIGHT` solo si `noteId == 0`; y `./gradlew :app:testDebugUnitTest --tests
    '*DraftWriteTest'` pasa **todo** en verde.
  - Hecho cuando además: si `mutableIntStateOf` resultara no funcionar sin Robolectric, el estado observable se
    mueve a la pantalla y la puerta conserva solo el `Mutex` y el id, con el motivo escrito en el KDoc.
  - **Hecho.** `14/0` en verde. **`mutableIntStateOf` sí funciona en una prueba pura de JVM**: el sistema de
    instantáneas de Compose no necesita Android, así que la puerta se prueba entera aquí, con `runBlocking`, sin
    Robolectric. El id se lee dentro del candado y una escritura que devuelve `0` **devuelve el borrador a
    «sin escribir»** en vez de darlo por guardado.

- [x] **T5. El editor escribe por la puerta.** RF-180, RF-182, RF-184, RNF-116, RNF-119
  - Hecho cuando: `hasContentToSave` sube junto a `contentForSave`, `persistNote(content, onWritten = {})` llama
    a `draftWrite.launch()`, recalcula `draftGuard.hasUnsaved` en el acto y lanza `draftWrite.write` en
    `launchInViewModelScope`; existe `guardarConId(destino, content)` con la lista de campos; y
    `draftGuard?.saveAction` es la línea `persistNote(contentForSave(), onSaved)`; y
    `./gradlew :app:compileDebugKotlin` compila.
  - Hecho cuando además: el `LaunchedEffect` de la bandera usa `isDraftUnsaved(...)` con `draftWrite.state` entre
    sus claves, y `NoteEditorScreen.kt` tiene **menos** líneas que antes (RNF-116).
  - **Hecho.** Compila. `guardarConId` tiene que ir **antes** que `persistNote` (Kotlin no admite referencias
    adelantadas a funciones locales, el motivo ya estaba escrito en `contentForSave`) y ser `suspend`. Al quitar
    el `saveAction` de 17 líneas el archivo bajó a **2268**, nueve menos que el presupuesto (RNF-116). De paso,
    el comando «/» → página tenía una **tercera** copia de la lista de campos con `id = noteId`: también pasa por
    `persistNote`.

- [x] **T6. El autoguardado escribe lo que ya tiene fila.** RF-185
  - Hecho cuando: el autoguardado (`LaunchedEffect(Unit)` con `debounce(2000)`) y el observador de `ON_STOP` usan
    `draftWriteId(noteId, draftWrite.state) != 0` en vez de `noteId != 0`, y compila.
  - Hecho cuando además: una nota nueva **sin** fila sigue sin insertarse sola, que es lo que dice el RF-185.
  - **Hecho.** Los dos sitios. Un borrador sin fila no se inscribe solo: el autoguardado sigue igual que antes.

- [x] **T7. Invariantes de la spec 027.** RNF-113, RNF-116, RNF-117, RF-184, RF-185, RF-186
  - Hecho cuando: `EditorInvariantsTest` tiene el invariante nuevo de cableado (`isDraftUnsaved` en el efecto y en
    `persistNote`, `saveAction` sin lista de campos ni `id = 0`, `draftWriteId` en el autoguardado y en `ON_STOP`),
    `MAX_GUARDADOS` vale **1** con su motivo nuevo, `MAX_LINEAS` vale el número real que queda, y
    `./gradlew :app:testDebugUnitTest --tests '*EditorInvariantsTest' --tests '*DraftWriteTest'` pasa en verde.
  - Hecho cuando además: cada recorrido **quita comentarios** y **se autocomprueba**, como los que ya hay.
  - **Hecho.** **3 invariantes nuevos** en verde, `MAX_GUARDADOS` 2 → **1**, `MAX_LINEAS` 2277 → **2268**. El
    recorrido de imports nació con un `^` **sin** `MULTILINE` —que no encuentra nada y pasa por vacío— y se
    corrigió a una lista blanca de imports permitted con suelo `MIN_IMPORTS`: exactamente los cinco que tiene la
    pieza.

- [x] **T8. Compilación y suite completa, sin regresiones.** RNF-114, RNF-115, RNF-118
  - Hecho cuando: `./gradlew :app:test --continue` da **799 + las pruebas nuevas** sobre 41, con **las mismas
    clases rojas** de siempre y ninguna nueva, `./gradlew :app:assembleDebug` compila, y `git diff --stat` **no**
    toca ningún `build.gradle.kts` ni ningún `strings.xml`.
  - **Hecho.** `:app` **816/41** contra la base 799/41: los +17 son `DraftWriteTest` (14) y los tres invariantes
    nuevos. Las clases rojas son **las mismas once** de siempre. `assembleDebug` compila y el diff solo toca
    `NoteEditorScreen.kt`, `EditorInvariantsTest.kt` y los cuatro ficheros nuevos.

- [x] **T9. Memoria y guion.**
  - Hecho cuando: `MEMORY.md` registra la spec 027 (estado y decisiones del plan), `AGENTS.md` recoge la regla
    permanente si la hay, y el guion de verificación de `spec.md` queda tal cual.
  - **Hecho.** `AGENTS.md` recoge las tres reglas permanentes del guardado de un borrador.

- [x] **T10. Verificación en dispositivo y cierre.**
  - Hecho cuando: el guion de ocho pasos de `spec.md` se cumple entero en dispositivo, y `spec.md` queda con
    estado «implementada y verificada en dispositivo» y la nota de cierre.
  - **Hecho.** Verificado el 2026-10-17: atrás guarda y sale sin preguntar, la ✕ del borrador sigue preguntando, y
    los ocho caminos terminan en una sola nota.
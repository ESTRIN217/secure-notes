# Spec 028 — Tareas

> Orden de dependencia. Una tarea por sesión de trabajo (≤ 30 min). Los tests van **primero**.

- [x] **T1. Pruebas puras de las listas y de las dos funciones puras, en rojo.** RNF-120, RF-187, RF-188,
  RF-189, RF-190, RF-191
  - Hecho cuando: existe `app/src/test/java/com/example/data/backup/BackupSettingsTest.kt` con los trece casos de `plan.md` §
    Estrategia 1 y `./gradlew :app:compileDebugUnitTestKotlin` **falla** porque `com.example.data.backup.BackupSettings`
    todavía no existe.
  - Hecho cuando además: el fallo es **solo** por las referencias nuevas, para que la regresión esté medida antes
    de tocar la implementación.
  - Hecho cuando además: el primer caso es **la lista tiene catorce claves e incluye `THEME_ID_KEY`**, porque el
    defecto que esta spec arregla es una clave de menos.
  - Hecho cuando además: el caso del tema desconocido tiene nombre propio, y no es el último de la lista.
  - **Nota:** `org.json` funciona aquí sin Robolectric por `testImplementation(libs.org.json)`
    (`app/build.gradle.kts:151`); el precedente es `UpdaterViewModelTest`, que usa `JSONObject` sin `@RunWith`.
    Si `compileDebugUnitTestKotlin` lo confirma, se sigue sin añadir nada.
  - **Hecho.** Rojo por los **cinco** símbolos nuevos (`BACKUP_SETTINGS`, `DEFAULT_SETTINGS`, `backupSettingsJson`,
    `restorableSettings`, `BackupSetting`) y por sus derivados (`it`, `value`, `key`), y **por nada más**: ninguna
    referencia de la prueba a código existente falla, así que `AppThemes.oneDark`, `PasswordType.PIN` y
    `AUTO_LOCK_TIMEOUT_DEFAULT` son válidos y `org.json` resuelve sin Robolectric, como se esperaba. **13 casos.**
  - **Desviación del plan, con motivo.** El fichero va en `com/example/data/backup/` y no en `com/example/`: el
    paquete de la prueba **mira** el del código que prueba, como `DraftWriteTest` con `com.example.ui` y
    `app/src/test/java/com/example/data/storage/`. Con el paquete propio, las doce referencias al paquete de `:app`
    desaparecen en vez de añadirse.

- [x] **T2. La pieza pura.** RNF-120, RF-187, RF-188, RF-189, RF-190, RF-191
  - Hecho cuando: `app/src/main/java/com/example/data/backup/BackupSettings.kt` existe con `SettingType`,
    `BACKUP_SETTINGS` (14), `DEFAULT_SETTINGS` (14), `sealed class BackupSetting` (`Text`, `Flag`, `Number`) y
    las dos funciones puras `backupSettingsJson` y `restorableSettings`, **sin** importar `androidx.compose`, y
    `./gradlew :app:testDebugUnitTest --tests '*BackupSettingsTest'` pasa en verde.
  - Hecho cuando además: los **catorce** valores por defecto son los mismos que se leen hoy en los tres
    serializadores, uno a uno, para que el refactor no cambie de paso lo que viaja (RF-193).
  - Hecho cuando además: `restorableSettings` no lanza con el bloque vacío y **no** escribe un `theme_id` en
    blanco (decisión 2 del plan).
  - **Hecho.** **13/0** en verde. El fichero no importa `android` todavía: las dos adaptadoras son T4, así que
    todo lo escrito aquí es puro (RNF-120).
  - **Dos tropiezos del compilador, y los dos son trampas que se quedan escritas** en el fichero porque
    volverían a aparecer: dentro de `BackupSetting`, `Number` es la subclase y no `kotlin.Number`, así que el
    moldeado va como `kotlin.Number`; y `value` en la base tiene que ser `open`, porque un `val` de clase
    base es final y no admite el override covariante de cada subclase.
  - **Nombre cambiado respecto del plan**: `RestoredSetting` → `BackupSetting`. La tabla se usa **también al
    copiar** (para comprobar el tipo antes de escribirlo en el bloque), así que un nombre que dijera
    «restaurado» mentía en la mitad de los usos. El plan y esta lista están sincronizados.

- [x] **T3. El doble de `SharedPreferences` y las pruebas de las dos adaptadoras, en rojo.** RF-187, RF-192
  - Hecho cuando: `BackupSettingsTest` tiene los seis casos de `plan.md` § Estrategia 2 y están en rojo porque
    `readBackupSettings` y `applyRestoredSettings` todavía no existen.
  - Hecho cuando además: el doble es una **implementación escrita a mano** de la interfaz `SharedPreferences`
    (≈ 25 líneas, sin comportamiento de Android), no un `RobolectricTestRunner`, y se autocomprueba: una prueba
    escribe una preferencia y la lee con el doble antes de fiarse de él.
  - **Nota:** si el doble resultara necesitar Robolectric, las dos adaptadoras se quedan como fontantería sin
    prueba y **se dice en el guion de cierre**: la decisión ya está cubierta por las dos funciones puras.
  - **Hecho.** Rojo por **las dos funciones y nada más**: cuatro referencias a `readBackupSettings` y dos a
    `applyRestoredSettings`. El riesgo que la tarea apuntaba —que el doble escrito a mano **no** compilara sin
    Robolectric— no ocurrió: `PrefsFalso` implementa la interfaz entera y `compileDebugUnitTestKotlin` lo acepta.
    **Las dos adaptadoras sí se prueban**, sin la nota de escape del plan.
  - **Tres fallos míos de tipado** salieron al redactar el rojo y hubo que arreglarlos **antes** de dar la
    tarea por buena: `prefs.textos` (no existía; es `valores`), un `emptyList()` sin tipo que Kotlin no podía
    inferir, y un `when` que leía `valor.value` del `Any` crudo en vez de del ajuste ya envuelto. Se anotan
    porque son los tres fallos típicos al escribir una prueba **contra** una API que todavía no existe, y
    porque contarlos como «el rojo que toca» haría la tarea parecer más limpia de lo que fue.
  - **Tamaño del doble: unas 60 líneas, no 25.** La interfaz tiene diez métodos y su `Editor` otros diez, y
    además guarda **con qué método** se escribió cada clave, que es lo que deja comprobar que un `FLAG` no acabe
    escrito como texto.

- [x] **T4. Las dos adaptadoras.** RF-187, RF-192
  - Hecho cuando: `readBackupSettings` devuelve los catorce valores con su **tipo**, resuelve el tema con
    `resolveStoredTheme(guardado, antiguo)` —**no** con una caída propia— y `applyRestoredSettings` escribe con
    `putString`/`putBoolean`/`putLong` en un `edit()` y un `apply()`, y las trece pruebas de `BackupSettingsTest`
    pasan **todas** en verde.
  - Hecho cuando además: con las preferencias vacías el tema sale `dynamic` (instalación nueva) y con
    `dynamic_colors` apagado sin `theme_id` sale `default` (decisión 2 del plan).
  - **Hecho.** **20/0** en verde (13 puras + 6 de adaptadoras + la autocomprobación del doble). Único import de
    Android: `android.content.SharedPreferences`. Cero `androidx.compose` (RNF-120).
  - **El tipo llega de parámetro a `leer()` y no se busca por clave.** `BACKUP_SETTINGS[clave]` es un
    `SettingType?`, y el `when` sobre un sujeto nullable obliga a añadir una rama `null` —que sería **código
    muerto**: el recorrido de arriba ya tiene el tipo y no puede no tenerlo. Pasarlo de parámetro quita la
    rama en vez de disfrazarla de `else`.
  - **Duplicación que se acepta y se dice en voz alta.** Los dos valores del tema heredado se leen aquí y se
    leen también en `SharedPreferencesRepository.getAppTheme()`. Se podría borrar una de las dos copias
    metiendo una función con `SharedPreferences` en `data/theme/ThemePreference.kt`, que es pura **a
    propósito** y cuya regla ya cubre `ThemePreferenceTest`. Tres líneas duplicadas y documentadas en el
    KDoc de las dos, frente a romper la pureza del fichero que define la regla: se queda la duplicación.

- [x] **T5. Los invariantes, en rojo.** RF-191, RNF-120
  - Hecho cuando: existe `app/src/test/java/com/example/BackupSettingsInvariantsTest.kt` con los cinco recorridos de
    `plan.md` § Estrategia 3, cada uno con su suelo, y **están en rojo** porque los cinco sitios siguen escribiendo
    la lista a mano.
  - Hecho cuando además: el recorrido de los restauradores mira el **bloque**, no el fichero: `NotesViewModel` usa
    esas mismas claves para su estado vivo y eso no puede ponerse rojo (el motivo va escrito en el KDoc).
  - **Hecho.** **6 pruebas, 2 rojas**: las de los serializadores y las de los restauradores, con **13** apariciones
    cada una en `BackupViewModel.kt` y el nombre del fichero en el mensaje. Las otras cuatro están en verde por
    construcción: fijan la pieza que T2 y T4 ya escribieron. **El plan decía «los cinco en rojo» y solo lo están
    dos**, porque tres de los cinco invariantes no miran los sitios sino la pieza nueva.
  - **Un invariante del plan era FALSO, y por eso esta tarea vale por sí sola.** El tercero decía «el único
    fichero de `:app` que menciona `THEME_ID_KEY` junto a `DYNAMIC_COLORS_KEY`». No es cierto:
    `SharedPreferencesRepository.getAppTheme()` menciona los dos legítimamente —es el otro lector de la migración
    heredada— y `Constants.kt` los declara. Se reformuló a **declaraciones y no menciones**: exactamente dos
    `val BACKUP_SETTINGS`/`val DEFAULT_SETTINGS` en todo el árbol, y las dos en la pieza.
  - **Dos fallos míos al escribir el recorrido**, ambos de la misma familia: la constante `LISTA` era
    `"put(AppConstants."` **sin escapar**, que como `Regex` es un grupo sin cerrar y reventaba el recorrido entero
    con `PatternSyntaxException`; y el recuento de declaraciones iba sobre **ficheros**, así que un fichero con las
    dos listas contaba **una** vez y la comparación con `listOf(PIEZA, PIEZA)` salía falsa. Es justo el motivo por
    el que los recorridos llevan suelo y autocomprobación.
  - **Autocomprobación añadida** (no estaba en el plan): una prueba pasa por el recorrido un trozo de código
    inventado con un comentario de línea, uno de bloque, un texto con un `//` de URL dentro y una llamada real, y
    exige que solo sobreviva la llamada. Sin ella, un `Regex` mal escrito devuelve 0 y «no hay ni una copia de la
    lista» pasa en verde sin comprobar nada.

- [x] **T6. Los tres serializadores por la pieza compartida.** RF-187, RF-191, RNF-124
  - Hecho cuando: `BackupViewModel.buildBackupJson` (`:250`), `NotesViewModel.forceSyncCloud` (`:1075`) y
    `SyncWorker.doWork` (`:53`) escriben el bloque de ajustes con `backupSettingsJson(readBackupSettings(sharedPrefs))`
    y ya no queda ningún `put(AppConstants.<CLAVE>_KEY,` en `:app`; los tres invariantes de serializador pasan en
    verde.
  - Hecho cuando además: **no se toca** el estado vivo de `NotesViewModel` (`:104`, `:121`, `:123`, `:466`–`:522`)
    ni el `ENCRYPT_BACKUPS_KEY` de `SyncWorker:77` que decide si la copia va cifrada, y queda escrito por qué.
  - Hecho cuando además: el `version` sigue siendo `4` y `git diff --stat` no muestra ningún otro cambio (RF-122).
  - **Hecho.** Los tres escriben `val settings… = backupSettingsJson(readBackupSettings(sharedPrefs))` y el
    invariante de serializadores pasa **en verde** (1 llamada por fichero, 3 en total, y **0** `put(AppConstants.`).
    `BackupSettingsTest` sigue en **20/0**. El invariante de restauradores sigue **rojo**, que es lo de T7.
  - **`-45 +12` en los tres ficheros**, 13 líneas fuera en cada uno. El `version` sigue siendo **4** en los tres
    sitios y `git diff --name-only` no muestra ni un `strings.xml`, ni un `build.gradle.kts`, ni un `res/xml`.
  - **Lo que NO se tocó, comprobado:** el estado vivo de `NotesViewModel` (`autoUpdateCheck`, `isBiometricEnabled`,
    `encryptBackups`, `setEncryptBackups`…) y el `ENCRYPT_BACKUPS_KEY` de `SyncWorker` que decide si la copia va
    cifrada. Son ajustes de la app, no de la copia, y por eso el invariante apunta a `put(AppConstants.` y no a
    la clave.

- [x] **T7. Los dos restauradores por la pieza compartida.** RF-188, RF-189, RF-190, RF-191, RF-192
  - Hecho cuando: `BackupViewModel.restoreFromJson` (`:378`) y `NotesViewModel.finalizeRestore` (`:1370`) aplican
    con `applyRestoredSettings(sharedPrefs, restorableSettings(settings))`, las 26 líneas de `if (settings.has(...))`
    desaparecen de los dos sitios y **los cinco invariantes** pasan en verde.
  - Hecho cuando además: el `if (container.has("settings"))` / `if (payloadObj.has("settings"))` de fuera **se
    queda**, con el motivo escrito: es lo que evita restaurar ajustes en una copia que no los trae.
  - **Hecho.** Los dos llaman a `applyRestoredSettings(sharedPrefs, restorableSettings(...))`, las 26 líneas de cada
    uno desaparecen y **los seis tests de invariantes pasan en verde** (`6/0`), junto con `BackupSettingsTest`
    (`20/0`). **`-84` líneas netas** en los tres ficheros tocados desde el principio de la spec
    (`-103 +19` en `app/src/main`).
  - **Comprobado con `grep -E` en todo `:app`: 0 `put(AppConstants.` y 0 `settings.get…(AppConstants.`,** y las
    cinco llamadas a la pieza compartida están donde deben (tres que copian, dos que restauran).
  - **El `if (…has("settings"))` de fuera se queda**, con el motivo escrito en los dos sitios: es lo que evita
    que una copia sin bloque de ajustes toque preferencias. Dentro, la decisión es de `restorableSettings`.
  - **Aviso sobre `grep` en la consola**: la primera comprobación dio **510** falsos positivos porque usé `\|`
    dentro de un `grep` sin `-E`, y ahí `\|` es alternancia: el patrón se leía «`Boolean`» o «`Long)`» y
    contaba cualquier línea con esas palabras. El invariante de Kotlin usa `Regex`, donde `|` sí es alternancia, y
    por eso siempre dio 0. Un `grep` mal escrito parece un invariante roto y es al revés.

- [x] **T8. Cierre.** RNF-120, RNF-121, RNF-123
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` y `./gradlew :app:test --continue` contra la línea base
    **816/41**: ninguna clase roja nueva, y los fallos son los 41 de plataforma de siempre.
  - Hecho cuando además: `git diff --stat` no toca `build.gradle.kts`, `libs.versions.toml`, `strings.xml` (ni en
    los nueve locales) ni `backup_rules.xml` (RNF-121), y el guion de ocho pasos de `plan.md` queda escrito para
    cuando haya dispositivo. — **Cumplido: se escribió y se ejecutó entero el 2026-10-18.**
  - Hecho cuando además: `MEMORY.md` al día y la regla permanente propuesta para `AGENTS.md`, si la hay.
  - **Hecho.** `:app:compileDebugKotlin` en verde y **sin un solo aviso** en los tres ficheros nuevos. Suite:
    **842/41** contra la línea base **816/41** → **+26** (20 de `BackupSettingsTest` y 6 de
    `BackupSettingsInvariantsTest`), **los mismos 41 fallos**, ninguno en las clases nuevas.
  - **Composición real de los 41, medida sobre los XML:** **38** de plataforma
    (`DefaultNativeRuntimeLoader`, Robolectric sin runtime nativo para `linux/aarch64`) y **3** preexistentes y
    ajenos: `HtmlExportMediaTest.legacy item markup migrates to checklist html`,
    `OnDeviceModelSourceTest.every catalog model exposes a hugging face source` y
    `OnDeviceModelSourceTest.source label drops the scheme and the file`.
  - ⚠️ **`AGENTS.md` se equivoca en esa cuenta**: dice «**40** de ellos son de plataforma». Son **38**, y con los
    3 de aserciones son 41. La línea estaba escrita desde antes de que los tres preexistentes se añadieran y
    nunca se cuadró. Corregirla al proponer la regla permanente.
  - **La suite se lanzó dos veces, y por un motivo que conviene que conste.** La primera arrancó antes de que
    T8 arreglara el `?: ""` de `leer()`, así que su resultado era de un código que ya no existe; se relanzó
    entera con el código final para poder decir «842/41» de esta spec y no de la anterior.
  - **RNF-121 y RNF-122, comprobados con `git`:** ningún `strings.xml`, `build.gradle.kts`, `libs.versions.toml`,
    `res/xml` ni `oss-licenses.json` tocado en toda la spec, y el `version` del formato sigue siendo **4** en los
    tres sitios que arman la copia (RNF-122).
  - **Guion de dispositivo: ejecutado.** Los ocho pasos de `plan.md` se corrieron el 2026-10-18 y cumplieron, así que
    esta spec queda **verificada en dispositivo** y no solo comprobada en sus piezas puras. Lo que sí se verificó
    aquí fueron las piezas puras, los invariantes de fuentes y el cableado.
  - **Ficheros de la spec entera:** `SyncWorker.kt`, `BackupViewModel.kt`, `NotesViewModel.kt` y `MEMORY.md`
  modificados; nuevos `data/backup/BackupSettings.kt` (220 líneas), sus dos clases de prueba y
  `specs/028-tema-en-la-copia/`.
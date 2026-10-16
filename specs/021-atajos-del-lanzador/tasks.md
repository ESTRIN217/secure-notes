# Tareas — Spec 021: los atajos del lanzador

> Orden de dependencia. Cada tarea: **pruebas primero en rojo**, después el código, después en verde.
> Máximo 20-30 min cada una. El plan es `plan.md`; la spec manda sobre los dos.

- [x] **T1. El enum de destinos y su parser, con sus pruebas puras.**
  RF-125, RF-129
  - Crear `shortcut/ShortcutDestination.kt` con `EXTRA_DESTINO`, el `enum` de los cinco destinos y
    `desde(clave)`.
  - Pruebas **antes** del código, en `app/src/test/java/com/example/shortcut/ShortcutDestinationTest.kt`:
    cada una de las cinco claves devuelve su destino; `null`, `""` y `"nuevo"` devuelven `null` sin lanzar;
    las cinco claves son únicas.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests 'com.example.shortcut.*' --rerun` pasa **sin
    clases de plataforma**, y el fichero no importa `android` ni `androidx.compose`.

- [x] **T2. El destino pendiente y la decisión, con sus pruebas puras.**
  RF-127, RF-129
  - Crear `shortcut/ShortcutPending.kt`: `ShortcutDecision` (tres casos), la clase `ShortcutPending`
    (`registra`/`drena`) y `decidir(clave, abierta, pendiente)`.
  - Pruebas **antes**, en `ShortcutDecisionTest.kt` y `ShortcutPendingTest.kt`: abierta + válida →
    `Ejecutar` y el pendiente queda vacío; cerrada + válida → `Pendiente` **y** registrado; cerrada +
    inválida → `Ignorar` y **nada** pendiente; `drena()` sobre vacío → `null`; escupir **vacía**; dos
    registros → gana el último; `drena()` dos veces → la segunda `null`.
  - Hecho cuando: las dos pruebas pasan, y cada caso de `ShortcutDecision` está cubierto por aserción (no solo
    por compilar el `when`).

- [x] **T3. El XML de atajos y su declaración en el manifiesto.**
  RF-124, RF-130
  - Crear `app/src/main/res/xml/shortcuts.xml` con los cinco `<shortcut>` **en el orden del plan** (§4), cada
    uno con `shortcutId` literal, `shortcutShortLabel` de las claves existentes, `icon` y un `<intent>` con
    `action`, `targetPackage` y `targetClass` a `MainActivity`, **sin** `data` ni `mimeType`.
  - Añadir el `<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts"/>` dentro de
    `<activity>` de `MainActivity`, que es la que tiene `MAIN` + `LAUNCHER`.
  - Hecho cuando: `./gradlew :app:processDebugResources` compila el recurso, `git diff` no toca ningún
    `strings.xml`, y `aapt` no se queja de atributos inexistentes (**no** existe `monochrome`).

- [x] **T4. Los cinco iconos vectoriales.**
  RF-124
  - Crear `sc_new_note`, `sc_search`, `sc_ai_chat`, `sc_settings` y `sc_code` en `res/drawable/`: viewport
    `24dp`, **un solo** `android:fillColor`, sin `android:tint` ni `<gradient>`.
  - Hecho cuando: el recurso compila, y un invariante comprueba que **ninguno** lleva `tint` ni aparece la
    cadena `monochrome` en el XML (es un invariante, no una revisión a ojo).

- [x] **T5. Traducir un destino en navegación.**
  RF-125, RF-126
  - Crear `ui/ShortcutDestinations.kt` con `ejecutarDestino(destino, navigator, chatHistoryViewModel, backend,
    context)` y su `when` de cinco brazos.
  - `AI_CHAT` crea la sesión **antes** de navegar, como el botón de la lista; `CODE_EDITOR` llama a
    `launchCodeEditor(context)`.
  - Cambiar `launchCodeEditor` de `private` a `internal` en `ui/Screen.kt` para no duplicarlo.
  - Hecho cuando: `:app:compileDebugKotlin` en verde, y un invariante comprueba que el `when` tiene los cinco
    brazos y que `AI_CHAT` llama a `createSession` **antes** de navegar.

- [x] **T6. Cablear los dos caminos en `MainActivity`.**
  RF-127, RF-128
  - Añadir `atajoPendiente = ShortcutPending()` al `companion`.
  - Añadir `resolverAtajo(intent, abierta)` dentro de `AppMainContent`, **después** de `navigator` y de
    `chatHistoryViewModel`: llama a `decidir`, ejecuta lo que venga `Ejecutar`, y **siempre** hace
    `removeExtra(EXTRA_DESTINO)`.
  - Llamarla desde el `LaunchedEffect(Unit)` del arranque en frío **y** desde el `intentRelay`: la **misma**
    función en los dos.
  - Añadir el volcado `atajoPendiente.drena()?.let { ejecutarDestino(it, …) }` dentro del
    `LaunchedEffect(isUnlocked)` que ya existe, en su `if (isUnlocked)`.
  - Hecho cuando: `:app:compileDebugKotlin` en verde, y tres invariantes pasan —los dos caminos llaman a la
  misma función, el volcado está en el efecto cuya clave es `isUnlocked`, y está el `removeExtra`.

- [x] **T7. Los invariantes.**
  RF-124, RF-128, RF-130, RNF-72, RNF-73, RNF-74
  - Crear `ShortcutInvariantsTest.kt`: forma del XML, ids literales y únicos, las cinco etiquetas en los
    **nueve** locales, el `meta-data` dentro de una Activity `MAIN`/`LAUNCHER`, sin `data`/`mimeType`, los
    valores del XML **coinciden** con los que acepta `desde`, los ficheros puros sin `android` ni Compose, y el
    camino de decisión sin DAO ni `CipherService`.
  - **Recorridos que quitan comentarios antes de buscar** y **que se autocomprueban** (dos reglas de
    `MEMORY.md` nacidas de la spec 020; sin ellas un invariante se pone verde con su propio KDoc).
  - Hecho cuando: pasa todo, y **ninguno** de los invariantes pasa por un recorrido vacío.

- [x] **T8. Verificación completa y cierre.**
  RNF-71, RNF-72
  - `./gradlew :app:compileDebugUnitTestKotlin --rerun` en verde.
  - Suite completa con `--continue` y comparar **por clase de fallo**, no en cantidad: base **`:app` 711 / 41**,
    **`:editor-de-codigo` 145 / 2**. Un fallo nuevo, aunque compile, deja la tarea sin cerrar.
  - `git diff` no toca ningún `build.gradle.kts` ni ningún `strings.xml`.
  - Marcar la spec como **implementada** y actualizar `MEMORY.md` con lo que no era obvio: el candado, el
    `removeExtra` anti-rotación y lo de los cuatro atajos visibles.
  - Anotar en la spec lo que **solo** se puede comprobar en dispositivo: mantener pulsado el icono, y que con
    la bóveda cerrada salga el candado y luego el destino.
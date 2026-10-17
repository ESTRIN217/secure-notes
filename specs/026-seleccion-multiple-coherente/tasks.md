# Spec 026 — Tareas

> Orden de dependencia. Una tarea por sesión de trabajo (≤ 30 min). Los tests van **primero**.

- [x] **T1. Pruebas puras de la decisión, en rojo.** RNF-107, RF-173, RF-175, RF-176, RF-179
  - Hecho cuando: existe `app/src/test/java/com/example/NoteSelectionTest.kt` con los casos del `plan.md` §
    Estrategia 1 (vacío, entrar con una, añadir la segunda, quitar una habiendo otras, quitar la única,
    doble alternancia, no muta la entrada, ids ajenos intactos) y
    `./gradlew :app:compileDebugUnitTestKotlin` **falla** porque `com.example.ui.NoteSelection` todavía no
    existe.
  - Hecho cuando además: el fallo es **solo** por la referencia nueva, para que la regresión ya esté medida
    antes de tocar la implementación.
  - **Hecho.** Rojo por las **dos únicas** referencias nuevas (`Unresolved reference 'NoteSelection'` en el
    import y en un caso) y por nada más. **9 casos.**

- [x] **T2. La pieza pura.** RNF-107, RF-173, RF-175, RF-179
  - Hecho cuando: `app/src/main/java/com/example/ui/NoteSelection.kt` existe **sin** importar `android` ni
    `androidx`, con `isActive` y `toggle` según el pseudocódigo, y
    `./gradlew :app:testDebugUnitTest --tests '*NoteSelectionTest'` pasa en verde.
  - **Hecho.** `9/0` en verde.

- [x] **T3. El toque y la pulsación larga, por la misma regla.** RF-173, RF-174, RF-175, RF-176, RNF-111
  - Hecho cuando: en las **dos** llamadas a `NoteCardItem` de `MainListScreen.kt` el `onClick` alterna cuando
    hay selección y, si no, abre la nota (o la restaura en la papelera), y el `onLongClick` usa
    `NoteSelection.toggle` sin cambiar lo de hoy (en la papelera, nada); y
    `./gradlew :app:compileDebugKotlin` compila.
  - **Hecho.** Los cuatro gestos usan `NoteSelection.toggle`; el `if/else` anidado del toque pasó a guard
    clauses (`else if`), como pide la convención.

- [x] **T4. Atrás cierra el modo.** RF-177, RF-178, RF-179
  - Hecho cuando: la barra superior (`:376`, `:464`, `:494`) y el botón flotante (`:666`) usan
    `NoteSelection.isActive(...)`, ya no queda ninguna comprobación literal de la selección en la pantalla, el
    `BackHandler` se habilita con la selección activa y su cuerpo la cierra **antes** de navegar; y
    `./gradlew :app:compileDebugKotlin` compila.
  - **Hecho.** Hubo que **subir la declaración** de `selectedNoteIds` por encima del `BackHandler`: el atrás lo
    lee, y una variable local declarada más abajo no está en su ámbito. El
    `BackHandler` es el primer consumidor de la selección, así que la declaración vive junto a él.

- [x] **T5. Invariantes de la spec 026.** RNF-107, RF-173, RF-177, RF-179
  - Hecho cuando: existe `app/src/test/java/com/example/NoteSelectionInvariantsTest.kt` con los invariantes del
    `plan.md` § Estrategia 2 (la pieza pura no importa `android`/`androidx`; la pantalla no reescribe la
    comprobación; la alternancia aparece en los dos toques y las dos pulsaciones largas; el atrás la cierra),
    cada recorrido **quita comentarios** y **se autocomprueba**, y
    `./gradlew :app:testDebugUnitTest --tests '*NoteSelection*'` pasa.
  - **Hecho.** **4 invariantes en verde.** El recorte de las tarjetas va de la primera `NoteCardItem(` hasta
    `if (showCreateTagDialog) {` y no hasta `fun NoteCardItem(`: la función vive al final del fichero, así que
    el recorte largo se llevaba por delante las acciones por lotes (que **sí** limpian la selección al
    terminar) y el invariante daba rojo por una regla que debe seguir siendo cierta.

- [x] **T6. Compilación y suite completa, sin regresiones.** RNF-108, RNF-109, RNF-110
  - Hecho cuando: `./gradlew :app:test --continue` da la misma cuenta que la base **786/41** más las pruebas
    nuevas, con **las mismas clases rojas** de siempre y ninguna nueva, y `git diff --stat` **no** toca ningún
    `build.gradle.kts` ni ningún `strings.xml`.
  - **Hecho.** `:app` **799/41** contra la base **786/41**: los +13 son las pruebas nuevas (9 + 4) y las clases
    rojas son **las mismas** (`EquationScreenshotTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest`,
    `HtmlExportMediaTest`, `HtmlMediaEmbedderTest`, `MarkdownAttachmentCollectorTest`, `OnDeviceModelSourceTest`,
    `PdfExporterTest`, `StorageAudioFilesTest`, `WidgetNotesTest`; todas de Robolectric o las 3 preexistentes).
    El diff solo toca `MainListScreen.kt` y añade los cuatro ficheros nuevos.

- [x] **T7. Memoria y guion.**
  - Hecho cuando: `MEMORY.md` registra la spec 026 (estado y decisiones del plan) y el guion de verificación de
    `plan.md` se copia al final de `spec.md`.
  - **Hecho.**

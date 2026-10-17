# Tasks — Spec 025 — Un tema para toda la app

**Once tareas.** La undécima es limpieza y cierre; si se quiere el límite de diez, cabe dentro de la décima
—sus tres cadenas están en los mismos nueve ficheros— y quedaría en diez.

Cada tarea: pruebas primero en rojo, luego el código, luego `./gradlew` en verde.

> **Entorno.** La suite con Robolectric no corre aquí (`DefaultNativeRuntimeLoader`, `linux/aarch64`). Todo lo
> nuevo de esta spec es **puro**, así que su comprobación es `:tema:testDebugUnitTest` más el Directed test de
> `:app`/`:editor-de-codigo`, y la línea base de `./gradlew test` sirve solo para no añadir fallos nuevos.

---

### T1. Módulo `:tema`, contraste y generador tonal

RF-161, RNF-99, RNF-101

- [x] **T1. Crear el módulo `:tema` con `Contrast.kt` y `TonalScheme.kt`.**
  - `settings.gradle.kts`: `include(":tema")` con el mismo comentario que explica por qué existe `:markdown`.
  - `tema/build.gradle.kts`: calcado del de `:markdown` (`android.library` + `kotlin.compose`, `minSdk = 33`,
    BOM, `material3`, `compose.ui`, `compose.ui.graphics`, `junit`). Ni una dependencia de artefacto nuevo.
  - `Contrast.kt`: `relativeLuminance(Color)` y `ratio(Color, Color)` de WCAG 2.1. Sin `android`.
  - `TonalScheme.kt`: sRGB ↔ OKLCH, gamut por reducción de croma en pasos de 1/32, rampa de croma fija, tabla de
    36 tonos por rol copiada de la línea base de Material 3 y `ajustarTono()` (§3.1 del plan).
  - Primero `TonalSchemeColorTest` en rojo: determinismo (misma semilla → mismo `ColorScheme`), gamut sRGB
    (ningún canal fuera de 0..255) y un color de papel (croma casi 0) que conserva su tono.
  - Hecho cuando: `./gradlew :tema:testDebugUnitTest` pasa y `:tema:assembleDebug` compila.
  - Verde: *cero*. `AGENTS.md` lo dice: sin `Context` es una función pura y sin prueba es una regla que nadie
    sabe si funciona.

### T2. El catálogo del resaltador se muda a `:tema`

RF-159, RNF-103

- [x] **T2. Mover `SyntaxTheme`/`SyntaxThemes` de `:editor-de-codigo` a `:tema` sin tocar un valor.**
  - `tema/.../SyntaxColors.kt` con los 22 campos y las cuatro paletas idénticas.
  - `implementation(project(":tema"))` en `editor-de-codigo/build.gradle.kts`.
  - Rewritear el `import` en `SyntaxHighlighter`, `KotlinSyntaxHighlighter`, `CodeEditorApp`, `CodeEditorViewModel`,
    `MarkdownPreviewPane`, `CodeEditorView`, `EditorDialogs`, `QuickSymbolBar`, `SearchReplaceBar` y los dos
    tests (`CodeEditorUnitTest`, `MarkdownSyntaxHighlightTest`). **Ninguna aserción cambia.**
  - `ThemeCatalogTest` en rojo→verde: las cuatro paletas tienen los mismos 22 valores, incluidos
    `#1F1F1F`, `#CE9178`, `#6A9955` y `0x66F9E2AF`.
  - Hecho cuando: `./gradlew :editor-de-codigo:testDebugUnitTest :tema:testDebugUnitTest` verde con el recuento
    de tests del editor igual al de antes (149), y `SyntaxColors.kt` ya no está en el editor.
  - Verde: *cero*. Es mover código, no añadir comportamiento.

### T3. `AppTheme`, el invariante del catálogo y `forNoteBlock`

RF-160, RF-161, RF-167

- [x] **T3. El modelo de tema, sus invariantes y la paleta del bloque de nota.**
  - `tema/.../AppTheme.kt`: `AppTheme(id, isDynamic, light?, dark?, syntax?)` y `AppThemes` con `all` (los seis),
    `byId`, `schemeFor`, `editorPalette`, `notePalette`, y las semillas de los cuatro temas fijos.
  - `SyntaxThemes.Standard`: los catorce acentos de `CodeSyntax.blockTheme` más superficies oscuras neutras
    concretas, para que sirva de paleta de editor y de bloque.
  - `SyntaxTheme.forNoteBlock()`: a `Unspecified` **exactamente** los siete campos de superficie.
  - `ThemeCatalogTest`: seis temas con esos identificadores, sin duplicados, invariante
    `isDynamic ⟺ los tres nulos` en el catálogo y en `byId` con datos inventados.
  - `NoteBlockPaletteTest`: los siete de superficie sin especificar y **los catorce de acento intactos**.
  - `ThemeFallbackTest`: `schemeFor` da `null` para el dinámico; `editorPalette` cae a `VSCodeDark` y
    `notePalette` a `Standard`; un tema fijo devuelve su propio esquema según el modo.
  - Hecho cuando: `./gradlew :tema:testDebugUnitTest` verde.
  - Verde: *cero*.

### T4. Las semillas y el contraste de los ocho esquemas

RF-161, RNF-101

- [x] **T4. Sembrar los cuatro temas fijos y comprobar que los ocho esquemas pasan AA.**
  - Las semillas quedaron puestas en T3; aquí solo se **comprueba** que los ocho esquemas pasan AA: `#3794FF`/
    `#4EC9B0`/`#CE9178` para VS Code, `#89B4FA`/`#94E2D5`/`#F9E2AF` para One Dark, `#F92672`/`#A6E22E`/`#E6DB74`
    para Monokai y `#0969DA`/`#8250DF`/`#1A7F37` para GitHub.
  - Los dos morados de `default` **se mudan literales** de `app/ui/theme/Color.kt` (RNF-103).
  - `TonalSchemeContrastTest`: para los ocho esquemas, `onSurface/surface`, `onSurfaceVariant/surfaceVariant`,
    `onBackground/background`, `onPrimary/primary`, `onPrimaryContainer/primaryContainer`, `onSecondary`,
    `onTertiary` y el par de `error` ≥ 4.5; `primary/surface` ≥ 3.
  - Si un par no llega, el arreglo es **la semilla o el tono de ese rol**, no bajar el umbral.
  - ⚠️ Salió un defecto real aquí: el tono de Material es un `L*` de CIELAB, **no** OKLab L. Tomarlos por la misma
    escala dejó las superficies oscuras en `#000101` en vez de `#121316`. La traducción va en `ligereza(tone)`, y el
    ajuste de acento tiene que mirar **dos** contrastes (3:1 contra la superficie y 4.5:1 con `onPrimary`), no uno.
  - Hecho cuando: `./gradlew :tema:testDebugUnitTest` verde y el catálogo tiene seis temas con sus dos
    esquemas salvo el dinámico.
  - Verde: *cero*. `Contrast.ratio` es puro y ya tiene su propia prueba en T1.

### T5. `LocalAppTheme`, el tema de la app y `isDark`

RF-162, RF-171, RNF-100

- [x] **T5. `MyApplicationTheme` toma el tema del catálogo y lo publica.**
  - `tema/.../LocalAppTheme.kt`: `staticCompositionLocalOf { AppThemes.dynamic }`.
  - `app/ui/theme/Theme.kt`: `MyApplicationTheme(appTheme = AppThemes.dynamic, darkTheme = isSystemInDarkTheme())`,
    resuelve el esquema y publica el `CompositionLocal`. Fuera el parámetro `dynamicColor`. `LocalContext` se
    sigue leyendo **solo** en la rama dinámica.
  - `app/ui/theme/DarkMode.kt`: `DarkModeOption.isDark(systemInDarkTheme: Boolean)`.
  - `DarkModeTest`: los tres ajustes con el sistema en claro y en oscuro.
  - Hecho cuando: `:app:assembleDebug` compila y `MyApplicationTheme { }` sigue siendo legal (las dos pruebas
    de captura no se tocan) y `DarkModeTest` pasa.
  - Verde: *cero* en `:app`; en `:tema` no se toca nada.

### T6. La preferencia: guardar el tema y migrar el interruptor

RF-163, RF-164, RF-165, RNF-104

- [x] **T6. `resolveStoredTheme`, repositorio y `ThemeViewModel`.**
  - `app/data/theme/ThemePreference.kt`: la regla de §3.3, **sin `android`**. Primero `ThemePreferenceTest` en
    rojo con las tres ramas, incluida la de identificador corrupto y la de `legacyDynamicColor == null`.
  - `PreferencesRepository`: fuera `getIsDynamicColor`/`setIsDynamicColor`; entra `getAppTheme`/`setAppTheme`.
  - `SharedPreferencesRepository`: lee `theme_id` y, solo si no está, el `dynamic_colors` antiguo.
  - `Constants.kt`: `THEME_ID_KEY = "theme_id"`; `DYNAMIC_COLORS_KEY` con KDoc de **solo lectura**.
  - `ThemeViewModel`: `appTheme: StateFlow<AppTheme>` + `setAppTheme`, fuera `isDynamicColor`.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest` verde y `:app:assembleDebug` compila.
  - Verde: *cero*. Nada lee `isDynamicColor` todavía porque las superficies se cablean en T7.

### T7. Cablear las seis superficies

RF-162

- [x] **T7. Toda superficie que pinta algo toma el tema del `ViewModel` o de la preferencia.**
  - `MainActivity`, `CrashReportActivity`, `CodeEditorActivity`: `appTheme` en vez de `dynamicColor`, `isDark()`
    en vez del `when` copiado.
  - `PdfViewerActivity` y `widget/SingleNoteConfigActivity`: **hoy ignoran la preferencia**; pasan a leerla.
  - `service/FloatingBubbleService`: lee el tema; `isDarkThemeEnabled` delega en `isDark()`.
  - Comprobación de que no queda ningún `MyApplicationTheme(dynamicColor` ni ningún `when (darkModeOption)`
    copiado: `grep` sobre `app/src/main` en el guion de cierre.
  - ⚠️ `PdfViewerActivity` y `SingleNoteConfigActivity` no necesitan `ThemeViewModel`: no cambian el tema, así que
    leen la preferencia una vez con `remember`. Un `ViewModel` que nunca emite es ruido.
  - Hecho cuando: `:app:assembleDebug` compila y el `grep` no encuentra restos.
  - Verde: *cero*. Es reescritura de llamadas.

### T8. El editor sigue al tema y pierde su selector

RF-166

- [x] **T8. El editor pinta con la paleta del tema activo y su menú del tema desaparece.**
  - `CodeEditorApp`: `val theme = AppThemes.editorPalette(LocalAppTheme.current)` en vez de
    `viewModel.syntaxTheme.collectAsState()`. **La firma no cambia.**
  - `CodeEditorViewModel`: fuera `_syntaxTheme` y `setSyntaxTheme`; `refreshPreview()` sigue por sus otras tres
    llamadas.
  - Fuera el ítem de menú `menu_theme` (con su `testTag`) y fuera `ThemeSelectorDialog` de `EditorDialogs`.
  - Fuera `menu_theme`, `theme_title` y `active_cd` de los **ocho** `strings.xml` del editor. `selected_cd` se
    queda: lo usa el diálogo de idiomas.
  - ⚠️ Con el diálogo fuera, `SyntaxTheme.name` se quedó sin reader: se borró en T10, porque los nombres visibles
    de los temas salen de `strings.xml` (D9) y dejar el campo sería dejar código muerto que además hay que
    mantener al añadir una paleta.
  - Hecho cuando: `:editor-de-codigo:assembleDebug` compila, los 149 tests del editor siguen pasando y
    `grep -rn "menu_theme\|theme_title" editor-de-codigo/src` no devuelve nada.
  - Verde: *cero*. La paleta que el editor recibía era `VSCodeDark`, que es justo la caída de `editorPalette`
    para el tema por defecto: el caso Dynamic no se mueve.

### T9. Los bloques de nota siguen al tema

RF-167

- [x] **T9. `CodeSyntax.highlight` recibe la paleta y el bloque ya no tiene una propia.**
  - `util/CodeSyntax.kt`: fuera `blockTheme`; `highlight(code, storedId, syntax)`.
  - `ReadOnlyTextBlock` y `EditableTextBlock`: `LocalAppTheme.current` →
    `remember(theme) { AppThemes.notePalette(theme) }`, y esa paleta en la clave del `remember` del resaltado.
    **Sin la paleta en la clave, cambiar de tema no repinta los bloques.**
  - `CodeSyntaxInvariantsTest`: reescribir el invariante de `blockTheme` (ahora: la paleta del bloque sale del
    tema y los siete campos de superficie van sin especificar) y el del `remember`, cuya clave pasa a
    `remember(annotated, highlightLanguage, syntax)`. Los dos reescritos, no relajados.
  - `CodeSyntaxTest`: actualizar las llamadas a `highlight` con la paleta del tema por defecto.
  - Hecho cuando: `:app:testDebugUnitTest` verde y `:app:assembleDebug` compila.
  - Verde: *cero*. Las pruebas que cambian cambian la **firma** que vigilan, no lo que vigilan.

### T10. El selector de Ajustes

RF-169, RF-170, RNF-102

- [x] **T10. La fila «Tema», su diálogo y las nueve claves en los nueve idiomas.**
  - `ui/settings/ThemePickerDialog.kt`: una fila por tema con la forma del que tenía el editor —dos muestras
    redondas de la paleta, nombre y marca de activo—, fila pulsable y descripción accesible.
    Para `dynamic`, que no tiene paleta, las muestras y el fondo salen de `MaterialTheme`; el KDoc dice por qué.
  - `ui/settings/ThemeLabels.kt`: `labelRes(theme)` con las seis claves. Primero `ThemeLabelsTest` en rojo:
    cada clave existe en los **nueve** `strings.xml`.
  - `SettingsScreen`: fuera el interruptor y su `HorizontalDivider`; entra la fila «Tema» antes de la de modo.
    `ThemeDialog` (Sistema/Encendido/Apagado) no se toca.
  - Nueve claves en los nueve idiomas: `settings_theme`, `theme_picker_title`, `theme_dynamic`,
    `theme_default`, `theme_vscode_dark`, `theme_one_dark`, `theme_monokai`, `theme_github_light`,
    `theme_active_cd`. Fuera `settings_dynamic_colors` y `settings_dynamic_colors_desc`.
  - Recordatorio de `AGENTS.md`: apóstrofo escapado (`\'`) en `values/`, `values-fr/` y `values-it/`, y
    compilar Kotlin **no** valida los recursos.
  - Hecho cuando: `:app:assembleDebug` compila (que es cuando AAPT ve los recursos) y `ThemeLabelsTest` verde.
  - Verde: *cero*.

### T11. Limpieza, invariantes y guion de verificación

RF-172

- [x] **T11. Fuera el tema muerto de `:visor-pdf` y cerrar la spec.**
  - Borrar `visor-pdf/src/main/java/com/example/ui/theme/{Theme,Color,Type}.kt`. Comprobar antes con `grep` que
    ningún `.kt` importa `com.estrin217.pdfviewer.ui.theme` (hoy no lo importa nadie).
  - `MEMORY.md`: estado, decisiones y el aprendizaje de que un catálogo compartido necesita un módulo cuando la
    dependencia va en un solo sentido.
  - `AGENTS.md`: la regla permanente que salga de aquí (un `CompositionLocal` con el dato, no con lo derivado;
    y las dos caídas distintas de `dynamic`).
  - El guion está en `verificacion.md`, y empieza por lo que más importa: **que nadie vea cambios**. Si el
    primer punto falla, no hay spec que valga.
  - Hecho cuando: `:app:assembleDebug :visor-pdf:assembleDebug` compila, el `grep` del invariante no encuentra
    restos y `MEMORY.md` y `AGENTS.md` están al día.
  - Verde: *cero*.

---

## Orden y dependencias

```
T1 → T2 → T3 → T4 → T5 → T6 → T7 → T8 → T9 → T10 → T11
            └────────── el catálogo existe ──────────┘
```

`T4` es la única que puede salir con un par de contraste en rojo y ajustes de semilla; si eso se alarga, se corta
en dos (semillas de un tema, luego los otros tres) sin tocar el orden.
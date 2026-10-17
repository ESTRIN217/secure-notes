# Plan — Spec 025 — Un tema para toda la app

## 1. Mapa de ficheros

### 1.1 Módulo nuevo `:tema` (`com.estrin217.tema`)

| Fichero | Responsabilidad |
|---|---|
| `SyntaxColors.kt` | `SyntaxTheme` (22 `Color`) **movida** del editor, sin tocar un valor. `SyntaxThemes` con las 4 paletas de hoy + `Standard`. `SyntaxTheme.forNoteBlock()`. |
| `AppTheme.kt` | `AppTheme` (id, `isDynamic`, `light?`, `dark?`, `syntax?`) y el catálogo `AppThemes` con sus funciones puras. |
| `TonalScheme.kt` | Generador tonal en OKLCH: de 3 semillas por tema a los ~36 roles de `ColorScheme`, con búsqueda de tono que garantiza contraste. |
| `Contrast.kt` | `relativeLuminance()` y `contrastRatio()` en WCAG, puras. |
| `LocalAppTheme.kt` | `LocalAppTheme: ProvidableCompositionLocal<AppTheme>`: el tema activo, leído por el editor y por los bloques de nota sin que `:editor-de-codigo` conozca `:app`. |
| `src/test/.../ThemeCatalogTest.kt` | Catálogo: seis temas, identificadores únicos, invariante `isDynamic ⟺ nulos`, paletas intactas. |
| `src/test/.../TonalSchemeContrastTest.kt` | Los ocho esquemas pasan AA y las semillas son las del catálogo. |
| `src/test/.../TonalSchemeColorTest.kt` | El generador es determinista, respeta el gamut sRGB y no mueve el tono del papel. |

`build.gradle.kts` calcado del de `:markdown`: mismo plugin, mismo `compileSdk`, `minSdk = 33`, BOM de Compose,
`material3`, `compose.ui`, `compose.ui.graphics` y `junit` de prueba. **Ni una dependencia de artefacto nuevo.**

### 1.2 `:editor-de-codigo` (consumidor)

| Fichero | Cambio |
|---|---|
| `syntax/SyntaxColors.kt` | **Se borra**: su contenido pasa a `:tema`. |
| `syntax/SyntaxHighlighter.kt`, `KotlinSyntaxHighlighter.kt` | Solo el `import` del tipo. |
| `ui/CodeEditorApp.kt` | `val theme = AppThemes.editorPalette(LocalAppTheme.current)` en vez de `viewModel.syntaxTheme`; se va el ítem de menú del tema (`menu_theme`, `testTag` incluido). **Sin cambio de firma.** |
| `ui/CodeEditorViewModel.kt` | Se quitan `_syntaxTheme`, `setSyntaxTheme` y el import de `SyntaxThemes`. `refreshPreview()` sigue vivo por sus otras tres llamadas. |
| `ui/components/EditorDialogs.kt` | Se borra `ThemeSelectorDialog`. |
| `res/values*/strings.xml` (8) | Se van `menu_theme`, `theme_title` y `active_cd`, que se quedan sin uso. |
| `build.gradle.kts` | `implementation(project(":tema"))`. |
| `src/test/...` (2 ficheros) | Solo el `import` del tipo. |

### 1.3 `:app`

| Fichero | Cambio |
|---|---|
| `ui/theme/Theme.kt` | `MyApplicationTheme(appTheme, darkTheme, content)`: resuelve el esquema del catálogo y **publica `LocalAppTheme`**. Sin el parámetro `dynamicColor`. |
| `ui/theme/Color.kt` | Los morados de hoy **se mudan** a `:tema`; aquí solo quedan colores que use el tema. |
| `ui/theme/ThemeLabels.kt` | **Nuevo**: `labelRes(theme)`: el nombre de cada tema sale de `strings.xml`. |
| `ui/theme/DarkMode.kt` | **Nuevo**: `DarkModeOption.isDark(systemInDarkTheme: Boolean)` puro, que sustituye a las cuatro copias del `when`. |
| `data/theme/ThemePreference.kt` | **Nuevo**: `resolveStoredTheme(storedId, legacyDynamicColor)` puro, la regla de migración de RF-164 y RF-165. |
| `data/PreferencesRepository.kt` | Fuera `getIsDynamicColor`/`setIsDynamicColor`; entra `getAppTheme`/`setAppTheme`. |
| `data/SharedPreferencesRepository.kt` | Lee `theme_id` y, solo si no está, el `dynamic_colors` antiguo. |
| `ui/viewmodel/ThemeViewModel.kt` | Fuera `isDynamicColor`; entra `appTheme` + `setAppTheme`. |
| `Constants.kt` | Entra `THEME_ID_KEY`. `DYNAMIC_COLORS_KEY` pasa a **solo lectura**, con KDoc. |
| `ui/settings/SettingsScreen.kt` | Fuera el interruptor; entra la fila «Tema» que abre el selector. El diálogo de modo no cambia. |
| `ui/settings/ThemePickerDialog.kt` | **Nuevo**: el selector, con la forma del que tenía el editor. |
| `MainActivity.kt`, `CrashReportActivity.kt`, `CodeEditorActivity.kt` | `appTheme` en vez de `dynamicColor`; `isDark()` en vez del `when`. |
| `PdfViewerActivity.kt`, `widget/SingleNoteConfigActivity.kt` | **Hoy ignoran la preferencia**: pasan a leerla (RF-162). |
| `service/FloatingBubbleService.kt` | Lee el tema; `isDarkThemeEnabled` delega en la función pura. |
| `util/CodeSyntax.kt` | Fuera `blockTheme`; `highlight(code, storedId, syntax)`. |
| `ui/ReadOnlyTextBlock.kt`, `ui/EditableTextBlock.kt` | Leen `LocalAppTheme` y lo pasan a `CodeSyntax.highlight`. |
| `res/values*/strings.xml` (9) | Entran `settings_theme`, `theme_picker_title`, `theme_dynamic`, `theme_default`, `theme_vscode_dark`, `theme_one_dark`, `theme_monokai`, `theme_github_light`, `theme_active_cd`. Fuera `settings_dynamic_colors` y `settings_dynamic_colors_desc`. |
| `src/test/.../CodeSyntaxInvariantsTest.kt` | El invariante de la paleta del bloque se reescribe (ya no hay `blockTheme`), y el `remember` del resaltado incluye la paleta. |
| `build.gradle.kts` | `implementation(project(":tema"))`. |

### 1.4 `:visor-pdf`

| Fichero | Cambio |
|---|---|
| `com/example/ui/theme/{Theme,Color,Type}.kt` | **Se borran los tres**: sin usuarios, y su `MyApplicationTheme` hace ambigua la firma del de `:app` (RF-172). |

## 2. Funciones puras

Todas en `:tema` salvo las dos marcadas, y **ninguna** necesita `Context` ni `android`:

| Función | Decide |
|---|---|
| `AppThemes.byId(id: String?): AppTheme` | Identificador desconocido o vacío → `dynamic`. |
| `AppThemes.schemeFor(theme, dark): ColorScheme?` | `null` si el tema es dinámico; si no, el esquema del modo. |
| `AppThemes.editorPalette(theme): SyntaxTheme` | `theme.syntax` o, si no hay, `VSCodeDark`. |
| `AppThemes.notePalette(theme): SyntaxTheme` | `(theme.syntax ?: Standard).forNoteBlock()`. |
| `SyntaxTheme.forNoteBlock(): SyntaxTheme` | Superficies y texto base a `Color.Unspecified`; **acentos intactos**. |
| `Contrast.ratio(a, b): Double` | WCAG 2.1 sobre luminancia relativa. |
| `TonalScheme.scheme(seeds, dark): ColorScheme` | Semillas + modo → `ColorScheme` completo. |
| `DarkModeOption.isDark(systemInDarkTheme): Boolean` (`:app`) | `SYSTEM` → el parámetro; `ON`/`OFF` → constantes. |
| `resolveStoredTheme(storedId, legacyDynamicColor)` (`:app`) | RF-164, RF-165. |
| `labelRes(theme): Int` (`:app`) | Nombre del tema → clave de `strings.xml`. |

## 3. Algoritmos

### 3.1 Generador tonal (`TonalScheme`)

```
esquema(semillas, oscuro):
    paletas = { primario, secundario, terciario, neutro, error } → RampaTonal(semilla)
    RampaTonal(semilla) = para tono en 0..100: oklch(L=tono/100, C=croma(tono)*cromaSemilla, H=matizSemilla)
    tonoPrimario = ajustarTono(primario, objetivo = superficie)      # ver abajo
    roles = TABLA_TONOS[oscuro]                                       # 36 pares (rol → tono)
    devolver lightColorScheme/darkColorScheme(roles → paletas[tono])

croma(tono) = 1.0 si 20 ≤ tono ≤ 70; cae a 0.25 en los extremos        # rampa fija, no HCT
ajustarTono(rampa, superficie, minimo = 3.0):
    t = tonoPrimarioDelModo          # 40 en claro, 80 en oscuro
    mientras ratio(rampa[t], superficie) < minimo y t puede moverse hacia el extremo:
        t = t - 2                    # más oscuro en claro, más claro en oscuro
    devolver t
```

Todo es determinista. La conversión `sRGB ↔ OKLCH` es aritmética; el gamut se respeta reduciendo croma en
pasos de 1/32 hasta que el color cabe en sRGB.

**Por qué el ajuste de tono y no solo la rampa:** las semillas salen de paletas reales, con matices claros
(el verde de Monokai `#A6E22E` a tono 40 sobre superficie 98 se queda en 1.2:1). Sin el ajuste, el generador
produciría un tema que ni el test de contraste ni una persona pasarían. El ajuste mueve el tono, no el
matiz, así que el tema sigue siendo reconocible como el de su familia.

### 3.2 Invariante del catálogo

```
isDynamic ⟺ light == null ∧ dark == null ∧ syntax == null
¬isDynamic → light != null ∧ dark != null ∧ syntax != null
```

Se comprueba en `AppThemes.all` y en `byId` con datos inventados. Un tema a medio declarar no puede entrar.

### 3.3 Migración de la preferencia (`resolveStoredTheme`)

```
SI storedId existe en el catálogo            → ese tema
SI no, y legacyDynamicColor == false          → default
EN OTRO CASO                                  → dynamic
```

`legacyDynamicColor` es `null` cuando la clave antigua **no está nunca se escribió**; quien la tiene apagada
tiene `false`. Sin escribir nada: leer la clave antigua no la borra (la conservan `DYNAMIC_COLORS_KEY` y
`DARK_MODE_KEY`, que ya conviven así).

## 4. Interfaz

```kotlin
// :tema
data class AppTheme(
    val id: String,
    val isDynamic: Boolean = false,
    val light: ColorScheme? = null,
    val dark: ColorScheme? = null,
    val syntax: SyntaxTheme? = null,
)
val LocalAppTheme: ProvidableCompositionLocal<AppTheme>

// :app
@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppThemes.dynamic,   # valor por defecto: las pruebas de captura no cambian
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
)
```

`MyApplicationTheme` resuelve el esquema y publica `LocalAppTheme`. Quien pinte sin tema (una prueba, una
previsualización) recibe el valor por defecto.

`CodeEditorApp` **no cambia de firma**: lee `LocalAppTheme.current` y deriva la paleta del editor con
`AppThemes.editorPalette`. Esa es la razón de que el paso 1 sea un módulo y no una carpeta.

## 5. Decisiones

### D1. El catálogo vive en un módulo nuevo, no en `ui/theme/` de `:app`

**Descartado: mover las paletas a `com/example/ui/theme/`.** `:app` depende de `:editor-de-codigo`, no al revés;
el editor se quedaría sin catálogo, que es justo lo que hay que evitar.
**Descartado: dejarlo en `syntax/` del editor.** Funciona (`:app` lo ve), pero el catálogo de **temas de la app**
seguiría siendo una sublínea de un módulo que es un editor de código.
**Elegido: `:tema`.** `:markdown` ya es un módulo exactamente por esta razón (la spec 019 lo dice), así que hay
precedente y el coste real es un `build.gradle.kts` y un `include`.

### D2. Los ocho esquemas se generan, no se escriben a mano

**Descartado: 36 roles × 8 esquemas = 288 literales.** Ningún revisor los lee y el error de un tono no se ve
hasta que alguien elige el tema en un móvil.
**Descartado: `dynamicLightColorScheme` con el `ColorScheme` del catálogo.** No existe un generador tonal
público en Material 3: solo los de `Context`, que son los dinámicos.
**Elegido: generador OKLCH con los tonos que fija Material por rol.** Tres semillas por tema, una tabla de
tonos copiada de la línea base de Material 3, gamut sRGB respetado y una prueba que juzga el resultado. Es una
aproximación y su KDoc lo dice: no es HCT.

### D3. `AppTheme` plano con campos nulos, no `sealed`

**Descartado: `sealed` con `DynamicTheme` y `FixedTheme`.** Obliga a un `when` en cada consumidor, y el
consumidor más numeroso (el `CompositionLocal`) solo necesita el valor. Con la invariante de 3.3 comprobada por
prueba, la seguridad del `sealed` no aporta nada que el test no dé.

### D4. El tema dinámico no tiene paleta propia

Es lo que mantiene RF-164/RF-168 y HU-4. Si le diéramos una paleta, quien hoy ve Material You + VS Code Dark
abriría la app con un editor de otro color. Las dos caídas (`editorPalette` → `VSCodeDark`, `notePalette` →
`Standard`) son distintas **a propósito**, y por eso el `CompositionLocal` lleva el `AppTheme` entero y no una
paleta ya derivada: si llevara la paleta, habría que elegir una de las dos y perder la otra.

### D5. Un solo `CompositionLocal`, no dos

**Descartado: `LocalEditorPalette` + `LocalNotePalette`.** Dos globales que siempre tienen que
moverse juntas.
**Elegido: `LocalAppTheme`**, y cada consumidor deriva lo que necesita con una función pura. Añadir una tercera
superficie que pinte con el tema no obliga a tocar la que ya existe.

### D6. Fuera el selector del editor

**Descartado: dejarlo y que arranque desde el tema global.** Dos caminos para lo mismo, y el que está en el
editor seguiría sin guardarse —una opción dentro de la app que se pierde al reiniciar—, lo que es el
problema que esta spec viene a cerrar. El menú del editor pasa a ser, en su lugar, el de Ajustes.

### D7. `forNoteBlock()` es una función, no un `SyntaxTheme` a mano

**Descartado: seguir con `CodeSyntax.blockTheme` y cambiar sus colores al vuelo.** Habría que decidir, para
cada superficie, qué campo se ignora, y con veintidós campos es fácil equivocarse. Una función que pone a
`Unspecified` **exactamente** los siete campos de superficie deja los catorce de acento intactos, y eso lo
comprueba el test.

### D8. `isDark()` como función pura, no cuatro `when`

Hoy el `when` está copiado en `MainActivity`, `CrashReportActivity`, `CodeEditorActivity` y el servicio de la
burbuja (con una variante para `Configuration`). La decisión es pura («¿qué modo corresponde a este ajuste?»);
el `Context` se queda en quien llama, como manda `AGENTS.md`.

### D9. Los nombres de los temas en los `strings.xml` de `:app`

**Descartado: recursos en `:tema`.** Obligaría a crear nueve ficheros de idioma en un módulo que solo tiene
datos, y a repartir el vocabulario visible de la app en dos sitios.
**Elegido: `labelRes()` en `:app`.** Una función pura con seis entradas, seis claves en los nueve idiomas que
ya existen, y el módulo nuevo sin un solo recurso. La lista de idiomas de la app es una, y esta es ella.

## 6. Estrategia de tests

Todo lo nuevo es **puro**, así que corre en JVM sin Robolectric — que aquí no funciona (`AGENTS.md`).

| Prueba | Dónde | Qué fija |
|---|---|---|
| `ThemeCatalogTest` | `:tema` | Seis temas y sus identificadores; unicidad; invariante de nulos; **las cuatro paletas con los mismos valores** y `default` con sus morados (RNF-103, RF-160). |
| `TonalSchemeContrastTest` | `:tema` | Los ocho esquemas pasan AA en los pares que importan (RNF-101). |
| `TonalSchemeColorTest` | `:tema` | Determinismo, gamut sRGB, y que un color de papel se queda en su tono. |
| `ThemeFallbackTest` | `:tema` | `schemeFor` devuelve `null` para el dinámico y el esquema del modo para el resto; `editorPalette` y `notePalette` caen donde deben (RF-161, RF-168). |
| `NoteBlockPaletteTest` | `:tema` | `forNoteBlock()` pone a `Unspecified` los siete campos de superficie y **no toca ningún acento** (RF-167, D7). |
| `ThemePreferenceTest` | `:app` | Las tres ramas de `resolveStoredTheme`, incluida la de identificador corrupto (RF-164, RF-165). |
| `DarkModeTest` | `:app` | Los tres ajustes, con el parámetro del sistema en `true` y en `false` (RF-171). |
| `ThemeLabelsTest` | `:app` | Las seis claves existen en los nueve idiomas (RNF-102). |
| `CodeSyntaxInvariantsTest` | `:app` | Se reescribe el invariante de la paleta del bloque y el `remember` incluye la paleta. |
| `CodeEditorUnitTest`, `MarkdownSyntaxHighlightTest` | `:editor-de-codigo` | Solo cambian de `import`: las aserciones no se tocan (prueba de que el mudanza no movió nada). |
| `ThemeSelectorDialog` + `MyApplicationTheme` | — | Sin prueba de captura: Robolectric no corre aquí. Va a dispositivo. |

## 7. Trazabilidad

| RF | Dónde |
|---|---|
| RF-159 | `:tema` + `implementation(project(":tema"))` en `:app` y `:editor-de-codigo` |
| RF-160 | `AppThemes.all` |
| RF-161 | `TonalScheme` + invariante del catálogo |
| RF-162 | `MyApplicationTheme` en las seis superficies, incluidas `PdfViewerActivity` y el widget |
| RF-163 | `SharedPreferencesRepository` + `ThemeViewModel` |
| RF-164, RF-165 | `resolveStoredTheme` |
| RF-166 | `LocalAppTheme` leído en `CodeEditorApp` |
| RF-167 | `notePalette` + los dos bloques de código |
| RF-168 | `editorPalette` / `notePalette` con sus dos caídas |
| RF-169 | `ThemePickerDialog` + la fila de Ajustes |
| RF-170 | Fuera el interruptor y sus dos claves |
| RF-171 | `DarkModeOption.isDark` + `darkTheme` |
| RF-172 | Borrar los tres ficheros de `:visor-pdf` |
| RNF-99 | El `build.gradle.kts` de `:tema` |
| RNF-100 | §2: las nueve funciones puras |
| RNF-101 | `TonalSchemeContrastTest` |
| RNF-102 | `labelRes` + `ThemeLabelsTest` |
| RNF-103 | `ThemeCatalogTest` |
| RNF-104 | `resolveStoredTheme` |
| RNF-105 | No se toca `NoteContentBlock`, `NoteDao` ni `DataBlock` |
| RNF-106 | El módulo no declara ninguna dependencia de red |
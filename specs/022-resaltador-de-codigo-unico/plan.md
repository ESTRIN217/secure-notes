# Spec 022 — Plan

> Adjunto a `spec.md` (spec 022, **aprobada**). Los RF/RNF se citan tal cual. El plan describe **cómo**, con
> su alternativa descartada; los nombres de fichero son los reales del repo.

## Alcance de ficheros

### `:app` — nuevo

- **`app/src/main/java/com/example/util/CodeSyntax.kt`** — **puro** (no importa `android` ni
  `androidx.compose`): el pegamento entre el catálogo del editor y el bloque de código. Contiene:
  - la tabla de **identificador guardado → `SupportedLanguage`** (`fromStoredId`), con los alias de
    compatibilidad;
  - la vuelta (`storedIdFor`);
  - la **paleta** del bloque como `SyntaxTheme` (los hex de hoy, con `text = Color.Unspecified`);
  - `highlight(code: String, storedId: String?): AnnotatedString`, que delega en
    `SyntaxHighlighter.highlight(...)`;
  - `options(): List<CodeLanguageOption>` — las opciones visibles (mismo conjunto y orden que hoy).
  Sí importa `androidx.compose.ui.text` (es el tipo de retorno del resaltado) pero **nada de `android`**: se
  prueba sin Robolectric (RNF-73 de la 020/hábito del repo).

- **`app/src/main/java/com/example/ui/CodeLanguageSheet.kt`** — la hoja de selección, extraída de
  `CodeLanguages.kt`, alimentada por `CodeSyntax.options()`. Usa `stringResource` **solo** para las dos
  etiquetas que hoy son traducción.

- **`app/src/test/java/com/example/CodeSyntaxTest.kt`** — pruebas puras del pegamento (ver estrategia).

### `:app` — modificado

- **`ui/ReadOnlyTextBlock.kt`** — `CodeHighlighter.highlight(annotated, highlightLanguage)` →
  `CodeSyntax.highlight(annotated, highlightLanguage)`. RF-131.
- **`ui/EditableTextBlock.kt`** — ídem. RF-131.
- **`ui/WysiwygCodeBlock.kt`** — etiqueta vía `CodeSyntax` y `CodeLanguageSheet` nueva; el `onSelected`
  guarda `storedIdFor(...)`. RF-132, RF-136, RF-139.

### `:app` — borrado

- **`util/CodeHighlighter.kt`** (387 líneas) — RNF-77.
- **`ui/CodeLanguages.kt`** — su contenido se reparte entre `CodeSyntax.kt` y `CodeLanguageSheet.kt` (RNF-77).
- **`test/java/com/example/CodeHighlighterTest.kt`** — probaba la implementación eliminada. Su cobertura ya vive
  en el módulo del editor (p. ej. `MarkdownSyntaxHighlightTest`, `KotlinSyntaxHighlighter`); no se reescribe.

### `:editor-de-codigo` — sin cambios

El resaltador (`SyntaxHighlighter`), el catálogo (`SupportedLanguage`) y los temas (`SyntaxThemes`) **no se
tocan**. No se le añade ninguna dependencia hacia `:app` (RNF-76). `CodeLanguageDetector` no se toca (fuera de
alcance).

## Funciones puras

```kotlin
// CodeSyntax.kt  — puro, probado sin Robolectric

fun fromStoredId(id: String?): SupportedLanguage?      // RF-133, RF-135, RF-137
fun highlight(code: String, storedId: String?): AnnotatedString  // RF-131, RF-138
fun options(): List<CodeLanguageOption>                // RF-132, RF-136, RF-139
```

`CodeLanguageOption` = `(storedId: String, language: SupportedLanguage, labelRes: Int?, labelLiteral: String?)`.

> **Sin función inversa.** No hay `storedIdFor(language)`: `storedId` no es único por idioma (`toml` e `ini`
> comparten `INI_TOML`), así que **cada opción lleva su propio identificador** y el selector guarda el de la
> opción elegida. Una inversa tendría que elegir un canónico y dejaría una opción sin poder volver.

## Algoritmo (pseudocódigo)

```
fromStoredId(id):
    si id es null, vacío o solo espacios → null            // RF-137: sin idioma ⇒ sin resaltado
    clave = id.trim().lowercase()
    "toml" | "ini" → INI_TOML                              // dos identificadores, un idioma
    "bash" | "shell" → BASH
    ENTRADA_POR_ID[clave] ?: null                          // desconocido ⇒ null  (RF-135)

highlight(code, storedId):
    lang = fromStoredId(storedId) ?: return AnnotatedString(code)   // plano, sin estilos
    return SyntaxHighlighter.highlight(code, lang, BlockSyntaxTheme)

options():
    lista fija (mismo orden y mismas etiquetas que hoy), cada una con su storedId y su idioma
```

`ENTRADA_POR_ID` es una tabla **constante** de 24 pares (los identificadores que la app guarda o detecta hoy).
No contiene listas de palabras clave ni de tipos: eso es del módulo (RF-132).

## Interfaz

- `WysiwygCodeBlock(...)`: **firma sin cambios**; solo cambia de dónde saca etiqueta y opciones.
- `CodeLanguageSheet(current, onSelected, onDismiss)`: **firma sin cambios**; se muda de fichero.
- `ReadOnlyTextBlock` / `EditableTextBlock`: **firma sin cambios**; cambia la llamada interna.

Ninguna superficie fuera del bloque de código se entera del cambio.

## Paleta del bloque (`BlockSyntaxTheme`)

Los hex que hoy usa `CodeHighlighter`, colocados en los campos del `SyntaxTheme` del módulo; el color base va
**sin especificar** para que lo siga poniendo el tema Material (RF-134):

| Estilo de hoy | Hex | Campo de `SyntaxTheme` |
|---|---|---|
| keyword | `#3B82F6` | `keyword` |
| string | `#16A34A` | `string` |
| stringInterpolation | `#F59E0B` | `variable` |
| number | `#0891B2` | `number` |
| comment | `#8B93A3` | `comment` |
| type | `#9333EA` | `type` |
| annotation | `#10B981` | `annotation` |
| function | `#A855F7` | `function` |
| attribute | `#F59E0B` | `propertyKey` |
| bracket | `#8B93A3` | `punctuation` |
| primitive | `#0D9488` | `operator` |
| booleans (`true`/`false`) | usa keyword | `booleanNull` = `#3B82F6` |
| base text | tema Material | `text` = `Color.Unspecified` |

`background`, `gutterBackground`, `activeLineBackground`, `lineNumber`, `activeLineNumber` y
`searchMatchBackground` quedan sin especificar: el bloque **no** los usa y no debe pintar fondo propio.

## Decisiones (con alternativa descartada)

1. **El catálogo es `SupportedLanguage`; `:app` solo mapea y etiqueta.** `options()` es una lista de
   *identificadores guardados* con su idioma, no una definición de lenguajes. *Descartado:* mantener la lista de
   26 entradas en `:app` con su unión de lenguajes — sería la segunda lista que RF-132 prohíbe.
2. **`text = Color.Unspecified`.** `SyntaxHighlighter` escribe el color base `0..length`; con `Unspecified` el
   `SpanStyle` es vacío y el color lo sigue dando el `TextStyle` de la app (RF-134). *Descartado:* usar
   `SyntaxThemes.all` — traería fondo y texto del editor y cambiaría el aspecto.
3. **Se siguen guardando los identificadores de hoy en minúsculas** (`"kotlin"`, `"toml"`, `"ini"`…), no
   `enum.name`. *Descartado:* guardar `"KOTLIN"`/`"INI_TOML"` — obligaría a migrar notas y rompería RF-133.
4. **`toml` e `ini` siguen siendo dos opciones**, ambas resueltas a `INI_TOML`. *Descartado:* agruparlas en una
   («INI / TOML») — cambia lo que ve el usuario (decisión de quien Decide: mantener).
5. **El resaltado vive dentro de `remember(texto, idioma)`** en los dos composables, como hoy (RNF-78).
   *Descartado:* llamar al resaltado directamente en el `VisualTransformation` sin memorizar — recalcularía por
   fotograma.
6. **`CodeLanguageDetector` se queda intacto.** *Descartado:* unificarlo ahora — es otra spec y hoy solo lo usan
   las pruebas.
7. **Cero cadenas nuevas.** Se reutilizan `code_lang_plain` y `code_lang_shell`; el resto de etiquetas son
   literales (nombres propios de lenguaje), como hoy (RNF-80). *Descartado:* meter los 24 nombres en
   `strings.xml` y sus 9 locales — es una limpieza legítima pero infla esta spec en 200+ entradas.

## Riesgos

- **La clasificación de tokens cambia** (RF-131 lo exige): alguna palabra puede colorearse distinto a hoy. La
  paleta y el fondo no. Está escrito en el caso límite 9 y en RF-134.
- **`SyntaxHighlighter.applyRegex` compila el patrón en cada llamada** (no cachea). Con `remember` corre una vez
  por texto, no por fotograma; si en dispositivo se notara, cachear patrones sería una spec aparte.
- **`RichTextConverter`** (export/HTML) también lee `meta["language"]`, pero **solo como cadena**: no depende de
  la lista de idiomas y no se toca.

## Estrategia de tests

Pruebas **puras** (`:app`, sin Robolectric — es lo único que corre aquí):

- `CodeSyntaxTest`:
  - **RF-133**: `fromStoredId` de **cada** identificador que la app guarda hoy (los 26 de `CodeLanguages.all`
    menos el vacío) y de cada salida de `CodeLanguageDetector` → idioma correcto, ninguno `null`.
  - **RF-135/137**: `null`, `""`, `"   "` y `"rust2020"` → `null`; `highlight` devuelve texto plano sin estilos.
  - **RF-138**: `highlight(x, id).text == x` para varios idiomas.
  - **RF-136**: cada opción de `options()` resuelve a un idioma (ninguna `null`).
  - **RF-139**: la etiqueta de cada opción es la de hoy (26 etiquetas esperadas, incluidas las dos de recurso).
  - **RF-132**: `options()` no reenumera lenguajes: se compara el conjunto de idiomas de las opciones con un
    subconjunto esperado del enum, y el invariante de fuente comprueba que `:app` no tiene listas de palabras
    clave.

**Invariantes** (recorrido de fuentes, quitando comentarios antes de buscar):

- **RNF-77**: `:app/src/main` no contiene `CodeHighlighter` ni `CodeLanguages`, y los ficheros no existen.
- **RF-132**: ningún fichero de `:app/src/main` define conjuntos de palabras clave para resaltar.
- **RNF-76**: `:editor-de-codigo/src/main` no menciona `com.example.`.
- **RF-134/RNF-78**: los dos composables resaltan dentro de un `remember`.

Comandos:

```bash
./gradlew :app:compileDebugUnitTestKotlin --rerun
./gradlew test --continue          # comparar fallos POR CLASE con la base 742/41 y 136/2
```

**Cobertura por RF del plan:** RF-131 → composables + `highlight`; RF-132 → `options` + invariante; RF-133/135/137
→ `fromStoredId`/`highlight`; RF-134 → composables + paleta; RF-136 → `options`/hoja; RF-138 → `highlight`;
RF-139 → `options` + hoja; RNF-76/77/78/80 → invariantes; RNF-79 → comandos.

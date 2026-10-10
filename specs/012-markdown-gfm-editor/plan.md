# Plan — Spec 012 · Markdown GFM: resaltado, mitad y mitad y vista previa

Spec: `specs/012-markdown-gfm-editor/spec.md`

Cubre RF-64 a RF-75 y RNF-26 a RNF-32.

## 1. Archivos y responsabilidades

Todo dentro de `:editor-de-codigo`. **`:app` no se toca** (el paso a notas es la spec 013).

| Archivo | Cambio | ¿Puro? |
|---|---|---|
| `syntax/MarkdownTokens.kt` *(nuevo)* | `MdTokenKind`, `MdToken`, `MarkdownTokenizer.tokenize(md): List<MdToken>` | ✅ |
| `syntax/SyntaxHighlighter.kt` | `highlightMarkdown` pasa de 7 regex a mapear tokens → `SpanStyle` | ❌ |
| `markdown/MarkdownRenderer.kt` *(nuevo)* | `PreviewTheme`, `MarkdownRenderer.toHtmlDocument(md, theme): String` | ✅ |
| `ui/EditorViewMode.kt` *(nuevo)* | `ViewMode`, `EditorModes.availableFor(language)` | ✅ |
| `ui/CodeEditorViewModel.kt` | `viewMode`, `previewHtml`, `setViewMode`, `setLanguage` revalida | ❌ |
| `ui/components/MarkdownPreview.kt` *(nuevo)* | `AndroidView(WebView)` + `WebViewClient` | ❌ |
| `ui/components/CodeEditorView.kt` | `isEditMode: Boolean` → `isEditable: Boolean` | ❌ |
| `ui/CodeEditorApp.kt` | selector de modos, `Row` con dos mitades, `CodeEditorApp` sin cambios de firma | ❌ |
| `build.gradle.kts` | commonmark (core + tables + strikethrough) | — |
| `res/values*/strings.xml` (×8) | 6 claves nuevas | — |

### La regla que ordena el resto

> **Un token es un intervalo `[start, end)` con un tipo. Dos tokens del mismo tipo nunca se solapan,
> y un token emitido bloque nunca se reemite en línea.**

Todo lo demás —la precedencia, el `finally`, que no haya dos colores encima— sale de esa regla. Por
eso `MdToken` lleva **un solo** `kind` y no una lista de estilos: si pudiera llevar dos, la pregunta
«cuál gana» volvería a estar abierta en el dibujado, que es el error que la spec 008 ya pagó.

## 2. El tokenizador

### Por qué no más regex

El resaltador actual falla por **precedencia**, no por cantidad de patrones. `applyRegex` aplica
`SpanStyle` encima de lo que haya, así que el resultado depende del orden de llamadas. Con siete
llamadas el orden es legible; con veinte es imposible de revisar a ojo. Un tokenizador por prioridad
convierte el orden en **datos**: cada construction dice quécedence sobre cuál, y una prueba puede
comprobarlo sin mirar colores.

`AGENTS.md` avisa de la trampa relacionada: `KotlinSyntaxHighlighter` ya existe como fichero aparte
porque el resaltador de Kotlin necesitó lógica que no cabía en un regex. Esto es lo mismo, un paso
más allá.

### El algoritmo, en dos pasadas

```
tokenize(md):
  1. front matter: si md empieza con "---\n", busca el cierre "---\n"; si existe,
     emite FRONT_MATTER [0, cierre] y sigue desde ahí
  2. recorrer LÍNEA a LÍNHA sobre el tramo restante:
     a. cercas:  ``` o ~~~ de al menos 3 chars → hasta el cierre o hasta el fin (caso límite 1)
        el cuerpo se emite CODE_BLOCK, la línea de apertura y la de cierre CODE_FENCE
        → CONTINÚA, no se pasa la pasada en línea (RF-65)
     b. tablas:  | a | b | seguido de |---|---| → TABLE_ROW por fila, TABLE_DELIM por tubería
        → CONTINÚA
     c. ATX: ^ {0,3}#{1,6}(\s|$) → HEADING_MARK en los #, HEADING en el resto
     d. setext: línea seguida de ^ {0,3}(=+|-+)\s*$ → HEADING_MARK arriba, HEADING abajo
     e. thematic break: ^ {0,3}([-*_])[ \t]*\3[ \t]*\3[\s*_\t]*$ → THEMATIC_BREAK
     f. blockquote: ^ {0,3}>+ → QUOTE_MARK, y el resto de la línea es en línea
     g. lista: ^(\s*)([-+*]|\d+[.)])\s+ → BULLET_MARK/ORDERED_MARK en el marcador
        si le sigue "[ ]" o "[x]" → TASK_MARK
     h. referencia: ^ {0,3}\[([^\]]+)\]:\s+(\S+) → LINK_REF
     i. el resto de la línea → pasada en línea (paso 3)
  3. pasada en línea sobre los tramos que quedaron:
     escapes \x → ESCAPE (literal, sin estilo)
     código en línea: cuenta de acentos graves igual en apertura y cierre, EN LA MISMA LÍNEA (caso 4)
     autolink <http…> / <mailto:…>
     HTML embebido <etiqueta …>
     enlace/imagen: [texto](destino "título") | [texto][ref] | [ref] | ![alt](destino)
       LINK_TEXT en el texto, LINK_URL en el destino
     strike: ~~…~~
     strong: **…** o __…__   (delimitadores que no pueden abrir/cerrar dentro de una palabra)
     em: *…* o _…_            (idem)
     el texto que no casa con nada no emite token
```

Lo emite todo en **un único buffer de `start..end`**, ordenado y sin solapes, porque cada rama consume
su rango exacto y avanza el cursor. Cuando una construcción de bloque se reconoce, la línea **no**
vuelve a la pasada en línea: esa es la materialización de RF-65.

### El `finally` que evita el peor fallo

`try { … } catch { }` sobre cada regex es hoy la razón de que un patrón mal escrito **no rompa nada
visiblemente**: se traga la excepción y el texto sale sin colorear, en silencio. Eso es exactamente lo
que `AGENTS.md` prohíbe con «nunca tragar excepciones en silencio». El tokenizador **no puede**
tragarse fallos: un rango fuera de `0..length` es un invariante roto, y hay un test que lo comprueba
en todos los ficheros de prueba. El fallo se ve al escribir el test, no en un `.kt` de 40 000 líneas
sin colorear.

## 3. El mapa token → color

Usa **solo** colores que ya existen en `SyntaxTheme` (RNF-28). La elección reparte las construcciones
en familias que los cuatro temas ya distinguen:

| Token | Color | Razonamiento |
|---|---|---|
| `HEADING`, `HEADING_MARK` | `keyword` bold | Es lo más estructurado del bloque; `keyword` es lo que ya tiñen los encabezados del resaltador actual |
| `CODE_FENCE`, `CODE_BLOCK` | `string` | El cuerpo de una cerca **es** una cadena en Markdown, y `string` ya está en los cuatro temas |
| `INLINE_CODE` | `function` | Distinto de `string` para que el código en línea se distinga del bloque, como ya los distingue el resaltador actual |
| `STRONG` | `propertyKey` bold | `**` es énfasis fuerte |
| `EM` | `propertyKey` | `*` es énfasis simple |
| `STRIKETHROUGH` | `propertyKey` con `LineThrough` | La familia de énfasis, distinguida por el tachado, no por el color |
| `LINK_TEXT`, `LINK_URL` | `keyword` / `function` | El destino es lo accionable |
| `BULLET_MARK`, `ORDERED_MARK` | `operator` bold | Un marcador de lista es un operador de bloque, como en YAML |
| `TASK_MARK` | `booleanNull` | `[x]` es un valor, y `booleanNull` es el color de los literales en los cuatro temas |
| `THEMATIC_BREAK` | `comment` | Un separador es decorativo |
| `QUOTE_MARK` | `comment` | El `>` es estructura, no contenido |
| `TABLE_DELIM` | `punctuation` | La tubería |
| `TABLE_HEADER` | `type` bold | La fila de cabecera es el «tipo» de la tabla |
| `TABLE_ROW` | `text` | Las de datos son texto normal |
| `FRONT_MATTER` | `comment` | No se muestra en el render, se marca como metadato |
| `ESCAPE`, `HTML_TAG`, `AUTOLINK` | `annotation`, `propertyKey`, `variable` | — |

`SyntaxColors.kt` **no se toca**. El mapa vive en `SyntaxHighlighter.kt`, junto al resto del dibujado.

## 4. El renderizador

### Por qué commonmark y por qué esa versión

`compose-markdown` ya arrastra `com.atlassian.commonmark:commonmark:0.13.0`, `commonmark-ext-gfm-tables`
y `commonmark-ext-gfm-strikethrough`. Declararlos en `:editor-de-codigo` **no descarga nada y no
cambia lo que resuelve Gradle**: el APK ya los lleva dentro (RNF-29).

Se comprobó que el jar es seguro para Android antes de decidir: **bytecode Java 7** (`major version: 51`),
sin referencias a `java.awt`, `javax.swing` ni `java.sql` en `DocumentParser`, y licencia BSD-2-Clause.

**Alternativa descartada —escribir el renderizador.** Descartada por coste, no por gusto: GFM son
algunas cientos de líneas de casos límite (anidamiento de énfasis, escapes en tablas, definiciones de
referencia) y aquí no hay forma de comprobarlas más que con tests. Un tokenizador propio para
**resaltar** es barato porque el error es visible; un renderizador propio para **convertir** es caro
porque un error se ve como una maquetación rare, y eso no se prueba con un `assertEquals`.

**Alternativa descartada —subir a commonmark 0.22**, que trae `ext-gfm-task-list-items` y
`ext-gfm-footnotes`. Descartada porque **forzaría la versión transitiva que usa `compose-markdown`
0.7.2** (consumido en `UpdateInfoScreen` y `ChangelogSheet`), y un salto de API en 0.13→0.22 no es
gratis. En la 0.13 no existe la extensión de listas de tarea: se resuelve con un post-proceso de seis
líneas sobre el HTML **que ya generamos nosotros**, que es un texto en el que sí se puede(pattern)ar sin
miedo.

### El HTML

```
MarkdownRenderer.toHtmlDocument(markdown, PreviewTheme):
  1. parser = Parser.builder().extensions([TablesExtension, StrikethroughExtension]).build()
  2. html    = HtmlRenderer.builder().escapeHtml(true).build().render(parser.parse(markdown))
     → escapeHtml(true) neutraliza <script>, <iframe> y onerror= (RF-71)
  3. src = neutralizeRemoteSources(html)      → src="http…"/"//…" pasa a un marcador (RF-70)
     → src="javascript:…" y src="data:…" desaparecen
  4. html = markTaskItems(html)               → <li>[ ] … pasa a <li class="task">
  5. devuelve el documento completo con el <style> de PreviewTheme (RF-72)
```

`PreviewTheme` son **cadenas hexadecimales**, no `Color`: así el renderizador es puro (RNF-26) y el
`Color` → hex vive en el Composable, que sí puede usar Compose. Es la misma frontera que ya usa
`SyntaxTheme` en el otro sentido.

El CSS es **embebido**, sin fuentes ni hojas externas: nada de lo que se pinta puede salir a la red.
Incluye `prefers-color-scheme` no hace falta, porque el color lo pone `PreviewTheme`, no el sistema.

### El `WebView`

`MarkdownPreview.kt`, modelado sobre `InAppYouTubePlayer` de `:visor-media`, que es el único
`WebView` del repo y ya resolvió los mismos problemas:

| Ajuste | Valor | Por qué |
|---|---|---|
| `javaScriptEnabled` | `false` | Una vista previa no necesita JS y sin él no hay superficie de XSS |
| `domStorageEnabled` | `false` | No se guarda nada entre recargas |
| `allowContentAccess` | `false` | El contenido es el Markdown del usuario, no un `content://` |
| `allowFileAccess` | `true` | **Necesario** para RF-73: imágenes locales relativas |
| `allowFileAccessFromFileURLs` | `false` | Sin esto, una página `file://` puede leer otras |
| Base URL | `file://<carpeta del archivo>` | RF-73. Sin archivo en disco, `https://localhost/` |

`WebViewClient.shouldOverrideUrlLoading` manda `http`, `https` y `mailto` **fuera** con un Intent; el
resto se ignora. El `onDispose` hace `stopLoading()`, `loadUrl("about:blank")` y `destroy()` (RNF-30),
que es lo que evita la fuga de `InAppYouTubePlayer`.

**Alternativa descartada —`androidx.webkit`.** Descartada: es una dependencia nueva para nada, y los
ajustes que hacen falta están todos en `WebSettings` desde API 17.

## 5. Los modos

### Por qué un `ViewMode` y no dos booleanos

`isEditMode` ya existe y funciona; el problema es que `SPLIT` y `PREVIEW` no son «editar o no editar»:
son **dos formas de presentar los dos lados**. Con booleanos habría que añadir `isSplit` y `isPreview`
y un invariante que nadie va a mantener: ¿qué pasa si `isSplit` e `isPreview` son verdad a la vez? La
pregunta se responde sola con un `enum`.

```kotlin
enum class ViewMode { EDIT, VIEW, SPLIT, PREVIEW }

object EditorModes {
    fun availableFor(language: SupportedLanguage): List<ViewMode> =
        if (language == SupportedLanguage.MARKDOWN) ViewMode.entries
        else listOf(ViewMode.EDIT, ViewMode.VIEW)     // RF-66
}
```

`availableFor` es **puro y testeable**: la regla «Markdown tiene cuatro modos, el resto dos» está en un
sitio comprobable, no repartida por los `if` de un Composable.

`SPLIT` y `PREVIEW` implication: cambiar a un modo no disponible al cambiar de archivo **resetea a
`EDIT`** (caso límite 7). Se hace en `setLanguage()`, que es donde ya se revalida el archivo.

### El chip, no una barra segmentada

La barra superior ya lleva el chip, deshacer, rehacer, guardar y el menú. Una
`SingleChoiceSegmentedButtonRow` de cuatro botones **desborda en pantalla estrecha**, y el enunciado
no pedía un cambio de barra. Se mantiene el `FilterChip` y se convierte en el disparador de un
`DropdownMenu` con los modos **disponibles**: el ancho del chip no cambia al cambiar de modo (que es la
razón de que sobre la barra) y el usuario ve las cuatro opciones antes de elegir.

**Alternativa descartada —ciclo de modo con un solo toque.** Descartada: obliga a leer la etiqueta
para saber cuántas pulsaciones faltan, y en un archivo normal (donde solo hay dos) es un clic de más
para lo que hoy es un clic.

### El reparto de las dos mitades

```
Row(Modifier.weight(1f)) {
    if (mode == EDIT || mode == VIEW || mode == SPLIT) {
        CodeEditorView(Modifier.weight(1f), isEditable = mode == EDIT)   // gutter incluido
    }
    if (mode == SPLIT) VerticalDivider(1.dp)
    if (mode == SPLIT || mode == PREVIEW) {
        MarkdownPreview(Modifier.weight(1f))                            // sin gutter
    }
}
```

Cada lado `weight(1f)` es exactamente la mitad (RF-67). El gutter de números de línea va **dentro** del
lado del editor, así que el texto del editor y el HTML comparten la misma altura de línea y no hay
desalineación vertical entre las dos columnas: es el mismo motivo por el que `CodeEditorView` calcula
`lineHeight` como `fontSizeSp * 1.55f` y el CSS debe usar la misma proporción.

`PREVIEW` **no compone** `CodeEditorView` (RF-68): no está en modo «no visible», no está en absoluto.

## 6. Dónde se calcula el HTML

`CodeEditorViewModel` expone `previewHtml: StateFlow<String>` y lo calcula en un `debounce` sobre el
texto, en `viewModelScope` con `Dispatchers.Default` (RF-75). La composición **no** llama al
renderizador: `MarkdownPreview` solo lee el `StateFlow`.

`AGENTS.md` es explícito en esto —«nunca en el hilo principal»— y además el coste lo justifica: sin
debounce, cada tecla de un documento largo dispara un parseo completo en el hilo de la interfaz.

El cálculo vive en el ViewModel y **no** en `MarkdownRenderer`, que se mantiene como función pura
`markdown + tema → String`, con su prueba sin ninguna dependencia de Android.

## 7. Estrategia de tests

Todo JUnit puro, **sin** `RobolectricTestRunner`, porque aquí no hay runtime nativo para
`linux/aarch64` y las pruebas de Robolectric no se ejecutan (RNF-27). La suite pura existente
(`CodeEditorUnitTest`, 14 pruebas) sigue verde y es la línea de base.

| Prueba | Qué fija | Cubre |
|---|---|---|
| `MarkdownTokenizerTest` (~28 casos) | Encabezados ATX y setext, `#` de cierre, cercas con lenguaje y sin cerrar, tabla de 3 columnas con alineación, `---` de la tabla ≠ raya, listas de tarea, anidamiento, citas `>>`, rayas, `**`/`*`/`_`/`~~`, código en línea con tiradas de acentos, enlace en línea / referencia / colapsado, autolink, HTML, front matter, escapes, y **el invariante universal** en todos los fixtures: ordenado, sin solapes, dentro de rango | RF-64, RF-65 |
| `MarkdownRendererTest` (~13 casos) | Tabla, tachado y `<li class="task">` en el HTML; `<script>`/`<iframe>`/`onerror=` **escapados**; `src` remoto → marcador; `src="javascript:"`/`data:` fuera; el CSS lleva el fondo y el color de `PreviewTheme`; documento válido con entrada vacía | RF-70, RF-71, RF-72 |
| `EditorViewModeTest` (~7 casos) | `availableFor`: Markdown 4, resto 2; cambio de archivo restaura el modo; modo no disponible al cambiar de lenguaje → `EDIT`; `SPLIT`/`PREVIEW` implican no editable | RF-66, RF-69 |
| `SyntaxHighlighterTest` (nuevo, en `CodeEditorUnitTest`) | Markdown produce spans, el texto vuelve **idéntico**, y el contenido de una cerca **no** lleva el color de código en línea | RF-65 |
| `CodeEditorUnitTest` (sin tocar) | La regresión: los 14 casos de los otros 23 lenguajes siguen verdes | RNF-27 |

**Comprobación de que los tests prueban** (regla de `MEMORY.md`): mutar el tokenizador para que sí
admita tokens en línea dentro de una cerca y comprobar que `MarkdownTokenizerTest` **se pone rojo**
antes de seguir.

**Lo que no se puede comprobar desde aquí**: que el `WebView` pinte, que el reparto 50/50 sea el
esperado en pantalla y que el chip no desborde con la etiqueta más larga de los 8 locales. Se
declara como riesgo abierto, igual que las specs 005–011.

## 8. Decisiones y alternativas descartadas

1. **Tokenizador con prioridad, no más regex.** El defecto actual es de precedencia, no de cantidad;
   con veinte regex el orden dejaría de ser revisable a ojo. `KotlinSyntaxHighlighter` ya anticipó
   este paso.
2. **commonmark 0.13 ya presente, no subir a 0.22.** Subir forzaría la versión transitiva de
   `compose-markdown` 0.7.2, que se usa en dos pantallas. Las listas de tarea se cubren con seis
   líneas de post-proceso sobre HTML propio.
3. **`PreviewTheme` en hex, no en `Color`.** Es lo que permite que el renderizador sea puro y por tanto
   probado aquí (RNF-26).
4. **Imágenes remotas bloqueadas.** Abrir un `.md` no debe generar una petición saliente: es
   información de la existencia del documento que viaja a un tercero sin pedirlo (principio 5 de la
   constitución). Las imágenes **locales** sí funcionan, porque para eso está `allowFileAccess`.
5. **`WebSettings` a pelo, sin `androidx.webkit`.** Todo lo necesario existe desde API 17.
6. **El chip se despliega en vez de segmentar.** La barra superior ya está llena; cuatro botones
   segmentados desbordan en pantalla estrecha.
7. **`ViewMode` enum, no dos booleanos.** `SPLIT`/`PREVIEW` no son «editar o no»: son dos formas de
   presentar los dos lados, y dos booleanos obligan a mantener un invariante que nadie va a
   mantener.
8. **El modo se recuerda por archivo, no por sesión.** Abrir un `.md` en vista previa y después un
   `.kt` no debería dejar el `.kt` sin poder editar (caso límite 7).
9. **Nada de scroll sincronizado ni divisor arrastrable.** No se pidieron, y cada uno es un
   subproblema de interacción que no se puede comprobar desde aquí.

## 9. Orden de implementación

`MarkdownTokens` → `SyntaxHighlighter` → `MarkdownRenderer` → `ViewMode` → `WebView` → `CodeEditorApp`.
Los dos núcleos puros van primero porque son los únicos que se pueden **probar aquí**; si algo va mal,
se sabe antes de haber tocado un `WebView` que nadie puede ver desde esta plataforma.
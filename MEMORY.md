# MEMORY.md — secure-notes 

Memoria entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte. 

## Estado actual
- **Spec 019 (la vista previa del Markdown del editor de código, con Markwon y sin `WebView`) IMPLEMENTADA**, T1-T11.
  `specs/019-preview-markwon-sin-webview/`, RF-110…RF-116, RNF-57…RNF-62. **Nuevo módulo `:markdown`** con el motor,
  el composable, `ImageSource` (puro) y `LocalFileImageLoader`; lo que es **del chat** (`ChatMarkdownSource`,
  `ChatMarkdownText`) se queda en `:app`. El motor se llama con `imagenesLocalesDe: String?` —la ruta del
  **archivo**—, no con un cargador. **`:app` 668/41** contra base 689/41 con el mismo conjunto de fallos **clase por
  clase** (los 22 de menos se mudaron al módulo con su código); `:editor-de-codigo` 136/2; `:markdown` 49/0.
  **Árbol de `:app` 630 → 630: 0 ganadas, 0 perdidas.** APK **+2.224 bytes**. Verificado **dentro de los 29 `dex`**:
  `MarkdownRenderer` **0**, `WebView` del editor **0**. **Nada de esto se ha visto en un dispositivo.**

- **Spec 018 (Markdown con Markwon en Ajustes) IMPLEMENTADA**, T1-T10 cerradas. `specs/018-markdown-markwon-ajustes/`, RF-104…RF-109, RNF-53…RNF-56. La pantalla legal (que usaba `RichTextParser`), la de actualización y la hoja de novedades (que usaban `MarkdownText` de terceros) **pintan con el motor de la 017**. **`compose-markdown` RETIRADA**: `ChatMarkwon` → `MarkdownEngine`, composable genérico `MarkdownText`, y `ChatMarkdownText` reducido a delegar. **33 pruebas** nuevas (17 invariantes + 16 puras): suite **689/41** contra base **656/41**, **+0 regresiones** comparadas **clase por clase**; `:editor-de-codigo` 145/2. **Árbol de dependencias 650 → 630**: 20 perdidas, **0 ganadas** (`comm` en las dos direcciones). **APK 162.600.361 → 162.117.759 bytes** y sin ninguna clase `jeziellago` en los 29 `dex`. ✅ **VERIFICADO EN DISPOSITIVO por quien Decide: «ya revisé toda la UI y está todo perfecto».**

- **Spec 017 (Markdown en el chat con IA) IMPLEMENTADA**, T1-T7. `specs/017-markdown-chat-ia/`. El cuerpo de los mensajes se pinta con **Markwon directo** (`ui/markdown/`). 45 pruebas, cero regresiones, cero artefactos nuevos. **Sin ver en dispositivo.**

- **Spec 016 (GFM en el markup heredado) IMPLEMENTADA**, T1-T8. `specs/016-gfm-legacy/`. Gramática pura en `util/GfmMarkup.kt` cableada en `RichTextParser`. **Sin ver en dispositivo.**

- **Spec 014 (descomponer el editor) IMPLEMENTADA**, T1-T8. `NoteEditorScreen.kt` de **3504 a 2342 líneas**, partida en 4 ficheros, con diez lógicas puras en `ui/editor/` y 79 pruebas. ✅ **VERIFICADO EN DISPOSITIVO** por quien Decide tras T5.

- **Spec 012** y **013** implementadas; specs 005-011 encadenadas (la 011 **verificada en dispositivo**). Sin verificación visual: 005, 008, 009, 012, 013.

- **Spec 006 (chat con IA) implementada**: tres prototipos y siete lógicas puras en `ui/chat/`, sin Compose ni reloj.

- **Inset del teclado unificado** en editor y chat (decisión 12): una sola fuente, en el `bottomBar`.

- `./gradlew test` completo **no está verde** y no puede estarlo aquí: Robolectric sin runtime nativo para `linux/aarch64`. Los tests **puros sí se ejecutan**. Base: `:app` **656 / 41** y `:editor-de-codigo` **145 / 2**; de los 41, **38** son de plataforma y **3** son ajenos y preexistentes (`HtmlExportMediaTest.legacy item markup migrates to checklist html` y las dos de `OnDeviceModelSourceTest`).

## Decisiones (y por qué)

- **26. Las imágenes de la vista previa las pinta un cargador propio, sin Coil.** Tres razones y la tercera manda:
  **cero dependencias nuevas**; la regla de `AGENTS.md` del `ImageLoader` único **no aplica**, porque aquí no hay
  ningún `ImageLoader` que crear; y **la seguridad es del tipo, no de la configuración** —un cargador de Coil con la
  máscara bien puesta se puede configurar mal, y este **no tiene el método** de pedir nada por red—. La decisión de
  qué imagen se puede leer es una **función pura** (`clasificarImagen`) con cuatro salidas y no un `Boolean`,
  porque «no es una imagen», «es remota» y «está fuera de la carpeta» son tres hechos distintos.

- **27. La decisión va por ruta, no por esquema.** Porque `file:///sdcard/Download/x.png` es un `file://` y **no**
  es del archivo abierto. Y de paso tapa un agujero que el `WebView` tenía abierto: un `![](../fuera.png)` sí podía
  leer ficheros de fuera de la carpeta. Además el orden importa y no es arbitrario: **desenvolver `%20` antes de
  normalizar `..`** es lo que para `%2e%2e`, y **mirar el esquema antes que la extensión** es lo que hace que
  `data:image/png;base64,…` se bloquee en vez de decir «no es una imagen».

- **28. El motor se llama con la ruta del archivo, no con un cargador.** `AsyncDrawableLoader` no cruza el módulo
  (`implementation` no lo filtra) y necesitarlo habría obligado a exponerlo. Pasando la ruta, el panel **no nombra
  ninguna clase de Markwon** y **no puede** dar al motor un cargador de red aunque quisiera: la garantía se vuelve
  estructural en vez de una promesa del KDoc.

- **29. `markwon-image` no hace falta, y casi se declara por error.** `ImagesPlugin` vive en un artefacto aparte que
  **no está en el APK** —la librería que la 018 retiró se traía su propio fork, y de ahí salía—, así que declararlo
  rompía RNF-57. Pero **toda** la maquinaria de imágenes (`AsyncDrawable`, `AsyncDrawableLoader`, `ImageSpanFactory`,
  `MarkwonConfiguration.Builder.asyncDrawableLoader()`) **sí está dentro de `markwon-core`**: el plugin son diez
  líneas escritas aquí.

- **30. La comparación de dependencias tiene que ser la del `:app`.** El árbol de `:markdown` por su cuenta resuelve
  **23 coordenadas por encima** de las de `:app`, y **ninguna** es nueva: son versiones **viejas** de librerías que
  la app ya tiene, porque Gradle resuelve a la más alta al compilar `:app`. «Un módulo nuevo con 23 dependencias
  extra» es exactamente lo que una comparación ingenua daría por malo. `:app` 630 → 630 es la medida.

- **22. Las cuatro superficies de Markdown pintan con un motor y la segunda librería se retira.** Lo que obligaba salió del **bytecode** de `compose-markdown` 0.7.2, no de memoria: registraba **`ImagesPlugin` con su propio `ImageLoader` de Coil** —una `![img](url)` en unas notas de release **salía a la red**— y envolvía su vista en un `key(markdown.hashCode())`, que destruye y recrea la vista en cada cambio de texto. Retirarla **encogió** el APK y **arregló un agujero de licencias**: arrastraba un fork de Markwon (`com.github.jeziellago:Markwon`) que nunca estuvo en el inventario, más `coil-gif`, que se llevaba por delante el Coil 2.x y un `okhttp` 4.10 al árbol de resolución.

- **23. `textStyle` es opcional y `null` significa «no toques la vista».** Si fuera obligatorio, el chat tendría que pasar `bodyMedium` y eso **cambiaría su apariencia** —hoy usa el tamaño por defecto de la plataforma, que además respeta el ajuste de fuente del usuario—. RNF-56 dice que el chat no cambia.

- **24. Solo la burbuja adapta el markup heredado**; el texto legal y las notas de una release se entregan **literales**: en GitHub el HTML crudo es legítimo y adaptarlo **retiraría** etiquetas que ahí sí significan algo. Por eso `ChatMarkdownText` **no** se renombró aunque el motor sí: sigue siendo «lo que adapta una nota para una burbuja».

- **25. Una dirección se abre sin la puntuación de su frase.** El único enlace del texto legal va metido en paréntesis **en los nueve idiomas**, y un `URLSpan` es un texto con posición. **No se puede comprobar aquí** (`Linkify` es framework de Android, sin runtime nativo), así que es una **función pura** con pruebas: `.` `,` `;` `!` `?` van siempre; `)` `]` `}` `>` **solo si quedan sin parear**, porque `https://ejemplo.com/a_(b)` es válida.

- **19. El chat se pinta con Markwon directo.** Su «resaltado de sintaxis» en la librería retirada solo pintaba nodos delimitados por `==` que nada produce. Las seis piezas de Markwon entran como `runtime` por `compose-markdown` —**retirada en la 018**— y hubo que declararlas aparte porque **`runtime` no llega al classpath de compilación**.

- **20. El gesto se queda porque el `TextView` NO lleva método de movimiento.** El menú de pulsación larga y el deslizamiento los sigue atendiendo la burbuja, y el motor **no abre nada por su cuenta**. El hit-test es nuestro: `getOffsetForHorizontal` → `getSpans(offset, offset, URLSpan)` → `isWebLink`, y **se devuelve `true` solo si abre de verdad**, así que un `javascript:` no abre **ni se traga el gesto**. La 018 **reutiliza** esa política tal cual en Ajustes.

- **21. `<item>` se traduce a casilla `[ ]` y no a viñeta `- `.** Con una **sonda** contra `RichTextParser`: hoy `<item>` y `<item checked="false">` pintan **lo mismo**.

- **22-bis. La réplica en curso NO usa el motor (RF-103)**: el texto a medio escribir cambiaría de motor a mitad, y una tabla sin línea separadora se vería como texto y un renglón después como tabla.

- **16. El setext GFM le gana a la raya**, y por eso hubo que tocar también la capa de bloques: solo con `RichTextParser` **no se veía nunca**. Las dos capas deciden con la **misma** regla («¿la línea de arriba tenía texto?»).

- **17. La regla de énfasis del editor de notas es la de `MarkdownInline` (spec 012), no una nueva.** Editor de código y editor de notas son **dos superficies del mismo dialecto** y no pueden decidir distinto.

- **18. Las referencias por referencia necesitan una pre-pasada**: una definición `[r]: url` puede estar **después** del enlace que la usa. Gana la **primera** si se repite.

- **15. El techo de 600 líneas de RNF-43 se aplica a lo que sale del composable, no al `NoteEditorScreen.kt`** (que mide 2342). **No se le baja el presupuesto a `BlockEditor`, `EditableTableBlock` ni `FloatingEditorToolbar` para que pasen**: un invariante que nace rojo entrena a ignorar el rojo.

- **13. `commonmark` ya no se declara en ninguna parte (spec 019).** Antes había **tres** caminos que podían
  mover su versión —la librería de Markdown de terceros (retirada en la 018), el generador de HTML (borrado en la
  019) y `markwon-core`— y por eso hacía falta un pin. Ya solo queda `markwon-core`, así que la versión es suya y
  el pin era decoración. **Una línea de catálogo sin nada detrás no fija nada: solo lo aparenta**, y un invariante
  que la comprobaba daba verde sobre un control que ya no existía.

- **12. El inset del teclado se aplica UNA vez, en el `bottomBar`, con `union`.** La barra invisible con su hueco visible era doble conteo entre `innerPadding` del `Scaffold` y el `.imePadding()` del contenido.

- **9. La cabecera es una fila y una columna, no un dato aparte** (spec 008). Al leer una nota vieja, los títulos antiguos se anteponen **siempre**.

- **7. La clave de las fichas de adjuntos es posicional + nombre**, única por construcción: `FileAttachment` no tiene id estable. **Excepción consciente** a la `key` estable de `AGENTS.md`: al cambiar la clave Compose **destruye y recompone** el nodo, y la ficha no tiene estado interno.

- El host de IA vive en la `Application`, no en el ViewModel. Descarga por **referencias + inactividad**.

## Aprendizajes y errores a evitar 

- **Un invariante que lee comentarios no mide lo que dice medir.** Cuatro invariantes nuevos se pusieron rojos con el
  código **correcto**, los cuatro porque buscaban `WebView` y `ChatMarkdownText` en el texto completo y solo quedaban
  en el KDoc que explica que ya no se usan. Todo recorrido por fuente **quita comentarios antes de buscar**.

- **Escribir las pruebas primero pagaba la cuarta parte de los bugs.** En la 016, uno de los tres fallos de gramática lo pilló una prueba ya escrita. En la 018, **dos** defectos de `direccionParaAbrir` los pillaron sus pruebas puras y **ninguno** se habría visto en una pantalla.

- **Dos guardas encadenadas sobre el mismo conjunto fallan; un `when` por carácter no.** `direccionParaAbrir` tenía `if (!esPuntuacion(c)) break` y `esPuntuacion` **no** incluía los cierres, así que un `)` no se recortaba nunca. La forma que no puede tener ese fallo nombra **qué hace** cada rama y deja `else` para «no se quita».

- **Contar una diferencia no dice cuál sobra.** `estaPareado` acababa en `saldo > 0` y daba por huérfano un paréntesis **bien pareado**, rompiendo `https://ejemplo.com/a_(b)`. Lo correcto es preguntar por el **último** carácter: recorrer **parando antes** de él y sin bajar el saldo de cero.

- **Comparar los fallos por clase, no en cantidad.** «689/41 contra 656/41» suena a cero regresiones y no lo prueba: hay que comprobar que las mismas clases fallan lo mismo.

- **Cuando una prueba falla, leer el nombre además del mensaje.** Una expectativa mía decía «`?q=(a)` quita el `)`» con el nombre «se conserva», y la implementación tenía razón: el `(` sí lo abre.

- **Una expectativa de memoria es una suposición**: `[a](b)` son **dos** tokens, `*b*` no incluye los espacios, `[ref]` **sí** es un enlace colapsado, `a*b*c` no es énfasis.

- **`commonmark` y `markwon` los fija un invariante que lee el fichero**, porque si Graddy resolviera otra versión se romperían el chat, el editor de código y las tres superficies de Ajustes **a la vez** y ninguna prueba funcional se pondría roja.

- **Una dependencia `runtime` no llega al classpath de compilación**; `toPx` es **miembro de `Density`**, no extensión suelta (`with(densidad) { … }`); y Compose **no tiene API pública** para `FontFamily` → `Typeface` de Android (`fontFamilyName` es interno).

- **Un `AndroidView` envuelto en `key(texto.hashCode())` destruye y recrea la vista en cada cambio.** `onRelease` de `AndroidView` se dispara con la vista aún en el árbol: `destroy()` ahí deja la pantalla en negro.

- **El ancla `^` de Kotlin con `Regex.find(text, start)` no se mueve del índice 0**, y `^` no ancla por línea sin `RegexOption.MULTILINE`. Buscar sobre **la línea**, no sobre el documento.

- **Kotlin NO admite referencias adelantadas a funciones locales** (sí capturar una `var` declarada después desde una lambda).

- **Apóstrofos crudos** en `strings.xml` rompen `mergeDebugResources`, **después** de que Kotlin compila limpio. Los saltos son `\n` literal, nunca 0x0A.

- **Un `BUILD SUCCESSFUL` puede no ejecutar nada**: `testDebugUnitTest` sale `UP-TO-DATE`; usar `--rerun`, y `--continue` porque si no para en el primer módulo que falla y el otro nunca se mide.

- **Una tirada sin su cierre tiene que quedar visible ENTERA, no un carácter.** Copiar la tirada entera y saltar por encima.

- **Un `try/catch` que se traga la excepción en un filtro de seguridad no da error, solo no filtra.** Y una tabla no oculta texto: ni `singleLine`, ni `maxLines`.

## Próximos pasos
- **Verificar en dispositivo, spec 019, EN ESTE ORDEN**: (1) **que la vista previa se desplace** —un `TextView` en un
  panel con peso no se desplaza solo, y sin esto es inútil—; (2) que una **imagen local** salga; (3) que una
  remota **no** salga a la red y salga el recuadro; (4) que `<script>` y `<iframe>` se vean **como texto**, que es lo
  único que RF-114 no puede comprobar sin Android; (5) que editor, resaltado, búsqueda y cambio de modo sigan igual.
- **Verificar en dispositivo, spec 018**: que una lista numerada del texto legal se vea como lista y la negrita salga; que la dirección de GitHub **se abra sin el paréntesis**; que un `##` de una release se vea como cabecera y una tabla como tabla; y —**el riesgo de verdad**— que las burbujas del chat se vean **igual** que antes. Lo único que no se puede comprobar aquí es si el detector mete el `)` dentro del `URLSpan`.
- **Revisión pendiente, spec 018**: `ChatMarkdownText` **no** pasa `textStyle`, así que la burbuja conserva el tamaño por defecto de la plataforma. Pasarle `bodyMedium` sería coherente con el resto de la app, pero cambia su apariencia (decisión 23).
- **Spec 015 (siguiente)**: (1) decidir cómo se modelan los `InsertMediaDialog` ×2 (14 parámetros, 8 de ellos eventos) — de ello depende si salen como un diálogo o como dos; (2) agrupar el estado del editor (95 declaraciones) y recordar los ~11 manejadores de las barras — es lo único que puede bajar `NoteEditorScreen.kt` de las 600 líneas; (3) desbloquear partir `handlePickedFile`, `openExternalFile`, `moveBlockToAnotherNote` y `handleSlashSelect`.
- **Verificar en dispositivo, T6-T8 de la 014** (solo superficie movida): que la hoja de voz siga grabando y que cerrar con una grabación en curso la detenga; que la paleta siga aplicando color; y que los cinco diálogos acepten, cancelen y cierren como antes.
- **Verificar en dispositivo, specs 005-013**: **ninguna se ha visto en una pantalla** salvo la 011. Detalle largo en `specs/*/plan.md`.
- **Defectos conocidos, sin spec**: `removeTransformForTag("normal", …)` no tiene brazo `normal`, así que el botón «normal» de la barra no hace nada (~2 líneas); el enfasis dentro de una celda de tabla o del texto de un título no se tokeniza; `BlockEditor.kt:1011` tiene un stub para `BlockType.TABLE` que ignora `meta["table"]` → la tabla sale vacía en TTS y previsualización; `BlockEditor.kt:92` comparte un único `pendingSelection` entre todos los bloques; `NoteContentBlock.TableBlock` sigue guardando los títulos aparte (segundo modelo de tabla).
- Spec 002, T4-T5: generar los dos goldens que faltan y fijar `roborazzi.test.verify`.
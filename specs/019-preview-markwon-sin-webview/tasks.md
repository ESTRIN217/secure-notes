# Tareas — Spec 019: la vista previa del editor de código, con Markwon y sin WebView

> Orden de dependencia. Cada tarea dura entre 20 y 30 minutos. Las pruebas van **primero**: la T2 deja la suite en
> rojo a propósito, y ninguna tarea posterior puede ponerse verde por la razón equivocada.

- [x] **T1. Medir la línea base.** RNF-60, RNF-61
  - Anotar `:app` y `:editor-de-codigo` con `--rerun --continue`, y el recuento **por clase**, no solo el total.
  - Guardar ordenadas las coordenadas del árbol de dependencias de **los dos** módulos: `comm` necesita las dos
    listas, y comparar solo `:app` dejaría pasar un artefacto nuevo que solo entra por la librería.
  - Hecho cuando: hay dos recuento y dos listas, y **no** están de memoria.
  - **Medido:** `:app` **689 / 41** (idéntico al cierre de la 018) y `:editor-de-codigo` **145 / 2**. Los 3 fallos
    ajenos de `:app` siguen siendo los mismos 3, y los 2 de `:editor-de-codigo` son los de plataforma. Coordenadas:
    `:app` **630** y `:editor-de-codigo` **256**. Las tres de `commonmark` que se van están en el árbol de la
    librería, y siguen en el APK por `markwon-core`.
  - ⚠️ T1 se tuvo que repetir: entre medias entró una edición de `MainListScreen.kt` que **no compilaba**
    (`isFabExpanded` se usaba 44 líneas antes de declararse). Con `:app` roto no hay línea base, y comparar solo un
    módulo habría sido justo el error que RNF-60 avisa de no cometer.

- [x] **T2. Las pruebas, en rojo.** RF-110, RF-111, RF-112, RF-113, RNF-57, RNF-58
  - `ImageSourceTest` **nuevo**, en `:markdown`, sin `RobolectricTestRunner`: las ocho pruebas de la sección 8 del
    plan, con la **autocomprobación** de lectura que usan los invariantes de la 017 y la 018.
  - `MarkdownPreviewInvariantsTest` **nuevo**, en `:editor-de-codigo`: sin `WebView`, sin `commonmark`, sin
    `MarkdownRenderer`, `PreviewLayoutTest` intacta, cargador sin Coil ni red, `:markdown` sin mencionar ni IA ni
    editor de código.
  - **Medido:** las 13 del invariante y las 25 de `ImageSourceTest` más las 16 de `DireccionParaAbrirTest` (que
    se mudaron de `:app` al módulo nuevo **con su código**, no se borraron). En rojo de verdad: los invariantes, y
    `ImageSourceTest` no compilaba porque `ImagenPermitida` y `clasificarImagen` no existían.
  - Hecho cuando: compila y **fallan**, cada una por su motivo. Las que ya están verdes se anotan como
    **guardarraíles**, no como pruebas del cambio —una prueba que no se pone roja no comprueba el cambio—.

- [x] **T3. El módulo `:markdown`.** RF-110, RNF-59
  - `settings.gradle.kts` y `markdown/build.gradle.kts`: las seis piezas de Markwon, Compose, **nada más**.
  - Mover `MarkdownEngine.kt` y `MarkdownText.kt` desde `:app` a `com.estrin217.markdown`.
  - `MarkdownEngine` parametrizado: `MarkdownColors` en `Int` y `cargadorDeImagenes` opcional (sección 3 del plan).
    `:app` pasa los colores de Material y `null`; **el chat no cambia de apariencia**.
  - `:app` declara `project(":markdown")` en lugar de las seis piezas sueltas.
  - Actualizar las rutas de los dos ficheros en los invariantes de la 017 y la 018.
  - **Medido:** `:app` compila y su suite queda en **667 / 41** contra **689 / 41**, y el conjunto de fallos se
    comparó **clase por clase**: los mismos 41, los mismos 3 ajenos. La diferencia de 22 pruebas son las que se
    mudaron al módulo nuevo con su código (16 de `direccionParaAbrir` y 6 de `isWebLink`), no pruebas perdidas.
  - ⚠️ **Un hallazgo que cambió la forma de la spec:** `ImagesPlugin` no es de `markwon-core` sino de un
    artefacto aparte, `markwon-image`, que **no está en el APK**. Declararlo rompía RNF-57. Lo que hay es que
    **toda** la machinery de imágenes (`AsyncDrawable`, `AsyncDrawableLoader`, `ImageSpanFactory`, y hasta
    `MarkwonConfiguration.Builder.asyncDrawableLoader()`) **sí está dentro de `markwon-core`**, así que el plugin
    se escribe aquí en diez líneas. Imágenes con **cero** dependencias nuevas, que era lo prometido.
  - ⚠️ **Dos invariantes de la 017 y la 018 quedaron desfasados por esto**, y no por un fallo: decían «el motor
    no menciona ninguna clase de imágenes», que era verdad cuando el motor no sabía de imágenes. Se reescribieron
    a la regla real —«el módulo no tiene nada que sepa salir a la red»— y se añadió el invariante que sí importa:
    **ninguna** superficie de `:app` pasa un cargador.
  - Hecho cuando: `:app:compileDebugKotlin` en verde y la suite de `:app` **exactamente igual** que en T1.

- [x] **T4. `ImageSource`, la lógica pura.** RF-111, RF-112, RF-113, RNF-58
  - `ImagenPermitida` con sus **cuatro** respuestas, y `clasificarImagen(destino, carpetaBase)`: normalizar la ruta,
    resolver contra la carpeta base, y decidir. Sin `android`, sin Compose, sin reloj.
  - Sus ocho pruebas en verde.
  - **Medido:** **25 pruebas en verde a la primera**, incluidas las que importan: una ruta codificada con
    `%20` y `%C3%B1`, y un `%2e%2e` que sale de la carpeta. Las dos mitades del error de orden se provisaron antes
    de implementar: `data:image/png;base64,AAAA` **no** termina en extensión de imagen, así que hay que mirar el
    esquema **antes**, y `//host/x.png` no tiene esquema y hay que reconocerlo aparte.
  - Hecho cuando: `grep -c '^import android'` vale **0** y las ocho pruebas pasan sin `RobolectricTestRunner`.

- [x] **T5. El cargador local.** RF-111, RF-112, RNF-57
  - `LocalFileImageLoader` como `AsyncDrawableLoader`: `BitmapFactory` en `Dispatchers.IO`, `complete()` si sale,
    `fail()` con el marcador que toque si no. **Sin Coil**, sin ninguna clase de red.
  - **Medido:** el cargador **no** menciona Coil ni ninguna clase de red, y `setResult` —público— sustituye al
    planificador de Markwon, que además es lo que permite **cambiar** el marcador por el de «no se pudo leer» al
    fallar la carga. Los dos marcadores son distintos a propósito: uno explica una decisión y el otro un fallo.
  - Hecho cuando: compila y un invariante confirma que el fichero **no** menciona `coil`, `URL`, `Http` ni
    `http`.

- [x] **T6. El panel de la vista previa.** RF-110, RF-115, RNF-62
  - `ui/MarkdownPreviewPane.kt`: `ProvidesMarkdownEngine` con los colores del `SyntaxTheme` y el cargador local,
    envolviendo `MarkdownText` en `verticalScroll` con el estado **en el panel**.
  - `ui/PreviewThemeMapping.kt`: de `PreviewTheme` a `MarkdownColors`. **No** se borra todavía —la 018 decido que se
    borre en T8, y `PreviewTheme` sigue vivo hasta que el `MarkdownRenderer` deje de existir.
  - **Medido:** el desplazamiento vive en el panel, con el estado **recordado fuera** del composable del motor:
    si viviera dentro, cada pulsación devolvería la vista previa al principio. Los colores y el tamaño salen del
    `SyntaxTheme`, con los **mismos números** que pintaba el CSS del `WebView` (`15px`, `line-height: 1.6`), no con
    una escala inventada.
  - Hecho cuando: el panel se compone y el `PreviewLayoutTest` sigue en verde.

- [x] **T7. El ViewModel y la llamada.** RF-115
  - `_previewHtml` → `_previewMarkdown`. El retardo se queda; el **hilo aparte se desaparece**, porque ya no hay
    documento que generar: el motor parsea dentro de `setMarkdown`.
  - `CodeEditorApp` llama al panel nuevo. `PreviewPane` deja de pasar `basePath` al `WebView` y lo pasa a la carpeta
    del archivo, que es de donde sale la ruta base del cargador.
  - **Medido:** `_previewHtml` → `_previewMarkdown`. El hilo aparte **desaparece** y el `withContext` con él,
    porque ya no hay documento que generar. El retardo se queda y por eso sigue en el ViewModel.
  - ⚠️ **Un cambio de API que salió del compilador y mejora el diseño:** `ProvidesMarkdownEngine` tomaba un
    `AsyncDrawableLoader`, y ese tipo **no cruza el módulo** (`implementation` no lo filtra). Se cambió por
    `imagenesLocalesDe: String?` —la ruta del **archivo**— y el módulo construye el cargador. Así el panel de
    Ajustes **no nombra ninguna clase de Markwon** y **no puede** dar al motor un cargador de red aunque quisiera:
    la garantía se vuelve estructural en vez de una promesa del KDoc.
  - Hecho cuando: `:editor-de-codigo:compileDebugKotlin` en verde y escribir rápido en el editor **no** compila
    dos veces el mismo texto.

- [x] **T8. Fuera el pipeline a HTML.** RF-110, RNF-60
  - Borrar `markdown/MarkdownRenderer.kt`, `markdown/MarkdownPreview.kt`, `ui/PreviewThemeMapping.kt`,
    `ui/components/MarkdownPreview.kt` y `MarkdownRendererTest.kt`.
  - Reescribir `MarkdownPreviewContractTest`: el cruce que vigilaba —«se genera una vez y se muestra lo generado»—
    sigue valiendo y ahora tiene otro destinatario.
  - **Medido:** fuera `MarkdownRenderer.kt`, `MarkdownPreview.kt`, `PreviewThemeMapping.kt`,
    `ui/components/MarkdownPreview.kt` y `MarkdownRendererTest.kt`. `MarkdownPreviewContractTest` se **reescribió**
    conservando lo que el contrato tenía de bueno: que lo que llega al pintado es el texto, sin convertirlo antes,
    y que la adaptación del chat no se cuela en el editor.
  - `:editor-de-codigo` baja de **145 a 136** pruebas: son las 23 de `MarkdownRendererTest` menos las 14 de los
    invariantes nuevos. No se pierde cobertura de nada que siga existiendo.
  - Hecho cuando: `:editor-de-codigo` compila y el invariante de RNF-60 pasa.

- [x] **T9. Fuera `commonmark` de la librería.** RNF-60
  - Las tres declaraciones `commonmark.core`, `commonmark.tables` y `commonmark.strikethrough` fuera de
    `:editor-de-codigo/build.gradle.kts`, con el comentario actualizado: sigue en el APK, pero **por
    `markwon-core`**, que lo exige en `compile`.
  - **Medido:** fuera las tres declaraciones de `commonmark`. Y el árbol dice lo que el plan esperaba: **no se
    pierden** los tres artefactos, porque `markwon-core` los exige en `compile`. Lo que se fueron son las
    **declaraciones**, que es lo que pedía RNF-60.
  - **Y del catálogo**, que es donde la regla 1 duele de verdad: las tres entradas `commonmark-*` de
    `libs.versions.toml` **ya no las declara nadie**, así que también se van. Con ellas cae la línea
    `commonmark = "0.13.0"`, y con ella **el invariante que fijaba esa versión**: una línea de catálogo sin nada
    detrás no fija nada, solo lo aparenta. Se invirtió a dos invariantes que sí protegen —«markwon sigue en
    4.6.2» y «commonmark no vuelve a declararse en ningún módulo»—.

  - Hecho cuando: `:editor-de-codigo:assembleDebug` en verde y el invariante lo confirma.

- [x] **T10. Verde y medición final.** RNF-57, RNF-60, RNF-61
  - `./gradlew :app:testDebugUnitTest :editor-de-codigo:testDebugUnitTest --rerun --continue`: **cero fallos
    nuevos** respecto de T1, y los de `:editor-de-codigo` siguen siendo los **dos** de plataforma.
  - `comm` de los dos árboles de dependencias en las dos direcciones: **ningún artefacto nuevo** en ninguno de los
    dos módulos, y el tamaño de `:editor-de-codigo` sin crecer.
  - `:app:assembleDebug`.
  - **Medido, todo con `comm` en las dos direcciones:**
    - `:app`: **630 → 630**, **0 ganadas y 0 perdidas**. Es la medida que importa, porque es el árbol del APK.
    - `:editor-de-codigo`: 256 → 262, y las 6 ganadas son **exactamente** las seis piezas de Markwon, que ya
      estaban en el APK. Los tres artefactos de `commonmark` **no se pierden**, porque `markwon-core` los exige en
      `compile`: lo que se quitaron fueron las declaraciones.
    - `:markdown` por su cuenta resuelve 224 coordenadas, **23 por encima** de las de `:app`, y **ninguna** es
      nueva: son versiones **viejas** de librerías que la app ya tiene (`activity:1.8.2`, `core-ktx:1.9.0`,
      `lifecycle:2.9.4`…). Gradle resuelve a la más alta al compilar `:app`, y por eso su árbol **no se mueve**.
      Merece la pena decirlo porque «un módulo nuevo con 23 dependencias extra» suena a lo contrario, y es
      exactamente lo que la comparación ingenua daría por malo.
    - `:app` **668 / 41** con el conjunto de fallos comparado **clase por clase** contra la base: los mismos 41, los
      mismos 3 ajenos. `:editor-de-codigo` **136 / 2**, solo los de plataforma. `:markdown` **49 / 0**.
  - **El APK se mueve +2.224 bytes** (162.117.759 → 162.119.983), y verificado **por dentro** en los 29 `dex`:
    `MarkdownRenderer` aparece **0** veces, el componente `WebView` del editor **0**, y `LocalFileImageLoader`
    **16**. Un invariante que leyera el texto de los fuentes no habría visto nada de esto.
  - ⚠️ **Cuatro invariantes míosen rojo siendo el código correcto**, los cuatro por lo mismo: buscaban en
    el texto **completo**, y `WebView` y `ChatMarkdownText` solo quedaban en el **KDoc** que explica que ya no se
    usan. Un invariante que lee comentarios está midiendo comentarios. Se añadió `sinComentarios()` al recorrido.
  - Hecho cuando: los tres números están medidos, y la **reducción** de la suite de `:editor-de-codigo` está
    explicada por pruebas borradas de código borrado, con el recuento delante.

- [x] **T11. Cerrar la memoria.** RNF-62
  - `MEMORY.md`: estado de la 019, las cuatro decisiones (cargador propio, módulo nuevo, sin caché, pipeline
    borrado) y la lista de lo que **no** se ha visto en un dispositivo, con el desplazamiento el primero.
  - `AGENTS.md`: su regla de Markdown dice «cuatro superficies» y ahora son **cinco**, y `:markdown` es el módulo
    del motor.
  - **Medido:** `MEMORY.md` sube a 142 líneas, y `AGENTS.md` actualizado: su regla de Markdown decía «cuatro
    superficies» y ahora son **cinco**, y el motor vive en `:markdown`.
  - Se deja escrito que nada de la 019 se ha visto en un dispositivo, y **qué** es lo que no se puede comprobar
    desde aquí: si el desplazamiento funciona y si el HTML incrustado sale literal.
  - Hecho cuando: nada de esto contradice lo ya escrito y la regla de `AGENTS.md` dice la verdad.
# Tasks — Spec 017 · Markdown (GFM) en el chat con IA

Plan: `specs/017-markdown-chat-ia/plan.md`

7 tareas. Las dos primeras son **lógica pura** en un fichero nuevo, y son las únicas que se pueden comprobar
de verdad en esta plataforma: si el texto que llega al pintado está mal, se ve antes de haber tocado nada de
pantalla. Lo que no se puede comprobar desde aquí —que una tabla se vea como tabla, que un enlace se abra y
que el menú de la burbuja siga apareciendo— está marcado en cada tarea como **pase manual**.

---

- [ ] **T1. Declarar el motor de GFM y dejarlo en los avisos legales.** RNF-48
  - `libs.versions.toml`: versión del motor (`4.6.2`) y alias de sus seis piezas: núcleo, tablas, tachado,
    listas de tarea, HTML y autoenlace.
  - `app/build.gradle.kts`: las seis como `implementation`. **Motivo de que hagan falta**: hoy entran como
    `runtime` por la librería de Markdown del changelog, y una dependencia `runtime` no llega al classpath de
    compilación, así que sin declararlas no hay forma de escribir una línea de código contra ellas.
  - **No se quita `compose-markdown`**: el changelog y la pantalla de actualización la siguen usando.
  - `THIRD-PARTY-NOTICES` y `assets/oss-licenses.json`: entrada del motor (Apache-2.0, noties/Markwon).
    Cumplir la constitución 1: cada dependencia declarada tiene que aparecer en los avisos, y hoy Markwon **no**
    aparece, pese a estar ya en el APK.
  - Hecho cuando: `./gradlew :app:assembleDebug` compila, el diff de los avisos tiene la entrada nueva y el
    recuento de artefactos de `:app` **no sube**.
  - **Pase manual**: ninguno, es configuración.
  - **Hecho**: `:app:assembleDebug` → **BUILD SUCCESSFUL** (offline, sin red). Avisos actualizados:
    `THIRD-PARTY-NOTICES` con la fila de Markwon junto a la de `compose-markdown`, y
    `assets/oss-licenses.json` con la entrada nueva **y** con el «uso» de `compose-markdown` corregido a
    «changelog, pantalla de actualización», que hasta aquí decía «Render Markdown» a secas y mentía sobre la
    mitad de los usos. **Cero artefactos nuevos, comprobado y no supuesto**: se volcó el árbol de
    `releaseRuntimeClasspath` de `:app` con y sin los cambios (647 coordenadas en los dos casos) y `comm` sale
    **vacío en las dos direcciones** — ni una entrada añadida ni una quitada. Las seis piezas solo cambiaron de
    `runtime` a `compile`, que es exactamente lo que RNF-48 pedía.

- [x] **T2. `markdownForMessage`: el texto que se pinta.** RF-99, RF-100, RNF-49
  - `ui/markdown/ChatMarkdownSource.kt` **nuevo**: `markdownForMessage(raw: String): String`.
  - Sin `import androidx.compose`, sin `import android.*`, sin `Context`, sin `R`, sin reloj.
  - `ChatMarkdownSourceTest` **en rojo antes**, JUnit puro sin `RobolectricTestRunner`:
    - enlace `<url=https://ejemplo.com>texto</url>` → `[texto](https://ejemplo.com)`;
    - `<url=…>` **sin cerrar** → la etiqueta desaparece **entera**, con la dirección dentro (paridad con lo
      que hace hoy el analizador, comprobado con una sonda: `Ver  la doc`);
    - `<item>`, `<item checked="true">` y `<item checked="false">` → `- [ ]`, `- [x]` y `- [ ]`;
    - el cierre `</item>` **no** se ve al final de la línea (hoy sí se ve: `☐ Comprar pan</item>`);
    - `<b>`, `<i>`, `<u>`, `<s>`, `<sub>`, `<sup>`, `<code>` → **intactas** (las pinta el motor);
    - `<color=#FF0000>`, `<bg=#00FF00>`, `<size=14>`, `<font=mono>`, `<normal>`, `<h1>`, `<h2>`, `<h3>`,
      `<ol>`, `<ul>`, `<li>`, `<img src=…>`, `<video …>`, `<audio …>`, `<hr>`, `<details>` → **fuera**, y su
      texto dentro;
    - `a < b` y `2 < 3` → **intactos**;
    - **paridad**: un texto sin markup alguno sale **idéntico** al de entrada;
    - una cercada con `<color=#FF0000>` dentro → la línea sale **exactamente igual** (la guarda de la cercada);
    - una cercada con tildes `~~~` cuenta igual que una de backticks;
    - una línea con `<img src="https://ejemplo.com/a.png">` → **sin** esa dirección en la salida (RNF-50).
  - Hecho cuando: la suite está en verde y `grep -c '^import androidx.compose\|^import android\.'` en el
    fichero devuelve **0**.
  - **Pase manual**: ninguno.
  - **Hecho**: 36 pruebas en verde. **Tres expectativas estaban mal y las pruebas las pillaron antes de
    que existiera el código**, que es justo para lo que se escriben primero:
    1. **`<item>` a secas es una casilla sin marcar, no una viñeta.** El plan decía `- `. Una sonda temporal
       contra `RichTextParser` enseñó que hoy `<item>` y `<item checked="false">` pintan **lo mismo**
       (`☐ Comprar pan`): traducirlo a viñeta le quitaría una casilla a quien hoy la ve. Corregido en el
       código, en la prueba, en RF-99 y en el plan.
    2. **Un `<url=…>` sin cerrar se retira ENTERO**, con la dirección dentro. El plan y el caso límite 2 de
       la spec decían que la dirección se quedaba como texto; la sonda enseñó que hoy tampoco se ve
       (`Ver  la doc`). Spec corregida.
    3. La sonda enseñó además que hoy el cierre `</item>` **se ve a la vista** (`☐ Comprar pan</item>`): es
       un fallo del analizador actual que la traducción no arrastra. Se añadió una prueba que lo fija.
  - **Dos bugs de implementación que также las pruebas pillaron**: el patrón de etiqueta retirada solo
    aceptaba el valor detrás de un **espacio**, y el markup propio lo pone detrás de un **`=`** sin espacio
    (`<color=#FF0000>`): con el patrón viejo **ninguna** etiqueta de color ni de tamaño se retiraba. Y el
    patrón de la cercada exigía una línea de solo la marca, así que `` ```kotlin `` —el bloque de código más
    común— **no** se reconocía como cercada y su contenido se limpiaba, que es justo lo que la guarda impide.
  - **Y una trampa de la plataforma, la de siempre**: `compileDebugKotlin` se quedó `UP-TO-DATE` con el
    fichero ya editado en disco y llegó a ejecutar pruebas contra el código viejo. `--no-configuration-cache`
    no lo arregló; lo que lo arregla es comprobar que la tarea de compilación **corrió**. Ojo a esto.

- [x] **T3. `isWebLink`: qué direcciones se pueden abrir.** RF-100, RF-51, RNF-49
  - En el mismo fichero puro: `isWebLink(url: String): Boolean`.
  - Pruebas: `http://` y `https://` **sí**; sin esquema con punto (`www.ejemplo.com`, `ejemplo.com/a`) **sí**;
    `javascript:`, `intent:`, `file:`, `content:`, `tel:`, `mailto:` **no**; vacío y basura **no**; el esquema
    en mayúsculas (`HTTPS://`) **sí**.
  - Hecho cuando: la suite está en verde.
  - **Hecho**: dentro de las 36 de `ChatMarkdownSourceTest`. `HTTPS://` en minúsculas y con espacios al
    rededor también.

- [x] **T4. El motor, uno por pantalla.** RF-98, RNF-48, RNF-50
  - `ui/markdown/ChatMarkwon.kt` **nuevo**: `chatMarkwon(): Markwon` (composable, con `remember`) y un
    `CompositionLocal` para repartirlo.
  - `Markwon.builder(context)` —**con** su núcleo, no el constructor sin núcleo— más `TablePlugin`,
    `StrikethroughPlugin`, `TaskListPlugin`, `HtmlPlugin`, `LinkifyPlugin` con máscara de **solo URLs web** y
    `SoftBreakAddsNewLinePlugin`.
  - **No** se registra ningún cargador de imágenes (RNF-50).
  - Tema afinado a MD3: color de enlace, subrayado de enlace, fondo de código en línea y de bloque de código,
    color de cita, color de raya y color de la línea bajo un encabezado. **El color del texto no se toca**:
    el motor no lo pinta, lo pinta cada burbuja.
  - Hecho cuando: compila, y revisar que el módulo **no** menciona ninguna clase de imágenes.
  - **Pase manual**: **sí**. Aquí es donde, si algo, sale raro: el motor resuelve colores y dibujitos de las
    casillas y de las tablas por su cuenta, y la app **no** usa un tema de la plataforma heredada. Si una
    casilla o una tabla sale con el color por defecto o no sale, la primera palanca es declarar
    `androidx.appcompat` y usar su `TextView` en T5.
  - **Hecho**: `:app:compileDebugKotlin` en verde. Ninguna clase de imágenes en el módulo. **Dos firmas no
    eran las que el plan daba por buenas**, y las dos se vieron al compilar contra los `.class` de verdad:
    `TableTheme` **no** tiene `builder()`, tiene `emptyBuilder()` — y se usa el vacío a propósito, porque el
    que trae los valores por defecto usa los colores de Material heredado, que no son los de la app—, y el
    `Context` de `LocalContext` had de leerse **fuera** del `remember`, porque dentro solo se evalúa la
    primera vez y leer un `Local` de composición ahí no es una llamada válida.
  - **La clave del `remember` es el esquema de color, no el texto.** Cambiar de tema claro a oscuro tiene que
    reconstruir el motor —sus colores se copiaron al construirlo— y cambiar el contenido de un mensaje **no**:
    de eso se encarga T5.

- [x] **T5. El pintado de la burbuja.** RF-98, RF-100, RF-101, RF-102, RNF-51
  - `ui/markdown/ChatMarkdownText.kt` **nuevo**: `ChatMarkdownText(markdown, color, modifier, onOpenLink)`.
  - `AndroidView` con un `TextView` plano: **sin** `movementMethod`, **sin** clave sobre el contenido (la
    vista sobrevive a los cambios de texto), **sin** texto seleccionable.
  - En la actualización: `setTextColor(color)` y `setMarkdown`. El texto a pintar se calcula **antes**, con
    `remember`, para que la adaptación no se repita en cada recomposición.
  - El toque: al soltar, línea y desplazamiento del texto bajo el dedo, enlaces en ese punto, y si alguno es
    web, abrirlo. Si no hay ninguno, o ninguno es web, **devolver «lo deja pasar»** para que el menú y el
    deslizamiento sigan siendo de la burbuja. Si no hay aplicación que atienda el destino, `Log.e` y
    «lo deja pasar» (RNF-51).
  - Hecho cuando: compila y `:app:assembleDebug` en verde.
  - **Pase manual**: **sí, y es el importante**. Que el menú de la pulsación larga siga apareciendo, que el
    deslizamiento siga editando y copiando, y que un toque **exactamente sobre un enlace** abra el navegador y
    no el menú.
  - **Hecho**: `:app:compileDebugKotlin` en verde. Dos cosas que el plan dio por buenas y no lo eran:
    (1) El color del texto **lo pasa la burbuja** y no se lee del tema, porque cambia con el rol y uno único
    haría ilegible la burbuja de la persona (`primaryContainer` con `onSurface` encima); (2) `ChatMarkdownText`
    recibe el texto **ya adaptado** y lo adaptationa con la línea como clave en la burbuja, porque aquí es
    donde se puede usar `remember` y dentro de la actualización del `AndroidView` no.

- [x] **T6. La pantalla del chat.** RF-98, RF-103, RNF-52
  - `AiChatScreen.kt`: `MessageBubble` pinta el cuerpo con el motor en lugar del analizador actual. Los dos
    roles, el mismo camino (RF-98). El texto a pintar se adapta con `markdownForMessage`, envuelto en
    `remember` **con la línea como clave**: sin la clave se reharía en cada recomposición, y con una clave que
    no sea el contenido se quedaría congelado al primer texto.
  - `StreamingBubble` **no cambia** (RF-103), con un comentario que diga **por qué**: el texto a medio
    escribir cambiaría de motor a mitad y una tabla a medio escribir parpadearía.
  - El motor se construye una vez en `ChatMessageList` y se reparte con el `CompositionLocal` de T4.
  - Hecho cuando: el diff toca **solo** el pintado del cuerpo del mensaje, y `:app:assembleDebug` en verde.
  - **Pase manual**: **sí**. Que una tabla se vea como tabla, que una casilla salga marcada o sin marcar, y que
    el contenido guardado de una conversación siga siendo **exactamente** el mismo de antes.
  - **Hecho**: `:app:compileDebugKotlin` en verde. El diff de `AiChatScreen.kt` son **cinco huncos**: el
    cuerpo del mensaje, los tres imports, el `ChatMarkwon.ProvidesChatMarkwon` alrededor de la lista y el
    comentario del streaming. **No** se ha tocado el menú, el deslizamiento, las fichas de adjuntos, el pie de
    estado, la reintenta ni el historial.

- [x] **T7. Cierre: invariantes, suite y memoria.** RNF-48, RNF-49, RNF-52
  - `MarkdownRenderInvariantsTest` (puro, leyendo ficheros):
    - `commonmark-java` sigue en `0.13.0` — **es el accidente caro**: si sube, se rompe el chat y el editor de
      código y **ninguna prueba functional se pone roja**;
    - la versión del motor es la misma en `libs.versions.toml` y en las dependencias de `:app`;
    - `ChatMarkdownSource.kt` no importa nada de composición ni de la plataforma;
    - el módulo de render **no** menciona ninguna clase de imágenes.
  - `./gradlew :app:testDebugUnitTest --rerun`: el recuento de fallos no sube frente a la línea base (41, todos
    preexistentes y documentados).
  - `./gradlew :app:assembleDebug` en verde.
  - `MEMORY.md` actualizado con las decisiones y con el hecho de que **nada de esto se ha visto en un
    dispositivo**; `spec.md` a **implementada** cuando T7 esté verde.
  - Hecho cuando: las cuatro comprobaciones están comprobadas, no supuestas.
  - **Hecho, las cuatro comprobadas**:
    1. `./gradlew :app:testDebugUnitTest --offline --rerun-tasks` → **656 pruebas, 41 fallos**. Y la
       **línea base se midió, no se supuso**: moviendo los ficheros fuera y revirtiendo la pantalla y el
       catálogo, la suite da **611 / 41**. Cero regresiones y **45 pruebas** nuevas (36 de la lógica pura + 9
       de invariantes). Los 41 fallos son **los mismos de siempre**: 38 de runtime nativo de Robolectric en
       `linux/aarch64`, 2 de `OnDeviceModelSourceTest` y 1 de `HtmlExportMediaTest`.
    2. `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**.
    3. `MarkdownRenderInvariantsTest`, 9 en verde: `commonmark` sigue en `0.13.0`, el motor en `4.6.2`, las
       seis piezas declaradas, `compose-markdown` sigue en el catálogo, el módulo de render **no menciona
       ninguna clase de imágenes** ni registra un plugin de imágenes, el fichero puro **no importa nada de
       composición ni de la plataforma**, la pantalla del chat **conserva sus tres gestos**, y la réplica en
       curso **sigue con el analizador y no con el motor**.
    4. `MEMORY.md` actualizado y `spec.md` en *implementada*.
  - **Mutaciones, para comprobar que las pruebas pueden ponerse rojas.** Tres, como en la spec 012 y la 014:
    (a) *la guarda de la cercada deja de proteger* (`salida.add(adaptarLinea(linea))` sin mirar el estado) →
    **5 rojas**, las de la cercada y ninguna más. (b) *`isWebLink` acepta cualquier esquema* → **2 rojas**, y
    son las que importan: son las que impedían que una respuesta del modelo lanzara un `javascript:`. (c) *las
    etiquetas de estilo que el motor sí sabe pintar se retiran también* → **1 roja**. Al revertir las tres, todo
    vuelve a verde.
  - **Ninguna mutación dejó verde la prueba que decía cubrir.** La más cercana fue (c): con `<b>` e `<i>` en la
    lista de retiradas, 35 de 36 pruebas siguen verdes y se hunde solo la que las fija. Es la prueba que
    defendía, y hunde a la primera.
  - ⚠️ **Dos veces se perdió trabajo por un reinicio del servidor en mitad de un `git stash`**, y las dos se
    recuperaron de un volcado en `/tmp/opencode`. Si una tarea larga se va a hacer con `stash`, la copia de
    seguridad se hace **antes**, no el comando que la borra.

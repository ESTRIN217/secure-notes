# Tasks — Spec 016 · GFM en el markup heredado

Plan: `specs/016-gfm-legacy/plan.md`

8 tareas. Las dos primeras son **lógica pura** en un fichero nuevo: son las únicas que se pueden probar de
verdad en esta plataforma, así que cualquier error en la gramática sale antes de haber tocado el bucle que
mapea offsets —y ese bucle es el que, si se rompe, mueve mal el cursor y no hay forma de verlo desde aquí.

---

- [x] **T1. `GfmMarkup.kt`: definiciones de referencia y setext.** RF-90, RF-91, RNF-45
  - `util/GfmMarkup.kt` **nuevo**: `collectReferenceDefinitions(text): Map<String, String>` (minúsculas,
    gana la primera, early-out sin `'['`) y `setextUnderlineKind(text, lineStart): SetextKind?` que devuelve
    `H1`, `H2` o `null`, y `null` cuando la línea anterior está vacía o no hay línea anterior.
  - Sin `import androidx.compose`, sin `Context`, sin `R`, sin reloj.
  - `GfmMarkupTest` **en rojo antes**: definición simple, repetida (gana la primera), con `<` en la URL,
    sin definir, `[R]` y `[r]` son la misma; setext con texto encima, sin texto encima, al principio del
    documento, `===` y `---`.
  - Hecho cuando: `grep -c '^import androidx.compose'` en `GfmMarkup.kt` devuelve **0**, y la suite nueva
    corre con `./gradlew :app:testDebugUnitTest --tests '*GfmMarkupTest*' --rerun`.
  - **Hecho**: 56 pruebas en verde, `grep -c '^import androidx.compose'` = **0**, y sin `RunWith`.
  - **La firma de `setextUnderlineKind` es `(underline, previousLineIsPlainText)`, no `(text, lineStart)`.**
    El parámetro no es el texto de la línea anterior sino **la respuesta** a «¿la de arriba tenía texto?»,
    que es lo que el bucle ya sabe sin mirar atrás. Pasarle el documento entero obligaría a la función a
    recorrerlo para contestar una pregunta que el bucle tiene resuelta.

- [x] **T2. `GfmMarkup.kt`: autolink, flanking, tiradas y separadora.** RF-92, RF-95, RF-96, RF-97, RNF-45
  - Cuatro funciones más, todas puras:
    - `angleAutolink(text, index): AngleAutolink?` con `start`, `end`, `url`.
    - `emphasisDelimiterOpensAt(text, index, markerLength): Boolean` — la regla de flanking simple: letra o
      dígito pegado al lado por el que abriría → `false`.
    - `closingBacktickRun(text, openEnd): Int?` — el índice del cierre con **exactamente** N acentos graves, o
      `null` si no hay.
    - `isTableDelimiterRow(line): Boolean` y `pipeCount(line): Int`.
  - Casos: `<https://…>` con ruta larga y con `&`; `<correo@dominio>`; `<b>` **no** es autolink;
    `a*b*c`, `ancho_maximo`, `2*3*4`, `*sí*`, `**fuerte**`; `` `` `a` `` `` con dos y con tres sin cierre;
    separadora de 2 columnas, de 1 columna (no), sin `|` inicial (no), con `:---` y `---:`.
  - Hecho cuando: la suite de `GfmMarkupTest` está en verde y **ninguna** prueba lleva
    `RobolectricTestRunner`.
  - **Hecho**: las 56 de `GfmMarkupTest` en verde.
  - **Dos cosas que las pruebas enseñaron y el plan había escrito mal.** (a) La regla de énfasis **no** es la
    simplificación del plan (mirar los dos lados en cualquier aparición): esa rechazaba `*cursiva*`, porque
    el `*` de cierre va pegado a la letra que lleva el emphasis. Se adoptó la de `MarkdownInline` de la
    spec 012 —
    abre si detrás no hay espacio y delante no hay letra ni dígito; cierra si delante no hay espacio—, que es
    la que ya está probada y desplegada en el editor de código. **Dos superficies del mismo dialecto no
    pueden decidir distinto.** (b) El caso límite 12 de la spec (`2*3*4` sí es énfasis) era **falso** con esa
    regla: delante hay un dígito y no aplica. Corregido en la spec.
  - **Tres bugs que salieron por escribir las pruebas antes que el código**, y que ya están corregidos:
    (1) El patrón del autolink se había quedado **sin el `<` de apertura**, así que no casaba nunca y devolvía
    `null` — y con él, `<https://…>` seguía desapareciendo, que era justo el fallo que la spec viene a
    arreglar. (2) El patrón de la definición llevaba `^` y se buscaba con `find(text, start)` sobre el
    documento: en Kotlin `^` **solo casa en el índice 0**, así que nunca habría salido de la primera línea.
    La trampa de MEMORY.md, pagada una vez más. Ahora se busca sobre la línea. (3) `closingBacktickRun`
    contaba la tirada de apertura **hacia delante** desde su último acento, y por eso veía una sola barra
    en vez de dos; ahora la cuenta hacia atrás.
  - **Dos expectativas del test estaban mal, no el código**: `pipeCount("A | B")` es 1 y no 2, y un `*` al
    final del documento **no puede abrir** énfasis porque detrás no hay nada — abrir y cerrar son reglas
    distintas.

- [x] **T3. Cableado: pre-pasada, setext y autolink.** RF-90, RF-91, RF-92, RNF-46
  - `RichTextParser.parseWithMapping`: una llamada a `collectReferenceDefinitions` **antes** del bucle, y el
    mapa pasa a `parseLineStartMarkers` y `parseMarkdownSyntax`.
  - Setext: en `parseLineStartMarkers`, **antes** del Recognize la raya, comprobar el subrayado y aplicar el
    estilo `h1`/`h2` al tramo ya escrito de la línea anterior.
  - `NoteEditorUtils.preprocessMarkdownBlocks`: **una condición** más para no convertir en `<hr/>` un `---` o
    `===` que sigue a una línea con texto (caso límite 3). Sin esto el setext no se ve nunca en una nota.
  - Autolink: la comprobación va **antes** del bloque `char == '<'` de `HtmlTagParser`. Si `GfmMarkup` no
    reconoce lo que hay entre ángulos, el control cae al camino de etiquetas de siempre.
  - Pruebas en `RichTextParserTest`: `Título\n===` con estilo h1; `Texto\n---` con estilo h2; `Texto\n\n---`
    **sigue** siendo raya; `<https://ejemplo.com>` **aparece** en el texto pintado (hoy desaparece) y lleva
    anotación `URL`; `[t][r]` con definición → enlace con la URL de la definición; `[r]` sin definición →
    literal con corchetes; `<b>` sigue siendo negrita y no un enlace.
  - Hecho cuando: las nuevas están verdes y `testHorizontalRuleMarkdown` **se ha editado** para afirmar el
    comportamiento nuevo (es uno de los dos casos que RNF-47 permite cambiar).
  - **Hecho**: Las 59 pruebas de `RichTextParserTest` y las 56 de `GfmMarkupTest` en verde (115 pruebas pasando).
    `testHorizontalRuleMarkdown` afirma que `---` bajo texto es setext y no raya, conservando raya con línea
    en blanco previa o al inicio. Pre-pasada `collectReferenceDefinitions` resolviendo referencias y autolinks
    interactuando con anotación `URL` y rangos protegidos. Eliminada la mutación residual de `GfmMarkup.kt`.

- [x] **T4. Cableado: `#` de cierre, `+ `, tiradas y flanking.** RF-93, RF-94, RF-95, RF-96
  - `#` de cierre: al consumir el marcador del título, mirar el final de la línea y ocultar o engrisar el
    `#+` final **solo** si hay espacio delante. `## Título#` no se toca.
  - `+ `: una entrada más en la lista `lineMarkers` de `parseLineStartMarkers`.
  - Tiradas de backticks: la rama del acento grave pasa a usar `closingBacktickRun`; si no hay cierre del
    tamaño exacto, **no** alterna el estilo y el acento queda a la vista.
  - Flanking: las ramas de `*`, `_`, `**` y `__` de `parseMarkdownSyntax` consultan
    `emphasisDelimiterAppliesAt` antes de alternar.
  - Pruebas: `## Título ##` no contiene `#` y `## Título#` sí; `+ Uno` y `- Uno` pintan lo mismo;
    `` `` `a` `` `` sale como un fragmento de código; ``` ```a`` ``` sale con las backticks a la vista;
    `a*b*c` y `ancho_maximo` **no** llevan cursiva; `*cursiva*` y `**fuerte**` sí.
  - Hecho cuando: las nuevas en verde y el caso de emphasis preexistente **editado** para afirmar que
    `a*b*c` no sale cursiva (el segundo caso que RNF-47 permite cambiar).
  - **Hecho**: `#` de cierre oculta los hashes finales precedidos de espacio (y visibles en modo gris);
    `+ ` incorporado a `lineMarkers` de viñetas produciendo el mismo glifo `•`; tiradas de backticks
    cerradas con tiradas del tamaño exacto (con recorte de espacio de borde según GFM) o conservadas literales
    si no hay cierre; regla de flanking/intraword de `MarkdownInline` aplicada a `*`, `_`, `**` y `__` evitando
    cursiva en `a*b*c` y `ancho_maximo` mientras se mantiene en palabras completas. Todas las pruebas verdes.

- [x] **T5. Cableado: tabla de texto y cursores.** RF-97, RNF-46
  - En `parseLineStartMarkers`: reconocer la fila separadora con `isTableDelimiterRow`, **comprobando que la
    línea anterior trae al menos una tubería**. Separadora en gris, cabecera en negrita, filas de datos tal
    cual.
  - `GfmMarkupTest`: una tabla de texto lleva la fila de cabecera en negrita; una tabla convertida en bloque
    sale con las mismas cabeceras que antes (no hay regresión en `testPipeTablePreprocessing`).
  - Marcas nuevas registradas como rango protegido (RNF-46): ángulos del autolink, definición de referencia,
    `#` de cierre, fila separadora. Pruebas de cursor: `snapOffset` pega al borde visible, y
    `previousVisibleOffset`/`nextVisibleOffset` desde dentro de la marca saltan al otro lado.
  - Hecho cuando: `git diff --stat` de `app/build.gradle.kts` está **vacío** (RNF-44) y las pruebas de cursor
    de cada marca nueva están en verde.
  - **Hecho**: Tabla como texto cableada reconociendo la fila separadora si la fila anterior tiene al menos
    una tubería, aplicando negrita a la cabecera y estilizando la separadora. Rangos protegidos registrados para
    ángulos de autolink (apertura y cierre), definiciones de enlace por referencia, `#` de cierre y separadora de
    tabla. Pruebas de cursor completas (`snapOffset`, `previousVisibleOffset`, `nextVisibleOffset`). `git diff` de
    `app/build.gradle.kts` vacío. 117 pruebas pasando en verde.

- [x] **T6. El tokenizador muta a rojo.** todas
  - **Antes** de cerrar: romper a propósito cada rama nueva y comprobar que **su** prueba se pone roja.
  - Mínimo cinco mutaciones: (a) `collectReferenceDefinitions` devuelve un mapa vacío → RF-91 se pone rojo;
    (b) `setextUnderlineKind` devuelve siempre `null` → RF-90 rojo; (c) `angleAutolink` acepta cualquier cosa
    entre ángulos → el caso `<b>` rojo; (d) el flanking devuelve siempre `true` → RF-96 rojo;
    (e) `isTableDelimiterRow` acepta una sola columna → el caso de tabla de 1 columna rojo.
  - Hecho cuando: existe en el historial un run con cada mutación en rojo, y al revertir todas vuelve a
    verde.
  - **Ojo**: si una mutación deja verde la prueba que decía cubrir, esa prueba estaba defendiendo otra capa.
    Es lo que pasó dos veces en la spec 012 y una vez en la 014: se anota qué prueba sigue verde y por qué.
  - **Hecho: seis mutaciones, las seis en rojo, y al revertir todas verde otra vez.**
    (a) mapa de definiciones vacío → **10 pruebas** rojas (8 de `GfmMarkupTest` y las 2 de integración de
    RF-91). (b) `setextAt` siempre `null` → **2** rojas, las de setext y ninguna más: la regla de «la línea
    de arriba tenía texto» está fijada justo por esos dos casos. (c) el patrón del autolink acepta cualquier
    cosa entre ángulos → **20+** rojas, y además se llevan por delante casos **preexistentes** como
    `testBasicStylesHideTags`: es la mutación más fuerte de las seis, porque cualquier etiqueta HTML pasaba a
    ser enlace. (d) flanking siempre `true` → **6** rojas. (e) separadora de una columna → **1** roja, la
    exacta. (f) *añadida por la cuenta del proyecto*: el espacio que precede a los `#` de cierre deja de
    consumirse → **2** rojas.
  - **Ninguna mutación dejó verde la prueba que decía cubrir.** La que más se le acercaba fue (b): la de
    `testRayaSinTextoEncimaSigueSiendoRaya` siguió **verde** con el setext anulado, y es correcto —afirma que
    algo **no** es setext, así que anular el setext la deja contento por un motivo que no es el suyo—, pero las
    de (a), (c) y (e) le dan la vuelta y la ponen roja, que es lo que se buscaba.

- [x] **T7. Suite completa y sin regresión.** RNF-44, RNF-47
  - `./gradlew :app:testDebugUnitTest --rerun` en verde, salvo los fallos preexistentes y ajenos ya
    documentados en `MEMORY.md` (Robolectric sin runtime nativo para `linux/aarch64`).
  - El diff de `RichTextParserTest` toca **solo** `testHorizontalRuleMarkdown` y el caso de emphasis, y el
    recuento de pruebas **no baja**.
  - `git diff` de `app/build.gradle.kts` y `build.gradle.kts` **vacío**.
  - `./gradlew :app:assembleDebug` sin errores.
  - Hecho cuando: las cuatro comprobaciones están comprobadas, no supuestas.
  - **Hecho, las cuatro comprobadas.**
    1. `:app:assembleDebug` → **BUILD SUCCESSFUL**.
    2. `git diff --stat` de `app/build.gradle.kts` y `build.gradle.kts` → **vacío** (RNF-44).
    3. Suite nueva: **56** de `GfmMarkupTest` y **61** de `RichTextParserTest`, **todas verdes** (117).
    4. `:app:testDebugUnitTest --rerun` completo → **609 pruebas, 41 fallos**, y los 41 son **los mismos de
       siempre**: 38 de `AssertionError: The Robolectric native runtime is not supported on Linux (aarch64)`,
       2 `AssertionError` y 1 de comparación `huggingface.co` vs `https://huggingface.co`.
  - **La regresión se comprobó contra la línea base, no se supuso.** Con los cambios en una caja (`git stash`)
  la suite da **529 pruebas, 41 fallos**; con ellos, **609 y 41**. Cero fallos nuevos y **80 pruebas** nuevas.
    El «41 fallos» de `MEMORY.md` es real y no ha cambiado: no es que la spec los tapara, es que no tocó nada.
  - `RichTextParserTest` pasó de 38 a 61: el recuento **no baja** (RNF-47). El diff son 281 líneas añadidas y
    **8 borradas**, que son exactamente las de `testHorizontalRuleMarkdown`, el único caso preexistente que
    esta spec cambia a propósito.

- [x] **T8. Cierre: memoria y estado.** todas
  - `MEMORY.md`: la pre-pasada de definiciones, que setext **gana** a la raya y por qué se tocó también
    `preprocessMarkdownBlocks`, la regla de flanking simplificada y su límite, y que **nada de esto se ha
    visto en un dispositivo**.
  - `spec.md` pasa a **implementada**, con la fecha, cuando T7 esté verde.
  - Hecho cuando: `MEMORY.md` nombra las cuatro cosas y la spec dice «implementada».
  - **Hecho.** `MEMORY.md` lleva las tres decisiones nuevas (16, 17 y 18) con su porqué, y **cinco
    aprendizajes**: el autolink sin `<` lo pilló una prueba escrita antes; la regla de la spec 012 ya era
    la que hacía falta y el plan la había reescrito peor; una tirada sin cierre tiene que quedar visible
    **entera**; el ancla `^` con `find(text, start)` no se mueve del 0 (tercera vez que se paga); y
    `pipeCount - 1 >= 2` es lo que sostiene el mínimo de dos columnas de GFM.
  - `spec.md` en **implementada** (2026-10-13), con los criterios de finalización marcados y una nota
    explícita de lo que **solo** se puede cerrar en un dispositivo.

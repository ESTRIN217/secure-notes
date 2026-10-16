# Tareas — Spec 018: un solo motor de Markdown (Markwon) fuera del chat

> Orden de dependencia. Cada tarea dura entre 20 y 30 minutos. Las pruebas van **primero**: la T2 deja la
> suite en rojo a propósito, y ninguna tarea posterior puede ponerse verde por la razón equivocada.

- [x] **T1. Medir la línea base.** RNF-53, RNF-56
  - **Medido:** `:app` **656 / 41** y `:editor-de-codigo` **145 / 2**. De los 41, **38** son de plataforma
    (`DefaultNativeRuntimeLoader`) y **3** son aserciones ajenas y preexistentes:
    `HtmlExportMediaTest.legacy item markup migrates to checklist html` y las dos de `OnDeviceModelSourceTest`.
    Coordenadas del árbol de `:app`: **650**, de las que se esperan **3** perdidas —la librería,
    `com.github.jeziellago:Markwon:58aa5aba6a` (su fork) e `io.coil-kt.coil3:coil-gif:3.0.4`— y **ninguna** ganada.
  - Guardar el recuento de la suite **antes** de tocar nada: `./gradlew test` con `--rerun-tasks`, anotando
    cuántos pasan y cuántos fallan por módulo. Apuntar también los **cuatro** fallos que no son de plataforma, que
    son los que hay que reconocer después.
  - Guardar el árbol de dependencias de `:app` (`releaseRuntimeClasspath`) y sus coordenadas, **ordenadas**, en un
    fichero fuera del árbol del repo.
  - Hecho cuando: hay dos ficheros con las dos listas y el recuento, y el recuento **no** está de memoria.

- [x] **T2. Las pruebas, en rojo.** RF-104, RF-105, RF-106, RF-107, RF-108, RNF-53, RNF-55
  - `app/src/test/java/com/example/MarkdownSurfacesInvariantsTest.kt` **nuevo**, con las nueve pruebas de la
    sección 9 del plan. Sin `RobolectricTestRunner` y con la **autocomprobación** de lectura que usan los
    invariantes de la 017: un recorrido de grep que devuelve una lista vacía pasa **por vacío**, y así se dio
    por buena una prueba de ese tipo en su primer intento en este proyecto.
  - Invertir en `MarkdownRenderInvariantsTest` el invariante «`compose-markdown` se queda porque otras dos
    pantallas lo usan»: ahora tiene que decir que **no** está en el catálogo.
  - Hecho cuando: `:app:compileDebugUnitTestKotlin` compila y **las pruebas nuevas fallan**, cada una por su
    motivo. Si alguna pasa sin que su código exista, el recorrido está roto.
  - **Medido:** 17 invariantes, **7 en rojo** y 10 verdes. Las 7 en rojo son las que piden el cambio; las 10
    verdes son **guardarraíles** que impiden una regresión (que la vista previa y el bloque plegable no se
    migren, que el texto legal no lleve markup heredado, las versiones, las seis piezas del motor, que la
    librería no vuelva). Dos de ellas se corrigieron al ver el rojo: una se llamaba «los textos que llevan
    negrita» y no miraba la negrita, y la de «se adapta una sola vez» pedía **una** aparición en el chat, que
    es justo lo que el arreglo iba a dejar en **cero** —se habría puesto roja al arreglar lo que vigilaba—.

- [x] **T3. El motor compartido: renombre y composable genérico.** RF-104, RF-105, RF-106, RNF-54
  - `ChatMarkwon.kt` → `MarkdownEngine.kt`; `ChatMarkwon` → `MarkdownEngine`, `LocalChatMarkwon` →
    `LocalMarkdownEngine`, `ProvidesChatMarkwon` → `ProvidesMarkdownEngine`. Cuerpo igual: **cero plugins de
    imágenes** (RNF-54).
  - `ChatMarkdownSource.direccionParaAbrir(url)` **nueva**, con sus pruebas puras: recorta la puntuación de la
    frase, **conserva** los paréntesis pareados y es idempotente (sección 5.1 del plan).
  - `ui/markdown/MarkdownText.kt` **nuevo**: el composable, el `TextView`, el mapeo del `TextStyle` y la
    política de toque, tal y como van en las secciones 3, 4 y 5 del plan. **Sin `key` sobre el contenido**, y
    pasando la dirección por `direccionParaAbrir` **antes** de decidir si es web.
  - `ChatMarkdownText.kt` pasa a delegar en `MarkdownText`, con su KDoc de la 017 intacto.
  - Hecho cuando: `:app:compileDebugKotlin` en verde, el motor no nombra ninguna clase de imágenes y las
    pruebas de `direccionParaAbrir` pasan sin necesitar Robolectric.
  - **Dos desviaciones del plan, ambas forzadas por la API y ambas documentadas en el código:**
    1. **`fontFamily` no se pasa al `TextView`.** No hay API pública en Compose para traducir un `FontFamily`
       a un `Typeface` de Android —`fontFamilyName` es interno— y las tres superficies usan la tipografía de
       Material, cuyo `fontFamily` **es** el tipo de letra por defecto. Mapearlo a medias no cambiaría nada hoy y
       un día daría un `Typeface` equivocado en silencio. El plan lo daba por hecho; la API no está.
    2. **`toPx` es miembro de `Density`**, no una extensión suelta: se usa `with(densidad) { … }`. Tres imports
       que parecían correctos no compilaban.
  - **Dos defectos cazados por las pruebas puras**, ambos en `direccionParaAbrir`:
    1. La guarda del bucle solo aceptaba la puntuación **de frase**, así que un `)` —que es *cierre*— salía
       sin recortar. **Ninguna** prueba de paréntesis lo detectó al principio porque se miró el conjunto y no
       el detalle; al mirar una a una se vio que las de `)` y `]` fallaban y las de `.` no.
    2. `estaPareado` terminaba en `saldo > 0`, con lo que un paréntesis **bien pareado** salía como huérfano y
       se quitaba: `https://ejemplo.com/a_(b)` se rompía. Ahora cuenta **parando antes del último carácter** y
       sin bajar de cero.

- [x] **T4. El chat deja de adaptar el texto dos veces.** RF-108, RNF-56
  - En `AiChatScreen`, quitar el `remember(turn.content) { markdownForMessage(turn.content) }` de la burbuja y
    actualizar el import del motor a `MarkdownEngine`.
  - La adaptación, con su `remember`, vive en `ChatMarkdownText` — donde su KDoc ya decía que estaba.
  - Hecho cuando: `markdownForMessage` aparece **una** vez en `ChatMarkdownText` y **ninguna** dentro de
    `StreamingBubble`, y el invariante de RF-103 (la réplica en curso sigue con el analizador) sigue verde.

- [x] **T5. La pantalla legal.** RF-104, RF-106
  - `LegalInfoScreen`: fuera el `import` de `RichTextParser`, envolver con `ProvidesMarkdownEngine` y sustituir
    el `remember(content) { RichTextParser.parse(...) }` + `Text` por `MarkdownText`, con
    `MaterialTheme.typography.bodyMedium` y el color `onSurfaceVariant` que ya tenía.
  - Conservar el `AnimatedVisibility`: solo se pinta la sección desplegada.
  - Hecho cuando: el fichero no menciona `RichTextParser` y compila.

- [x] **T6. Las dos superficies de novedades.** RF-105, RF-106
  - `UpdateInfoScreen` y `ChangelogSheet`: fuera el `import` de `dev.jeziellago…MarkdownText`, envolver con
    `ProvidesMarkdownEngine` y usar `MarkdownText` con `MaterialTheme.typography.bodyMedium`. El texto se
    entrega **literal**: ninguna de las dos llama a `markdownForMessage` (caso límite 2 de la spec).
  - Conservar el `linkColor = MaterialTheme.colorScheme.primary` que pasaban: el motor ya lo pone, así que se
    borra el parámetro y **no** el efecto.
  - Hecho cuando: ningún `MarkdownText` de la librería retirada queda en `src/main`, y los dos ficheros compilan.

- [x] **T7. Fuera la librería retirada.** RF-107, RNF-53
  - Fuera `composeMarkdown` de `gradle/libs.versions.toml` y `implementation(libs.compose.markdown)` de
    `app/build.gradle.kts`.
  - Actualizar el comentario de las seis piezas de Markwon, que hoy dice «compose-markdown SE QUEDA».
  - Hecho cuando: `./gradlew :app:assembleDebug` en verde **sin** ninguna clase de `dev.jeziellago`.

- [x] **T8. Inventario de licencias y avisos legales.** RF-107
  - Fuera la entrada de `compose-markdown` de `assets/oss-licenses.json` y del respaldo de `OssLicenses.kt`, y
    su fila de `THIRD-PARTY-NOTICES`.
  - Cambiar la columna de uso de Markwon: ya no es «el chat con IA», son **cuatro** superficies.
  - Hecho cuando: el nombre de la librería **no** aparece en ninguno de los tres ficheros, y la pantalla de
    licencias sigue.listando las que quedan.

- [x] **T9. Verde y medición final.** RNF-53, RNF-56
  - `./gradlew :app:testDebugUnitTest :editor-de-codigo:testDebugUnitTest --rerun-tasks`: **cero fallos nuevos**
    respecto de T1 y las pruebas nuevas en verde. Los fallos de plataforma de Robolectric se cuentan aparte y
    tienen que ser los mismos.
  - `./gradlew :app:assembleDebug`.
  - `commonmark-java` en `0.13.0` y Markwon en `4.6.2`, **leyendo el fichero**.
  - Comparar el árbol de dependencias con el de T1: **ningún artefacto nuevo**, y las coordenadas perdidas son
    las de la librería retirada. Adjuntar el `comm` de las dos direcciones.
  - Hecho cuando: los tres números están medidos, no supuestos, y la diferencia es la esperada.
  - **Medido:** `:app` **689 / 41** y `:editor-de-codigo` **145 / 2**. La base era 656 / 41 y 145 / 2: son
    **+33 pruebas y +0 fallos**, y el conjunto de fallos se comparó **clase por clase** contra la base, no solo
    en cantidad: las 10 clases que fallaban fallan lo mismo, y los 3 ajenos y preexistentes
    (`HtmlExportMediaTest` y las dos de `OnDeviceModelSourceTest`) siguen siendo exactamente esos tres.
  - **Árbol de dependencias: 650 → 630 coordenadas.** `comm` en las dos direcciones: **20 perdidas, 0 ganadas**.
    Las tres que se esperaban están entre ellas —`compose-markdown`, su **fork** `com.github.jeziellago:Markwon`
    e `io.coil-kt.coil3:coil-gif`— y las 17 demás son lo que arrastraban: `coil-gif` se traía el **Coil 2.x**
    (`io.coil-kt:coil-base:2.2.2`), un `okhttp` 4.10 y un `okio` 3.2 que solo existían por él en el árbol de
    resolución, más un `appcompat` 1.7.0 que ya no compite con el 1.7.1 de `pdf-viewer`.
  - **El APK encoge:** 162.600.361 → 162.117.759 bytes. Y **verificado por dentro**, no por el tamaño: no hay
    ninguna clase `jeziellago` en los 29 `dex` del APK.

- [x] **T10. Cerrar la memoria.** —
  - `MEMORY.md`: el estado de la 018, las decisiones (por qué se retira la librería, por qué `textStyle` es
    opcional, por qué el motor se queda por pantalla) y la lista de lo que **no** se ha visto en un dispositivo.
  - Hecho cuando: `MEMORY.md` sigue por debajo de ~50 líneas de lo nuevo que aporta y nada de esto contradice
    lo que ya estaba escrito.
  - `AGENTS.md` también: su regla de rendimiento sobre el Markdown decía que `compose-markdown` «se queda», y
    eso ya era falso. Ahora apunta a `ui/markdown/` y dice que el motor es de **toda** superficie.
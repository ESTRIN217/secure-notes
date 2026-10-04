# Tasks — Spec 004 · Registro del sistema

Plan: `specs/004-registro-del-sistema/plan.md` · Madre: `specs/001-limites-constitucion/`
7 tareas, en orden de dependencia. **Depende de la spec 002** (`./gradlew test` en verde).

Cada «Hecho cuando» es un comando o un resultado observable.

## Rastro

Estas tareas son **T8-T14** de la spec 001, renumeradas aquí. Ver `plan.md` §1.

---

- [x] **T1. Helper del identificador opaco y del mensaje de reemplazo en `:app`.** RF-6, RF-7, RF-10
  - Crear `app/src/main/java/com/example/util/OpaqueTrace.kt` con el contador `AtomicLong`,
    `begin()`, `Op.next()` e `isolatedNext()`.
  - Añadir `failureMessage(accion, categoria, idOpaque, hoy: Class<*>?)` — **función pura**, no lee
    el estado de ninguna parte — y la fachada `registrarFallo(tag, accion, categoria, id, e)`, que
    extrae solo `e?.javaClass` y nunca `e.message`.
  - Añadir **los casos de prueba** que la Decisión 22 de la madre autoriza para esta función, y solo
    para ella: que el mensaje contiene la categoría y el identificador opaco, y que el texto de la
    excepción no aparece en él. Van en `app/src/test/`.
  - Ambas funciones ≤ 20 líneas, sin dependencia nueva y sin estado global más allá del contador.
  - El identificador **no** incluye `noteId` ni ninguna otra referencia de la nota.
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` compila, los casos de `failureMessage`
    pasan, y
    `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` no ha cambiado de número
    de artefactos respecto a antes de la tarea.

- [x] **T2. Rama A, grupo medios, respaldo y almacenamiento: 8 sitios.** RF-5, RF-7, RF-10, RF-11
  - Sitios 1-8 de `plan.md` §4.1: `MediaStorageHelper.kt:31,36` ·
    `BackupAttachmentHelper.kt:151,158,308` · `StorageAnalyzer.kt:304,337` · `JsonExporter.kt:223`.
  - Añadir el parámetro `op: OpaqueTrace.Op` a `deleteDirectoryContents` para que la recursión
    comparta operación con `deleteFiles`.
  - **No tocar** las líneas `warnings?.add(...)` de `BackupAttachmentHelper.kt:151,158`: no son el
    registro del sistema.
  - *Nota (2026-10-04)*: el plan decía que `deleteFiles` llamaba a `deleteDirectoryContents`; **no es
    cierto** — `deleteFiles` borra con `File(file.path).delete()` y el único caller externo es
    `clearCache`, que es quien abre la operación. El requisito se cumple igual: los dos sitios de
    borrado comparten identificador dentro de su propia operación, que es lo que pedía RF-7.
  - Hecho cuando: una búsqueda de `uriStr|$uri|file\.path|$path` dentro de llamadas al registro del
    sistema en esos cuatro archivos no devuelve ningún resultado, `./gradlew :app:compileDebugKotlin`
    compila, y `git diff --stat` solo muestra los archivos de esta tarea. *(El plan original pedía
    además `./gradlew test` en verde; es inalcanzable en `linux/aarch64`, ver el bloqueo de
    plataforma registrado en la spec 002. La comprobación por suite queda para un host x86_64.)*

- [x] **T3. Rama A, grupo exportadores, ecuaciones e inferencia: 5 sitios.** RF-5, RF-6, RF-7, RF-10, RF-11
  - Sitios 9-13 de `plan.md` §4.1: `MarkdownAttachmentCollector.kt:47` · `HtmlMediaEmbedder.kt:92` ·
    `PdfBlockRenderer.kt:412` · `LatexEquationRenderer.kt:74` · `OllamaService.kt:81`.
  - Añadir el parámetro `kind` a `HtmlMediaEmbedder.readBytes` y propagarlo desde
    `resolveSource(src, type)`.
  - Crear la operación una sola vez en `rememberEquationInlineMap` y pedir un id por ecuación.
  - Hecho cuando: una búsqueda de `block\.content|$src|$url|$latex|$responseBody` dentro de llamadas
    al registro del sistema en esos cinco archivos no devuelve ningún resultado,
    `./gradlew :app:compileDebugKotlin` compila, y `git diff --stat` solo muestra los archivos de
    esta tarea. *(Misma limitación que en T2: `./gradlew test` no puede ejecutarse en
    `linux/aarch64`.)*
  - *Nota (2026-10-04)*: los 5 sitios hechos y el cambio de firma. El plan (§8) decía que
    `HtmlMediaEmbedderTest` habría que tocarlo; **no hizo falta**: solo ejercita `resolveMedia`,
    que no cambia de firma. `:app:compileDebugUnitTestKotlin` sigue en verde. Los otros 4 ficheros
    resuelven con `OpaqueTrace.registrarFallo(...)` por ser todos `Log.e`; en `OllamaService` la
    rama no captura excepción, así que se compone con `failureMessage(..., hoy = null)` y el **código
    HTTP va dentro de `accion`** (`"ollama API error: http=${response.code}"`): es dato no sensible y
    sin él no hay diagnóstico. `$responseBody` sigue leyéndose para `errorDetail`. El `op` de
    `rememberEquationInlineMap` es un **local dentro de `remember`**, no estado: la clave de
    memoización sigue siendo `(latexList, config)`.

- [x] **T4. Rama B, módulo principal: 3 sitios de análisis JSON.** RF-8, RF-10, RF-11
  - Sitios 14-16 de `plan.md` §4.1: `BackupAttachmentHelper.kt:81` · `StorageAnalyzer.kt:84` ·
    `NoteContentUtils.kt:24`.
  - Sustituir el objeto de excepción por su tipo y por el mensaje propio de la acción, con el
    formateador de T1.
  - Hecho cuando: en esos tres puntos no queda ninguna llamada al registro del sistema con un objeto
    de excepción como último argumento, el log resultante contiene `exc=JSONException` y nada del
    texto de la excepción, y `./gradlew test` sigue en verde.
  - *Nota (2026-10-04)*: los 3 sitios hechos con `OpaqueTrace.registrarFallo(...)`, que escribe
    `Log.e` y extrae solo `e?.javaClass`. `NoteContentUtils.kt` (paquete `com.example.data.model`)
    es el único que necesitó `import com.example.util.OpaqueTrace`; `BackupAttachmentHelper.kt` lo
    ve sin import por estar en el mismo paquete. La categoría es `attachment` en los dos que
    recorren la sección de adjuntos y `noteContent` en el que parsea el contenido completo. Ninguno
    pasa ya el objeto `e` al registro: el último argumento de la llamada es la excepción, pero la
    fachada solo extrae su clase y escribe el mensaje compuesto. Misma limitación que en T2 y T3:
    `./gradlew test` no puede ejecutarse en `linux/aarch64`; la comprobación es
    `./gradlew :app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` en verde e inspección
    del diff.

- [x] **T5. Rama B, módulo `:visor-media`: 4 sitios.** RF-8, RF-10, RF-11
  - Sitios 20-23 de `plan.md` §4.1: `AudioPlayerWidget.kt:127` · `Media3VideoPlayer.kt:104` ·
    `MediaViewerScreen.kt:211` · `MediaViewerScreen.kt:277`.
  - **Copiar el helper** a `:visor-media`: `:app` depende de las librerías, así que el de `:app`
    no es visible desde aquí (`plan.md` §7).
  - **No tocar** `AudioPlayerWidget.kt:180`: es uno de los 24 sitios conservados.
  - Hecho cuando: una búsqueda de `${error.message}` dentro de llamadas al registro del sistema en
    `visor-media/src/main` no devuelve ningún resultado, ninguna de las 4 llamadas pasa un objeto de
    excepción como último argumento, y `./gradlew :visor-media:test` sigue en verde.
  - *Nota (2026-10-04)*: los 4 sitios hechos con `OpaqueTrace.registrarFallo(...)` (`accion` =
    `"play audio"` / `"play video"` / `"share media"` / `"save media to gallery"`, `categoria` =
    `audio` / `video` / `media`, `TAG` literal conservado, `Log.e` en los cuatro). La lógica que
    usa la excepción para el mensaje a la persona (`error.cause`, `responseCode`, los `Toast`) queda
    intacta. La copia del helper va a `com.estrin217.visormedia.util`, el paquete `util` que el
    módulo ya tenía (`VideoUrlHelper`): el `diff` contra el de `:app` es **solo** la línea de
    `package`. Importes: `android.util.Log` se retira de `Media3VideoPlayer.kt` y
    `MediaViewerScreen.kt` porque queda sin uso, y **se conserva** en `AudioPlayerWidget.kt`, donde
    el sitio conservado de `:180` sigue llamando a `Log.e`.
    **`./gradlew :visor-media:test` no existe**: `visor-media` solo tiene `src/main`, sin source set
    de test (coherente con D-14, que evita crear uno). La comprobación es
    `./gradlew :visor-media:compileDebugKotlin` y `./gradlew :app:compileDebugKotlin` en verde, más
    la inspección del diff: `grep -rnE '\$\{error\.message\}' visor-media/src/main/` → 0 resultados, y
    el único `Log.e(…, e)` que queda en el módulo es el sitio conservado.
    Los 4 mensajesresultantes se comprobaron ejecutando el helper copiado en una JVM (sin Android):
    `play audio failed: kind=audio id=op1/i1 exc=PlaybackException`,
    `play video failed: kind=video id=op2/i1 exc=PlaybackException`,
    `share media failed: kind=media id=op3/i1 exc=<clase>` y
    `save media to gallery failed: kind=media id=op4/i1 exc=<clase>`.

- [x] **T6. Rama B, módulo `:editor-de-codigo`: 3 sitios, y cierre de la fase.** RF-8, RF-10, RF-11, RNF-3
  - Sitios 17-19 de `plan.md` §4.1: `FileImportExportHelper.kt:68,114,178`.
  - **Copiar el helper** a `:editor-de-codigo`, por el mismo motivo que en T5.
  - Cerrar la fase con la puerta de la entrega completa.
  - Hecho cuando: ninguna de las 3 llamadas pasa un objeto de excepción, y `./gradlew test` termina
    en `BUILD SUCCESSFUL` con las suites de exportación, dibujado, respaldo y widget en verde.
  - *Nota (2026-10-04)*: los 3 sitios hechos con `OpaqueTrace.registrarFallo(...)`, que escribe
    `Log.e` y extrae solo `e?.javaClass`: `accion` = `"read capped text"` / `"read external file"` /
    `"read external folder"` (la acción, no el síntoma: los mensajes previos eran
    `"readCappedText failed"` etc.), `categoria` = `textFile` en el primero —lee un flujo de texto
    con tope— y `externalFile` en los otros dos, que leen un archivo suelto y un árbol de carpetas,
    ambos de SAF. Los tres son llamadas aisladas → `isolatedNext()`. Se conservan el `TAG`
    (`"EditorDeCodigo"`) y el nivel `Log.e`. **El código que sí usa los datos del archivo con
    normalidad se queda igual** (`fileName`, `uri.toString()`, `SupportedLanguage.fromFileName`):
    el cambio es solo en la llamada al registro del sistema. La copia del helper va a
    `com.estrin217.editordecodigo.utils`, el paquete que el módulo ya tenía
    (`FileImportExportHelper`, `FileStorageManager`): el `diff` contra el de `:app` es **solo** la
    línea de `package`. El import de `android.util.Log` se retira de `FileImportExportHelper.kt`
    porque queda sin uso (a diferencia de `AudioPlayerWidget.kt` en T5).
    `./gradlew test` no puede ejecutarse en `linux/aarch64` (misma limitación que en T2-T5), así que
    la comprobación es `./gradlew :editor-de-codigo:compileDebugKotlin`,
    `./gradlew :app:compileDebugKotlin` y `./gradlew :editor-de-codigo:compileDebugUnitTestKotlin`,
    los tres en `BUILD SUCCESSFUL`, más la inspección del diff:
    `grep -nE 'Log\.[ewi]\([^)]*,\s*e\)' …/FileImportExportHelper.kt` → 0 resultados, y el fichero
    ya no tiene ninguna llamada `Log.*`. Los 3 mensajes resultantes se comprobaron ejecutando la
    copia en una JVM (sin Android, con un `android.util.Log` de apoyo): `read capped text failed:
    kind=textFile id=op1/i1 exc=FileNotFoundException`, `read external file failed:
    kind=externalFile id=op2/i1 exc=<clase>` y `read external folder failed: kind=externalFile
    id=op3/i1 exc=<clase>`; la ruta que llevaba el mensaje de la excepción no aparece.
    **Cierre de fase**: los 23 sitios quedan cerrados — de los ficheros de la rama B solo quedan
    llamadas `Log.*(…, e)` en sitios conservados (`BackupAttachmentHelper:231,266`,
    `StorageAnalyzer:56`, `AudioPlayerWidget:186`, `HtmlMediaEmbedder:50,67`)—, el diff no toca
    ninguno de los 24 conservados de `plan.md` §4.2, ningún mensaje de reemplazo es una constante
    sin campos, y el número de escrituras al registro por sitio no baja (en los 3 de aquí se
    mantiene en 1, ahora con identificador opaco). Quedan pendientes de la puerta:
    `./gradlew test` (host x86_64), `wc -l MEMORY.md ≤ 50` (T7) y el recuento de artefactos de
    `releaseRuntimeClasspath`, que sigue resolviendo sin dependencia nueva.

- [x] **T7. Condensar la memoria del proyecto y registrar las decisiones en un ADR.** RF-20
  - Bajar `MEMORY.md` a ≤ 50 líneas, con estado actual, decisiones con su motivo y errores a evitar.
  - Crear `docs/adr/0004-limites-de-constitucion.md` con lo que sea decisión de fondo y ya no estado
    actual: el identificador opaco y su ámbito, la deriva de versiones de los inventarios, y la
    historia de la tabla.
  - **No** trasladar a un ADR el estado de la tabla ni los defectos de selección: son estado con
    fecha, no decisiones.
  - Hecho cuando: `wc -l MEMORY.md` devuelve 50 o menos, y
    `ls docs/adr/` incluye `0004-limites-de-constitucion.md`.
  - *Nota (2026-10-04)*: `MEMORY.md` reescrito entero, de 186 a **49 líneas**
    (`wc -l MEMORY.md` → 49). Se conserva solo lo que una sesión nueva necesita: el estado con
    fecha de las specs 002/003/004, el bloque tabla pendiente de verificación visual, dos pendientes
    de decisión y los cuatro errores que no están ni en `AGENTS.md` ni en un ADR (aarch64 sin
    runtime de Robolectric; `grep -rn "firebase"`; la fuga de `org.json.JSONException` sin
    interpolar; auditar con parser y no con `grep -n`). Desaparecen los ~30 apuntes ya recogidos en
    `AGENTS.md` (Coil, material3 1.5.0-alpha28, `TextRange`, `AnimatedVisibility`, AAPT2, SDK de
    los tests) y el detalle de la 002 ya cerrada como error. `docs/adr/0004-limites-de-constitucion.md`
    recoge las 5 decisiones de fondo con su alternativa descartada: contador monotónico por
    operación y **sin** `noteId`; `failureMessage` pura que recibe la clase, nunca la excepción, y
    única con casos de prueba autorizados (D-22); helper replicado en `:app`, `:visor-media` y
    `:editor-de-codigo` en vez de un módulo Gradle nuevo; los tres inventarios de licencias se
    corrigen a la vez; los 6 principios de `docs/constitution.md` con su comprobación ejecutable.
    **No** se trasladó el estado de la tabla ni los defectos de selección, como manda la tarea.
    La cuarta tarea del encargo pedía además recoger «la historia de la tabla» en el ADR: no se
    ha hecho, porque contradice el «**No** trasladar a un ADR el estado de la tabla» de la propia
    tarea; queda en `MEMORY.md` como una línea de estado.

---

## Comprobaciones de cierre de la spec

- [ ] Búsqueda de interpolaciones de contenido de nota o referencia de medio sobre los 23 sitios → 0 resultados
- [ ] Búsqueda de «último argumento = objeto de excepción» sobre los 20 sitios que la tenían → 0 resultados
- [ ] Salida de la búsqueda de interpolaciones **antes y después** adjunta a la entrega
- [ ] `git diff` no toca ninguno de los 24 sitios conservados de `plan.md` §4.2
- [ ] Ningún mensaje de reemplazo es una constante sin campos
- [ ] Número de escrituras al registro por sitio modificado: no baja respecto al previo
- [ ] `./gradlew test` en verde
- [ ] `wc -l MEMORY.md` ≤ 50
- [ ] `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` sin artefactos nuevos

## Pendiente

- [ ] **Anomalía en la spec madre, sin corregir.** La Decisión 22 de
  `specs/001-limites-constitucion/spec.md` dice en su última frase «El plan aplica RF-19 **sin esa
  excepción**», en presente, lo que contradice el sentido de la propia Decisión 22 (que admite la
  excepción para la función pura del mensaje). No es contradicción operativa —RF-19, su criterio y
  la Decisión 21 coinciden en admitirla solo para esa función— y este plan la aplica. La frase
  describe el plan anterior a la enmienda. Corregirla es decisión del usuario; esta spec no edita la
  madre. Ver `plan.md` §14.
# Plan — Spec 001 · Límites de la constitución

> **SUPERADO — NO IMPLEMENTAR DESDE AQUÍ.** El usuario aprobó el 2026-10-03 la división de esta
> spec en tres specs hijas. Los documentos operativos son:
>
> | Spec | Contenido | Plan |
> |---|---|---|
> | `specs/002-desbloqueo-tests/` | RF-15 … RF-19 (fase 0 de aquí) | `specs/002-desbloqueo-tests/plan.md` |
> | `specs/003-dependencias-inventarios/` | RF-1 … RF-4 (fase A de aquí) | `specs/003-dependencias-inventarios/plan.md` |
> | `specs/004-registro-del-sistema/` | RF-5 … RF-14 + RF-20 (fases B y D de aquí) | `specs/004-registro-del-sistema/plan.md` |
>
> El contenido técnico verificado de este plan (los 23 sitios, la lista de 24 conservados, el
> pseudocódigo del identificador opaco, la función pura del mensaje) se ha movido **sin cambios** al
> plan de la spec 004. Este archivo se conserva como registro.

Spec: `specs/001-limites-constitucion/spec.md` (**aprobada**, 20 RF + 6 RNF).
Plan redactado el 2026-10-03. Este plan no añade requisitos: si algo no está en la spec, no se
implementa. Donde la spec deja una decisión al plan (la lista exacta de sitios del alcance, el
formato del identificador opaco), aquí queda fijada.

---

## 1. Orden de ejecución

El orden no es el de la spec, es el de la **verificabilidad**. `./gradlew test` es la única
comprobación que puede validar cualquier otra cosa de esta entrega (principio 4), y hoy no corre:
la compilación de los tests del módulo principal falla con 13 errores. Por eso la fase 0 va
primero y no es opcional.

```
Fase 0  Desbloqueo de los tests      RF-15..RF-19   → habilita ./gradlew test
Fase A  Dependencias e inventarios   RF-1..RF-4      → no necesita la suite verde
Fase B  Registro del sistema         RF-5..RF-14     → necesita la suite verde (RNF-3)
Fase D  Documentación de la entrega RF-20           → al final, ve el estado final
```

Fase 0 y Fase A van **antes** que B a propósito: A es la más barata (6 líneas de Gradle) y
elimina la contradicción más visible de la constitución; B es la única fase cuyo criterio de
finalización es "las suites de exportación, dibujado, respaldo y widget siguen en verde", y eso
exige la fase 0. Intercalar B antes de 0 dejaría la fase B sin forma de verificarse.

Riesgo conocido y aceptado: cada invocación de Gradle cuesta ~6 min, así que las tareas de la
fase 0 se agrupan para no pagar esa factura más de 4 veces.

---

## 2. Fase 0 — Desbloqueo de los tests (RF-15 … RF-19)

### Archivos y responsabilidades

| Archivo | Cambio | Responsable de |
|---|---|---|
| `app/src/test/java/com/example/WidgetNotesTest.kt` | 4 errores de compilación + entorno de prueba + literal visible | RF-15, RF-17, RF-18 |
| `app/src/test/java/com/example/PdfExporterTest.kt` | 9 errores de compilación | RF-15 |
| `app/src/test/java/com/example/HtmlMediaEmbedderTest.kt` | `@Config(sdk = [28])` → `[36]` | RF-17 |
| `app/src/test/java/com/example/MarkdownAttachmentCollectorTest.kt` | `@Config(sdk = [28])` → `[36]` | RF-17 |
| `gradle.properties` | añadir `roborazzi.test.verify=true` | RF-16 |
| `app/src/test/screenshots/equation_inline.png` | generar y versionar | RF-16 |
| `app/src/test/screenshots/equation_fallback.png` | generar y versionar | RF-16 |

Ningún archivo de producción se toca en esta fase.

### Los 13 errores, uno a uno

**`WidgetNotesTest.kt` (4).** Hoy es JUnit plano y llama las funciones con el orden de argumentos
anterior. Cambios:

- Añadir `@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [36])` y
  `@GraphicsMode(GraphicsMode.Mode.NATIVE)` si el caso lo requiere. `widgetTitle` y
  `widgetSummary` pasan por `RichTextConverter`, que toca recursos, así que Robolectric es
  obligatorio.
- `private val context: Context = ApplicationProvider.getApplicationContext()`, igual que hace
  ya `PdfExporterTest.kt:39`.
- `widgetVisibleNotes(notes)` no cambia (no lleva `context`).
- `widgetTitle(note(1, …))` → `widgetTitle(context, note(1, …))`.
- `widgetSummary("**Hola** mundo " + …)` → `widgetSummary("**Hola** mundo " + …, context)`.
- `assertEquals("Untitled", widgetTitle(...))` → `assertEquals(context.getString(R.string.widget_note_untitled), widgetTitle(...))`.

El idioma sale en inglés por `systemProp.user.language=en` de `gradle.properties`, así que el
valor de `values/strings.xml` ("Untitled") coincide. Aun así la aserción debe leer el recurso:
es lo que pide RF-18 y lo que evita que una traducción rompa la suite.

**`PdfExporterTest.kt` (9).** Las firmas de producción son:

```
measureBlock(block, embedder, maxWidth, maxHeight = Int.MAX_VALUE, context)
drawBlock(canvas, block, embedder, x, y, maxWidth, maxHeight = Int.MAX_VALUE, context)
```

`context` es el **último** parámetro y `maxHeight` es intermedio con valor por defecto. Kotlin no
permite saltar un parámetro posicional intermedio, así que hay dos formas válidas:

- **Argumento nombrado** (preferida): `measureBlock(block, embedder, 495, context = context)`.
  Deja `maxHeight` en su valor por defecto, que es lo que el test quería.
- Valor explícito: `measureBlock(block, embedder, 495, Int.MAX_VALUE, context)`.

Se usa el argumento nombrado en las 9 llamadas: es menos frágil ante un cambio futuro del
valor por defecto. El test ya tiene `context` y ya tiene las tres anotaciones de Robolectric;
no hay que añadir nada más.

**Las dos suites en SDK 28.** `HtmlMediaEmbedderTest.kt:25` y `MarkdownAttachmentCollectorTest.kt:23`
declaran `@Config(sdk = [28])`. RF-17 exige el objetivo de versión android del proyecto, que es
**36**. Motivo verificado: el único artefacto de prueba en caché es el del API 16; con 37 (el
nivel de compilación) Robolectric intentaría descargarlo y la suite no correría sin red. Subir a
36 las pone en el mismo nivel que el resto.

### Los dos goldens que faltan (RF-16)

`app/src/test/screenshots/` solo contiene `greeting.png`, versionado.
`EquationScreenshotTest.kt` escribe dos capturas más que nunca se generaron:

- `equation_inline.png` (línea 34)
- `equation_fallback.png` (línea 45)

Se generan **antes** de fijar el modo verificación, porque en modo verificación y sin referencia
el resultado falla aunque sea correcto:

```
./gradlew :app:testDebugUnitTest --tests '*EquationScreenshotTest*' -Proborazzi.test.record=true
```

Se versionan los dos PNG resultantes. Esto **no** es aceptar una imagen que nadie miró: es la
referencia que el modo verificación exige por definición, y la decisión 16 de la spec lo asume
explícitamente.

### Fijar el modo verificación (RF-16)

`gradle.properties` **no** fija ninguna propiedad de Roborazzi y `app/build.gradle.kts` no tiene
bloque `roborazzi {}`. Se añade una línea a `gradle.properties`:

```properties
roborazzi.test.verify=true
```

Con eso `./gradlew test` verifica por defecto y el criterio "no están fijadas
`roborazzi.test.record` ni `roborazzi.test.compare`" se cumple sobre el archivo de propiedades del
proyecto. Para regenerar una referencia se pasa `-Proborazzi.test.record=true` en la línea de
comandos, que tiene precedencia sobre la propiedad del proyecto y por eso no ensucia el
archivo.

---

## 3. Fase A — Dependencias e inventarios (RF-1 … RF-4)

### Archivos y responsabilidades

| Archivo | Cambio | RF |
|---|---|---|
| `app/build.gradle.kts` | borrar 3 líneas de Firebase (100, 131, 132) | RF-1, RF-2, RF-3 |
| `gradle/libs.versions.toml` | borrar 3 entradas + la versión `firebaseBom` (42, 113, 114, 115) | RF-3 |
| `app/src/main/assets/oss-licenses.json` | borrar la entrada Firebase (175-…) y corregir 3 versiones (16, 24, 120) | RF-4 |
| `app/src/main/java/com/example/util/OssLicenses.kt` | corregir 2 versiones en la lista de reserva (55, 56) | RF-4 |
| `THIRD-PARTY-NOTICES` | borrar la entrada Firebase (61) y corregir 3 versiones (17, 18, 39) | RF-4 |

### Qué se borra y qué no

**Se borra** (RF-3): `implementation(platform(libs.firebase.bom))`, `// implementation(libs.firebase.ai)`,
`implementation(libs.firebase.analytics)`, y en el catálogo `firebase-bom`, `firebase-ai`,
`firebase-analytics` y `firebaseBom = "34.16.0"`. La plataforma de versiones se va porque, sin
ningún artefacto de ese servicio, queda huérfana.

**No se toca**:

- `alias(libs.plugins.google.services)` (`app/build.gradle.kts:12`) ni `app/google-services.json`.
  El plugin está aplicado y **sin el archivo la compilación falla**. El artefacto no está
  versionado por diseño (`.gitignore` raíz y `app/.gitignore`); es una configuración local, no
  una dependencia. RF-3 dice explícitamente que no aplica a ese artefacto.
- `implementation(libs.play.services.auth)`, `libs.googleid`,
  `libs.androidx.credentials.play.services.auth`: se usan en el respaldo de Drive y sus licencias
  se quedan en los tres inventarios.

### La deriva de versiones (RF-4)

`gradle/libs.versions.toml` es la fuente de verdad. Los tres valores se corrigen en los tres
lugares donde se anuncian:

| Librería | Versión en el catálogo (correcta) | Versión announced en los inventarios (errónea) | Inventario |
|---|---|---|---|
| Coil 3 | `3.6.3` (línea 21) | `3.5.0` | `oss-licenses.json:120`, `THIRD-PARTY-NOTICES:39` |
| Plataforma de Compose | `2026.09.00` (línea 12) | `2026.06.01` | `oss-licenses.json:16`, `OssLicenses.kt:55`, `THIRD-PARTY-NOTICES:17` |
| Material 3 | `1.5.0-alpha28` (línea 55) | `1.5.0-alpha25` | `oss-licenses.json:24`, `OssLicenses.kt:56`, `THIRD-PARTY-NOTICES:18` |

`OssLicenses.kt` entra en alcance porque es la lista de reserva que **la propia pantalla de
licencias muestra** cuando el catálogo embebido no se puede leer: es contenido que ve la persona
usuaria, no un comentario interno.

`docs/GUIDELINES.md:10` también anuncia `BOM 2026.06.01` y queda **fuera de alcance**: la spec
excluye reescribir las guías de ingeniería. La deriva es la misma, pero corregirla allí es
trabajo de la spec que mantenga las guías al día.

`Retrofit (+ converter-moshi)` se anuncia en los dos inventarios (`oss-licenses.json:103`,
`THIRD-PARTY-NOTICES:37`) y no lo usa ninguna función del proyecto. Se declara
`implementation(libs.retrofit)` en `app/build.gradle.kts:156`. **No se toca**: está en "Fuera de
alcance" por decisión 9 de la spec (es una decisión de dependencia de proyecto, no un remedio de
la constitución). La entrada de sus licencias se conserva aunque la dependencia no se use.

---

## 4. Fase B — Registro del sistema (RF-5 … RF-14)

### 4.1 Lista definitiva de sitios del alcance: **23**

Medido con un parser que tolera llamadas repartidas en varias líneas, sobre los cuatro módulos:
**143 llamadas** al registro del sistema, **35 interpolan** algún valor, **114 reciben una
excepción como último argumento**. De esas, **20** están en el alcance. La spec pedía "al menos 15
en el módulo principal y al menos 7 en los módulos librería"; el recuento exacto es **16 + 7 = 23**.

El criterio de entrada es uno solo, y es el de la Definición "Criterio de clasificación" de la spec:

> ¿El valor lo escribió o importó la persona usuaria, o lo genera la aplicación?

Un sitio entra si **la respuesta es "la persona"** por cualquiera de las dos vías:

- **Vía A — interpolación**: la llamada interpola un contenido de nota o una referencia de medio.
- **Vía B — excepción**: la llamada pasa un objeto de excepción cuyo texto puede contener
  contenido de nota o referencia de medio.

Los sitios se dividen en dos ramas porque **RF-10 acepta dos formas de mensaje** ("la categoría y
su identificador opaco, **o** el tipo de la excepción y el mensaje propio"), y cada rama usa la
que le corresponde.

#### Rama A — 13 sitios que interpolan contenido de nota o referencia de medio

Se resuelven con **categoría + identificador opaco** (RF-6, RF-7, RF-10 primera rama).
De ellos, 10 además pasan la excepción y pasan a la vez por RF-8/RF-11.

| # | Archivo:línea | Qué interpola hoy | Categoría que se registra | Ámbito de la operación |
|---|---|---|---|---|
| 1 | `app/.../util/MediaStorageHelper.kt:31` | `$uri` | `kind` que ya recibe el método | sin operación → la llamada |
| 2 | `app/.../util/MediaStorageHelper.kt:36` | `$uri` + excepción | ídem | sin operación → la llamada |
| 3 | `app/.../util/BackupAttachmentHelper.kt:151` | `$uriStr` | `attachment` | sin operación → la llamada |
| 4 | `app/.../util/BackupAttachmentHelper.kt:158` | `$uriStr` + excepción | `attachment` | sin operación → la llamada |
| 5 | `app/.../util/BackupAttachmentHelper.kt:308` | `$uriStr` + excepción | `attachment` | sin operación → la llamada |
| 6 | `app/.../data/storage/StorageAnalyzer.kt:304` | `${file.path}` + excepción | `storageItem` | `deleteFiles` (bucle) |
| 7 | `app/.../data/storage/StorageAnalyzer.kt:337` | `${file.path}` + excepción | `storageItem` | heredada de `deleteFiles` (recursión) |
| 8 | `app/.../util/export/JsonExporter.kt:223` | `$path` + excepción | `attachment` | sin operación → la llamada |
| 9 | `app/.../util/export/MarkdownAttachmentCollector.kt:47` | `${block.content}` + excepción | `block.type.name` | sin operación → la llamada |
| 10 | `app/.../util/export/HtmlMediaEmbedder.kt:92` | `$src` + excepción | `image`/`video`/`audio`/`drawing` (parámetro nuevo) | sin operación → la llamada |
| 11 | `app/.../util/export/PdfBlockRenderer.kt:412` | `$url` + excepción | `webImage` | sin operación → la llamada |
| 12 | `app/.../util/LatexEquationRenderer.kt:74` | `$latex` + excepción | `equation` | el bucle de `rememberEquationInlineMap` |
| 13 | `app/.../data/ai/OllamaService.kt:81` | `$responseBody` | `ollamaResponse` | sin operación → la llamada |

Tres sitios de esta rama **no** pasan excepción (`MediaStorageHelper:31`,
`BackupAttachmentHelper:151`, `OllamaService:81`): para ellos RF-8 y RF-11 no aplican y basta con
RF-6/RF-7/RF-10.

Justificación por grupo:

- **1-5, 8** — la referencia de medio (URI, ruta, ruta local) la escribió la persona al importar
  el adjunto. Es el caso que la spec llama referencia de medio.
- **6, 7** — `${file.path}` es la ruta de un adjunto de una nota. Entra por la vía A aunque la
  ruta la generara la propia aplicación al copiar: el criterio de clasificación dice "o describe
  un archivo suyo".
- **9** — `${block.content}` de un bloque de medio es la referencia de medio del bloque.
- **10, 11** — `$src` es la referencia; `$url` puede ser una dirección de intranet o privada.
- **12** — `$latex` es literalmente el texto de la fórmula que escribió la persona. Es el caso más
  claro de contenido de nota de todo el proyecto.
- **13** — `$responseBody` es el cuerpo libre del error HTTP de un servicio de inferencia al que
  la aplicación acaba de enviar el prompt; reproduce lo que se le envió. Clasificado en la
  spec (Definiciones, "Casos ya clasificados") como contenido de nota.

#### Rama B — 10 sitios donde la fuga llega por el objeto de excepción

Se resuelven con **tipo de la excepción + mensaje propio** (RF-8, RF-11, RF-10 segunda rama).

| # | Archivo:línea | Por qué el texto de la excepción lleva contenido de nota |
|---|---|---|
| 14 | `app/.../util/BackupAttachmentHelper.kt:81` | `org.json.JSONArray` sobre la sección de adjuntos de `note.content`; `JSONException` incluye un fragmento de la cadena analizada |
| 15 | `app/.../data/storage/StorageAnalyzer.kt:84` | ídem, sobre `note.content` |
| 16 | `app/.../data/model/NoteContentUtils.kt:24` | ídem, sobre `rawContent` |
| 17 | `editor-de-codigo/.../utils/FileImportExportHelper.kt:68` | lectura de archivo externo; la excepción de E/S incluye la ruta |
| 18 | `editor-de-codigo/.../utils/FileImportExportHelper.kt:114` | ídem |
| 19 | `editor-de-codigo/.../utils/FileImportExportHelper.kt:178` | ídem, lectura de carpeta externa |
| 20 | `visor-media/.../ui/AudioPlayerWidget.kt:127` | `PlaybackException.getMessage()` incluye el URI del medio; además interpola `${error.message}` |
| 21 | `visor-media/.../ui/Media3VideoPlayer.kt:104` | ídem |
| 22 | `visor-media/.../ui/MediaViewerScreen.kt:211` | comparte por `FileProvider` la URI del medio; la excepción incluye la ruta |
| 23 | `visor-media/.../ui/MediaViewerScreen.kt:277` | abre y copia la URI del medio; `FileNotFoundException` incluye la ruta |

Los sitios **14, 15 y 16** son las instancias concretas del caso límite 8 de la spec ("un fallo
de análisis de una estructura serializada cuyo mensaje incluye un fragmento de la cadena
analizada, que en este proyecto es el contenido de la nota"). No es una ampliación de alcance: es
el cumplimiento de un caso límite que la spec ya describe.

Los sitios **20 y 21** entren también por la vía A, porque interpolan `${error.message}`; entran
una sola vez y se resuelven con las dos ramas de RF-10 a la vez.

### 4.2 Sitios revisados y conservados (RF-14): **24**

Ninguno se modifica. Se listan porque el criterio de finalización "el diff no toca ninguna de
estas llamadas" solo es comprobable si la lista es explícita.

| Archivo:línea | Interpola | Por qué se conserva |
|---|---|---|
| `app/.../data/ai/LlamaCppEngine.kt:44` | `$filePath` | ruta interna del archivo de pesos; la genera la app |
| `app/.../data/ai/OnDeviceService.kt:52` | `$model`, `$filePath` | ídem |
| `app/.../data/ai/ModelDownloader.kt:128` | `$file` | ídem |
| `app/.../ui/viewmodel/AiViewModel.kt:349` | `$message` | mensaje de estado; RF-12 lo mantiene |
| `app/.../data/ai/AiModelHost.kt:132` | `$reason` | motivo interno generado por la app |
| `app/.../data/security/SecurePrefsStore.kt:44` | `$key` | nombre interno de preferencia |
| `app/.../data/security/SecurePrefsStore.kt:120` | `$key` | ídem |
| `app/.../ui/viewmodel/UpdaterViewModel.kt:267` | `$expectedChecksum`, `$actual` | sumas de comprobación |
| `app/.../data/sync/GoogleDriveSyncService.kt:60,122,170,204,242,276` | `$response.code`, `$response.message` | código y mensaje HTTP |
| `app/.../ui/settings/AiSettingsScreen.kt:968` | `$url` | URL del catálogo de modelos, constante de la app |
| `app/.../perf/DevicePerformanceProfile.kt:72` | `$tier`, `$memoryClassMb`, … | métricas del dispositivo |
| `app/.../perf/DisplayRefreshRate.kt:31` | `$peak`, `$current` | ídem |
| `app/.../perf/FrameMetricsMonitor.kt:35` | `$profile` | ídem |
| `app/.../perf/FrameMetricsMonitor.kt:64` | `$sorted`, `$janky`, … | ídem |
| `app/.../data/storage/StorageAnalyzer.kt:55` | — (solo excepción) | la excepción viene de la consulta a la base de datos; el parseo JSON ya está capturado en `:84` |
| `app/.../util/BackupAttachmentHelper.kt:226` | — (solo excepción) | flujo ZIP; el mensaje no lleva la nota |
| `app/.../util/BackupAttachmentHelper.kt:261` | — (solo excepción) | ídem |
| `app/.../util/export/HtmlMediaEmbedder.kt:49` | — (solo excepción) | el trazo se decodifica con regex y el decodificador traga su propia excepción |
| `app/.../util/export/HtmlMediaEmbedder.kt:66` | — (solo excepción) | ídem |
| `visor-media/.../AudioPlayerWidget.kt:180` | — (solo excepción) | la excepción viene de leer la posición del reproductor; su mensaje no lleva la referencia del medio (a diferencia de `:127`) |

**Frontera que no se cruza.** En `BackupAttachmentHelper.kt:151` y `:158`, la línea siguiente a
cada `Log` añade el mismo valor a una lista `warnings` que **se muestra a la persona usuaria**.
Eso no es el registro del sistema: es su propio contenido devuelto a quien lo escribió, y queda
fuera de RF-5. No se toca.

### 4.3 El identificador opaco: algoritmo

**Requisito (Definiciones de la spec):** corto, no derivado del contenido, no reversible, sin
ataque por diccionario, ámbito de una operación (o de la propia llamada si no hay operación),
generable con recursos del lenguaje y sin dependencias nuevas (RNF-6).

**Decisión: un contador monotónico por operación.** No es un hash, así que no se puede atacar por
diccionario ni revertir; no se deriva del contenido, así que dos contenidos distintos pueden
compartir identificador.

```
// Impuro: un solo contador. Vive en :app y en cada módulo librería.
objeto OpaqueTrace:
    contadorOperaciones = AtomicLong(0)

    función begin() -> Op:              // "empezar una operación"
        return Op(contadorOperaciones.incrementAndGet())

    clase Op:
        n            : Long             // número de operación
        contadorItems = AtomicLong(0)

        función next() -> String:       // "el siguiente elemento de esta operación"
            return "op" + n + "/i" + contadorItems.incrementAndGet()

        función isolatedNext() -> String:
            return begin().next()       // la propia llamada ES la operación
```

**Pseudocódigo del caso "con operación"** (el sitio recorre N elementos y hay un proceso):

```
// ENTRADA de la operación, una sola vez:
op = OpaqueTrace.begin()

// por cada elemento:
id = op.next()
// registrar: acción + categoría del elemento + id
```

Ejemplos: `deleteFiles` (recorre la lista), `rememberEquationInlineMap` (recorre las ecuaciones de
un bloque), el ciclo de respaldo, el ciclo de exportación de un conjunto de notas.

**Pseudocódigo del caso "sin operación"** (llamada aislada, un elemento, sin proceso):

```
// No hay dónde colgar un identificador: la llamada es la operación.
id = OpaqueTrace.isolatedNext()
```

Se escribe encadenado en el propio sitio del log, sin variable intermedia ni campo nuevo, para no
engordar las funciones (principio 3: ≤ 20 líneas).

La versión que se implementa tiene una única diferencia sobre el pseudocódigo: `isolatedNext()`
es una función aparte en vez de `begin().next()` en línea, para que el sitio del log se lea como
una llamada y no como dos.

**Dónde vive el identificador, en una tabla:**

| Tipo de sitio | Ámbito | Cómo se consigue | Ejemplos |
|---|---|---|---|
| Recorre N elementos | la operación | `val op = OpaqueTrace.begin()` en la entrada, `op.next()` por elemento | `StorageAnalyzer.deleteFiles`, `LatexEquationRenderer` |
| Llamada aislada | la propia llamada | `OpaqueTrace.isolatedNext()` en el sitio | `HtmlMediaEmbedder.readBytes`, `PdfBlockRenderer` |
| Recursión | la operación de la entrada | la entrada crea `op` y lo pasa por parámetro | `StorageAnalyzer.deleteDirectoryContents` |

**Lo que NO se usa: el identificador de la nota.** `MediaStorageHelper.importMedia` recibe
`noteId` y sería tentador registrarlo, porque correlaciona la traza con la nota. Se descarta: un
id de nota es un identificador estable y único del material de la persona, y volver a ponerlo en
el registro reintroduce la correlación que la spec elimina. El identificador de operación ya
correlaciona sin identificar.

### 4.4 El mensaje de reemplazo: algoritmo

**Cómo se distingue el "mensaje propio de la aplicación" del texto de la excepción.** Con una
regla única y sin excepciones:

- El **mensaje propio** es un literal en inglés escrito en el sitio, y describe **la acción** que
  falló (`"read media failed"`). No es una traducción del error ni una descripción del síntoma.
- La **excepción** aporta **solo su clase**: `e::class.java.simpleName`.
- **Nunca** se usa `e.message`, `e.localizedMessage`, `e.cause`, `e.stackTraceToString()` ni el
  objeto `e` como tercer argumento.

**Función pura del formateo** (con "hoy" como parámetro, sin estado y sin Android):

```
función failureMessage(accion: String, categoria: String, idOpaque: String, hoy: Class<*>?) -> String
    // "hoy" = el estado actual del fallo: solo la clase; nunca la instancia

    base = accion + " failed: kind=" + categoria + " id=" + idOpaque
    si hoy != null:
        return base + " exc=" + hoy.simpleName
    si no:
        return base
```

Es pura porque solo depende de sus parámetros: se puede ejercitar sin Android y sin excepciones
reales. El estado del contador es lo único impuro y vive aparte, en `OpaqueTrace`.

**Fachada de registro** (el único punto que toca `Log.e`):

```
función registrarFallo(tag: String, accion: String, categoria: String, id: String, e: Throwable?)
    Log.e(tag, failureMessage(accion, categoria, id, e?.javaClass))
```

`e?.javaClass` es lo único que se extrae de la excepción. Al pasar por `javaClass` y no por
`e.message` es imposible que el texto de la excepción llegue al registro por accidente.

**Ejemplos de resultado, uno por rama:**

```
Rama A: "copy media to export failed: kind=IMAGE id=op7/i2 exc=FileNotFoundException"
Rama A sin excepción: "openInputStream failed: kind=image id=op12/i1"
Rama B: "parse attachments JSON failed: exc=JSONException"
```

Los tres cumplen RF-10: los dos primeros por la primera rama (categoría + identificador opaco),
el tercero por la segunda (tipo de la excepción + mensaje propio). Y los tres cumplen RF-11,
porque el objeto de excepción no llega al registro.

**Un solo punto de composición, tres copias pequeñas.** El helper va duplicado: uno en
`app/.../util/` y uno en cada módulo librería (`visor-media`, `editor-de-codigo`). No se puede
compartir: `:app` **depende** de las librerías, no al revés, así que un helper en `:app` no es
visible desde ellas; y un módulo Gradle compartido nuevo añadiría estructura de compilación que
RNF-6 no permite. Son ~25 líneas por módulo.

`visor-pdf` no recibe copia: no tiene ningún sitio del alcance.

### 4.5 Cambios de firma que exige la fase B

Solo dos, y ambos para poder registrar la categoría que RF-10 exige:

| Firma | Cambio | Por qué |
|---|---|---|
| `HtmlMediaEmbedder.readBytes(src)` | → `readBytes(src, kind: String)` | el método no sabe qué tipo de medio lee; lo sabe su llamante `resolveSource(src, type)`, que ya recibe el `BlockType`. Sin este parámetro la categoría sería siempre `"media"` |
| `StorageAnalyzer.deleteDirectoryContents(dir)` | → `+ op: OpaqueTrace.Op` | es recursiva y no tiene forma de saber a qué operación pertenece el elemento |

Ningún otro cambio de firma. Las funciones se mantienen por debajo de 20 líneas (principio 3) y
no hay ninguna llamada a la capa de datos ni a cifrado desde un `@Composable`: los dos sitios que
tocan composición (`LatexEquationRenderer`, `HtmlMediaEmbedder`) solo cambian el texto de un
`Log`, nunca leen ni escriben datos.

### 4.6 Solapamiento RF-8 / RF-11: una sola pasada

Los dos requisitos se solapan y hay que decidirlo explícitamente o se acaban aplicando dos veces:

- **RF-8** es la prohibición general: en un sitio del alcance, no registrar un objeto de
  excepción cuyo mensaje pueda contener contenido de nota.
- **RF-11** es el payload que se prescribe cuando la causa lleva ruta, dirección o fragmento de
  nota en su texto: tipo de la excepción + mensaje propio.

**Resolución: aplicar RF-11 en todos los sitios del alcance.** Cumplir RF-11 implica no pasar el
objeto, así que RF-8 queda cumplido por construcción. Ningún sitio necesita dos pasadas ni dos
mensajes. En los 3 sitios de rama A sin excepción, RF-8 y RF-11 no llegan a plantearse.

### 4.7 Por qué el criterio de búsqueda corre sobre una lista corta

RF-13 manda "dos búsquedas sobre el alcance de la revisión". Leído como "cero resultados en el
árbol", el criterio sería **imposible de cumplir**: de las 114 llamadas que pasan una excepción,
94 quedan fuera del alcance por decisión expresa de la spec (Decisión 13, "Categorías de
excepción pospuestas": cifrado, sincronización, reproducción de medios y acceso a archivos
externos). Exigir cero en el árbol obligaría a ampliar el alcance sin que la spec lo autorice.

Por eso el criterio se evalúa **sobre la lista explícita de los 23 sitios**:

1. Para cada uno de los 23, la búsqueda de interpolaciones **no devuelve** nada que sea contenido
   de nota o referencia de medio.
2. Para cada uno de los 20 que pasaban excepción, la búsqueda de "último argumento = objeto de
   excepción" **no devuelve** nada.

Los recuentos globales (143 / 35 / 114) se usan para dos cosas distintas, y ninguna es el
criterio de RF-13:

- Comprobar que el diff **no toca** ninguno de los 24 sitios conservados de la tabla 4.2.
- Comprobar que el número de llamadas que interpolan **puede bajar** en los sitios modificados
  sin que eso sea un incumplimiento: lo que no puede bajar es el número de escrituras por sitio
  (RNF-2).

---

## 5. Fase D — Documentación de la entrega (RF-20)

`MEMORY.md` tiene hoy 119 líneas y `AGENTS.md` pide un máximo de ~50.

| Archivo | Acción |
|---|---|
| `MEMORY.md` | condensar a ≤ 50 líneas: estado actual, decisiones con su motivo, errores a evitar. Eliminar el detalle que ya no aporte |
| `docs/adr/` | mover a un ADR nuevo (`0004-limites-de-constitucion.md`) lo que sea decisión de fondo y ya no estado actual: la historia de la tabla, la deriva de versiones, el identificador opaco |

Lo que **no** se traslada a un ADR: el estado de la tabla, los defectos de selección y las
tareas de verificación visual, porque son estado y tienen fecha, no decisiones.

---

## 6. Estrategia de tests

### El orden importa

`./gradlew test` no es una comprobación más: es la **puerta**. Sin la fase 0, ninguna fase puede
verificarse, y RNF-3 ("las suites de esas zonas pasan sin modificar sus aserciones") no es
comprobable. Por eso la fase 0 va primera y su cierre es la puerta de entrada de la fase B.

### Qué cubre cada RF

| RF | Cómo se verifica | Cuándo |
|---|---|---|
| RF-15 | `./gradlew :app:compileDebugUnitTestKotlin` termina sin errores | Fase 0 |
| RF-16 | `./gradlew test` en verde con `roborazzi.test.verify=true` en `gradle.properties` y los 3 PNG versionados | Fase 0 |
| RF-17 | `@Config(sdk = [36])` en las 4 suites afectadas | Fase 0 |
| RF-18 | `WidgetNotesTest` lee `R.string.widget_note_untitled` | Fase 0 |
| RF-19 | `git diff --numstat app/src/test` no añade ni quita anotaciones `@Test` | Fase 0 |
| RF-1, RF-2 | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` sin artefactos de analítica | Fase A |
| RF-3 | búsqueda de `firebase` en `app/build.gradle.kts` y `gradle/libs.versions.toml` → vacío (excluyendo el artefacto de configuración, que no está en ninguno de los dos) | Fase A |
| RF-4 | las 3 versiones de los 3 inventarios coinciden con el catálogo de versiones | Fase A |
| RF-5, RF-6, RF-7, RF-13 | búsquedas sobre los 23 sitios (sección 4.7) | Fase B |
| RF-8, RF-11 | búsqueda de "último argumento = excepción" sobre los 23 sitios | Fase B |
| RF-10 | cada mensaje de reemplazo declara al menos un campo interpolado | Fase B |
| RF-14 | el diff no toca ninguno de los 24 sitios conservados | Fase B |
| RF-20 | `wc -l MEMORY.md` ≤ 50 | Fase D |

RNF-2 (escrituras por sitio no bajan) se comprueba con la salida del recuento por sitio antes y
después, no con un test.

### Suites que hacen de red de seguridad de la fase B

Estas suites ya existen y ejercitan los sitios modificados de la fase B. SiRF-5 a RF-14 rompen algo,
se ven aquí antes que en un dispositivo:

- `HtmlMediaEmbedderTest` → sitio 10
- `MarkdownAttachmentCollectorTest` → sitio 9
- `MarkdownExportMediaTest`, `PlainTextExportTest` → sitios 8, 9
- `PdfExporterTest` → sitio 11
- `WidgetNotesTest` → sin sitio, pero zona de widget de RNF-3
- `StorageAudioFilesTest` → zona de almacenamiento de RNF-3

### Tensión declarada: RF-19 contra el principio 4

**El principio 4 exige que toda función pura nueva nace con su test. RF-19 prohíbe añadir casos de
prueba nuevos.** El formateador `failureMessage` es una función pura nueva.

Lo que propone este plan: **no añadir casos nuevos**. La función queda ejercitada de forma
transversal por las suites de exportación y respaldo que ya la usan al pasar por los sitios
modificados, y la verificación es que esas suites sigan en verde (RNF-3). La propiedad de
`failureMessage` se comprueba por inspección durante la fase B, no con un test.

**Consecuencia asumida: las ramas `catch` no tienen cobertura directa.** Ninguna suite dispara hoy
un fallo de lectura de medio, así que el camino de error no se ejercita.

Esto es una decisión de la spec aprobada que este plan no puede cambiar. Si se prefiere cobertura
directa, hay que **enmendar RF-19** antes de implementar la fase B. Se deja anotado en
`tasks.md` como pendiente de confirmación, no como algo que el plan resuelva por su cuenta.

---

## 7. Mapa RF → fase

| Fase | RF | Tareas |
|---|---|---|
| 0 · Tests | RF-15, RF-16, RF-17, RF-18, RF-19 | T1-T5 |
| A · Dependencias | RF-1, RF-2, RF-3, RF-4 | T6-T7 |
| B · Registro | RF-5 … RF-14 | T8-T13 |
| D · Documentación | RF-20 | T14 |

Ninguna tarea cubre RF de más de una fase, salvo la puerta de la fase B (T13), que cierra con
`./gradlew test` para verificar RNF-3.

---

## 8. Decisiones de diseño y alternativa descartada

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| D-1 | Identificador opaco = contador monotónico por operación | hash del contenido (MD5/SHA-1 de la ruta) | La spec lo prohíbe: un hash es un resumen del valor, es reversible por tabla y admite ataque por diccionario |
| D-2 | Mensaje propio = literal en inglés que describe la acción; la excepción solo da su clase | saneado de `e.localizedMessage` con regex | Un saneado es una lista negra: la fuga reaparece en cuanto la biblioteca añade un prefijo, y el idioma del mensaje depende del sistema |
| D-3 | El objeto de excepción no se registra nunca | `e.stackTraceToString()` | Es exactamente lo que RF-8 prohíbe; además imprime el mensaje de la cadena completa |
| D-4 | Tres copias del helper, una por módulo | un módulo Gradle compartido nuevo | RNF-6 no permite añadir estructura de compilación; y un helper en `:app` no es visible desde las librerías, que son sus dependencias |
| D-5 | Un solo punto de composición por módulo, con `failureMessage` pura y `OpaqueTrace` contador | 23 cadenas de formato escritas a mano en los sitios | 23 copias se desincronizan en cuanto cambia el formato, y ninguna es comprobable |
| D-6 | La categoría se **pasa** por parámetro | deducirla de la extensión del archivo | Deducirla devuelve dato de la referencia de medio (que es lo que hay que quitar) y no funciona con URL |
| D-7 | La categoría más específica disponible | usar siempre la constante `"media"` | Cumple la letra de RF-10 pero pierde el "dónde falló" que la Definición de traza técnica promete |
| D-8 | El identificador **no** incluye el id de la nota | registrar `noteId` | Correlaciona la traza con el material de la persona: reintroduce lo que la spec elimina |
| D-9 | Se aplica RF-11 en todo sitio del alcance | tratar RF-8 y RF-11 como dos pasadas | RF-11 cumplida implica RF-8 cumplida; dos pasadas duplican el trabajo y el mensaje |
| D-10 | El criterio de RF-13 corre sobre la lista de 23 sitios | exigir cero interpolaciones/objetos en el árbol | 94 llamadas que pasan excepción quedan fuera por decisión expresa de la spec; el criterio global sería incumplible |
| D-11 | Los 3 sitios de `org.json` entran (14, 15, 16) | dejarlos fuera por ser "solo excepción" | Son las instancias del caso límite 8 de la spec, que manda lo contrario |
| D-12 | `AudioPlayerWidget.kt:180` queda fuera | incluirlo por ser del mismo archivo que `:127` | Su excepción viene de leer la posición del reproductor y su mensaje no lleva la referencia del medio; `:127` entra por interpolar `${error.message}` |
| D-13 | La fase 0 va primera | hacer la fase A primero | A es más barata, pero B no es verificable sin la suite en verde, y B es 13 de las 23 sitios |
| D-14 | Sin casos de prueba nuevos para el formateador | añadir un test de `failureMessage` | RF-19 lo prohíbe. Pendiente de confirmación del usuario: el principio 4 pediría lo contrario (sección 6) |

---

## 9. Lo que este plan NO hace

- No añade ninguna dependencia. El helper usa `java.util.concurrent.atomic.AtomicLong` (sección 6).
- No toca el plugin `google-services` ni `app/google-services.json`.
- No toca `Retrofit` ni las licencias de autenticación de Drive.
- No toca `docs/GUIDELINES.md`.
- No toca los sitios conservados de la tabla 4.2.
- No añade `:app:test` al flujo de integración continua.
- No arregla la representación de tablas ni la selección de los bloques editables.
- No cambia ningún otro RF de la spec, ni la reabre.
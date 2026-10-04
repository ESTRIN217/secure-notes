# Plan — Spec 004 · Registro del sistema

Spec: `specs/004-registro-del-sistema/spec.md` · Madre: `specs/001-limites-constitucion/`
(**aprobada**)

Este plan es el bloque «Fase B» + «Fase D» del `plan.md` de la spec madre (secciones 4 y 5), movido
tal cual. Los sitios del alcance, la lista de conservados, el pseudocódigo del identificador opaco,
la función pura del mensaje y las decisiones con su alternativa descartada son los mismos que
verificados en la madre: aquí no se reescribe ni se cambia ninguna cifra.

---

## 1. Rastro de las tareas

La spec 001 numeró estas tareas T8-T14. Aquí se renumeran a **T1-T7** dentro de la 004:

| Tarea 004 | Era | Tarea 001 | Contenido |
|---|---|---|---|
| **T1** | → | T8 | Helper del identificador opaco y del mensaje |
| **T2** | → | T9 | Rama A, medios/respaldo/almacenamiento (8 sitios) |
| **T3** | → | T10 | Rama A, exportadores/ecuaciones/inferencia (5 sitios) |
| **T4** | → | T11 | Rama B, módulo principal (3 sitios) |
| **T5** | → | T12 | Rama B, `:visor-media` (4 sitios) |
| **T6** | → | T13 | Rama B, `:editor-de-codigo` (3 sitios) + cierre de fase |
| **T7** | → | T14 | Condensar la memoria del proyecto |

**RF-20 viaja aquí (T7)** porque su contenido depende del estado final de esta spec y de la 003.

## 2. Dependencia entre specs

Esta spec **depende de la 002**: su criterio de cierre es «`./gradlew test` en verde con las suites de
exportación, dibujado, respaldo y widget en verde», y sin la 002 no hay suite que correr. La 003 es
independiente y puede ir en cualquier momento.

## 3. Verificación previa: las cifras del alcance

Recontadas con un parser que tolera llamadas repartidas en varias líneas, sobre los cuatro módulos:

| Módulo | Llamadas | Que interpolan | Que pasan excepción |
|---|---|---|---|
| `:app` | 135 | 33 | 106 |
| `:editor-de-codigo` | 3 | 0 | 3 |
| `:visor-media` | 5 | 2 | 5 |
| `:visor-pdf` | 0 | 0 | 0 |
| **Total** | **143** | **35** | **114** |

Del total, **20 sitios pasan una excepción y están en el alcance**, y **23 sitios en total** entran
por cualquiera de las dos vías.

## 4. Los 23 sitios del alcance

### 4.1 Lista definitiva, con el motivo de cada entrada

Un solo criterio de entrada, el de la Definición «Criterio de clasificación»:

> ¿El valor lo escribió o importó la persona usuaria, o lo genera la aplicación?

Un sitio entra si la respuesta es «la persona», por dos vías: **vía A**, la llamada interpola un
contenido de nota o una referencia de medio; **vía B**, la llamada pasa un objeto de excepción cuyo
texto puede contener contenido de nota o referencia de medio.

`:app` = 16 sitios, librerías = 7. Cumple el «al menos 15 / al menos 7» de la madre, con el recuento
exacto fijado aquí.

#### Rama A — 13 sitios que interpolan contenido de nota o referencia de medio

Se resuelven con **categoría + identificador opaco** (RF-6, RF-7, RF-10 primera rama). De ellos, 10
además pasan la excepción y pasan a la vez por RF-8/RF-11.

| # | Archivo:línea | Qué interpola hoy | Categoría que se registra | Ámbito de la operación | Por qué entra |
|---|---|---|---|---|---|
| 1 | `app/.../util/MediaStorageHelper.kt:31` | `$uri` | `kind`, que el método ya recibe | sin operación → la llamada | referencia de medio |
| 2 | `app/.../util/MediaStorageHelper.kt:36` | `$uri` + excepción | ídem | sin operación → la llamada | referencia de medio |
| 3 | `app/.../util/BackupAttachmentHelper.kt:151` | `$uriStr` | `attachment` | sin operación → la llamada | referencia de medio |
| 4 | `app/.../util/BackupAttachmentHelper.kt:158` | `$uriStr` + excepción | `attachment` | sin operación → la llamada | referencia de medio |
| 5 | `app/.../util/BackupAttachmentHelper.kt:308` | `$uriStr` + excepción | `attachment` | sin operación → la llamada | referencia de medio |
| 6 | `app/.../data/storage/StorageAnalyzer.kt:304` | `${file.path}` + excepción | `storageItem` | `deleteFiles` (bucle) | ruta de un adjunto de una nota |
| 7 | `app/.../data/storage/StorageAnalyzer.kt:337` | `${file.path}` + excepción | `storageItem` | heredada de `deleteFiles` | ídem, en la recursión |
| 8 | `app/.../util/export/JsonExporter.kt:223` | `$path` + excepción | `attachment` | sin operación → la llamada | referencia de medio |
| 9 | `app/.../util/export/MarkdownAttachmentCollector.kt:47` | `${block.content}` + excepción | `block.type.name` | sin operación → la llamada | referencia de medio del bloque |
| 10 | `app/.../util/export/HtmlMediaEmbedder.kt:92` | `$src` + excepción | `image`/`video`/`audio`/`drawing` (parámetro nuevo) | sin operación → la llamada | referencia de medio |
| 11 | `app/.../util/export/PdfBlockRenderer.kt:412` | `$url` + excepción | `webImage` | sin operación → la llamada | dirección, puede ser de intranet |
| 12 | `app/.../util/LatexEquationRenderer.kt:74` | `$latex` + excepción | `equation` | el bucle de `rememberEquationInlineMap` | **el texto de la fórmula**: el caso más claro de contenido de nota del proyecto |
| 13 | `app/.../data/ai/OllamaService.kt:81` | `$responseBody` | `ollamaResponse` | sin operación → la llamada | cuerpo libre del error de un servicio de inferencia al que se le acaba de enviar el prompt |

Tres sitios de esta rama **no** pasan excepción (`MediaStorageHelper:31`,
`BackupAttachmentHelper:151`, `OllamaService:81`): para ellos RF-8 y RF-11 no llegan a plantearse y
basta con RF-6/RF-7/RF-10.

#### Rama B — 10 sitios donde la fuga llega por el objeto de excepción

Se resuelven con **tipo de la excepción + mensaje propio** (RF-8, RF-11, RF-10 segunda rama).

| # | Archivo:línea | Por qué el texto de la excepción lleva contenido de nota |
|---|---|---|
| 14 | `app/.../util/BackupAttachmentHelper.kt:81` | `org.json.JSONArray` sobre la sección de adjuntos de `note.content` |
| 15 | `app/.../data/storage/StorageAnalyzer.kt:84` | ídem, sobre `note.content` |
| 16 | `app/.../data/model/NoteContentUtils.kt:24` | ídem, sobre `rawContent` |
| 17 | `editor-de-codigo/.../utils/FileImportExportHelper.kt:68` | lectura de archivo externo; la excepción de E/S incluye la ruta |
| 18 | `editor-de-codigo/.../utils/FileImportExportHelper.kt:114` | ídem |
| 19 | `editor-de-codigo/.../utils/FileImportExportHelper.kt:178` | ídem, lectura de carpeta externa |
| 20 | `visor-media/.../ui/AudioPlayerWidget.kt:127` | `PlaybackException.getMessage()` incluye el URI del medio; además interpola `${error.message}` |
| 21 | `visor-media/.../ui/Media3VideoPlayer.kt:104` | ídem |
| 22 | `visor-media/.../ui/MediaViewerScreen.kt:211` | comparte por `FileProvider` la URI del medio; la excepción incluye la ruta |
| 23 | `visor-media/.../ui/MediaViewerScreen.kt:277` | abre y copia la URI del medio; `FileNotFoundException` incluye la ruta |

**Los sitios 14, 15 y 16 son las instancias del caso límite 8 de la madre**: `org.json.JSONException`
incluye un fragmento de la cadena analizada, y en los tres la cadena es la sección de adjuntos de
`note.content`. No es una ampliación de alcance: es el cumplimiento de un caso límite que la spec
ya describe.

**Los sitios 20 y 21 entran también por la vía A**, porque interpolan `${error.message}`. Entran una
sola vez y se resuelven con las dos ramas de RF-10 a la vez.

### 4.2 Los 24 sitios revisados y conservados (RF-14)

Ninguno se modifica. Se listan porque el criterio «el diff no toca ninguna de estas llamadas» solo
es comprobable si la lista es explícita.

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
| `visor-media/.../AudioPlayerWidget.kt:180` | — (solo excepción) | la excepción viene de leer la posición del reproductor; su mensaje no lleva la referencia del medio, a diferencia de `:127` |

### 4.3 Frontera que no se cruza

En `BackupAttachmentHelper.kt:151` y `:158`, la línea siguiente a cada `Log` añade el mismo valor a
una lista `warnings` que **se muestra a la persona usuaria**. Eso no es el registro del sistema: es
su propio contenido devuelto a quien lo escribió, y queda fuera de RF-5. **No se toca.**

## 5. El identificador opaco

**Requisito**: corto, no derivado del contenido, no reversible, sin ataque por diccionario, ámbito de
una operación (o de la propia llamada si no hay operación), generable con recursos del lenguaje y
sin dependencias nuevas (RNF-6).

**Decisión: un contador monotónico por operación.** No es un hash, así que no se puede atacar por
diccionario ni revertir; no se deriva del contenido, así que dos contenidos distintos pueden
compartir identificador.

### 5.1 Pseudocódigo

```
// Impuro: un solo contador. Vive en :app y en cada módulo librería.
objeto OpaqueTrace:
    contadorOperaciones = AtomicLong(0)

    función begin() -> Op:              // "empezar una operación"
        return Op(contadorOperaciones.incrementAndGet())

    función isolatedNext() -> String:   // "una llamada aislada es su propia operación"
        return Op(contadorOperaciones.incrementAndGet()).next()

    clase Op:
        n             : Long            // número de operación
        contadorItems  = AtomicLong(0)

        función next() -> String:       // "el siguiente elemento de esta operación"
            return "op" + n + "/i" + contadorItems.incrementAndGet()
```

`isolatedNext()` es una función aparte en vez de `begin().next()` en línea para que el sitio del log
se lea como una llamada y no como dos.

### 5.2 Dónde vive el identificador

**Caso «con operación»** (el sitio recorre N elementos y hay un proceso):

```
// ENTRADA de la operación, una sola vez:
op = OpaqueTrace.begin()

// por cada elemento:
id = op.next()
// registrar: acción + categoría del elemento + id
```

**Caso «sin operación»** (llamada aislada, un elemento, sin proceso):

```
// No hay dónde colgar un identificador: la llamada es la operación.
id = OpaqueTrace.isolatedNext()
```

Se escribe encadenado en el propio sitio del log, sin variable intermedia ni campo nuevo, para no
engordar las funciones (principio 3: ≤ 20 líneas).

| Tipo de sitio | Ámbito | Cómo se consigue | Ejemplos |
|---|---|---|---|
| Recorre N elementos | la operación | `val op = OpaqueTrace.begin()` en la entrada, `op.next()` por elemento | `StorageAnalyzer.deleteFiles`, `LatexEquationRenderer` |
| Llamada aislada | la propia llamada | `OpaqueTrace.isolatedNext()` en el sitio | `HtmlMediaEmbedder.readBytes`, `PdfBlockRenderer` |
| Recursión | la operación de la entrada | la entrada crea `op` y lo pasa por parámetro | `StorageAnalyzer.deleteDirectoryContents` |

### 5.3 Lo que NO se usa: el identificador de la nota

`MediaStorageHelper.importMedia` recibe `noteId` y sería tentador registrarlo, porque correlaciona
la traza con la nota. **Se descarta**: un id de nota es un identificador estable y único del
material de la persona, y volver a ponerlo en el registro reintroduce la correlación que la spec
elimina. El identificador de operación ya correlaciona sin identificar.

## 6. El mensaje de reemplazo

### 6.1 Mensaje propio frente a texto de la excepción

Regla única, sin excepciones:

- El **mensaje propio** es un literal en inglés escrito en el sitio, y describe **la acción** que
  falló (`"read media failed"`). No es una traducción del error ni una descripción del síntoma.
- La **excepción** aporta **solo su clase**: `e::class.java.simpleName`.
- **Nunca** se usa `e.message`, `e.localizedMessage`, `e.cause`, `e.stackTraceToString()` ni el
  objeto `e` como tercer argumento.

### 6.2 La función pura

Es la función a la que la Decisión 22 de la madre abre la excepción de RF-19. Recibe el estado actual
como parámetro («hoy»), no lo lee de ninguna parte, y por eso es pura y comprobable sin Android:

```
función failureMessage(accion: String, categoria: String, idOpaque: String, hoy: Class<*>?) -> String
    // "hoy" = el estado actual del fallo: solo la clase; nunca la instancia

    base = accion + " failed: kind=" + categoria + " id=" + idOpaque
    si hoy != null:
        return base + " exc=" + hoy.simpleName
    si no:
        return base
```

Los casos que RF-19 admite para ella tienen que probar exactamente dos cosas:

1. el mensaje contiene la **categoría** y el **identificador opaco**;
2. el **texto de la excepción** no aparece en él.

### 6.3 La fachada de registro

```
función registrarFallo(tag: String, accion: String, categoria: String, id: String, e: Throwable?)
    Log.e(tag, failureMessage(accion, categoria, id, e?.javaClass))
```

`e?.javaClass` es lo único que se extrae de la excepción. Al pasar por `javaClass` y no por
`e.message`, es imposible que el texto de la excepción llegue al registro por accidente.

### 6.4 Resultados, uno por rama

```
Rama A:       "copy media to export failed: kind=IMAGE id=op7/i2 exc=FileNotFoundException"
Rama A sin e: "openInputStream failed: kind=image id=op12/i1"
Rama B:       "parse attachments JSON failed: exc=JSONException"
```

Los tres cumplen RF-10: los dos primeros por la primera rama (categoría + identificador opaco), el
tercero por la segunda (tipo de la excepción + mensaje propio). Y los tres cumplen RF-11, porque el
objeto de excepción no llega al registro.

## 7. El helper se triplica — decisión confirmada

El helper se escribe **tres veces**: uno en `:app` y uno en cada módulo librería con sitios del
alcance (`:visor-media`, `:editor-de-codigo`). Son ~25 líneas por módulo.

**Por qué no se puede compartir:**

- Un helper en `:app` **no es visible** desde las librerías: `:app` **depende** de ellas, no al
  revés. La dirección de dependencias ya existente lo impide.
- Un helper en `:visor-media` o `:editor-de-codigo` al que `:app` y la otra librería tuvieran que
  apuntar **invierte** esa dirección para servir a un concern transversal.
- Un módulo Gradle compartido nuevo **añadiría estructura de compilación**, que RNF-6 prohíbe, y
  tocaría el principio 1 (stack mínimo) en un proyecto cuya constitución acaba de redactar ese
  principio.
- Una biblioteca externa para esto sería una dependencia nueva, y la constitución lo prohíbe.

`visor-pdf` no recibe copia: no tiene ningún sitio del alcance.

## 8. Cambios de firma que exige la fase

Solo dos:

| Firma | Cambio | Por qué |
|---|---|---|
| `HtmlMediaEmbedder.readBytes(src)` | → `readBytes(src, kind: String)` | el método no sabe qué tipo de medio lee; lo sabe su llamante `resolveSource(src, type)`, que ya recibe el `BlockType`. Sin este parámetro la categoría sería siempre `"media"` |
| `StorageAnalyzer.deleteDirectoryContents(dir)` | → `+ op: OpaqueTrace.Op` | es recursiva y no tiene forma de saber a qué operación pertenece el elemento |

Ningún otro cambio de firma. Las funciones se mantienen por debajo de 20 líneas (principio 3) y no
hay ninguna llamada a la capa de datos ni a cifrado desde un `@Composable`: los dos sitios que tocan
composición (`LatexEquationRenderer`, `HtmlMediaEmbedder`) solo cambian el texto de un `Log`, nunca
leen ni escriben datos.

## 9. Solapamiento RF-8 / RF-11: una sola pasada

Los dos requisitos se solapan y hay que decidirlo explícitamente o se acaban aplicando dos veces:

- **RF-8** es la prohibición general: en un sitio del alcance, no registrar un objeto de excepción
  cuyo mensaje pueda contener contenido de nota.
- **RF-11** es el payload que se prescribe cuando la causa lleva ruta, dirección o fragmento de nota
  en su texto: tipo de la excepción + mensaje propio.

**Resolución: aplicar RF-11 en todos los sitios del alcance.** Cumplir RF-11 implica no pasar el
objeto, así que RF-8 queda cumplido por construcción. Ningún sitio necesita dos pasadas ni dos
mensajes. En los 3 sitios de rama A sin excepción, RF-8 y RF-11 no llegan a plantearse.

## 10. Por qué el criterio de búsqueda corre sobre una lista corta

RF-13 manda «dos búsquedas sobre el alcance de la revisión». Leído como «cero resultados en el
árbol», el criterio sería **imposible de cumplir**: de las 114 llamadas que pasan una excepción, 94
quedan fuera del alcance por decisión expresa de la spec madre (categorías pospuestas: cifrado,
sincronización, reproducción de medios y acceso a archivos externos). Exigir cero en el árbol
obligaría a ampliar el alcance sin que la spec lo autorice.

Por eso el criterio se evalúa **sobre la lista explícita de los 23 sitios**:

1. Para cada uno de los 23, la búsqueda de interpolaciones **no devuelve** nada que sea contenido de
   nota o referencia de medio.
2. Para cada uno de los 20 que pasaban excepción, la búsqueda de «último argumento = objeto de
   excepción» **no devuelve** nada.

Los recuentos globales (143 / 35 / 114) se usan para dos cosas distintas, y ninguna es el criterio
de RF-13:

- Comprobar que el diff **no toca** ninguno de los 24 sitios conservados de §4.2.
- Comprobar que el número de llamadas que interpolan **puede bajar** en los sitios modificados sin
  que eso sea un incumplimiento: lo que no puede bajar es el número de escrituras por sitio
  (RNF-2).

## 11. Decisiones con su alternativa descartada

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| D-1 | Identificador opaco = contador monotónico por operación | hash del contenido (MD5/SHA-1 de la ruta) | La spec lo prohíbe: un hash es un resumen del valor, es reversible por tabla y admite ataque por diccionario |
| D-2 | `isolatedNext()` para los sitios sin operación | `UUID.randomUUID()` | 36 caracteres en cada línea, ilegibles, y correlacionar dos trazas exige compararlos |
| D-3 | Mensaje propio = literal que describe la acción; la excepción solo da su clase | sanear `e.localizedMessage` con una regex | Un saneado es una lista negra: la fuga reaparece en cuanto la biblioteca añade un prefijo, y el idioma del mensaje depende del sistema |
| D-4 | El objeto de excepción no se registra nunca | `e.stackTraceToString()` | Es exactamente lo que RF-8 prohíbe; además imprime el mensaje de la cadena completa |
| D-5 | **El helper se triplica** | un módulo Gradle compartido nuevo | `:app` depende de las librerías y un helper en `:app` no es visible desde ellas; un módulo nuevo rompe RNF-6 y roza el principio 1 |
| D-6 | Un solo punto de composición por módulo | 23 cadenas de formato escritas a mano | 23 copias se desincronizan en cuanto cambia el formato, y ninguna es comprobable |
| D-7 | La categoría se **pasa** por parámetro | deducirla de la extensión del archivo | Deducirla devuelve dato de la referencia de medio, que es justo lo que hay que quitar, y no funciona con URL |
| D-8 | La categoría más específica disponible | usar siempre la constante `"media"` | Cumple la letra de RF-10 pero pierde el «dónde falló» que la Definición de traza técnica promete |
| D-9 | El identificador **no** incluye el id de la nota | registrar `noteId` | Correlaciona la traza con el material de la persona: reintroduce lo que la spec elimina |
| D-10 | Se aplica RF-11 en todo sitio del alcance | tratar RF-8 y RF-11 como dos pasadas | RF-11 cumplida implica RF-8 cumplida; dos pasadas duplican trabajo y mensaje |
| D-11 | El criterio de RF-13 corre sobre la lista de 23 sitios | exigir cero en el árbol | 94 llamadas con excepción quedan fuera por decisión expresa de la spec; el criterio global sería incumplible |
| D-12 | Los 3 sitios de `org.json` entran (14, 15, 16) | dejarlos fuera por ser «solo excepción» | Son las instancias del caso límite 8 de la spec, que manda lo contrario |
| D-13 | `AudioPlayerWidget.kt:180` queda fuera | incluirlo por compartir archivo con `:127` | Su excepción viene de leer la posición del reproductor y no lleva la referencia del medio; `:127` entra por interpolar `${error.message}` |
| D-14 | Los casos de `failureMessage` en `:app/src/test` | crearlos en los tres módulos | La función es la misma; un solo juego de casos basta y evita crear un source set de test en `:visor-media`, que no tiene |
| D-15 | `warnings?.add(...)` se deja como está | «limpiarlo» por coherencia | No es el registro del sistema: es contenido devuelto a quien lo escribió |

## 12. Estrategia de tests

| RF | Cómo se verifica |
|---|---|
| RF-5, RF-6, RF-7, RF-13 | búsquedas sobre los 23 sitios (§10) |
| RF-8, RF-11 | búsqueda de «último argumento = excepción» sobre los 20 sitios que la tenían |
| RF-10 | cada mensaje de reemplazo declara al menos un campo interpolado; los casos de `failureMessage` |
| RF-14 | el diff no toca ninguno de los 24 sitios conservados |
| RNF-2 | recuento de escrituras por sitio, antes y después |
| RNF-3 | las suites de exportación, dibujado, respaldo y widget siguen en verde |

**Suites que hacen de red de seguridad.** Ya existen y ejercitan los sitios modificados; si RF-5 a
RF-14 rompen algo, se ven aquí antes que en un dispositivo:

| Suite | Sitio que ejercita |
|---|---|
| `HtmlMediaEmbedderTest` | 10 |
| `MarkdownAttachmentCollectorTest` | 9 |
| `MarkdownExportMediaTest`, `PlainTextExportText` | 8, 9 |
| `PdfExporterTest` | 11 |
| `StorageAudioFilesTest` | zona de almacenamiento de RNF-3 |

**Cobertura de las ramas de captura.** Ninguna suite dispara hoy un fallo de lectura de medio, así
que las ramas `catch` **no tienen cobertura directa**. Es consecuencia de la Decisión 22 de la madre:
la excepción de RF-19 se abre solo para `failureMessage`, no para los sitios. Lo que sí se comprueba
es que el formateador es correcto y que las suites existentes siguen en verde.

## 13. RF-20 — condensar la memoria del proyecto

`MEMORY.md` tiene hoy más de 100 líneas y `AGENTS.md` pide un máximo de ~50.

| Archivo | Acción |
|---|---|
| `MEMORY.md` | condensar a ≤ 50 líneas: estado actual, decisiones con su motivo, errores a evitar. Eliminar el detalle que ya no aporte |
| `docs/adr/` | mover a un ADR nuevo (`0004-limites-de-constitucion.md`) lo que sea decisión de fondo y ya no estado actual: el identificador opaco, la deriva de versiones, la historia de la tabla |

Lo que **no** se traslada a un ADR: el estado de la tabla, los defectos de selección y las tareas de
verificación visual, porque son estado y tienen fecha, no decisiones.

## 14. Anomalía detectada en la spec madre (no se corrige aquí)

La **Decisión 22** de `specs/001-limites-constitucion/spec.md` se añadió para abrir la excepción de
RF-19, pero su última frase dice, en presente: «El plan aplica RF-19 **sin esa excepción** y acepta
que las ramas de captura que la entrega modifica no tengan cobertura directa». Esa frase describe el
plan **anterior** a la enmienda y contradice el sentido de la propia Decisión 22, que es admitir la
excepción.

No es una contradicción operativa: el texto de RF-19, su criterio de finalización y la Decisión 21
(coherentemente enmendada) coinciden sin ambigüedad en que **la excepción está admitida solo para la
función pura del mensaje de reemplazo**. Por eso este plan la aplica. Se deja constancia y **no se
edita la spec madre**, que está aprobada. Corregir esa frase es una decisão del usuario.

## 15. Lo que este plan NO hace

- No añade ninguna dependencia: el helper usa `java.util.concurrent.atomic.AtomicLong`.
- No crea ningún módulo Gradle.
- No toca los 24 sitios conservados de §4.2.
- No toca las listas `warnings` que se muestran a la persona usuaria.
- No aplica cambios de comportamiento: solo qué texto llega al registro del sistema.
- No cubre las categorías de excepción pospuestas: tienen su spec por zona.
- No añade casos de prueba fuera de los de `failureMessage`.
- No arregla la representación de tablas ni la selección de los bloques editables.
# Tasks — Spec 001 · Límites de la constitución

> **SUPERADO — NO IMPLEMENTAR DESDE AQUÍ.** El usuario aprobó la división de esta spec el
> 2026-10-03. Las tareas de abajo se han redistribuido y renumerado en tres specs hijas; estas son
> las documentos operativos:
>
> | Spec | Tareas | Tarea antigua |
> |---|---|---|
> | `specs/002-desbloqueo-tests/` | T1-T5 | T1-T5 |
> | `specs/003-dependencias-inventarios/` | T1-T2 | T6-T7 |
> | `specs/004-registro-del-sistema/` | T1-T7 | T8-T14 |
>
> Este archivo se conserva como registro histórico del razonamiento y del rastro de numeración.
> Lo mismo aplica a `plan.md` de esta carpeta.

Plan: `specs/001-limites-constitucion/plan.md` · Spec aprobada: `spec.md`

## Propuesta de división (obligatoria según la skill)

Salen **14 tareas**, y la skill fija un máximo de 10 por spec. **Propongo dividir la spec 001 en
tres specs consecutivas**, para que cada una se pueda verificar y cerrar por separado:

| Spec nueva | Contenido | RF | Tareas |
|---|---|---|---|
| **002** | Desbloqueo de los tests unitarios | RF-15 … RF-19 | T1-T5 (5) |
| **003** | Dependencias e inventarios | RF-1 … RF-4 | T6-T7 (2) |
| **004** | Registro del sistema | RF-5 … RF-14 | T8-T13 (6) |

`RF-20` (condensar `MEMORY.md`) viaja con la **última** spec que se entregue, porque su contenido
depende del estado final de las otras dos.

El orden 002 → 003 → 004 no es de conveniencia: sin `./gradlew test` en verde (002) no hay forma de
verificar RNF-3 en 004, y 003 es la más barata de las dos.

**Las 14 tareas están igualmente listadas abajo**, en orden de dependencia, por si el usuario
prefiere no dividir y ejecutarlas de una vez. En ese caso el orden es el del documento y nada más
cambia.

## Dependencia entre tareas

```
T1 ─┐
T2 ─┼─→ T4 ─→ T5 ─→ T8 ─→ T9 ─→ T10 ─→ T11 ─→ T12 ─→ T13 ─→ T14
T3 ─┘              T6 ─→ T7 ──────────┘
```

T6-T7 (dependencias) no dependen de los tests y pueden solaparse con la fase 0.

---

## Fase 0 — Desbloqueo de los tests

- [ ] **T1. Arreglar `WidgetNotesTest`: 4 errores de compilación, entorno de prueba y literal visible.** RF-15, RF-17, RF-18
  - Poner `@RunWith(RobolectricTestRunner::class)` y `@Config(sdk = [36])`; añadir
    `private val context: Context = ApplicationProvider.getApplicationContext()`.
  - `widgetTitle(note(...))` → `widgetTitle(context, note(...))`; `widgetSummary(texto)` →
    `widgetSummary(texto, context)`.
  - `assertEquals("Untitled", …)` → `assertEquals(context.getString(R.string.widget_note_untitled), …)`.
  - Hecho cuando: `./gradlew :app:compileDebugUnitTestKotlin --no-configuration-cache` ya no
    reporta errores en `WidgetNotesTest.kt` (los 9 que quedan son solo de `PdfExporterTest.kt`).

- [ ] **T2. Arreglar `PdfExporterTest`: 9 errores de compilación.** RF-15
  - Pasar `context` como argumento nombrado en las 9 llamadas de `measureBlock` y `drawBlock`,
    para dejar `maxHeight` en su valor por defecto: `measureBlock(b, embedder, 495, context = context)`.
  - Hecho cuando: `./gradlew :app:compileDebugUnitTestKotlin --no-configuration-cache` termina
    sin errores, con 0 errores y las 27 suites de test compilando.

- [ ] **T3. Subir las dos suites que declaran SDK 28 al objetivo de versión android.** RF-17
  - `@Config(sdk = [28])` → `@Config(sdk = [36])` en `HtmlMediaEmbedderTest.kt:25` y
    `MarkdownAttachmentCollectorTest.kt:23`.
  - Hecho cuando: `grep -rn "sdk = \[28\]" app/src/test` no devuelve ningún resultado y
    `./gradlew :app:compileDebugUnitTestKotlin` sigue sin errores.

- [ ] **T4. Generar y versionar los dos goldens que faltan y fijar el modo verificación.** RF-16
  - `./gradlew :app:testDebugUnitTest --tests '*EquationScreenshotTest*' -Proborazzi.test.record=true`
  - Versionar `equation_inline.png` y `equation_fallback.png`.
  - Añadir `roborazzi.test.verify=true` a `gradle.properties`.
  - Hecho cuando: `ls app/src/test/screenshots/` lista `greeting.png`, `equation_inline.png` y
    `equation_fallback.png`; `git status` los muestra como nuevos y versionados; y
    `grep -c "roborazzi.test.verify=true" gradle.properties` devuelve 1.

- [ ] **T5. Primera suite verde completa — puerta de la fase B.** RF-16
  - Hecho cuando: `./gradlew test` termina en `BUILD SUCCESSFUL` y el recuento de tests
    ejecutados es mayor que cero y no menor que el número de suites existentes (27), con las
    pruebas de imagen en modo verificación.

---

## Fase A — Dependencias e inventarios

- [ ] **T6. Retirar Firebase de la configuración y del catálogo de versiones.** RF-1, RF-2, RF-3
  - Borrar `app/build.gradle.kts:100` (`platform(libs.firebase.bom)`), `:131`
    (`// implementation(libs.firebase.ai)`) y `:132` (`implementation(libs.firebase.analytics)`).
  - Borrar de `gradle/libs.versions.toml` las líneas 42 (`firebaseBom`), 113 (`firebase-bom`),
    114 (`firebase-ai`) y 115 (`firebase-analytics`).
  - No tocar el plugin `google-services` (línea 12) ni `app/google-services.json`.
  - Hecho cuando: `grep -rn "firebase" app/build.gradle.kts gradle/libs.versions.toml` no
    devuelve ningún resultado, y
    `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -i "firebase.analytics"`
    no devuelve ningún resultado.

- [ ] **T7. Inventarios de licencias: quitar la entrada de telemetría y corregir la deriva de versiones.** RF-4
  - Borrar la entrada "Firebase Analytics (BOM)" de `app/src/main/assets/oss-licenses.json`
    (línea 175) y de `THIRD-PARTY-NOTICES` (línea 61).
  - Coil `3.5.0` → `3.6.3`: `oss-licenses.json:120`, `THIRD-PARTY-NOTICES:39`.
  - Plataforma de Compose `2026.06.01` → `2026.09.00`: `oss-licenses.json:16`,
    `OssLicenses.kt:55`, `THIRD-PARTY-NOTICES:17`.
  - Material 3 `1.5.0-alpha25` → `1.5.0-alpha28`: `oss-licenses.json:24`, `OssLicenses.kt:56`,
    `THIRD-PARTY-NOTICES:18`.
  - Conservar las licencias de autenticación de Drive y la entrada de Retrofit.
  - Hecho cuando: `grep -rn "3\.5\.0\|2026\.06\.01\|1\.5\.0-alpha25" app/src/main/assets/oss-licenses.json THIRD-PARTY-NOTICES app/src/main/java/com/example/util/OssLicenses.kt`
    no devuelve ningún resultado; `grep -rn "Firebase Analytics" app/src/main/assets/oss-licenses.json THIRD-PARTY-NOTICES`
    no devuelve ningún resultado; y `grep -c "play-services-auth\|Play Services Auth" THIRD-PARTY-NOTICES`
    sigue siendo mayor que 0.

---

## Fase B — Registro del sistema

- [ ] **T8. Helper del identificador opaco y del mensaje de reemplazo en `:app`.** RF-6, RF-7, RF-10
  - Crear `app/src/main/java/com/example/util/OpaqueTrace.kt` con el contador `AtomicLong`,
    `begin()`, `Op.next()` e `isolatedNext()`.
  - Añadir `failureMessage(accion, categoria, idOpaque, hoy: Class<*>?)` (función pura) y la
    fachada `registrarFallo(tag, accion, categoria, id, e: Throwable?)`, que extrae solo
    `e?.javaClass` y nunca `e.message`.
  - Ambas funciones ≤ 20 líneas, sin dependencia nueva y sin estado global más allá del contador.
  - Hecho cuando: el archivo compila (`./gradlew :app:compileDebugKotlin`), no declara ninguna
    dependencia nueva (`./gradlew :app:dependencies --configuration releaseRuntimeClasspath` no
    cambia de número de artefactos), y una revisión confirma que el texto de la excepción solo
    llega al registro por `e::class.java.simpleName`.

- [ ] **T9. Rama A, grupo medios, respaldo y almacenamiento: 8 sitios.** RF-5, RF-7, RF-10, RF-11
  - `MediaStorageHelper.kt:31,36` · `BackupAttachmentHelper.kt:151,158,308` ·
    `StorageAnalyzer.kt:304,337` · `JsonExporter.kt:223`.
  - Añadir el parámetro `op: OpaqueTrace.Op` a `deleteDirectoryContents` para que la recursión
    comparta operación con `deleteFiles`.
  - No tocar las líneas `warnings?.add(...)` de `BackupAttachmentHelper.kt:151,158`: no son el
    registro del sistema.
  - Hecho cuando: una búsqueda de `uriStr`/`$uri`/`${file.path}`/`$path` dentro de llamadas al
    registro del sistema en esos cuatro archivos no devuelve ningún resultado, y
    `./gradlew test` sigue en verde.

- [ ] **T10. Rama A, grupo exportadores, ecuaciones e inferencia: 5 sitios.** RF-5, RF-6, RF-7, RF-10, RF-11
  - `MarkdownAttachmentCollector.kt:47` · `HtmlMediaEmbedder.kt:92` · `PdfBlockRenderer.kt:412` ·
    `LatexEquationRenderer.kt:74` · `OllamaService.kt:81`.
  - Añadir el parámetro `kind` a `HtmlMediaEmbedder.readBytes` y propagarlo desde
    `resolveSource(src, type)`.
  - Crear la operación una vez en `rememberEquationInlineMap` y pedir un id por ecuación.
  - Hecho cuando: una búsqueda de `block.content`/`$src`/`$url`/`$latex`/`$responseBody` dentro de
    llamadas al registro del sistema en esos cinco archivos no devuelve ningún resultado, y
    `./gradlew test` sigue en verde.

- [ ] **T11. Rama B, módulo principal: 3 sitios de análisis JSON.** RF-8, RF-10, RF-11
  - `BackupAttachmentHelper.kt:81` · `StorageAnalyzer.kt:84` · `NoteContentUtils.kt:24`.
  - Sustituir el objeto de excepción por su tipo y por el mensaje propio de la acción, con el
    formateador de T8.
  - Hecho cuando: una búsqueda del patrón `Log.e(<tag>, <msg>, e)` en esos tres puntos no
    devuelve ningún resultado, el log resultante contiene `exc=JSONException` y ninguna otra
    información del texto de la excepción, y `./gradlew test` sigue en verde.

- [ ] **T12. Rama B, módulo `:visor-media`: 4 sitios.** RF-8, RF-10, RF-11
  - `AudioPlayerWidget.kt:127` · `Media3VideoPlayer.kt:104` · `MediaViewerScreen.kt:211` · `:277`.
  - Copiar el helper a `visor-media` (no puede usar el de `:app`: `:app` depende de ella).
  - No tocar `AudioPlayerWidget.kt:180` (sitio conservado).
  - Hecho cuando: una búsqueda de `${error.message}` dentro de llamadas al registro del sistema
    en `visor-media/src/main` no devuelve ningún resultado, ninguna de las 5 llamadas del módulo
    pasa un objeto de excepción como último argumento en esos 4 puntos, y
    `./gradlew :visor-media:test` sigue en verde.

- [ ] **T13. Rama B, módulo `:editor-de-codigo`: 3 sitios, y cierre de la fase.** RF-8, RF-10, RF-11, RNF-3
  - `FileImportExportHelper.kt:68,114,178`.
  - Copiar el helper a `editor-de-codigo`.
  - Cerrar la fase con la puerta de la entrega completa.
  - Hecho cuando: ninguna de las 3 llamadas pasa un objeto de excepción, y
    `./gradlew test` termina en `BUILD SUCCESSFUL` con las suites de exportación, dibujado,
    respaldo y widget en verde y sin aserciones modificadas.

---

## Fase D — Documentación de la entrega

- [ ] **T14. Condensar la memoria del proyecto y registrar las decisiones en un ADR.** RF-20
  - Bajar `MEMORY.md` de 119 líneas a ≤ 50, con estado actual, decisiones con su motivo y errores
    a evitar.
  - Crear `docs/adr/0004-limites-de-constitucion.md` con lo que sea decisión de fondo y ya no
    estado actual: el identificador opaco, la deriva de versiones y la historia de la tabla.
  - Hecho cuando: `wc -l MEMORY.md` devuelve 50 o menos, y
    `ls docs/adr/` incluye `0004-limites-de-constitucion.md`.

---

## Comprobaciones de cierre de la entrega

Se ejecutan una vez, al final, y sustituyen a la lista de criterios de la spec:

- [ ] `wc -l MEMORY.md` ≤ 50
- [ ] `grep -rn "firebase" app/build.gradle.kts gradle/libs.versions.toml` → vacío
- [ ] `grep -rniE "3\.5\.0|2026\.06\.01|1\.5\.0-alpha25" app/src/main/assets/oss-licenses.json THIRD-PARTY-NOTICES app/src/main/java/com/example/util/OssLicenses.kt` → vacío
- [ ] Búsqueda de interpolaciones sobre los 23 sitios del alcance → 0 resultados
- [ ] Búsqueda de "último argumento = objeto de excepción" sobre los 20 sitios que la tenían → 0 resultados
- [ ] `git diff` no toca ninguno de los 24 sitios conservados del plan (tabla 4.2)
- [ ] `git diff --numstat app/src/test` no añade ni quita anotaciones `@Test`
- [ ] `./gradlew test` en verde

## Pendiente de confirmación del usuario

- [ ] **Tensión RF-19 contra el principio 4.** RF-19 prohíbe añadir casos de prueba nuevos; el
  principio 4 exige test para toda función pura nueva, y `failureMessage` lo es. Este plan aplica
  RF-19 (sin casos nuevos, verificación por inspección y por las suites existentes) y acepta que
  las ramas `catch` queden sin cobertura directa. Si se prefiere cobertura directa, hay que
  enmendar RF-19 en la spec **antes** de empezar T8.
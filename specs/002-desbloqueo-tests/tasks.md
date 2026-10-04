# Tasks — Spec 002 · Desbloqueo de los tests unitarios

Plan: `specs/002-desbloqueo-tests/plan.md` · Madre: `specs/001-limites-constitucion/`
5 tareas, en orden de dependencia. Ninguna depende de otra spec.

Cada «Hecho cuando» es un comando o un resultado observable.

## Rastro

Estas tareas son **T1-T5** de la spec 001, renumeradas aquí. Ver `plan.md` §1.

---

- [x] **T1. Arreglar `WidgetNotesTest`: 4 errores de compilación, entorno de prueba y literal visible.** RF-15, RF-17, RF-18
  - Añadir `@RunWith(RobolectricTestRunner::class)` y `@Config(sdk = [36])`.
  - Añadir `private val context: Context = ApplicationProvider.getApplicationContext()`.
  - `widgetTitle(note(...))` → `widgetTitle(context, note(...))`.
  - `widgetSummary(texto)` → `widgetSummary(texto, context)`.
  - `assertEquals("Untitled", …)` → `assertEquals(context.getString(R.string.widget_note_untitled), …)`.
  - `widgetVisibleNotes(notes)` no se toca: no lleva `context`.
  - Hecho cuando: `./gradlew :app:compileDebugUnitTestKotlin --no-configuration-cache` ya no
    reporta ningún error en `WidgetNotesTest.kt`, y los errores que quedan son exactamente los 9 de
    `PdfExporterTest.kt`.

- [x] **T2. Arreglar `PdfExporterTest`: 9 errores de compilación.** RF-15
  - Pasar `context` como **argumento nombrado** en las 9 llamadas de `measureBlock` y `drawBlock`
    para dejar `maxHeight` en su valor por defecto:
    `measureBlock(b, embedder, 495, context = context)`.
  - Líneas afectadas: `drawBlock` en 59 y 203; `measureBlock` en 121, 142, 165, 166, 167, 168, 195.
  - Hecho cuando: `./gradlew :app:compileDebugUnitTestKotlin --no-configuration-cache` termina
    sin errores, y `grep -rn "measureBlock\|drawBlock" app/src/test/java/com/example/PdfExporterTest.kt`
    muestra `context = context` en las 9 llamadas.

- [x] **T3. Subir las dos suites que declaran SDK 28 al objetivo de versión android.** RF-17
  - `@Config(sdk = [28])` → `@Config(sdk = [36])` en `HtmlMediaEmbedderTest.kt:25` y
    `MarkdownAttachmentCollectorTest.kt:23`.
  - Hecho cuando: `grep -rn "sdk = \[28\]" app/src/test` no devuelve ningún resultado, y
    `grep -rn "sdk = \[36\]" app/src/test` devuelve al menos 6 coincidencias (las 4 preexistentes
    más las 2 recién subidas).

- [ ] **T4. Generar y versionar los dos goldens que faltan, y fijar el modo verificación.** RF-16
  - `./gradlew :app:testDebugUnitTest --tests '*EquationScreenshotTest*' -Proborazzi.test.record=true`
  - Versionar `equation_inline.png` y `equation_fallback.png`.
  - Añadir `roborazzi.test.verify=true` a `gradle.properties`.
  - Hecho cuando: `ls app/src/test/screenshots/` lista `greeting.png`, `equation_inline.png` y
    `equation_fallback.png`; `git status --short app/src/test/screenshots` los muestra como
    añadidos; y `grep -c "^roborazzi.test.verify=true$" gradle.properties` devuelve 1.

- [ ] **T5. Primera suite verde completa — puerta de las specs 003 y 004.** RF-16
  - Hecho cuando: `./gradlew test` termina en `BUILD SUCCESSFUL`, el recuento de casos ejecutados es
    mayor que cero y no menor que el número de suites existentes, y el resumen de la tarea de test
    del módulo principal muestra las pruebas de imagen en modo verificación.

---

## Comprobaciones de cierre de la spec

- [ ] `git diff -- app/src/main` está vacío: ningún cambio en código de producción
- [ ] `grep -rn "sdk = \[28\]" app/src/test` → vacío
- [ ] `ls app/src/test/screenshots/` lista los 3 PNG, los 2 nuevos versionados
- [ ] `grep -E "^roborazzi\.test\.(record|compare)=" gradle.properties` → vacío
- [ ] `git diff --numstat app/src/test` no elimina ni desactiva anotaciones `@Test`
- [ ] `./gradlew test` en verde

## Pendiente

Ninguno. La excepción de RF-19 no se usa en esta spec: la función pura del mensaje de reemplazo la
introduce la spec 004.
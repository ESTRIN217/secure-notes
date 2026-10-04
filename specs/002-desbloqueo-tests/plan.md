# Plan — Spec 002 · Desbloqueo de los tests unitarios

Spec: `specs/002-desbloqueo-tests/spec.md` · Madre: `specs/001-limites-constitucion/` (**aprobada**)

Este plan es el bloque «Fase 0» del `plan.md` de la spec madre (sección 2), movido tal cual y
ajustado a la nueva numeración de tareas. No reescribe nada: fija las firmas de producción que hay
que llamar y concreta las decisiones que la madre dejaba abiertas.

---

## 1. Rastro de las tareas

La spec 001 numeró estas tareas T1-T5. Aquí se renumeran a **T1-T5** dentro de la 002, pero el
identificador de la tarea madre se conserva en la primera línea para poder seguir el rastro:

| Tarea 002 | Era | Tarea 001 |
|---|---|---|
| **T1** | → | T1 |
| **T2** | → | T2 |
| **T3** | → | T3 |
| **T4** | → | T4 |
| **T5** | → | T5 |

El bloque completo de la madre es T1-T14; las tareas que no aparecen aquí viven en las specs 003
(T6-T7) y 004 (T8-T14).

## 2. Por qué esta spec va primera

`./gradlew test` no es una comprobación más: es la **puerta**. Sin ella no hay forma de verificar
RNF-3 en la spec 004 («las suites de exportación, dibujado, respaldo y widget siguen en verde»),
que es el criterio que protege 13 de los 23 sitios del registro del sistema. Por eso esta spec no
depende de ninguna otra y las otras dos sí dependen de ella.

Coste a tener en cuenta: cada invocación de Gradle paga ~6 minutos. Las tareas están agrupadas
para no pagar esa factura más de cuatro veces.

## 3. Archivos y responsabilidades

| Archivo | Cambio | RF |
|---|---|---|
| `app/src/test/java/com/example/WidgetNotesTest.kt` | 4 errores + entorno de prueba + literal visible | RF-15, RF-17, RF-18 |
| `app/src/test/java/com/example/PdfExporterTest.kt` | 9 errores de compilación | RF-15 |
| `app/src/test/java/com/example/HtmlMediaEmbedderTest.kt` | `@Config(sdk = [28])` → `[36]` | RF-17 |
| `app/src/test/java/com/example/MarkdownAttachmentCollectorTest.kt` | `@Config(sdk = [28])` → `[36]` | RF-17 |
| `gradle.properties` | añadir `roborazzi.test.verify=true` | RF-16 |
| `app/src/test/screenshots/equation_inline.png` | generar y versionar | RF-16 |
| `app/src/test/screenshots/equation_fallback.png` | generar y versionar | RF-16 |

**Ningún archivo de producción se toca en esta spec.** El criterio de finalización lo comprueba:
`git diff -- app/src/main` tiene que estar vacío.

## 4. Los 13 errores, uno a uno

### 4.1 `WidgetNotesTest.kt` — 4 errores

Hoy es JUnit plano y llama las funciones con el orden de argumentos anterior. Las funciones de
producción son, en `app/src/main/java/com/example/widget/WidgetNotes.kt`:

```
widgetTitle(context: Context, note: Note): String      // línea 19
widgetSummary(content: String, context: Context): String // línea 25
```

Cambios:

- Añadir `@RunWith(RobolectricTestRunner::class)` y `@Config(sdk = [36])`. `widgetTitle` y
  `widgetSummary` pasan por `RichTextConverter`, que toca recursos, así que Robolectric es
  obligatorio: sin él no hay contexto de aplicación.
- Añadir `private val context: Context = ApplicationProvider.getApplicationContext()`, igual que
  hace ya `PdfExporterTest.kt:39`.
- `widgetVisibleNotes(notes)` **no cambia**: no lleva `context`.
- `widgetTitle(note(1, …))` → `widgetTitle(context, note(1, …))`.
- `widgetSummary("**Hola** mundo " + …)` → `widgetSummary("**Hola** mundo " + …, context)`.
- `assertEquals("Untitled", widgetTitle(…))` → `assertEquals(context.getString(R.string.widget_note_untitled), …)`.

**Por qué el valor esperado tiene que salir del recurso y no del literal.** El idioma sale en
inglés por `systemProp.user.language=en` de `gradle.properties`, así que el valor de
`values/strings.xml` es hoy `"Untitled"` y el literal daría verde. Pero RF-18 lo exige, y con
motivo: el literal couples la suite a un idioma, y el día que ese texto se traduzca la suite
rompe sin que haya pasado nada. Además el literal contradice el principio 6 (un idioma por capa:
todo texto visible sale de sus recursos).

### 4.2 `PdfExporterTest.kt` — 9 errores

Las firmas de producción, en `app/src/main/java/com/example/util/export/PdfBlockRenderer.kt`:

```
measureBlock(block, embedder, maxWidth, maxHeight = Int.MAX_VALUE, context)   // línea 49
drawBlock(canvas, block, embedder, x, y, maxWidth, maxHeight = Int.MAX_VALUE, context)  // línea 52
```

`context` es el **último** parámetro y `maxHeight` es intermedio con valor por defecto. Kotlin no
permite saltar un parámetro posicional intermedio, así que hay dos formas válidas:

| Opción | Llamada | Veredicto |
|---|---|---|
| Argumento nombrado | `measureBlock(b, embedder, 495, context = context)` | **la que se usa**: deja `maxHeight` en su valor por defecto, que es lo que el test quería |
| Valor explícito | `measureBlock(b, embedder, 495, Int.MAX_VALUE, context)` | más frágil ante un cambio futuro del valor por defecto |

Las 9 llamadas affected: `drawBlock` en las líneas 59 y 203, `measureBlock` en 121, 142, 165, 166,
167, 168 y 195. El test ya tiene `context` y ya tiene `@RunWith(RobolectricTestRunner::class)`,
`@GraphicsMode(NATIVE)` y `@Config(sdk = [36])`: no hay que añadir nada más.

**Ojo con este error**: no basta con añadir el argumento al final. Compilaría con un error distinto
—esperaba `Int` y recibió `Context`— o, peor, de forma silenciosa con un tipo equivocado. Hay que
**nombrar** el argumento.

### 4.3 Las dos suites en SDK 28

`HtmlMediaEmbedderTest.kt:25` y `MarkdownAttachmentCollectorTest.kt:23` declaran
`@Config(sdk = [28])`. RF-17 exige el objetivo de versión android, que es **36**.

Motivo verificado: el único artefacto de prueba en caché es el del API 16; con 37 (el nivel de
compilación) Robolectric intentaría descargarlo y la suite no correría sin red. Subirlas a 36 las
pone en el mismo nivel que el resto y evita que aparezcan después como fallo preexistente.

## 5. Los dos goldens que faltan

`app/src/test/screenshots/` contiene un solo archivo versionado, `greeting.png`.
`EquationScreenshotTest.kt` escribe dos capturas más que nunca se generaron:

| Archivo | Test | Línea |
|---|---|---|
| `equation_inline.png` | `equation_inline_readonly` | 34 |
| `equation_fallback.png` | `equation_invalid_fallback` | 45 |

Se generan **antes** de fijar el modo verificación, porque en modo verificación y sin referencia
el resultado falla aunque sea correcto:

```
./gradlew :app:testDebugUnitTest --tests '*EquationScreenshotTest*' -Proborazzi.test.record=true
```

Se versionan los dos PNG. Esto no es aceptar una imagen que nadie miró: es la referencia que el
modo verificación exige por definición, y la Decisión 16 de la spec madre lo asume.

## 6. Fijar el modo verificación

`gradle.properties` no fija ninguna propiedad de Roborazzi y `app/build.gradle.kts` no tiene
bloque `roborazzi {}`. Se añade **una línea** a `gradle.properties`:

```properties
roborazzi.test.verify=true
```

Con eso `./gradlew test` verifica por defecto y el criterio «no están fijadas
`roborazzi.test.record` ni `roborazzi.test.compare`» se cumple sobre el archivo de propiedades del
proyecto. Para regenerar una referencia se pasa `-Proborazzi.test.record=true` en la línea de
comandos, que tiene precedencia sobre la propiedad del proyecto y por eso no ensucia el archivo.

## 7. Decisiones con su alternativa descartada

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| D-1 | Argumento nombrado para `context` | valor explícito `Int.MAX_VALUE` | Menos frágil ante un cambio del valor por defecto |
| D-2 | Robolectric en `WidgetNotesTest` | seguir con JUnit plano y no tocar las funciones | `widgetTitle` y `widgetSummary` leen recursos vía `RichTextConverter`; sin contexto no hay forma de probarlas |
| D-3 | Valor esperado desde `R.string` | dejar el literal `"Untitled"` | Daría verde hoy y rompería en cuanto se traduzca el texto; RF-18 lo prohíbe |
| D-4 | Generar los goldens con `-Proborazzi.test.record=true` antes de fijar verify | fijar verify primero | Con verify y sin referencia, la suite falla por falta de referencia y no distingue un fallo real |
| D-5 | La propiedad en `gradle.properties` | un bloque `roborazzi {}` en `app/build.gradle.kts` | El archivo de propiedades es el sitio donde ya viven las propiedades del proyecto; el bloque añadiría estructura de build |
| D-6 | Subir las dos suites de 28 a 36 | dejarlas en 28 | RF-17 exige el objetivo de versión android y dejasolas son un incumplimiento ya previsible |
| D-7 | Ningún cambio en código de producción | cambiar `PdfBlockRenderer` para que `context` tenga valor por defecto | Fuera de alcance: la spec madre no autoriza cambios de comportamiento |

## 8. Estrategia de tests

Esta spec **no** añade suites: desbloquea las que ya existen. El mapa de verificación:

| RF | Cómo se verifica |
|---|---|
| RF-15 | `./gradlew :app:compileDebugUnitTestKotlin` termina sin errores |
| RF-16 | `./gradlew test` en `BUILD SUCCESSFUL`, con `roborazzi.test.verify=true` en `gradle.properties` y los 3 PNG versionados |
| RF-17 | `grep -rn "sdk = \[28\]" app/src/test` vacío; `@Config(sdk = [36])` presente en las 4 suites afectadas |
| RF-18 | el test del widget llama a `context.getString(R.string.widget_note_untitled)` |
| RF-19 | `git diff --numstat app/src/test` no elimina ni desactiva anotaciones `@Test` |

Cobertura que se recupera al desbloquear la compilación (ninguna es nueva, todas existían):
`WidgetNotesTest` (5 casos), `PdfExporterTest` (su bloque completo), más las otras 25 suites del
módulo que hoy no se ejecutan.

RNF-6 se comprueba por inspección: no hay ninguna dependencia ni tarea de compilación nueva. El
único archivo de producción que se toca es `gradle.properties`, y lo que se añade es una
propiedad.

## 9. Salida de esta spec

`./gradlew test` en verde es la condición de entrada de las specs 003 y 004. Si al desbloquear la
compilación aparece un fallo preexistente distinto de las referencias de imagen, RF-16 obliga a
documentarlo y abrir su propia spec; no a desactivar el caso.
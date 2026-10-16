# MEMORY.md — secure-notes 

Memoria entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte. 

## Estado actual
- **Spec 020 (el registro de fallos) IMPLEMENTADA**, T1-T9. `specs/020-registro-de-fallos/`, RF-117…RF-123,
  RNF-63…RNF-70. Paquete nuevo `com.example.crash` (cuatro ficheros **puros** + `CrashReporter` y
  `CrashNotifier`), `CrashReportActivity` (**Activity propia**, no pantalla del Navigator), `ui/crash/` y
  `CrashReportViewModel`. **43 pruebas** (25 puras + 18 invariantes): `:app` **711/41** contra base **668/41**
  con **los mismos 41 fallos**. **Cero dependencias** (`git diff` no toca ningún `build.gradle.kts`).
  **Sin verificar en dispositivo.**
- **Specs 016-019 (Markdown: chat, Ajustes y vista previa) implementadas.** `:markdown` es el módulo del motor
  (`MarkdownEngine`, `MarkdownText`, `LocalFileImageLoader`), `compose-markdown` está **retirada** y el editor
  de código **sin `WebView`**. ✅ **Verificadas en dispositivo** por quien Decide: **016, 017, 018 y 019**
  (2026-10-16, «todo se ve bien»). Detalle largo y cifras en cada `specs/*/spec.md`.
- **Todo lo demás también está verificado** (2026-10-16, «todo se ve bien»): **005, 006, 007, 008, 009, 010,
  011, 012, 013 y 014** (esta última entera, T6-T8 incluidas). **La 020 es la única spec sin mirar**, y no por
  olvido: no se comprueba abriendo una pantalla, sino **provocando un fallo real**.
- `./gradlew test` completo **no está verde** y aquí no puede estarlo: Robolectric sin runtime nativo para
  `linux/aarch64`. Los tests **puros sí se ejecutan**. Base: `:app` **711 / 41**, `:editor-de-codigo` **145 / 2**;
  de los 41, **38** son de plataforma y **3** ajenos y preexistentes (`HtmlExportMediaTest` y dos de
  `OnDeviceModelSourceTest`).

## Decisiones (y por qué)
> Las **reglas permanentes** del registro de fallos ya viven en `AGENTS.md` (gotcha «Crash log»). Aquí solo
> queda lo que todavía no es regla: por qué se decidió así.

- **31. El aviso se consume al hacer algo con el informe, no al mirarlo.** Copiar **no** consume (el paso
  siguiente es mandarlo); compartir y enviar **sí**; cerrar **no**; «Descartar» es la única vía para silenciarlo.
  Con «solo el último fallo», consumir de más pierde el informe y de menos devuelve el aviso cada arranque.
- **32. «Ver informe» NO consume.** La primera versión consumía el fichero antes de `startActivity` y la
  `Activity` lo habría leído **vacío**. Un invariante lo cazó; el compilador, no.
- **35. El aviso de privacidad va SIEMPRE, sin mirar el informe.** El mensaje de la excepción es el único campo
  que la app **no** controla, y mirar para decidir si hay que avisar es mirar justo lo que no se debe. Si se
  equivoca, deja pasar lo que tenía que tapar.
- **29-30 (Markdown).** `markwon-image` no hace falta: su maquinaria está **dentro** de `markwon-core`. Y la
  comparación de dependencias tiene que ser la del **`:app`**: el árbol de `:markdown` resuelve 23 coordenadas
  **viejas** que la app ya tiene, y «un módulo nuevo con 23 dependencias extra» es lo que una comparación
  ingenua daría por malo.
- **26-28 (Markdown).** El cargador de imágenes **no es de Coil**: la seguridad es del **tipo**, no de la
  configuración —este no tiene el método de pedir nada por red—. La decisión va **por ruta**, no por esquema, y
  **desenvolver `%20` antes de normalizar `..`** es lo que para `%2e%2e`.
- **22-25 (Markdown).** Las cuatro superficies pintan con **un** motor; solo la burbuja adapta el markup
  heredado; `textStyle = null` significa «no toques la vista»; una dirección se abre **sin la puntuación de su
  frase**, y `)` solo se quita si queda sin parear (`https://ejemplo.com/a_(b)` es válida).
- **13 (Markdown).** `commonmark` ya no se declara en ninguna parte; el pin era decoración. **Una línea de
  catálogo sin nada detrás no fija nada: solo lo aparenta**, y un invariante daba verde sobre un control ya
  inexistente.
- **12. El inset del teclado se aplica UNA vez**, en el `bottomBar`, con `union`.
- **El host de IA vive en la `Application`**, no en el ViewModel. Descarga por **referencias + inactividad**.

## Aprendizajes y errores a evitar
- **Un invariante demasiado burdo es ruido, y el ruido se acaba silenciando.** Buscar `'` para «nada de
  apóstrofos crudos» marca rojo `d\'erreur`, que **es** la forma correcta: hay que mirar `(?<!\\)'`. Y un
  invariante que decía «`onCreate` no debe crear el canal» era **falso**. Cuando un invariante y el código
  discrepan, la duda es primero del invariante.
- **Delegar es la ÚLTIMA línea, fuera de todo `try`**; y escribir es `try/catch (Throwable)`, no `runCatching`:
  `OutOfMemoryError` no es `Exception`.
- **Todo recorrido por código fuente quita comentarios antes de buscar** y **autocomprueba que lee el
  fichero**: si no, el KDoc que explica por qué algo **no** está da verde, y un recorrido vacío pasa por vacío.
- **Comparar los fallos por clase, no en cantidad**: «689/41 contra 656/41» suena a cero regresiones y no lo
  prueba. Un `BUILD SUCCESSFUL` puede no ejecutar nada: `--rerun`, y `--continue` para no parar en el primer
  módulo que falla.
- **`Modifier.weight` no se importa** (es miembro de `RowScope`/`ColumnScope`) y un identificador entrecomillado
  no puede llevar `.`. **`runtime` no llega al classpath de compilación**; `toPx` es miembro de `Density`;
  Compose no tiene API pública `FontFamily` → `Typeface`. Un `AndroidView` en `key(texto.hashCode())` se
  destruye y recrea en cada cambio. El ancla `^` de Kotlin con `Regex.find(text, start)` no se mueve del 0.
- **Apóstrofos crudos** en `strings.xml` rompen `mergeDebugResources`, **después** de que Kotlin compile limpio.
- **Escribir las pruebas primero paga.** En la 020, un invariante pilló el fallo de «consumir antes de leer»,
  que no se habría visto hasta que el usuario pulsara el botón.

## Próximos pasos
- **Verificar en dispositivo, spec 020, EN ESTE ORDEN**: (1) provocar un fallo real y comprobar que **la app se
  cierra igual que antes**; (2) que al reabrir **salga la notificación** y abra la pantalla; (3) que **sin
  permiso** salga el aviso dentro, y **también con la bóveda bloqueada**; (4) que «Enviar» abra GitHub **con el
  cuerpo relleno** —lo único que ninguna prueba pura comprueba—; (5) que tras copiar o compartir el aviso
  **no** vuelva a salir.
- **Spec 015 (siguiente)**: (1) decidir cómo se modelan los `InsertMediaDialog` ×2 (14 parámetros, 8 de ellos
  eventos); (2) agrupar el estado del editor (95 declaraciones) —es lo único que puede bajar `NoteEditorScreen.kt`
  de las 600 líneas—; (3) desbloquear partir `handlePickedFile`, `openExternalFile`, `moveBlockToAnotherNote` y
  `handleSlashSelect`.
- **Sin verificar en dispositivo**: solo la **020**, y por cómo se comprueba —no se abre una pantalla y se
  mira, se provoca un fallo—. Detalle largo en `specs/*/plan.md`.
- **Defectos conocidos, sin spec**: `removeTransformForTag("normal", …)` no tiene brazo `normal`, así que el
  botón «normal» de la barra no hace nada (~2 líneas); el énfasis dentro de una celda de tabla o del texto de un
  título no se tokeniza; `BlockEditor.kt:1011` tiene un stub para `BlockType.TABLE` que ignora `meta["table"]` →
  la tabla sale vacía en TTS y previsualización; `BlockEditor.kt:92` comparte un único `pendingSelection` entre
  todos los bloques; `NoteContentBlock.TableBlock` sigue guardando los títulos aparte (segundo modelo de tabla).
- Spec 002, T4-T5: generar los dos goldens que faltan y fijar `roborazzi.test.verify`.
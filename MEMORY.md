# MEMORY.md — secure-notes 

Memoria entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte. 

## Estado actual
- **Spec 022 (un solo resaltador de código) IMPLEMENTADA Y VERIFICADA EN DISPOSITIVO** (2026-10-16, «todo se ve
  bien»), T1-T9.
  `specs/022-resaltador-de-codigo-unico/` (spec + plan + tasks), RF-131…RF-139, RNF-76…RNF-81. El bloque de
  código de `:app` ya **no tiene resaltador ni catálogo propios**: reutiliza el resaltador y el catálogo de
  `:editor-de-codigo` (dirección única app → editor). **Nuevos**: `util/CodeSyntax.kt` (puro: mapeo
  identificador→idioma, paleta `blockTheme`, `highlight`, `options`) y `ui/CodeLanguageSheet.kt`.
  **Borrados**: `CodeHighlighter.kt`, `CodeLanguages.kt` y `CodeHighlighterTest.kt`. **Sin cambios** en
  `:editor-de-codigo` ni en `strings.xml`. `:app` **726/41** (de 742: −38 del test borrado, +22 nuevos),
  `:editor-de-codigo` 136/2, `:markdown` 49/0; los 41 fallos son **las mismas clases** de plataforma/prexistentes.
  Aspecto igual (paleta y fondo), **clasificación de tokens** ahora la del editor. `toml` e `ini` siguen como
  **dos** opciones del selector (ambas → `INI_TOML`) y se conservan las etiquetas traducidas de hoy.
- **Spec 021 (atajos del lanzador) IMPLEMENTADA**, T1-T7, **sin verificación en dispositivo** (guion de 7 pasos al
  final de `specs/021-atajos-del-lanzador/spec.md`). RF-124…RF-130, RNF-71…RNF-75. **31 pruebas** (13 puras + 18
  invariantes) en verde; `:app` de **711/41 a 742/41** con **los mismos 41 fallos** y ninguna clase nueva rota.
  Cero dependencias y **cero `strings.xml` tocados**. Puro en `com/example/shortcut/` (enum + pendiente + `decidir`)
  y cableado con **una sola** `resolverAtajo` para el arranque en frío y para `onNewIntent`.
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
  `linux/aarch64`. Los tests **puros sí se ejecutan**. Base: `:app` **726 / 41** (tras la 022), `:editor-de-codigo`
  **136 / 2**. De los 41, **38** son de plataforma y **3** ajenos y preexistentes (`HtmlExportMediaTest` y dos de
  `OnDeviceModelSourceTest`).

## Decisiones (y por qué)
> Las **reglas permanentes** del registro de fallos ya viven en `AGENTS.md` (gotcha «Crash log»). Aquí solo
> queda lo que todavía no es regla: por qué se decidió así.

- **36 (atajos).** Los cinco van a `MainActivity` porque **solo una Activity `MAIN`/`LAUNCHER` puede declarar
  atajos**, y `CodeEditorActivity` solo tiene filtros `SEND`/`VIEW`: apuntarle es imposible, y darle
  `MAIN`/`LAUNCHER` pondría un **segundo icono** en el lanzador. Además **`shortcuts.xml` no admite banderas de
  `Intent` ni atributo `monochrome`**, y la doc avisa de que el icono «can't include tints»: «monocromo» son
  vectores de un color sin `android:tint`, y los tiñe el lanzador. Y muchos lanzadores muestran **4 de los 5**,
  por eso el editor de código es el último.
- **37 (atajos).** Las cinco etiquetas **ya existían** en los 9 locales: se comprobó clave por clave antes de
  escribir el XML. Una trampa conocida (falta de traducción) se esquivó sin tocar un solo `strings.xml`.
- **38 (ataajos).** `resolverAtajo` **quita el extra siempre**. `LaunchedEffect(Unit)` corre una vez *por
  composición*, y una rotación crea una nueva con el **mismo** `Intent`: sin el `removeExtra`, el atajo de la
  IA crearía una conversación nueva **en cada giro** (`createSession` inserta fila).
- **39 (ataajos).** `<intent>` de `shortcuts.xml` **no admite `@string`** para los extras, así que
  `EXTRA_DESTINO` y las cinco claves son **literales**. Y `shortcutId` tampoco puede ser un recurso.
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
  **En XML, «quitar comentarios» también hace falta aunque uses un parser**: los invariantes de la 021 buscaban
  «sin `android:tint`» y «sin `monochrome`» en ficheros cuyos **comentarios explican que no llevan esas cosas**,
  y se pusieron rojos con su propia explicación. En un DOM, `getTextContent()` de un `<action>` es **`""`** (su
  valor está en el atributo `android:name`) y no falla: un invariante que compara `""` contra el valor esperado
  sale `0` sin dar la cara. Y `LaunchedEffect(isUnlocked)` aparece **dos veces** en `MainActivity`: con
  `substringAfter` se mira el primero y el invariante es rojo sin motivo —`substringAfterLast`.
- **La secuencia `*/` escrita dentro de un KDoc cierra el KDoc**, aunque escaped con `\` (que en un comentario no
  escapa nada). Es la misma trampa que el apóstrofo crudo en `strings.xml`: **el fichero que documenta la
  trampa se rompe con ella**.
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
- **Un `displayName` no es una etiqueta visible.** `SupportedLanguage.INI_TOML.displayName` es «INI / TOML»,
  pero el selector de la nota ofrecía «TOML» e «INI» por separado: las etiquetas literales van **explícitas**,
  no derivadas del enum.
- **`SyntaxTheme.text = Color.Unspecified` es la forma de NO pisar el color base.** El resaltador del editor
  escribe `SpanStyle(color = theme.text)` sobre todo el texto; sin especificar, el color lo sigue poniendo el
  tema Material y el bloque de la nota no cambia de aspecto (RF-134).
- **Cada opción lleva su identificador: el mapeo inverso no siempre existe.** `toml` e `ini` comparten idioma,
  así que `storedIdFor(language)` no sería inyectiva; sin función inversa no hay nada sin uso.
- **Kotlin no hereda el paquete padre**: `com.example.util` necesita `import com.example.R`.

## Próximos pasos
- **Spec 022 (implementada y verificada)**: nada pendiente.
- **Spec 021 (ya implementada)**: solo falta **verificar en dispositivo**; lo único que ninguna prueba pura
  comprueba es que el lanzador **muestre** los atajos: mantener pulsado el icono.
- **Verificar en dispositivo, spec 020, EN ESTE ORDEN**: (1) provocar un fallo real y comprobar que **la app se
  cierra igual que antes**; (2) que al reabrir **salga la notificación** y abra la pantalla; (3) que **sin
  permiso** salga el aviso dentro, y **también con la bóveda bloqueada**; (4) que «Enviar» abra GitHub **con el
  cuerpo relleno** —lo único que ninguna prueba pura comprueba—; (5) que tras copiar o compartir el aviso
  **no** vuelva a salir.
- **Spec 015 (siguiente)**: (1) decidir cómo se modelan los `InsertMediaDialog` ×2 (14 parámetros, 8 de ellos
  eventos); (2) agrupar el estado del editor (95 declaraciones) —es lo único que puede bajar `NoteEditorScreen.kt`
  de las 600 líneas—; (3) desbloquear partir `handlePickedFile`, `openExternalFile`, `moveBlockToAnotherNote` y
  `handleSlashSelect`.
- **Sin verificar en dispositivo**: la **020** y la **021**, y por cómo se comprueban —no se abre una pantalla y
  se mira: se provoca un fallo o se mantiene pulsado un icono—. Detalle largo en `specs/*/plan.md`.
- **Defectos conocidos, sin spec**: `CodeEditorActivity` es `exported="true"` con filtros `SEND`/`VIEW` y **no
  tiene candado**: hoy se puede abrir desde fuera sin contraseña (la spec 021 lo deja igual, a propósito);
  `removeTransformForTag("normal", …)` no tiene brazo `normal`, así que el
  botón «normal» de la barra no hace nada (~2 líneas); el énfasis dentro de una celda de tabla o del texto de un
  título no se tokeniza; `BlockEditor.kt:1011` tiene un stub para `BlockType.TABLE` que ignora `meta["table"]` →
  la tabla sale vacía en TTS y previsualización; `BlockEditor.kt:92` comparte un único `pendingSelection` entre
  todos los bloques; `NoteContentBlock.TableBlock` sigue guardando los títulos aparte (segundo modelo de tabla).
- Spec 002, T4-T5: generar los dos goldens que faltan y fijar `roborazzi.test.verify`.
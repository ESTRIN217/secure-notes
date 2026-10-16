# Spec 022 — Tareas

> Orden de dependencia. Una tarea por sesión de trabajo (≤ 30 min). Los tests van **primero**.

- [x] **T1. Pruebas puras del pegamento, en rojo.** RF-133, RF-135, RF-136, RF-137, RF-138, RF-139
  - Hecho cuando: existe `app/src/test/java/com/example/CodeSyntaxTest.kt` con los casos de la spec
    (identificadores guardados de hoy y del detector, vacío/nulo/desconocido, texto intacto, todas las opciones
    resuelven, las 26 etiquetas esperadas) y la suite **falla** porque `CodeSyntax` todavía no existe.
  - **Hecho.** `:app:compileDebugUnitTestKotlin` en rojo por `Unresolved reference 'CodeSyntax'`.

- [x] **T2. Implementar `CodeSyntax` (puro).** RF-131 (parcial), RF-132, RF-133, RF-135, RF-136, RF-137, RF-138
  - Hecho cuando: `fromStoredId`, `highlight`, `options` y `blockTheme` existen en
    `app/src/main/java/com/example/util/CodeSyntax.kt`, **sin importar `android`**, y `CodeSyntaxTest` pasa en
    verde.
  - **Hecho.** 12 pruebas, 0 fallos. Sin `storedIdFor` (ver `plan.md`): `toml` e `ini` comparten idioma y cada
    opción lleva su identificador.

- [x] **T3. El bloque lee y edita con el resaltador compartido.** RF-131, RF-134, RF-138
  - Hecho cuando: `ReadOnlyTextBlock` y `EditableTextBlock` llaman a `CodeSyntax.highlight` (dentro de su
    `remember(annotated, highlightLanguage)`) y `compileDebugKotlin` compila.
  - **Hecho.** La guarda `if (highlightLanguage != null)` se conserva: los demás tipos de bloque siguen
    mostrando sus anotaciones intactas.

- [x] **T4. Etiqueta y selector del bloque desde el catálogo compartido.** RF-132, RF-136, RF-139
  - Hecho cuando: `CodeLanguageSheet` vive en su propio fichero, se alimenta de `CodeSyntax.options()` y
    `WysiwygCodeBlock` guarda el `storedId` de la opción elegida; el selector muestra las mismas 26 opciones con
    las mismas etiquetas que hoy.
  - **Hecho.** `ui/CodeLanguageSheet.kt` (nuevo) + `codeLanguageLabel(storedId)`; `WysiwygCodeBlock` usa la
    etiqueta resuelta y la hoja nueva.

- [x] **T5. Eliminar la copia de `:app`.** RNF-77
  - Hecho cuando: `util/CodeHighlighter.kt`, `ui/CodeLanguages.kt` y `CodeHighlighterTest.kt` ya no existen y no
    queda ninguna referencia a ellos en `:app`.
  - **Hecho.** `grep` sin referencias; el invariante lo comprueba en cada ejecución.

- [x] **T6. Invariantes de la spec 022.** RF-132, RF-134, RNF-76, RNF-77, RNF-78, RNF-80
  - Hecho cuando: existen pruebas que comprueban (recorriendo fuentes, **quitando comentarios antes de buscar** y
    **autocomprobando que leen el fichero**) que no queda resaltador ni catálogo en `:app`, que `:app` no define
    palabras clave, que `:editor-de-codigo` no menciona `com.example.`, y que el resaltado va dentro de un
    `remember`; y pasan en verde.
  - **Hecho.** `CodeSyntaxInvariantsTest`, 10 pruebas, 0 fallos.

- [x] **T7. Compilar y comparar la suite.** RNF-79
  - Hecho cuando: `./gradlew :app:compileDebugUnitTestKotlin --rerun` da `BUILD SUCCESSFUL` y
    `./gradlew test --continue` no empeora la cuenta **por clase** respecto a la base (`:app` 742 / 41,
    `:editor-de-codigo` 136 / 2, con los mismos 41 de plataforma).
  - **Hecho.** `:app` **726 / 41**, `:editor-de-codigo` **136 / 2**, `:markdown` 49 / 0. El total de `:app` baja
    (se fueron los 38 tests de `CodeHighlighterTest` y entraron 22 nuevos); los **41 fallos son las mismas
    clases** de plataforma/prexistentes (`HtmlExportMediaTest` 1, `OnDeviceModelSourceTest` 2, y el resto
    Robolectric/screenshot). Ninguna clase de resaltado falla.
  - **Ajeno a esta spec**: `:lib:testDebugUnitTest` y `:visor-pdf:compileDebugUnitTestKotlin` fallan por motivos
    propios de esos módulos (tests de plataforma; `Unresolved reference 'test'`), no tocados aquí.

- [x] **T8. Verificación en dispositivo.** RF-134
  - Hecho cuando: quien Decide abre una nota **guardada antes del cambio** con un bloque de código y confirma que
    se ve legible con la misma paleta y el mismo fondo, en claro y en oscuro, y que una palabra que se coloreaba
    antes se sigue coloreando.
  - **Hecho** (2026-10-16): «todo se ve bien».

- [x] **T9. Actualizar `MEMORY.md`.** (constitución 2)
  - Hecho cuando: `MEMORY.md` refleja la spec 022 como implementada, con las decisiones y los errores a evitar
    que hayan salido.

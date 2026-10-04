# MEMORY.md — secure-notes
Memoria entre sesiones: qué está a medias, qué se decidió y por qué, qué no hay que repetir.
Lo que ya es regla permanente va en `AGENTS.md` o en `docs/adr/`.

## Estado actual (2026-10-04)

- **004 `registro-del-sistema`**: T1-T7 hechas; los 23 sitios cerrados con `OpaqueTrace`
  (`:app`, `:visor-media`, `:editor-de-codigo`). Queda la puerta de cierre: `./gradlew test` en un
  host x86_64 y el recuento de `releaseRuntimeClasspath`.
- **003 `dependencias-inventarios`**: completa, 8/8 criterios verificados.
- **002 `desbloqueo-tests`**: T1-T3 hechas → `compileDebugUnitTestKotlin` ya compila (la nota de
  `AGENTS.md` que lo da por roto está caduca). Pendientes T4 (goldens `equation_inline.png` /
  `equation_fallback.png` + fijar `roborazzi.test.verify`) y T5 (suite verde), bloqueadas por la
  plataforma.
- **Bloque tabla**: rediseño implementado en `ui/EditableTableBlock.kt`, `assembleDebug` en verde,
  **sin verificación visual**. Bug aparte: `BlockEditor.toRenderingBlock` (`BlockEditor.kt:1011`)
  stub de `BlockType.TABLE` que ignora `meta["table"]` → tabla vacía en TTS (~4 líneas).
- **Pendientes de decisión**: (a) `android.enableResourceOptimizations=false` en
  `gradle.properties` desactiva el pase `OptimizeResourcesTask` de AGP; sin él el APK release se
  empaqueta sin manifest ni recursos — no pertenece a esta entrega, no tocar a ciegas. (b) La
  última frase de la Decisión 22 de la spec 001 contradice su propio sentido (`plan.md` 004 §14).

## Decisiones (y por qué)

- **Constitución** (`docs/constitution.md`, 2026-10-03): 6 principios innegociables, cada uno con su
  comprobación ejecutable; la cabecera que ya imponían `AGENTS.md`, `GUIDELINES.md` y la skill `sdd`.
- **Registro del sistema sin contenido de la persona**: identificador opaco = contador por
  operación (no hash) y sin `noteId`; `failureMessage` pura que recibe la clase, nunca la
  excepción; helper replicado por módulo en vez de un módulo Gradle nuevo. Motivos y alternativas
  descartadas: `docs/adr/0004-limites-de-constitucion.md`.
- **Spec 001 dividida** en `002` (desbloqueo de tests, va primera), `003` (inventarios) y `004`
  (registro); `001/plan.md` y `001/tasks.md` llevan banner de "SUPERADO". Motivo: la 001 era la
  puerta que destapó las 143 llamadas `Log.*` y no cabía en una entrega.
- **RF-19 tiene excepción acotada** (Decisión 22 de 001): casos de prueba **solo** para
  `failureMessage`. Ningún otro sitio del registro admite tests nuevos sin tocar la spec.

## Errores a evitar

- **Esta máquina es aarch64 y Robolectric 4.17 no tiene runtime nativo para `linux/aarch64`**: toda
  suite falla en `beforeTest` con «Robolectric native runtime is not supported on Linux (aarch64)»,
  tenga o no `@GraphicsMode(NATIVE)`. Solo se puede **compilar**: aquí la comprobación útil es
  `./gradlew :app:compileDebugUnitTestKotlin` y `:compileDebugKotlin`.
- **`grep -rn "firebase"` miente**: el plugin `google-services` **sí** está aplicado
  (`app/build.gradle.kts:12`) y `app/google-services.json` es obligatorio; está gitignored, por eso
  `git ls-files` no lo ve. La analítica se retiró (003); el plugin se conserva por el sync de Drive.
- **Buscar solo interpolaciones no encuentra toda fuga**: `org.json.JSONException` imprime un
  fragmento de la cadena analizada → `Log.e(tag, msg, e)` sobre `note.content` filtra sin interpolar.
- **Auditar con parser, no con `grep -n`**: las llamadas al registro se reparten en varias líneas;
  por líneas salen otras cifras (las reales: 143 `Log.*`, 35 interpolan, 114 con excepción).

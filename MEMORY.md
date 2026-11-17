# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **2026-11-17**: Spec **041** implementada y verificada en las suites dirigidas — desglose de caché del contexto en la tarjeta (specs/041-detalle-kv-cache, RF-293…RF-299 · RNF-179…RNF-185). El binding (`lib/`, fuente de verdad; espejo `/root/llama.cpp`) lleva `last_prompt_entry_position` (snapshot de `current_position` al entrar en `processUserPrompt`, **tras** `reset_short_term_states()`), reset a `-1` y export JNI `nativeLastPromptEntryPosition`; `OnDeviceService.currentPromptEntryPosition(): Int?` (`-1`/null = sin dato; Ollama null). Room v10→v11 (`MIGRATION_10_11`, `promptCachedTokens`, aditiva, `updatePromptCachedMetrics` reemplaza a `updatePromptMetrics`; `updatePromptMetrics` borrado en T4 → compilación rota T4–T6, cerrada en T7). Decisiones: cached = entry position al entrar el turno (no Σ previo); fila «Prompt» THIS TURN = cached+fresh (fallback fresh; sin fresh → null); ACROSS ALL TURNS = Σ fresh (RF-267 intacto) + sub-línea «reused from KV cache» solo con `totalCachedTokens != null` (Σ cached, doble conteo intencional, solo esa sub-línea); tags: strings `chat_context_reused_kv`/`chat_context_fresh_cached` × 9 locales. 18 tests ContextDetail verdes; `:app:compileDebugKotlin` OK; **build nativa OK** (`:lib:externalNativeBuildDebug`, 1m 29s). **Pendiente**: verificación en dispositivo (MIGRATION_10_11 real sobre BD v10; fresh/cached vs Web UI turno a turno; cancelación y modelo cambiado sin sub-líneas). T1–T10 cerradas; WIP del usuario intacto (AiChatScreen/AttachmentAiSheet/ModelManagerPanel solo con el arreglo mínimo autorizado en T3).
- **2026-11-16**: Spec **040** implementada (pendiente en dispositivo) — vista de detalles de modelo on-device (`specs/040-info-modelo/`, RF-281…RF-292): botón ⓘ junto a la hoja (<600 dp) y panel (≥600 dp); `ModalBottomSheet`/`420 dp lateral` + scrim; Context Size del GGUF (nunca 8192); Parameters vía `paramsLabel`; Chat Template on-demand con caché; eliminar con confirmación. QA: 962 tests, 39 fallos = 38 plataforma + `ShortcutInvariantsTest` preexistente (MainActivity.kt:209-212 viola RNF-74; ajeno). **Pendiente**: dispositivo — y decidir el invariante de `MainActivity.kt`.
- **2026-11-16**: Bug 036 corregido — `processingTimeMs` quedaba `NULL` (usuario perdía `formattedDuration` al restaurar); fix: la consulta escribe también `processingTimeMs = :promptMs`. Sin cambio de esquema.
- **2026-11-16**: Spec **039** implementada (pendiente en dispositivo) — métricas reales on-device desde el motor (`specs/039-…/`, RF-277…RF-280). **Binding vendorizado en `lib/`** (+97); el motor sigue externo en `/root/llama.cpp` (la build nativa falla fuera de ese host a propósito).
- **2026-11-16**: Spec **038** implementada (pendiente en dispositivo) — chats vacíos se descartan (`specs/038-…/`, RF-273…RF-276), análoga a 032: exit sin turnos/draft/adjuntos → borrado en cascada; purga en `onDestroy` de sesiones creadas en el proceso con `messageCount == 0`. 6 tests puros verdes.
- **2026-11-15**: Spec **037** implementada y verificada en dispositivo — detalle de ocupación del contexto al tocar el anillo (`specs/037-…/`, RF-265…RF-272): `OutlinedCard` 200×245 dp (enmienda spec 041), `ContextDetail` puro (`buildContextDetail`), THIS TURN • KV CACHE, ACROSS ALL TURNS, KV total, Avg speed ponderado; 10 tests puros verdes.
- **2026-11-15**: Spec **036** implementada y verificada en dispositivo — metadatos reales por burbuja (`specs/036-…/`, RF-260…RF-264): usuario `prompt_eval_*`, asistente `eval_*`; MIGRATION_9_10 aditiva (v9→10); `updatePromptMetrics`→(spec 041) `updatePromptCachedMetrics`.
- **2026-11-15**: Spec **035** implementada — barra de carga del motor on-device en el chat; piezas puras `ui/chat/ModelLoading.kt`; 14 tests verdes.
- **2026-11-15**: Spec **034** implementada — anillo de ocupación del contexto (`specs/034-…/`, RF-248…RF-257): exacto Ollama (`parseOllamaContextUsage`) / estimado chars÷4; ventana 8192 fijo on-device; 16 tests verdes. Detalle Ollama: gana la última de las peticiones con tool-calls.
- **Spec 033 (2026-11-14), implementada; falta verificación en dispositivo**: razonamiento y modelo en el chat IA (`specs/033-…/`, RF-215…RF-247). Esfuerzo global en `AiViewModel`; modelo en hoja <600 dp / panel pantalla completa ≥600 dp (réplica «Ollama Models»: favoritos y ocultos `ai_hidden_models` en prefs y en la copia —dieciséis ajustes—); Context = `context_length` (GGUF vía `GgufMetadataReader`+`ai_model_context_lengths` prefs / `/api/show` / «—»); `think` top-level en `/api/chat`; suite dirigida 896 tests OK (38 plataforma).
- Specs 016–031 implementadas; spec 027 verificada en dispositivo: las notas nuevas ya no se duplican.
- **Spec 032 (2026-11-11), cerrada y verificada en dispositivo**: la nota nueva vacía se descarta. `ui/DraftEmptiness.kt` (`isNoteEmpty`) decide «vacía»; una nota existente vaciada sí se guarda (`canPersistChanges`, RF-214); la fila creada en la sesión se borra definitivo al salir (`DraftWriteGate.createdRowId`); /page, Markdown, lista, «Nuevo dibujo» e histórico intactos. `./gradlew test` → **868 tests, 38 fallos (todos de plataforma)**.
- **Spec 031 (2026-10-18)**: sitio de marca estático en `public/` (raíz Firebase Hosting); **publicado en https://notes-502213.web.app**. Assets una sola vez en `public/assets/`; editar y redesplegar.
- **v4.0.0 publicada** (2026-10-18): tag + GitHub Release (Latest), APK debug/release `arm64-v8a`; `versionName = "4.0.0"` / `versionCode = 4`.
- Spec 031: **Acerca de (2026-11-10)**: tarjeta de llama.cpp con logo local (CC-BY-NC-4.0, atribución en `THIRD-PARTY-NOTICES`/`oss-licenses.json`); `BuildConfig.LLAMA_CPP_VERSION` parseado del checkout (hoy `0.9.0-dev`).
- Spec 028: los ajustes de tema viajan en la copia de seguridad; `BACKUP_SETTINGS` centraliza los valores.
- Specs 029-030: pruebas dirigidas de HTML/catálogo/PDF **26/0** y captura de bloqueo sin ViewModel/Room/Keystore.

## Decisiones que conviene recordar
- **2026-11-16**: El motor de llama.cpp **nunca se vendoriza en el repo** (decisión firme del usuario). Solo el binding Android (`lib/`, spec 039) vive en el repo; el árbol de fuentes del motor sigue externo en `/root/llama.cpp` y la build nativa fuera de ese host falla a propósito. Nada de submódulos, copias totales o parciales ni «solo lo necesario».
- Las reglas permanentes sobre nota nueva, tema, `AiModelHost`, `CodeEditor`, markdown, almacenamiento,
  rendimiento y seguridad viven en `AGENTS.md`; no duplicarlas aquí.
- Antes de purgar `libs.versions.toml`, revisar el módulo externo `:lib`
  (`lib/build.gradle.kts` del binding vendorizado): consume datastore-preferences y
  junit-platform-launcher.
- Robolectric 4.17 en Java 25 necesita `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` en tareas
  `Test` que inicializan estado Android.
- El módulo externo `:lib` usa JUnit Platform/Jupiter; API y launcher no bastan, también hace falta Jupiter
  Engine cuando se incluye el módulo.
- `mutableIntStateOf` funciona en tests JVM puros; decisiones que no necesitan `Context` van en piezas puras.

## Aprendizajes
- Configuration cache funciona; usar `--no-configuration-cache` solo ante síntomas de caché/KSP obsoleta.
- Buscar código muerto con rutas acotadas; `grep -r` global recorre `build/` y puede agotar tiempo.
- Un `^` sin `RegexOption.MULTILINE` puede hacer que un invariante pase por vacío.
- Kotlin no admite referencias adelantadas a funciones locales; `Color` requiere `.toInt()` en asserts
  hexadecimales; compilar Kotlin no valida recursos XML.
- Un `include(":x")` condicional en `settings.gradle.kts` no basta: **toda** referencia `project(":x")` en
  cualquier otro build script tiene que usar la misma condición, o la configuración falla donde el módulo
  no existe. Comprobarlo con `./gradlew projects`, no con una compilación.

## Próximos pasos
- Validar PDF real en dispositivo; verificar spec 020 provocando un fallo real.
- Verificar spec 040 en dispositivo (ⓘ en hoja y panel; chat template real; borrar con y sin éxito; 4 estados del botón de ciclo de vida).
- Verificar spec 041 en dispositivo (MIGRATION_10_11 real sobre BD v10 sin wipe; fresh/cached vs Web UI turno a turno; cancelación y cambio de modelo sin sub-líneas).

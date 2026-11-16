# Tasks 039 — Métricas reales on-device desde el motor

Una tarea cada vez: verde tras cada una.

- [x] **T1. Binding nativo (`ai_chat.cpp`): contadores + finalizador + 3 exports.** RF-277, RNF-170
  - Hecho cuando: globals `last_prompt_tokens`, `last_prompt_eval_us`, `generation_start_us`, `last_generation_us`, `generated_token_count`, `last_metrics_csv` + `std::ostringstream`; reset en `reset_short_term_states()`; `processUserPrompt` fija `prompt_start` al inicio y, al final, `last_prompt_tokens`, `last_prompt_eval_us` y `generation_start_us`; `generated_token_count++` tras cada `common_sampler_sample`; `finalize_metrics()` llamada en STOP y EOG (+defensiva en decode failure); exports `Java_..._nkvPosition` (→ `current_position`, o `-1` si no hay modelo), `Java_..._lastRequestMetrics` (→ CSV), `Java_..._clearRequestMetrics` (→ limpia CSV).

- [x] **T2. Binding Kotlin: interfaz `com.arm.aichat.InferenceEngine` + `InferenceEngineImpl` con los 3 métodos.** RF-277, RNF-170
  - Hecho cuando: interfaz gana `fun nkvPosition(): Int`, `fun lastRequestMetrics(): String`, `fun clearRequestMetrics()`; Impl los expone con los `@FastNative external` privados; compila el módulo `:lib` (CMake incluido vía Gradle).

- [x] **T3. Interfaz app `InferenceEngine : AIService` + `LlamaCppEngine`.** RF-277, RF-278
  - Hecho cuando: las 3 funciones públicas en la interfaz; `LlamaCppEngine` las delega (`engine.nkvPosition()`, `engine.lastRequestMetrics()`, `engine.clearRequestMetrics()`); `executeStreaming`/`execute` llaman `clearRequestMetrics()` antes de `sendUserPrompt`.

- [x] **T4. `parseOllamaEngineCSV` puro + tests.** RF-278, RNF-172
  - Hecho cuando: tests JVM en verde: CSV completa (números, ns→ns), «0,0,0,0» válida, CSV malformada/vacía/componentes ausentes → null, valores no numéricos → null.

- [x] **T5. `OnDeviceService`: `lastContextUsage` + `currentNkvSize` + wrapper del streaming.** RF-278, RF-279
  - Hecho cuando: `StateFlow<OllamaContextUsage?>` pública idéntica en forma (`asStateFlow`), reset al inicio, seteada al completar (via `parseOllamaEngineCSV(engine.lastRequestMetrics())`); `currentNkvSize(): Int?` devuelve nkv si >= 0, null en otro caso; flujo con `.flowOn(Dispatchers.IO)`.

- [x] **T6. `AiViewModel`: unificar fuente de métricas y anillo on-device.** RF-278, RF-279, RF-280
  - Hecho cuando: `exactUsage = if (onDevice) onDeviceService?.lastContextUsage?.value else ollamaService.lastContextUsage.value` en las dos localizaciones (turno y `refreshContextUsage`); on-device usa nkv (refreshContextUsage); `contextEstimateText` eliminado; si no hay dato → anillo oculto; turno con métricas null → burbuja sin métricas.

- [x] **T7. Burbuja y tarjeta: eliminar marcadores «~» e isOnDevice.** RF-280, RF-270†
  - Hecho cuando: `MessageBubble`/`ChatMessageList` sin `isOnDevice`; `ContextDetailCard` sin branches `exact`/~; `TokAmount` sin prefijo; el payload sigue pasando `exact=true` al anillo.

- [x] **T8. Strings muertas retiradas de los 9 locales.** RNF-171
  - Hecho cuando: `chat_msg_tokens_approx`, `chat_msg_tok_per_s_approx`, `chat_context_desc_estimate` ausentes en los 9 ficheros; AAPT2 procesa limpio.

- [x] **T9. Limpieza de código muerto (ejecución de estimaciones).** RNF-172
  - Hecho cuando: `estimateTokens` borrado; sus tests en `ContextUsageTest` borrados; ninguna referencia restante (`grep -n estimateTokens app/src/`), ArcDegree-TTY verde.

- [x] **T10. Build completa + suites dirigidas verdes + regresión 034/036/037.** RF-277…RF-280
  - Hecho cuando: `./gradlew :app:compileDebugKotlin` y `./gradlew :app:testDebugUnitTest --tests '*OllamaContextUsage*' --tests '*OllamaEngineCSVParser*' --tests '*ContextUsage*' --tests '*TokenMetrics*' --tests '*ContextDetail*' --tests '*ModelLoading*'` verde.

- [x] **T11. Docs: MEMORY.md, AGENTS.md (nota del binding modificado), specs 034/036/037 con línea «supersedido por spec 039».** RNF-173
  - Hecho cuando: AGENTS.md (section binding) documenta la patch local; cada spec afectada gana una nota de histórico; memoria actualizada; checklist dispositivo reproducido.

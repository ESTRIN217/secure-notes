# Plan 039 — Métricas reales on-device desde el motor

Spec: `specs/039-metricas-reales-ondevice/spec.md` (aprobada 2026-11-16)
Depende de: spec 034 (anillo), spec 036 (métricas por turno), spec 037 (detalle), spec 033 (Motor/AiModelHost)

## Archivos y responsabilidades

| Archivo | Responsabilidad | RFs |
|---|---|---|
| `/root/llama.cpp/examples/llama.android/lib/src/main/cpp/ai_chat.cpp` | Globals de conteo + `finalize_metrics()` + reset en `reset_short_term_states()`; `prompt_start_us` al entrar `processUserPrompt`, `last_prompt_tokens`/`last_prompt_eval_us` al terminar; `generated_token_count++` por token en `generateNextToken`; `generation_start_us` fijado al final de `processUserPrompt`; `finalize_metrics()` escrita en STOP y EOG; 3 exports JNI: `nkvPosition`, `lastRequestMetrics`, `clearRequestMetrics` | RF-277 |
| `/root/llama.cpp/examples/llama.android/lib/src/main/java/com/arm/aichat/InferenceEngine.kt` | añadir `fun nkvPosition(): Int`, `fun lastRequestMetrics(): String`, `fun clearRequestMetrics()` a la interfaz; `InferenceEngineImpl` las implementa | RF-277 |
| `/root/llama.cpp/examples/llama.android/lib/src/main/cpp` no toca nada más | | |
| `app/src/main/java/com/example/data/ai/OnDeviceService.kt` | añadir 3 funciones públicas a `interface InferenceEngine : AIService` (`nkvPosition`, `lastRequestMetrics`, `clearRequestMetrics`) + implementar `lastContextUsage: StateFlow<OllamaContextUsage?>` en `OnDeviceService` y `currentNkvSize(): Int?`; flujo streaming envuelto en `flow { }.flowOn(Dispatchers.IO)` | RF-277/278/279 |
| `app/src/main/java/com/example/data/ai/LlamaCppEngine.kt` | implementaciones que delegan al engine nativo; `executeStreaming`/`execute` llaman a `engine.clearRequestMetrics()` antes de pedir y leen al final (si está) | RF-277/278 |
| `app/src/main/java/com/example/data/ai/OnDeviceService.kt` | `lastContextUsage: StateFlow<OllamaContextUsage?>` público, misma semántica que OllamaService; `currentNkvSize(): Int?` público; el flujo de streaming envuelve en `flow { }.flowOn(Dispatchers.IO)` para fijar el estado exactamente al completar | RF-278/279 |
| `app/src/main/java/com/example/data/ai/OllamaContextUsage.kt` | + `parseOllamaEngineCSV(csv: String): OllamaContextUsage?` (puro, tests JVM) | RF-278 |
| `app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt` | métricas del turno de AMBOS backends: Ollama o CSV del motor; `exactUsage` unificado; `refreshContextUsage` on-device usa `onDeviceService?.currentNkvSize()` (exact nkv), Ollama sin cambio; eliminar `contextEstimateText` | RF-278/279/280 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | eliminar parámetro `isOnDevice` de `MessageBubble`/`ChatMessageList` (no quedan marcadores); wiring adaptado | RF-280 |
| `app/src/main/java/com/example/ui/chat/ContextDetailCard.kt` | eliminar marcadores «~» y branches `detail.exact`; siempre cifras exactas | RF-280 |
| `app/src/main/java/com/example/ui/chat/ContextDetail.kt` | (sin reescritura) sigue recibiendo `exact=true`; se mantiene por simetría con el anillo | RF-280 |
| `app/src/main/res/values*/strings.xml` (9 locales) | borrar `chat_msg_tokens_approx`, `chat_msg_tok_per_s_approx`, `chat_context_desc_estimate` (27 claves muertas) | RNF-171 |
| `app/src/test/java/com/example/data/ai/OllamaEngineCSVParserTest.kt` (**nuevo**) + borrar/ajustar `OllamaContextUsageTest` (queda) | `parseOllamaEngineCSV` tests JVM: CSV válida, malformada, sin tokens inconclusa, NS→ms | RF-278 |
| `app/src/test/java/com/example/ui/chat/ContextUsageTest.kt` | eliminar tests de `estimateTokens` (la función muere); mantener fracción/cerca-límite/buildUsage | RNF-172 |
| `app/src/test/java/com/example/ui/chat/TokenMetricsTest.kt`, `ModelLoadingTest.kt`, `ContextDetailTest.kt` | sin cambios (regresión íntegra) | |
| `AGENTS.md`, `MEMORY.md`, specs 034/036/037 | nota: el binding tiene modificaciones locales (contadores + 3 exports); specs 034/036/037 ganan párrafo «supersedido por spec 039» en sus RFs afectados | RNF-173 |

## Algoritmo

```
ai_chat.cpp (del binding):
  reset_short_term_states() { … + last_prompt_tokens=0; last_prompt_eval_us=0; generation_start_us=0; last_generation_us=0; generated_token_count=0; last_metrics_csv=""; }

  processUserPrompt(...) {
      prompt_start = ggml_time_us();
      reset_short_term_states();
      … (tokenize, decode en batches) …
      last_prompt_tokens = user_tokens.size();
      last_prompt_eval_us = ggml_time_us() - prompt_start;
      generation_start_us = ggml_time_us();
  }

  generateNextToken() {
      … STOP (current >= stop) → finalize_metrics(); return nullptr;
      … decode token; generated_token_count++;
      … EOG → finalize_metrics(); return nullptr;
  }

  finalize_metrics() {
      last_generation_us = generation_start_us ? ggml_time_us() - generation_start_us : 0;
      last_metrics_csv = "{prompt},{promptUs*1000},{generated},{generationUs*1000}";
  }

  nkvPosition() ← g_context ? current_position : -1
  lastRequestMetrics() ← last_metrics_csv
  clearRequestMetrics() ← last_metrics_csv.clear()
```

En la app, al completar el streaming on-device:

```
val exactUsage = if (onDevice) onDeviceService?.lastContextUsage?.value else ollamaService.lastContextUsage.value
```

El resto del `execute()` cambia solo en la fuente de `exactUsage`/`nkvSize` — el flujo de turno, la persistencia y el restore ya sirven igual (columnas anulables, ms conversiones ns/1e6).

## Decisiones con su alternativa descartada

- **CSV + `reset_short_term_states()` como finalizador** en vez de exponer los contadores crudos como propiedades JNI: una cadena compacta evita 4 exports frágiles y garantiza atomicidad.
- **reset BEFORE escribir contadores en `processUserPrompt`** en vez de en `reset_long`/unload: cada petición empieza limpia; el sistema-prompt no se cuenta como `last_request` por error.
- **`clearRequestMetrics()` antes de cada petición** (sendPrompt): un turno cancelado no hereda el CSV del turno anterior, por construcción.
- **Granularidad EOG + STOP**: ambos casos son «petición completada» — no existen más salidas en este binding (decode failure se finaliza también).
- **Alinear OnDeviceService con OllamaService** (`lastContextUsage: StateFlow`) en vez de una ruta nueva para el anillo: una sola pieza lógica para ambos backends, un único sitio de parsing (`parseOllamaEngineCSV` junto a `parseOllamaContextUsage`).
- **El denominador del anillo sigue en 8192 duro** — regla permanente de AGENTS.md.
- **Sin estimación de respaldo** (directriz del usuario): cualquier métrica faltante desaparece de la UI.
- **Logging fuera** de contadores (`LOGd` solo en paths existentes): cero ruido nuevo.

## Estrategia de tests

1. `OllamaEngineCSVParserTest`: CSV completo, sin tokens («0,0,0,0»), malformado, vacío, con bools ausentes → null; conversión ms.
2. Suite dirigida: `./gradlew :app:testDebugUnitTest --tests '*OllamaContextUsage*' --tests '*OllamaEngineCSVParser*' --tests '*ContextUsage*' --tests '*TokenMetrics*' --tests '*ContextDetail*' --tests '*ModelLoading*'`.
3. `./gradlew :app:compileDebugKotlin` — incluye el CMake de `:lib` vía Gradle.

## Riesgos

- El checkout externo `/root/llama.cpp` está sucio con la patch; si alguien lo resetea, la build rompe — documentado en AGENTS.md (section binding).
- El binding deja de coincidir con upstream: sincronizaciones manuales futuras.
- `nkvPosition()` devuelve la posición real pero con contexto shift (shift_context) se reduce — eso ES el KV real, el anillo lo mostrará encogiendo (correcto).

## Cobertura de RFs

RF-277 (binding) · RF-278 (OnDeviceService + AiViewModel) · RF-279 (anillo) · RF-280 (ausencias) · RNF-170-173 (tests, docs)

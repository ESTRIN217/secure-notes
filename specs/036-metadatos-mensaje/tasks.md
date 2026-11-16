# Tasks 036 — Metadatos de mensaje

Una tarea cada vez: rojo → verde → siguiente.

- [x] **T1. `tokensPerSecond` puro + tests (rojos).** RF-261, RNF-156
  - Hecho cuando: `TokenMetricsTest` pasa: caso normal (120 tokens, 30e9 ns → 4.0f), duración 0/negativa → null, count 0 → 0f, count negativo → null; implementación mínima en `ui/chat/TokenMetrics.kt`.

- [x] **T2. Ampliar `OllamaContextUsage` con duraciones.** RF-261
  - Hecho cuando: `OllamaContextUsage(promptTokens, evalTokens, promptEvalDurationNs: Long?, evalDurationNs: Long?)` con `sum` como antes; `parseOllamaContextUsage` los lee como *opcionales* (`null` si ausentes o no numéricos, sin fallar); tests de spec 034 siguen verdes y nuevos cubren presentes/ausentes/negativos (duración negativa → null).

- [x] **T3. `ConversationEntity` + `NoteDatabase` 9→10 + `ConversationDao`.** RF-263, RF-264, RNF-155
  - Hecho cuando: entidad con 4 campos anulables nuevos; `MIGRATION_9_10` con 4 `ALTER TABLE conversations ADD COLUMN`; `.addMigrations` actualizado; versión 10; nueva query de update para el turno de usuario; la app compila (`processDebugResources` incluido).

- [x] **T4. `ConversationTurn` con los 4 campos + map en restore.** RF-263
  - Hecho cuando: `ConversationTurn` los lleva con defecto null; `loadConversation` los mapea desde la entidad (incluyendo `processingTimeMs` como antes).

- [x] **T5. `AiViewModel.execute()`: captura de rowId + métricas en ambos turnos (éxito).** RF-261, RF-262, RF-264
  - Hecho cuando: insert del turno de usuario en `viewModelScope.async` → `await` en la rama final de éxito; update de `promptEvalCount/Duration` en ese rowId; asistente con `evalCount/Duration` en su `ConversationTurn`/`ConversationEntity`; on-device: estimación `estimateTokens` con marca de estimado (→ «~» en UI) y `processingTimeMs` en ambos turnos; `hasError`/cancel = sin update.

- [x] **T6. `MessageBubble`: eliminar «30 tokens» y «9,20 tok/s»; pintar métricas por rol.** RF-260, RNF-157, RNF-159
  - Hecho cuando: segmentos `hora · tokens · duración · tok/s` respetando el orden actual (estado, hora, métricas, modelo); usuario→prompt, asistente→generación; ausentes se omiten sin huecos; «~» on-device; nada se desborda en 320dp; sin strings nuevas fuera de las de T7.

- [x] **T7. Strings en los 10 ficheros (values + 9 variantes).** RNF-157
  - Hecho cuando: `chat_msg_tokens`, `chat_msg_tokens_approx`, `chat_msg_tokens_per_second` con `%1$d`/`%1$s`; verificación de AAPT2 (no hay apóstrofos).

- [x] **T8. Verificación dirigida + regresión spec 034.** RF-261…RF-264
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests '*TokenMetrics*' --tests '*OllamaContextUsage*' --tests '*ContextUsage*'` verde; `./gradlew :app:compileDebugKotlin` verde; revisión manual del `git diff` (nada de código muerto ni cambios fuera de alcance).

- [x] **T9. `MEMORY.md` + checklist de dispositivo.** RF-263, RNF-155
  - Hecho cuando: memoria actualizada; el usuario verifica en dispositivo: (1) migrar desde 9 con historial viejo sin perder conversaciones, (2) nuevo turno en Ollama → burbujas con split prompt/generación, (3) on-device → «~», (4) restaurar sesión → métricas persistidas visibles.

# Tasks 037 — Detalle de la ventana de contexto

- [x] **T1. `formatTokensCompact` + `formatPercentUsed`: impl + tests (rojos primero).** RF-266, RNF-160
  - Hecho cuando: `ContextDetailTest` pasa con 0, 42, 999, 1000→`1k`, 1234→`1.23k`, 2048→`2.05k`, 1024→`1.02k`, 1500→`1.5k`, 10_000→`10k`, 1_500_000→`1.5m` y `formatPercentUsed(0.023f) → 2`.

- [x] **T2. `accumulateTurnTokens` + `TurnMetrics`/`TurnTotals` + tests.** RF-267, RF-269, RNF-160
  - Hecho cuando: suma de prompt/eval ignorando nulos; `avgTokensPerSecond == null` sin duración acumulada; vacío → 0/0/null; errores no alteran el acumulado.

- [x] **T3. `buildContextDetail` + tests.** RF-265, RF-267, RF-268, RF-270, RF-272, RNF-160
  - Hecho cuando: `usage == null` → null; detalle completo con par `THIS TURN` (last user + last assistant); historial con errores intermedios → mantiene último par completo; vacío → stats en 0 con avg null.

- [x] **T4. `ContextDetailCard.kt` (Composable del diseño).** RF-266…RF-270, RNF-161…163
  - Hecho cuando: `OutlinedCard` 200×190 dp, `RoundedCornerShape(28.dp)`, border `outlineVariant`, `CardDefaults.outlinedCardColors` con `surfaceColorAtElevation(1.dp)`; título + `used/window` compactos, `LinearWavyProgressIndicator()` determinada con la fracción del anillo, fila `% usado`/`restante`, divisor, «Token usage details», secciones ACROSS ALL TURNS / THIS TURN • KV CACHE / KV cache total / Avg speed con «~» cuando `exact == false`; scroll interno; sin lógica de decisión de estado.

- [x] **T5. `FloatingChatInputField`: sustituir `contextUsage` por `contextDetail` + tap + hoja/anclado.** RF-265, RF-266, RF-271, RF-272
  - Hecho cuando: el parámetro nuevo alimenta anillo (fracción/color/ocultación idénticos) y el tap abre `ModalBottomSheet` en ancho `<600 dp` (`ModalBottomSheet` existente) o `DropdownMenu` anclado en `≥600 dp`; estado `remember` `showContextDetail`; tap fuera/atrás cierra; sin cambios en el resto del campo ni en la posición de los demás botones.

- [x] **T6. `AiChatScreen`: construir/memoizar el detalle.** RF-265, RF-272
  - Hecho cuando: `remember(contextUsage, conversationHistory)` calcula el detalle y lo pasa al campo; ya no queda `ContextUsage` en las firmas del campo (una fuente); `contextUsage` se sigue recolectando para el detalle.

- [x] **T7. Strings en los 9 locales.** RNF-162
  - Hecho cuando: ≈13 claves nuevas (`chat_context_details_title`, `chat_context_pct_used`, `chat_context_remaining`, `chat_context_usage_details`, `chat_context_across_all_turns`, `chat_context_prompt_evaluated`, `chat_context_tokens_generated`, `chat_context_this_turn_kv`, `chat_context_prompt`, `chat_context_generated`, `chat_context_kv_total`, `chat_context_avg_speed`, `chat_context_tok{,_approx}`) presentes y con «AAPT2 limpio».

- [x] **T8. Verificación dirigida + regresión 034/035/036.** RF-265…RF-272
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests '*ContextDetail*' --tests '*TokenMetrics*' --tests '*OllamaContextUsage*' --tests '*ContextUsage*' --tests '*ModelLoading*'` verde; `./gradlew :app:compileDebugKotlin` verde.

- [x] **T9. `MEMORY.md` + `tasks.md` + cierre.** RNF-164
  - Hecho cuando: memoria actualizada; checklist de dispositivo: tap con Ollama de varios turnos → acumulados correctos, KV total/restante coherentes, `~` on-device, cierre con atrás/tap fuera, `<600 dp` abre hoja y `≥600 dp` abre anclado; históricos previos a spec 036 muestran `0 tok` en ACROSS ALL TURNS sin romper nada.

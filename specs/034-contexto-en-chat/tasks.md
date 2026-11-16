# Tasks 034 — Contexto en el chat

Orden de dependencia. Una tarea cada vez: rojo → verde → siguiente.

- [x] **T1. Constante y modelo puro `ContextUsage`.** RF-248, RF-249, RNF-148
- [x] **T2. `estimateTokens` + tests (rojos).** RF-251, RNF-148
- [x] **T3. `contextUsageFraction` y `isContextNearLimit` + tests.** RF-249, RF-254, RNF-148
- [x] **T4. `buildContextUsage(window=null → null)` + tests.** RF-253, RNF-148
- [x] **T5. Parser puro `parseOllamaContextUsage` + tests.** RF-250, RNF-148

- [x] **T6. `OllamaService` publica `lastContextUsage`.** RF-250, RF-252
  - Hecho cuando: en `executeStreaming`, al trozo final `done` se setea el parser; al inicio de petición se pone en null; `execute()` (no stream) hace lo mismo con su JSON completo; el servicio compila.

- [x] **T7. `AiViewModel`: `contextUsage` + `refreshContextUsage()` + cableado en los puntos de cambio.** RF-248, RF-252, RF-253, RF-256, RF-257
  - Hecho cuando: campo nuevo `contextUsage: StateFlow<ContextUsage?>`; refresco suspend en: fin de streaming (éxito y error), `loadSession`, restaurar histórico, `setBackend`, cambio de modelo, conversación nueva/limpia; exacto solo con backend Ollama; verde `compileDebugKotlin`.

- [x] **T8. `FloatingChatInputField`: anillo determinado con umbral de error + contentDescription localizado.** RF-248, RF-254, RF-255, RNF-149, RNF-150, RNF-151
  - Hecho cuando: el `CircularWavyProgressIndicator()` sin estado se sustituye; visible solo si `contextUsage != null` (RF-253, RF-257); color `primary`/`error` con 85 %; `contentDescription` con porcentaje y marca «aprox.» si !exact; altura del campo y anclaje a la derecha intactos; no pinta texto.

- [x] **T9. `AiChatScreen` recoge `contextUsage` y lo pasa al campo.** RF-248
  - Hecho cuando: parámetro añadido; sin cambios de comportamiento del resto del campo.

- [x] **T10. Strings en los 10 ficheros `strings.xml` (values + 9 variantes).** RF-255, RNF-149
  - Hecho cuando: clave única `chat_context_desc` y `chat_context_desc_estimate` (con `%1$d` para el porcentaje) presentes en todos los locales; sin literales nuevos en el código.

- [x] **T11. `MEMORY.md` + documentación.** RF-253, RNF-152
  - Hecho cuando: anotado el 8192 upstream y la decisión de no persistir; `AGENTS.md` recibe la nota solo si procede (consultar al usuario); prueba manual en dispositivo (conversación larga hasta el aviso al 85 % en ambos backends).

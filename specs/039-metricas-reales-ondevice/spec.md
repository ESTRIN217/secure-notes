# Spec 039 — Métricas reales on-device desde el motor

Fecha: 2026-11-16
Estado: **aprobada — implementación en curso**
RF-277…RF-280 · RNF-170…RNF-173 · HU-1 · HU-2

## Contexto y objetivo

La spec 036 muestra en cada burbuja métricas del turno con Ollama **exactas**
(`prompt_eval_count`/`eval_count`, desde el trozo `done`) y en on-device una
estimación chars/4 con prefijo «~», porque el binding de llama.cpp no exponía contadores.
La directriz del usuario es explícita: **nada de datos estimados ni suposiciones**.
Revisado `ai_chat.cpp` (`examples/llama.android/lib/src/main/cpp`): el motor **sí**
conoce todo — `user_tokens.size()` (prompt real), cada token generado, `current_position`
(ocupación real del KV cache) y `ggml_time_us()` para tiempos. Lo único que falta es
exponer esos contadores hacia Kotlin.

Objetivo: que on-device mida y muestre exactamente lo mismo que Ollama — prompt tokens,
duración de prefill, tokens generados, duración de generación — y que el anillo de contexto
use la ocupación real del KV cache, sin ninguna estimación en ninguna superficie.

## Usuarios

Personas que usan Secure Notes con modelos on-device y quieren ver cifras reales del
motor en ambos backends, igual que en la referencia de llama.cpp.

## Historias de usuario

- **HU-1.** Como usuario, quiero que en mi chat on-device las burbujas muestren los
  tokens y tok/s reales de la ejecución, igual que en Ollama, sin datos inventados.
- **HU-2.** Como usuario, quiero que el anillo de contexto refleje el KV cache real de
  la conversación (system prompt incluido) aunque aún no haya enviado nada.
- **HU-3.** Como usuario, quiero que cualquier métrica que el motor no sea capaz de
  reportar (p.ej. cancelaciones) simplemente no aparezca, en vez de una cifra inventada.

## Definiciones

- **Métricas del motor**: las que reporta el propio binding: tokens de prompt
  (`user_tokens.size()` tras la codificación chat template), duración del prefill,
  tokens generados (contados en `generateNextToken`), duración de generación
  (de fin de prefill a la devolución `nullptr` por EOG/tope) — todo con tiempo de alta
  resolución (`ggml_time_us`).
- **KV position**: `current_position` del motor — tokens que ocupa el KV cache en este
  momento exactos.
- **CSV de métricas**: `"promptTokens,promptEvalNs,generatedTokens,generationNs"`.
  Vacía si no hubo petición completada todavía (o se limpió antes del envío).

## Requisitos funcionales

- **RF-277**: EL SISTEMA añade al binding nativo (vendorizado en `lib/`)
  los exports `nkvPosition()` (Int; `-1` si no hay modelo cargado), `lastRequestMetrics()`
  (String CSV; `""` si no hay métricas finalizadas) y `clearRequestMetrics()`, con reset por
  petición en `reset_short_term_states()`. La finalización escribe CSV en las dos salidas
  naturales: EOG (`llama_vocab_is_eog(...)`) y STOP por tope.
- **RF-278**: CUANDO el backend es on-device y una respuesta termina con normalidad, EL SISTEMA
  persiste por turno exactamente igual que Ollama: turno de usuario → `promptEvalCount`
  y `promptEvalDurationMs`; turno de asistente → `evalCount` y `evalDurationMs`; conversiones
  en milisegundos (ns / 1e6). Datos "~"-nunca: exactos.
- **RF-279**: CUANDO el backend es on-device, el anillo de ocupación de contexto (`contextUsage`)
  usa `nkvSize / 8192` (tamaño constante del motor) en los refrescos fijados en spec 034;
  si `nkvPosition() == -1` (motor sin cargar) el anillo no se pinta. El denominador sigue en
  `LLAMA_CPP_CONTEXT_TOKENS = 8192` (regla del proyecto en AGENTS.md).
- **RF-280**: SI falta algún dato del motor en un turno o en el anillo, ENTONCES el segmento o la
  visualización no se pinta, sin ninguna estimación de respaldo.

## Requisitos no funcionales

- **RNF-170**: La modificación del binding es **aditiva y selectiva**: solo añade globals de
  conteo, un finalizador, el reset en `reset_short_term_states()` y tres exports JNI. Ninguna
  firma existente cambia.
- **RNF-171**: Los marcadores «~» y las strings de aproximación (`chat_msg_tokens_approx`,
  `chat_msg_tok_per_s_approx`, `chat_context_desc_estimate`) desaparecen de los 9 locales,
  sin código ni strings muertos.
- **RNF-172**: `estimateTokens` y `contextEstimateText` se **eliminan** del código: no queda
  ninguna ruta de estimación en el chat (anillo, burbuja, acumulados, detalle).
- **RNF-173**: El cambio de RFs se aplica también a las specs 034 (RF-251), 036 (RF-262) y 037
  (RF-270) según el flujo SDD: esta spec 039 las supersede.

## Casos límite

- Petición con STOP por tope: CSV finalizado, métricas del turno se muestran igualmente.
- Respuesta cancelada o interrumpida: sin CSV de finalización para ese turno; si el usuario
  no obtuvo respuesta, métricas ausentes (burbuja sin datos, anillo con el nkv actual —
  que ES real aunque la petición se cancelara a mitad).
- Modelo sin cargar (NOT_LOADED/ERROR/LOADING): `nkvPosition() == -1` → sin anillo (RF-253);
  también sin métricas — como en Ollama sin conexión.
- `reset_short_term_states()` en cada `processUserPrompt`: contadores por petición; el CSV
  nunca arrastra valores de una petición anterior por accidente porque se limpia al inicio.
- Turno de error (`addErrorTurn`): sin métricas, como siempre (spec 036).
- `evalDurationMs = 0` (tokens inmediatemente detenidos por EOG, p.ej. modelo sin entrenar):
  tok/s no se pinta (división), pero el resto sí.

## Fuera de alcance

- Empujar cambios upstream (repo externo `llama.cpp`); la patch vive en el checkout local.
- Cambiar el formato de exportación del motor (nkv, métricas CSV).
- Medir el system prompt por fase (no se pinta por fase; es solo parte del KV).
- El anillo durante LOADING: sin modelo, se oculta (no hay posición real todavía).

## Criterios de finalización

`./gradlew :app:compileDebugKotlin` y `./gradlew :lib:...` reconstruye el binding limpio;
tests JVM del parser CSV y de la familia spec 034/036/037 (ajustada) en verde; verificación en
dispositivo con backend on-device: enviar mensaje, comprobar tokens/duración/tok-s reales en
ambas burbujas, anillo con system prompt aunque no haya primer turno; model change → anillo y
burbujas siguen sin estimaciones; `MEMORY.md` y `AGENTS.md` actualizados con la modificación
del binding anotada; las dos últimas micro-expectativas del usuario quedan cumplidas.

## Historial de cambios

- 2026-11-16 — Creación a petición del usuario: «Datos reales en el motor, nada estimado;
  si hay que tocar la lib, se toca».

# Plan 036 — Metadatos de mensaje

Spec: `specs/036-metadatos-mensaje/spec.md` (aprobada 2026-11-15)
Depende de: spec 017/018 (fila de metadatos de la burbuja), spec 034 (parseo del `done`), spec 035

## Archivos y responsabilidades

| Archivo | Responsabilidad | RFs |
|---|---|---|
| `app/src/main/java/com/example/data/ai/OllamaContextUsage.kt` | Ampliar `OllamaContextUsage` con `promptEvalDurationNs`/`evalDurationNs` (opcionales, `Long?`); el parser sigue exigiendo los dos conteos | RF-261 |
| `app/src/main/java/com/example/ui/chat/TokenMetrics.kt` (**nuevo**, puro, sin `android`) | `tokensPerSecond(count, durationNs): Float?` — derivación tok/s con clamp (§8); solo lo puro | RF-261, RF-262, RNF-156 |
| `app/src/test/java/com/example/ui/chat/TokenMetricsTest.kt` (**nuevo**) | Tests JVM puros del tok/s (normal, duración 0/negativa → null, count 0 → 0f) | RNF-156 |
| `app/src/main/java/com/example/data/local/ConversationEntity.kt` | +4 campos anulables `promptEvalCount`, `promptEvalDurationMs` (ms), `evalCount`, `evalDurationMs` | RF-263 |
| `app/src/main/java/com/example/data/local/NoteDatabase.kt` | `MIGRATION_9_10` con 4× `ALTER TABLE conversations ADD COLUMN …` (excepción: el viejo `fallbackToDestructiveMigration(true)` se mantiene, como el resto de la app; la migración explícita se añade igual) y bump a versión 10 | RF-263, RNF-155 |
| `app/src/main/java/com/example/data/local/ConversationDao.kt` | Nueva query `@Query("UPDATE conversations SET promptEvalCount=:pc, promptEvalDurationMs=:pd WHERE id=:turnId")` para el turno de usuario | RF-264 |
| `app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt` | `ConversationTurn` +4 campos; `execute()`: insert de usuario diferido (`viewModelScope.async { withContext(IO) { conversationDao.insert(...) } }`), id capturado al completar (`await`), update de métricas prompt; asistente crea su `ConversationTurn` y `ConversationEntity` ya con `evalCount`/`evalDurationMs` (del `lastContextUsage` tras el último `done`, spec 034); on-device: estimación chars÷4 (`estimateTokens` de spec 034) en usuario (mensaje+adjuntos) y asistente (`finalResult`), marca `exact=false` → UI renderiza con «~»; `processingTimeMs` se rellena en **ambos** turnos en on-device («duración del turno» marcada como estimación en la UI); restore carga las 4 columnas | RF-260…RF-264 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | `MessageBubble`: eliminar `"• 30 tokens"` y `"• 9,20 tok/s"` y sustituir por segmentos condicionales (tokens, duración, tok/s) según `turn`; usuario→prompt, asistente→generación; on-device → prefijo «~»; hora/estado/modelo se mantienen tal cual | RF-260, RNF-159 |
| `app/src/main/res/values*/strings.xml` (10) | `chat_msg_tokens` (`%1$d tokens`), `chat_msg_tokens_approx` (`~%1$d tokens`), `chat_msg_tokens_per_second` (`%1$s tok/s`) — 9 locales | RNF-157 |

## Algoritmo

```
al completar la respuesta (éxito, rama de streaming):
  exact = ollama?.lastContextUsage          // spec 034: campos done
  elapsed = now - startTime (ya existe)
  onDevice = backend == ON_DEVICE
  userMessageText = userMessage + adjuntos.content joined
  userEst  = estimateTokens(userMessageText)      // spec 034, reutilizado
  asstEst  = estimateTokens(finalResult)
  // Asistente:
  turn.evalCount   = if (onDevice) asstEst else exact?.evalTokens
  turn.evalMs      = if (onDevice) elapsed else exact?.evalDurationNs / 1_000_000
  // Usuario (update por rowId):
  update userTurn(promptEvalCount = if (onDevice) userEst else exact?.promptTokens,
                  promptEvalDurationMs = if (onDevice) elapsed else exact?.promptEvalDurationNs / 1_000_000)
  turno de usuario en memoria:
  turns[session] { userTurn.copy(promptEvalCount=…, promptEvalDurationMs=…, processingTimeMs=elapsed if onDevice) }
```

## Interfaz (nueva, pura)

```kotlin
// ui/chat/TokenMetrics.kt
fun tokensPerSecond(count: Int, durationNs: Long): Float?
  // null si durationNs <= 0 o count < 0; ceil(count / (durationNs / 1e9f) * 10f) / 10f
```

## Decisiones con su alternativa descartada

- **4 columnas planas en `conversations`** en vez de un blob JSON de métricas: la migración ALTER
  es el patrón que la app ya usa (000-033); columnas nulables sin defecto → legacy intocado
  (RNF-155); blob se habría descartado por opaco para backups/exports y por no permitir `UPDATE` dirigido barato.
- **Separación prompt/generación replicando llama.cpp** en vez de un total por turno: es lo que
  hace el feedback útil (el prompt largo es la causa típica de la latencia percibida en local).
- **Estimado con «~» on-device** en vez de ocultar las métricas: el usuario pidió datos en ambos y
  prefirió estimación marcada a ausencia; la marca cumple RF-262/HU-1.
- **Duración del turno rellenada en ambos turnos on-device** (en vez de dejarla solo en asistente):
  sin `processingTimeMs` la fila del usuario quedaría vacía también de tiempo — incoherente con «en ambos».
- **Update diferido del turno de usuario por rowId** en vez de buscar por contenido: colisión de
  mensajes idénticos en la misma sesión es realista; rowId no tiene ambigüedad, y el `insert` ya se
  difiere async → captura directa sin cambio visible.
- **Sin instrumentar `TOOL_CALLS` aparte**: prevalece el último `done` (la respuesta final es lo que
  consumió el turno completo); el primero se descarta — documentado en spec (RF-261).
- **`fallbackToDestructiveMigration(true)` se mantiene** (política de la app): la migración aditiva
  9→10 es el camino real; los que saltan versiones enteras siguen el comportamiento existente de la casa.

## Estrategia de tests

1. `tokensPerSecond`: valor normal (120 tokens, 30e9 ns → 4.0f), duración 0 → null, count 0 → 0.0,
   count negativo → null.
2. `parseOllamaContextUsage` ampliado: campos de duración presentes/ausentes (opcionales),
   negativos rechazados solo para conteos.
3. `./gradlew :app:testDebugUnitTest --tests '*TokenMetrics*' --tests '*OllamaContextUsage*'` verde.
4. `./gradlew :app:compileDebugKotlin` — incluye `processDebugResources` para AAPT.
5. Migración 9→10: revisión por pares + verificación en dispositivo (histórico anterior abierto,
   conversación nueva con métricas en ambas burbujas, restaurar y confirmar persistencia). Los tests
   JVM no cubren migraciones Room.

## Cobertura de RFs

RF-260 (burbuja) · RF-261 (parser + servicio) · RF-262 (estimación) · RF-263 (migración + entidad + restore) · RF-264 (update en dos fases) · RNF-155 (ALTER aditivo) · RNF-156 (tests) · RNF-157 (strings 9 locales) · RNF-158 (sin deps/nativo) · RNF-159 (fila con truncado)

## Historial de correcciones

- 2026-11-16 — `updatePromptMetrics` ahora escribe también `processingTimeMs = :promptMs` en el turno de usuario (antes solo `promptEvalCount`/`promptEvalDurationMs`; `processingTimeMs` quedaba `NULL` y la burbuja del usuario perdía `formattedDuration` al restaurar la sesión). Sin cambio de esquema; filas antiguas conservan `NULL` (sin duración recuperable).

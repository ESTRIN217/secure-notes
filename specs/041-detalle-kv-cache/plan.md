# Plan 041 — Desglose de ocupación de caché del contexto

Spec: `specs/041-detalle-kv-cache/spec.md` (aprobada 2026-11-17)
Depende de: spec 034 (anillo/`ContextUsage`), spec 036 (métricas por turno y `ConversationEntity`),
spec 037 (tarjeta de detalle, RF-265…RF-272), spec 039 (métricas reales on-device y regla
«dato ausente ⇒ elemento omitido»), spec 027/033 (AiModelHost, flujo deferred `userTurnDeferred`)

## Archivos y responsabilidades

| Archivo | Qué gana | RFs |
|---|---|---|
| `lib/src/main/cpp/ai_chat.cpp` | Nuevo global `last_prompt_entry_position` (init −1); reset del global en `reset_short_term_states()` siguiendo el patrón de spec 039; **tras** ese reset, snapshot de `current_position` en `processUserPrompt` (la `reset_short_term_states()` pone el global a −1: el snapshot tiene que ir después, o se borra); export JNI `Java_com_arm_aichat_internal_InferenceEngineImpl_nativeLastPromptEntryPosition` → `-1` si `g_model == nullptr || g_context == nullptr`, si no `(jint) last_prompt_entry_position` | RF-293 |
| `/root/llama.cpp` (espejo local del binding para el host de build; **nunca commiteado ni incorporado al repo** — decisión firme 2026-11-16) | **Espejo manual del mismo patch** (global + reset + export JNI) aplicado al árbol externo para que la build de este host compile; paso del plan, no archivo del repo | RF-293 · RNF-180 |
| `lib/src/main/java/com/arm/aichat/InferenceEngine.kt` | Interfaz gana `fun lastPromptEntryPosition(): Int` con KDoc de spec 041 (`-1` sin modelo cargado o sin posición) | RF-293 |
| `lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt` | `private external fun nativeLastPromptEntryPosition(): Int` (con `@FastNative` como el resto) + `override fun lastPromptEntryPosition(): Int = nativeLastPromptEntryPosition()` | RF-293 |
| `app/src/main/java/com/example/data/ai/LlamaCppEngine.kt` | `override fun lastPromptEntryPosition(): Int = if (_loadedInfo == null) -1 else engine.lastPromptEntryPosition()` (análogo exacto de `nkvPosition()`); el modelo no cargado ⇒ `-1` | RF-294 |
| `app/src/main/java/com/example/data/ai/OnDeviceService.kt` | `interface InferenceEngine : AIService` gana `fun lastPromptEntryPosition(): Int` (KDoc spec 041); servicio gana `fun currentPromptEntryPosition(): Int? = engine.lastPromptEntryPosition().takeIf { it >= 0 }` (análogo de `currentNkvSize()`): `null` ⇔ ausencia (sin modelo, reload, cancelación sin dato útil) | RF-294 |
| `app/src/main/java/com/example/data/local/ConversationEntity.kt` | Campo `val promptCachedTokens: Int? = null`; KDoc actualizado (spec 036 añadió `*Eval*` en 9→10; spec 041 añade `promptCachedTokens` en 10→11) | RF-295 · RNF-183 |
| `app/src/main/java/com/example/data/local/NoteDatabase.kt` | `MIGRATION_10_11` (`ALTER TABLE conversations ADD COLUMN promptCachedTokens INTEGER`), `@Database(version = 11, …)`, `.addMigrations(..., MIGRATION_9_10, MIGRATION_10_11)`; aditiva, registrada, nunca destructiva (`fallbackToDestructiveMigration(true)` global queda como red, no como mecanismo) | RF-295 · RNF-183 |
| `app/src/main/java/com/example/data/local/ConversationDao.kt` | **Reemplaza** `updatePromptMetrics(turnId, promptCount, promptMs)` por `updatePromptCachedMetrics(turnId: Long, promptCount: Int, promptMs: Long, cachedTokens: Int?)` (mismo `UPDATE … SET promptEvalCount, promptEvalDurationMs, processingTimeMs` + `promptCachedTokens = :cachedTokens`); un solo call site — no quedan muertos ni dos escrituras | RF-295 |
| `app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt` | `ConversationTurn` gana `val promptCachedTokens: Int? = null`; restore (`getConversations` → `ConversationTurn`) propaga `entity.promptCachedTokens`; bloque de métricas del turno: `val cachedTokens = if (onDevice) onDeviceService?.currentPromptEntryPosition() else null`; copia en memoria (`userIdx`) incluye `promptCachedTokens = cachedTokens`; en el guard existente `if (userTokens != null && promptMs != null)` se sustituye la llamada por `conversationDao.updatePromptCachedMetrics(userTurnId, userTokens, promptMs, cachedTokens)` (Ollama escribe NULL; cancelación ⇒ guard falso ⇒ nada persiste ⇒ sub-líneas ocultas, como spec) | RF-294 · RF-295 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | `conversationTokenMetrics` gana `cachedTokens = turn.promptCachedTokens` (la foto `ContextDetail` va igual, memoizada) | RF-294 · RF-296 |
| `app/src/main/java/com/example/ui/chat/ContextDetail.kt` (puro) | `TurnMetrics` gana `cachedTokens: Int? = null` (parámetro al final con default, nombrado en los call sites — compat de tests existentes); `TurnTotals` gana `cachedTokens: Int?` (null ⇔ ningún turno con dato); `accumulateTurnTokens` suma Σ cached solo de turnos con dato; `ContextDetail` gana `turnCachedTokens: Int?`, `totalCachedTokens: Int?` y `turnPromptRowTokens: Int?` (valor compuesto de la fila «Prompt»); `buildContextDetail` propaga los tres | RF-296 |
| `app/src/main/java/com/example/ui/chat/ContextDetailCard.kt` | Dos sub-líneas condicionales con los estilos existentes (8.sp, `onSurfaceVariant` alpha .5f): `totalCachedTokens != null` → bajo «Prompt tokens evaluated», `chat_context_reused_kv`; `turnCachedTokens != null && turnPromptTokens != null` → bajo «Prompt», `chat_context_fresh_cached` (con `turnPromptTokens` = fresh); la fila «Prompt» pinta `turnPromptRowTokens` (payload compuesto); los dos placeholders Text hardcodeados desaparecen; `OutlinedCard` sigue 200×245 dp, divisor intacto; KDoc corregido «200×245 dp» y referencia a spec 041 | RF-297 · RF-298 · RF-299 |
| `app/src/test/java/com/example/ui/chat/ContextDetailTest.kt` | Nuevos tests JVM (rojos primero) — ver «Estrategia de tests»; patrón existente con asserts `assertEquals`/`assertNull` | RF-296 · RNF-179 |
| `app/src/main/res/values*/strings.xml` (9 locales) | Nuevas claves `chat_context_reused_kv` («%1$d reused from KV cache») y `chat_context_fresh_cached` («%1$d fresh + %2$d cached»); «KV cache» se conserva verbatim en todos los locales (registro del card ya existente); propuesta de traducciones con el registro de `chat_context_*` (p.ej. ES: «%1$d reutilizados del KV cache» / «%1$d fresco + %2$d en caché»; FR: «%1$d réutilisés du KV cache» / «%1$d frais + %2$d en cache»; IT: «%1$d riutilizzati dalla KV cache» / «%1$d freschi + %2$d in cache»; PT: «%1$d reutilizados do KV cache» / «%1$d fresco + %2$d em cache»; en-rGB = EN) | RF-299 · RNF-182 |
| `AGENTS.md` | Nota breve del patch 041 junto a la nota del 039 (el binding vendorizado lleva ambos: counters 039 + entry position 041); sin duplicar el resto | RNF-185 |
| `MEMORY.md` | Al cerrar la implementación: estado y decisiones (con `plan.md`/`tasks.md`); fuera de este documento | RNF-185 |

## Interfaz (lo que queda fijado en `ContextDetail.kt`)

```kotlin
// Pura, sin android — spec 041
data class TurnMetrics(
    val promptTokens: Int?,        // fresh del turno (spec 039, = promptEvalCount)
    val generatedTokens: Int?,
    val evalDurationMs: Long?,
    val cachedTokens: Int? = null  // entry position persistida; null = sin dato
)
data class TurnTotals(
    val promptTokens: Int,         // Σ fresh (RF-267 intacto)
    val generatedTokens: Int,
    val avgTokensPerSecond: Float?,
    val cachedTokens: Int?         // null iff ningún turno trae dato; si no, Σ cached (doble conteo)
)
data class ContextDetail(
    val usedTokens: Int, val windowTokens: Int, val exact: Boolean,
    val totalPromptTokens: Int, val totalGeneratedTokens: Int, val avgTokensPerSecond: Float?,
    val turnPromptTokens: Int?, val turnGeneratedTokens: Int?,   // fresh y generated del último turno (como hoy)
    val turnCachedTokens: Int?,     // cached del último turno con dato (patrón lastOrNull{!=null})
    val totalCachedTokens: Int?,    // Σ cached con doble conteo (Definición spec 041)
    val turnPromptRowTokens: Int?   // composición de la fila «Prompt»: cached+fresh (ambos) o fresh (sin cached)
)
fun accumulateTurnTokens(turns: List<TurnMetrics>): TurnTotals
fun buildContextDetail(usage: ContextUsage?, turns: List<TurnMetrics>): ContextDetail?
```

Forma elegida para la fila «Prompt»: `turnPromptRowTokens` vive en la pieza pura y la tarjeta
solo lo pinta — alternativa descartada: sumar `cached+fresh` en el Composable (meter lógica
decisoria en la interfaz viola la constitución, principio 3, y obliga a tests de UI que aquí
no corren). Se conserva `turnPromptTokens` (fresh) porque la sub-línea «fresh» lo necesita
distinto del total de la fila: fresh = lo decodificado, no cached+fresh.

## Algoritmo

```
ai_chat.cpp (del binding, spec 041):
  static int last_prompt_entry_position = -1;                       // spec 039 globals pattern

  processUserPrompt(env, unused, juser_prompt, n_predict) {
      const int64_t prompt_start_us = ggml_time_us();
      reset_short_term_states();      // … + last_prompt_entry_position = -1; … ANTES del snapshot
      last_prompt_entry_position = (g_context == nullptr) ? -1 : (int) current_position;   // tras el reset, antes de decodificar
      … resto igual …
  }

  reset_short_term_states() { … last_metrics_csv.clear(); last_prompt_entry_position = -1; }

  Java_com_arm_aichat_internal_InferenceEngineImpl_nativeLastPromptEntryPosition()
      ← (g_model == nullptr || g_context == nullptr) ? -1 : (jint) last_prompt_entry_position

en la app, al completar el streaming on-device (mismo bloque de métricas, AiViewModel):
  val onDevice = _backend.value == AiBackend.ON_DEVICE
  val onDeviceService = (aiModelHost.serviceOrNull() as? OnDeviceService)
  val cachedTokens = if (onDevice) onDeviceService?.currentPromptEntryPosition() else null   // Ollama → null
  … mismo refresh de exactUsage …
  if (userTokens != null && promptMs != null)
      conversationDao.updatePromptCachedMetrics(userTurnId, userTokens, promptMs, cachedTokens)   // Ollama: NULL
  … copia en memoria userIdx incluye promptCachedTokens = cachedTokens …

en piezas puras (spec 041):
  accumulateTurnTokens(turns):
    promptT = Σ(promptTokens ?: 0); generated = Σ(generatedTokens ?: 0)
    evalNs  = Σ((evalDurationMs ?: 0) * 1e6)                        // avg ponderada, como hoy
    cachedSum = 0; cachedSeen = false
    for t in turns: if (t.cachedTokens != null) { cachedSum += t.cachedTokens; cachedSeen = true }
    TurnTotals(promptT, generated, tokensPerSecond(generated, evalNs),
               cachedTokens = if (cachedSeen) cachedSum else null)  // todo-null ⇒ null (RF-296)

  buildContextDetail(usage, turns):
    if (usage == null) return null                                  // RF-265, intacto
    totals        = accumulateTurnTokens(turns)
    turnPrompt    = turns.lastOrNull { it.promptTokens != null }?.promptTokens   // fresh (sin cambio)
    turnGenerated = turns.lastOrNull { it.generatedTokens != null }?.generatedTokens
    turnCached    = turns.lastOrNull { it.cachedTokens != null }?.cachedTokens   // último con dato
    turnPromptRow = when {
        turnCached != null && turnPrompt != null -> turnCached + turnPrompt      // nkv al enviar (DU-2)
        else                                       -> turnPrompt                 // histórico/Ollama ⇒ fresh
    }
    ContextDetail(…, turnPromptTokens = turnPrompt, turnGeneratedTokens = turnGenerated,
                  turnCachedTokens = turnCached, totalCachedTokens = totals.cachedTokens,
                  turnPromptRowTokens = turnPromptRow)

en la tarjeta (ContextDetailCard):
  fila «Prompt»  → turnPromptRowTokens (30)             // RF-298 enmendado (spec 037)
  sub-línea      → turnCachedTokens != null && turnPromptTokens != null
                      ? "{fresh} fresh + {cached} cached" (chat_context_fresh_cached)
  «Prompt tokens evaluated» → totalPromptTokens (Σ fresh)          // RF-267 intacto
  sub-línea      → totalCachedTokens != null
                      ? "{total} reused from KV cache"  (chat_context_reused_kv)
  resto de filas/divisor/altura: sin cambio
```

## Decisiones con su alternativa descartada

- **Entry position por export propio** (`nativeLastPromptEntryPosition`, análogo a `nkvPosition`)
  en vez de derivarla (`nkv_now − fresh − generated`): la resta se rompe con `shift_context()`
  (el KV encoge durante el turno) y con la truncación silenciosa de prompts largos (guard
  `user_prompt_size > max_batch_size`); no conoce la posición al **entrar** y obliga a tres
  lecturas en vez de una cifra atómica.
- **Snapshot al ENTRAR** (antes de decodificar, antes de cualquier shift del turno) en vez de
  tras el último token: tras terminar, `current_position = entry + fresh + generated` — correcto,
  pero derivado; un fallo de decode a mitad deja el snapshot de entrada intacto y exacto
  (casos límite de la spec respetados). Coste: una línea en `processUserPrompt`.
- **Acumulado con flag `cachedSeen` (0/null) en vez del patrón `evalNs` (todo-ausente ⇒ 0)**:
  tratar ausencia como 0 pintaría «0 reused from KV cache» para histórico/Ollama, cifra
  inventada; la spec exige null global ⇒ sub-línea omitida (RF-296/297, RNF-181). Los turnos
  sin dato contribuyen 0 a la suma, pero solo si **algún** turno trae dato; si ninguno, el
  acumulado es null. `promptTokens` conserva 0 (RF-267 intacto) porque su sub-línea no existe.
- **`turnCachedTokens` = último turno con dato** (`lastOrNull { it != null }`) en vez de «del
  último turno» a secas: turnos de error/cancelación al final del histórico no apagan la
  sub-línea del turno sano anterior — mismo patrón que `turnPromptTokens`/`turnGeneratedTokens`
  en spec 037.
- **Sub-líneas solo on-device** en vez de replicar en Ollama (o estimar por chars): Ollama no
  reporta el KV reutilizado; un valor inventado viola la regla de la spec 039 (RF-280).
  Con Ollama, ambas sub-líneas se omiten y la fila muestra fresh (o el histórico), como hoy.
- **Columna nullable sin backfill** en vez de `NOT NULL DEFAULT 0`: 0 sería un «dato» inventado
  para todo el histórico (y para Ollama); NULL representa la ausencia real, es la señal que
  gobierna la omisión de sub-líneas (RF-295) y no admite heurísticas de relleno.
- **Migración Room registrada `MIGRATION_10_11` (10→11, `.addMigrations(...)`)** en vez de
  `try/catch` en `onOpen` (mención de DU-1, descartada al revisar el código): Room valida el
  hash de esquema al abrir; una columna en la entidad sin bump y sin migración aborta la
  apertura; el try/catch ocultaría el fallo y tumbaría la app al arrancar. Alternativa
  «fallbackToDestructiveMigration como mecanismo» descartada por la constitución (datos del
  usuario nunca se destruyen).
- **`updatePromptCachedMetrics` que reemplaza a `updatePromptMetrics`** en vez de un método
  `updateCachedMetric` aparte llamado tras el primero: dos operaciones = ventana donde la
  prompt existe sin cached o viceversa; el call site es único, el cambio de nombre no deja
  muertos, y el UPDATE único es atómico a nivel de fila. Extender el método con parámetro por
  defecto descartado por semántica (el campo nuevo es explícito en el nombre, no opcional).
- **Persistir cached solo dentro del guard `userTokens != null && promptMs != null`** en vez de
  escribirlo siempre que la posición exista: en cancelación/error no hay métricas de turno
  donde «colgarlo» y la spec manda ausencia total; el guard ya existente es la señal exacta.
- **`turnPromptRowTokens` compuesto en puras** en vez de delegar la suma al Composable:
  constitución principio 3 + testabilidad JVM sin Robolectric (RNF-179).
- **Fila «Prompt» = cached+fresh (30, como la captura)** cuando ambos existen — decisión
  explícita del usuario (DU-2): la Web UI muestra el nkv del turno completo (1+29). **ACROSS
  ALL TURNS queda Σ fresh** (DU-3): el doble conteo vive solo en la sub-línea.
- **`TurnMetrics.cachedTokens` con default `= null`** en vez de obligatorio al final: los
  call sites y tests existentes (`TurnMetrics(promptTokens = …, …)`) compilan sin tocarse;
  named args en tests nuevos de todos modos.
- **Espejo de la patch en `/root/llama.cpp`**: el binding vendorizado (`lib/`) es la fuente de
  verdad; el espejo solo existe porque la build del host compila contra ese checkout
  (`add_subdirectory`); no se vendoriza, no se commitea, no se toca el motor por otros motivos.

## Estrategia de tests (con `./gradlew test`)

1. **Tests JVM puros primero (rojos → verdes)** en `ContextDetailTest.kt`, extensión del patrón:
   - `accumulateTurnTokens_allCachedNull_yieldsNullTotal` (listas todas sin dato → null);
   - `accumulateTurnTokens_sumsOnlyTurnsWithData` (p.ej. cached 10, null, 3 → 13; mezcla);
   - `accumulateTurnTokens_emptyList_yieldsNullCached`;
   - `buildContextDetail_propagatesCachedFields` (defensa de la coherencia **29+1+10=40** como
     dato de prueba: `ContextUsage(40, 8192, exact = true)`; turns [user(fresh=1, cached=29),
     asst(generated=10)] → `turnCachedTokens=29`, `totalCachedTokens=29`,
     `turnPromptRowTokens=30`, `turnPromptTokens=1`, `totalPromptTokens=1`);
   - `buildContextDetail_turnPromptRow_freshOnlyWhenCachedMissing` (histórico sin cached →
     `turnPromptRowTokens == fresh`; ambos → suma);
   - `buildContextDetail_turnCached_takesLastWithData` (error/cancel post-turno sano);
   - `buildContextDetail_nullUsage_returnsNull` y `…_emptyConversation_totalsNull` (regresión);
   - `TurnTotals`/`TurnMetrics` con default compilan sin cambios en tests viejos.
2. **Comandos**:
   - `./gradlew :app:testDebugUnitTest --tests '*ContextDetail*'` (JVM puros; los puros corren
     en linux/aarch64).
   - `./gradlew :app:compileDebugKotlin` (incluye CMake de `:lib` con el espejo; única build
     completa viable en este host).
   - **NO** `./gradlew test` completo como gate: en linux/aarch64 Robolectric no tiene runtime
     nativo (38 fallos de plataforma conocidos, ver AGENTS.md/MEMORY.md). El gate de esta spec
     es RNF-184: `:app:compileDebugKotlin` + tests JVM puros en verde.
3. **Lo no testeable aquí lo valida dispositivo (pendiente explícita, como otras specs)**:
   `MIGRATION_10_11` (apertura sobre BD real v10), sub-líneas con on-device real vs Web UI,
   y el flujo cancelado/error. La migración se revisará aditiva y registrada; no hay tests
   instrumentados útiles sin dispositivo.

## Cobertura de RFs

RF-293 (binding + exports + espejo) · RF-294 (LlamaCppEngine/OnDeviceService) ·
RF-295 (Room 10→11) · RF-296 (piezas puras) · RF-297 (tarjeta, sub-líneas) ·
RF-298 (enmienda 037, RF-267 sub-línea / RF-268 valor+sub-línea) · RF-299 (strings ×9,
placeholders off) · RNF-179…RNF-185 (tests/docs)

## Fuera de alcance (de la spec, sin ampliar)

Compactar/truncar/persistir el desglose (es derivado) · cambiar la ventana 8192/Anillo/refresh
(specs 034/039) · portar a Ollama · tocar el motor externo fuera del patch aditivo del binding
(y su espejo local nunca viaja al repo) · reimplementar prefix-reuse reenviando la conversación ·
Discovery/verificación de páginas web · enmendar «Prompt tokens evaluated» (Σ fresh) ni
«Generated» · backfill de `promptCachedTokens` histórico.

## Estimación de tareas (~10, para `tasks.md`)

- **T1.** Binding: `last_prompt_entry_position` + snapshot al entrar + reset + export JNI
  `nativeLastPromptEntryPosition` (`ai_chat.cpp`); espejo idéntico en `/root/llama.cpp`. RF-293
- **T2.** `lib/`: `InferenceEngine.lastPromptEntryPosition()` + `InferenceEngineImpl`
  (`nativeLastPromptEntryPosition()`, `@FastNative`). RF-293
- **T3.** App: `LlamaCppEngine.lastPromptEntryPosition()` (−1 sin modelo) +
  `OnDeviceService.currentPromptEntryPosition(): Int?`. RF-294
- **T4.** Room: `ConversationEntity.promptCachedTokens` + `NoteDatabase` `MIGRATION_10_11`
  (10→11, `.addMigrations`) + `ConversationDao.updatePromptCachedMetrics` (reemplaza
  `updatePromptMetrics`). RF-295 · RNF-183
- **T5.** `ContextDetailTest.kt`: tests nuevos en rojo (acumulado todo-null→null, mezcla,
  coherencia 29+1+10, fila composición cached+fresh / fresh solo, regresión build null). RNF-179
- **T6.** `ContextDetail.kt`: `TurnMetrics`/`TurnTotals`/`ContextDetail` ganan campos;
  `accumulateTurnTokens` con `cachedSeen`; `buildContextDetail` propaga + `turnPromptRowTokens`. RF-296
- **T7.** `AiViewModel.kt`: `ConversationTurn.promptCachedTokens` + restore + lectura de
  `currentPromptEntryPosition()` + escritura en el guard (Ollama NULL) + copia en memoria. RF-294 · RF-295
- **T8.** `AiChatScreen.kt`: `cachedTokens = turn.promptCachedTokens` en `conversationTokenMetrics`. RF-294 · RF-296
- **T9.** `ContextDetailCard.kt`: dos sub-líneas condicionales (estilos existentes), placeholders
  borrados, fila «Prompt» pinta `turnPromptRowTokens`, KDoc «200×245 dp». RF-297 · RF-298 · RF-299
- **T10.** `strings.xml` × 9 + `AGENTS.md` (nota 041 junto a 039) + verificación: `:app:testDebugUnitTest
  --tests '*ContextDetail*'` y `:app:compileDebugKotlin` en verde; `MEMORY.md` al cerrar. RNF-182 · RNF-185

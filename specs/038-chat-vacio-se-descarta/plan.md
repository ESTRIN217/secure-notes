# Plan 038 — Chats vacíos se descartan

Spec: `specs/038-chat-vacio-se-descarta/spec.md` (aprobada 2026-11-16)
Depende de: spec 032 (patrón «borra definitivo al salir»), spec 006/033 (chat, sesiones persistidas al crear)

## Archivos y responsabilidades

| Archivo | Responsabilidad | RFs |
|---|---|---|
| `app/src/main/java/com/example/ui/chat/ChatEmptiness.kt` (**nuevo**, puro, sin `android`) | `shouldDiscardChatSession(turnsCount, hasDraftOrAttachments): Boolean` y `isPurgeableSession(createdAtMs, processStartedAtMs, messageCount): Boolean` | RF-273, RF-274, RF-275, RNF-165 |
| `app/src/test/java/com/example/ui/chat/ChatEmptinessTest.kt` (**nuevo**) | Tests JVM puros | RNF-165 |
| `app/src/main/java/com/example/SecureNotesApplication.kt` | Exponer `processStartedAt` (epoch ms fijada en `onCreate`) — ancla para «creada en este proceso» | RF-274, RF-276 |
| `app/src/main/java/com/example/data/local/ChatSessionDao.kt` | Nueva query solo lectura: `emptySessionIdsSince(since)` → ids con `messageCount = 0 AND createdAt >= :since` (sin borrar) | RF-274, RF-276 |
| `app/src/main/java/com/example/MainActivity.kt` | En `onDestroy`, **solo** si `!isChangingConfigurations`: `runBlocking(Dispatchers.IO) { withTimeoutOrNull(500) { ids = emptySessionIdsSince(app.processStartedAt); repeat(deleteConversations) + repeat(deleteSession) } }` | RF-274, RF-276 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | `BackHandler(onBack)` (el que cierra el chat) → si `effectiveSessionId > 0` y `shouldDiscardChatSession(conversationHistory.size, hasDraft)` → `chatHistoryViewModel.deleteSession(effectiveSessionId)` y luego `onBack()` en cualquier caso | RF-273, RF-275 |

Nada más se toca: ni esquema, ni Room, ni VM para las sesiones viejas (RF-276).

## Algoritmo

```
shouldDiscardChatSession(turns, draft):
    return turns <= 0 && !draft                      // RF-273/RF-275

isPurgeableSession(createdAt, processStartedAt, count):
    return count == 0 && createdAt >= processStartedAt

// Al salir (AiChatScreen):
val discard = shouldDiscardChatSession(conversationHistory.size,
    hasDraftOrAttachments = effectiveText(inputText, editingMessage).isNotBlank()
        || pendingAttachments.isNotEmpty())
if (discard && effectiveSessionId > 0) chatHistoryViewModel.deleteSession(effectiveSessionId)
onBack()   // siempre

// Al destruir la Activity (MainActivity.onDestroy):
if (!isChangingConfigurations) {
    runBlocking(Dispatchers.IO) {
        withTimeoutOrNull(500) {
            val db = NoteDatabase.getDatabase(this@MainActivity)
            val ids = db.chatSessionDao.emptySessionIdsSince(app.processStartedAt)
            for (id in ids) { db.chatSessionDao.deleteConversations(id); db.chatSessionDao.deleteSession(id) }
        }
    }
}
```

## Interfaz

```kotlin
// ui/chat/ChatEmptiness.kt (pura)
const val PURGE_TIMEOUT_MS = 500L
fun shouldDiscardChatSession(turnsCount: Int, hasDraftOrAttachments: Boolean): Boolean
fun isPurgeableSession(createdAtMs: Long, processStartedAtMs: Long, messageCount: Int): Boolean
```

## Decisiones con su alternativa descartada

- **Ancla `processStartedAt` en Room** (`createdAt >= since AND messageCount = 0`) en vez de un
  registry en memoria: sin cambios en ViewModel ni fábrica; sobrevive y cumple RF-276 de forma
  trivial; el único riesgo es un `createdAt` justo en el arranque tocado por un proceso anterior
  → fuera de alcance por segundos y sin consecuencias (la sesión también estaría vacía y es suya).
- **Sin purga en `onStop`** (solo `onDestroy` + !config-change): `onStop` lo atraviesa cualquier
  cambio de config y el cambiador de apps; `isChangingConfigurations` lo excluye para siempre (RF-274).
- **`runBlocking` con `withTimeoutOrNull(500)`** en vez de coroutine suelta: `onDestroy` es el último
  momento garantizado; una coroutina sería matada con el proceso y no boraría nada; el timeout acota
  el peor caso a ~500 ms y, si vence, el borrado parcial es inofensivo (sigue siendo sesión vacía).
- **El draft vive solo en el camino de salida**; la vía destroy no puede observarlo y se acepta
  (documentado en la spec): la sesión borrada por cierre nunca tuvo un turno enviable.
- **El BackHandler existente es el gancho** (no se añade un `DisposableEffect(ON_STOP)`): duplicaría
  la semántica de «atrás», rompería la regla de un solo camino de descarte y dispararía en rotación.
- **Silencio total** (sin toast ni diálogo): un gesto de atrás no debe preguntar por el borrado de
  un cascarón vacío (RF-166).

## Estrategia de tests

`./gradlew :app:testDebugUnitTest --tests '*ChatEmptiness*'` — rojos primero.

Casos: turnos ≤ 0 con draft → falso; sin draft → true; turnos > 0 con draft vacío → falso; draft con
adjunto sin turno → falso (protege) — los de `isPurgeableSession`: `count == 0` y `createdAt >= since`
→ true; anteriores → false; `count > 0` → false.

`./gradlew :app:compileDebugKotlin` incluye `processDebugResources` (AAPT); pruebas JVM puros corren en aarch64.

## Cobertura de RFs

RF-273 (BackHandler + pura) · RF-274 (MainActivity + pura) · RF-275 (pura + screen) · RF-276 (query acotada) · RNF-165 (tests JVM) · RNF-166 (silencio, nada de strings) · RNF-167 (cascade existente) · RNF-168 (sin lógica por frame) · RNF-169 (sin esquema)

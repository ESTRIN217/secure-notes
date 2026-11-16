# Tasks 038 — Chats vacíos se descartan

- [x] **T1. Predicados puros `ChatEmptiness` + tests (rojos).** RF-273, RF-274, RF-275, RNF-165
  - Hecho cuando: `ChatEmptinessTest` pasa: turnos ≤ 0 sin draft → descarta; sin draft con adjunto/draft → no; turnos > 0 → no (aunque draft); `isPurgeableSession`: `count == 0 && createdAt >= since` true, anterior false, count > 0 false.

- [x] **T2. Ancla `processStartedAt` + query de SessionIds en BD.** RF-274, RF-276
  - Hecho cuando: `SecureNotesApplication` expone `processStartedAt`; `ChatSessionDao.emptySessionIdsSince(since: Long)` devuelve los ids con `messageCount = 0 AND createdAt >= :since`; nada cambia de esquema.

- [x] **T3. `MainActivity.onDestroy`: purga sin config-change.** RF-274, RF-276
  - Hecho cuando: si `isChangingConfigurations` es true → no ocurre nada; si no, `runBlocking(Dispatchers.IO) { withTimeoutOrNull(500ms) }` borra (cascade) las sesiones purgables; MainActivity compila con los imports de coroutines ya usados en el proyecto.

- [x] **T4. `AiChatScreen`: BackHandler con descarte.** RF-273, RF-275
  - Hecho cuando: el handler que cierra el chat evalúa el predicado con `conversationHistory.size`, `effectiveText(inputText, editingMessage)` y `pendingAttachments`; con discard, llama `chatHistoryViewModel.deleteSession(effectiveSessionId)` (cascade existente) y procesa `onBack()` siempre; sin cambios en el resto del archivo ni en otros handlers (drawer mantiene el suyo).

- [ ] **T5. Verificación en dispositivo.** RF-273…RF-276
  - Hecho cuando: 5 comprobaciones de la spec pasan (atrás sin escribir → no está en historial; con draft/adjuntos → conserva; con mensaje → conserva; cerrar la app completa desde reposo en el chat vacío → reabrir sin la sesión; rotar con chat vacío → conserva); `./gradlew :app:compileDebugKotlin` verde.

- [ ] **T6. `MEMORY.md` + cierre.** RF-273…RF-276
  - Hecho cuando: memoria actualizada; T1-T5 marcados; diff sin código fuera de alcance ni strings nuevas.

# Tasks 035 — Carga de modelo en el chat

Una tarea cada vez: rojo → verde → siguiente.

- [x] **T1. Piezas puras + tests (rojos).** RF-258, RF-259, RNF-153
  - Hecho cuando: `ModelLoading.kt` con las dos funciones + `ModelLoadingTest.kt` pasa; casos: (ON_DEVICE, LOADING) → barra visible; (OLLAMA, LOADING) → invisible; (ON_DEVICE, READY|ERROR|NOT_LOADED) → invisible; banner: `modelLoading=true` → oculto; `/available=false, historyEmpty=true, loading=false` → visible; `available=true` o `!historyEmpty` → oculto.

- [x] **T2. `AiChatScreen`: barra condicionada + banner suprimido en LOADING.** RF-258, RF-259
  - Hecho cuando: el stub `LinearWavyProgressIndicator()` se sustituye por `if (shouldShowModelLoadingIndicator(backend, onDeviceModelState)) LinearWavyProgressIndicator()` (ancho completo); `showError` usa la pieza pura con `historyEmpty = conversationHistory.isEmpty()`; sin strings nuevas ni otros cambios; build OK.

- [x] **T3. `MEMORY.md` + verificación.** RF-258, RF-259
  - Hecho cuando: build y tests dirigidos verdes; anotado en memoria; verificación en dispositivo pendiente del usuario (enviar con modelo en LOADING → barra visible y banner ausente; READY → barra desaparece).

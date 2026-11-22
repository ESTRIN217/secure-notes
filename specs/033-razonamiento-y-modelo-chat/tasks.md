# Tareas 033 — Razonamiento y modelo en el chat

Spec: `specs/033-razonamiento-y-modelo-chat/spec.md` · Plan: `plan.md`
Orden de dependencia. Tests primero (rojo→verde) en cada tarea que los lleva; `./gradlew test` en verde (baseline 868/38) al final de cada tarea que lo permita.

- [x] **T1. `ReasoningEffort.kt` + test.** RF-218, RF-219, RNF-140
  - Crear `data/ai/ReasoningEffort.kt` (enum `DEFAULT/OFF/LOW/MEDIUM/HIGH/MAX`, `reasoningMaxTokens`, `reasoningThink`) y `app/src/test/java/com/example/data/ai/ReasoningEffortTest.kt`.
  - Hecho cuando: el test existe y pasa; `./gradlew :app:testDebugUnitTest --tests "com.example.data.ai.ReasoningEffortTest"` verde.

- [x] **T2. `ScreenWidth.kt` + test.** RF-215, RF-223, RNF-140, RNF-144
  - Crear `ui/chat/ScreenWidth.kt` (`isLargeScreenWidth`) y `app/src/test/java/com/example/ui/chat/ScreenWidthTest.kt` (599→false, 600→true, 840→true).
  - Hecho cuando: test verde; el archivo no importa android ni Compose.

- [x] **T3. `AiRequest.think` + motores.** RF-218, RF-219, RNF-143
  - `AiModels.kt`: `val think: Boolean? = null` en `AiRequest`.
  - `OllamaService.kt`: en `buildChatRequest`, `if (request.think != null) jsonBody.put("think", request.think)` top-level (no en `options`).
  - `LlamaCppEngine.kt`: `val predict = request.maxTokens.takeIf { it > 0 } ?: 1_000_000` en los dos `sendUserPrompt`.
  - Hecho cuando: `:app:compileDebugKotlin` verde; grep confirma `"think"` fuera del bloque `options` en `OllamaService`.

- [x] **T4. `AiViewModel`: esfuerzo y release on-device.** RF-218..RF-221, RF-224, RF-226
  - `_reasoningEffort = MutableStateFlow(ReasoningEffort.DEFAULT)`; expone `reasoningEffort: StateFlow<…>`; `fun setReasoningEffort(e: ReasoningEffort)`.
  - En `execute()`: `maxTokens = reasoningMaxTokens(_maxTokens.value, _reasoningEffort.value)` y `think = reasoningThink(_reasoningEffort.value)` en el `copy`.
  - En `selectOnDeviceModel`: si `selectedOnDeviceModel.value?.id != model.id`, `aiModelHost.releaseAll("model-changed")` antes de fijar.
  - Hecho cuando: `:app:compileDebugKotlin` verde; `executeInPlace`/`coerceAtMost(512)` no tocado (grep).

- [x] **T5. `ReasoningEffortSheet.kt`.** RF-215, RF-217, RNF-142, RNF-145
  - Hoja modal (patrón `AttachmentAiSheet`), orden Default→Max; `Check` en la seleccionada; subtítulos `%1$d` / «Ilimitado» / «Usa el ajuste de la app (máx. %1$d t)» con pref; `contentDescription` con nivel; cierra en selección o dismiss.
  - Hecho cuando: `:app:compileDebugKotlin` verde; el composable recibe todo por parámetros (sin VM, sin StateFlow).

- [x] **T6. `FloatingChatInputField`: arreglo del stub + botón esfuerzo.** RF-215..RF-217, RF-225, RF-227, RNF-144/145
  - `ChatTextField` en **línea propia encima** de la fila (`fillMaxWidth()`); borrar `@Preview` vacía e import muerto.
  - Nuevos parámetros `reasoningEffort`, `onReasoningEffortChange`; botón Lightbulb (+ rótulo corto del nivel si ≠ DEFAULT, una línea, claves localizadas); `<600 dp` monta `ReasoningEffortSheet`; `>=600 dp` monta `DropdownMenu` anclado (6 ítems + check). `contentDescription` con nivel («Nivel de razonamiento: %1$s»).
  - Hecho cuando: el archivo compila; grep de `Lightbuld` y `@Preview` = 0; `:app:compileDebugKotlin` verde.

- [x] **T7. Botón modelo en el mismo archivo.** RF-222, RF-225, RF-227
  - Parámetros `modelName: String`, `onModelPickerClick: () -> Unit`; botón con `Box`-icon + `Text` nombre («Sin modelo» si vacío, maxLines=1, elipsis) + `KeyboardArrowDown`; `contentDescription` «Cambiar modelo, actual: %1$s»; `enabled` siempre; colocado entre esfuerzo y enviar.
  - Hecho cuando: `:app:compileDebugKotlin` verde; los cuatro botones de la fila caben con modelo elidido.

- [x] **T8. `ModelSelectorSheet.kt`.** RF-223, RF-224, RNF-145
  - Hoja con secciones: «En el dispositivo» (catálogo descargado: `downloadedModels`; vacío → aviso; tap → `onSelectOnDevice`) y «Ollama» (cacheado de `/api/tags` lista sin red; si no, modelo configurado + «Probar conexión»: loading → éxito lista / fallo inline `ai_connection_fail` con retry; hoja no se cierra). Receptor de estado `isTesting` + `connectionError` por parámetro.
  - Hecho cuando: `:app:compileDebugKotlin` verde; el composable no llama a red directamente.

- [x] **T9. `AiChatScreen` wiring.** RF-222..RF-228
  - Recolectar `reasoningEffort`, `connectionState`, `downloadedModelIds`, `selectedOnDeviceModel`; `displayModelName` con fallback «Sin modelo»; pasar los 4 parámetros nuevos; montar `ModelSelectorSheet` con `showModelSelector` (patrón `showAttachmentSheet`); callbacks: on-device → `selectOnDeviceModel`+`setBackend(ON_DEVICE)`; Ollama → `setModelName`+`setBackend(OLLAMA)`; «Probar conexión» → `testConnection()`.
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T10. Textos 9 locales + cierre.** RF-225, RNF-142, criterio 3-4
  - Añadir las 19 claves del inventario en `values/`, `values-es-rVE/`, `values-pt-rBR/`, `values-fr/`, `values-it/`, `values-en-rGB/`, `values-es-rES/`, `values-pt-rPT/`, `values-b+es+419/` (textos escapados ASCII-safe; apóstrofes con `\'` en fr).
  - `./gradlew test` en este host: sin regresiones frente a 868/38 (los nuevos tests puros pasan); `assembleDebug` verde.
  - Actualizar `MEMORY.md` (spec 033 cerrada) y marcar esta lista.
  - Hecho cuando: `./gradlew test` sin fallos nuevos y `assembleDebug` OK; los 9 `strings.xml` con las mismas claves (orden equivalente).

Tareas opcionales futuras (fuera de esta spec): `xhigh` model-defined, effort en el snapshot de sesión, thinking en streaming.

## Resultado de la implementación (2026-11-11)

- T1, T2: tests puros verdes en este host (`ReasoningEffortTest`, `ScreenWidthTest`).
- T3, T4, T5, T6, T7, T8, T9: `:app:compileDebugKotlin` BUILD SUCCESSFUL.
- T10: `./gradlew test` → **879 tests, 38 fallos** (todos de plataforma `linux/aarch64`; los 11 tests puros nuevos pasan; baseline 868/38 sin regresión). `:editor-de-codigo` → 2 fallos de plataforma (Robolectric), módulo intacto. `assembleDebug` BUILD SUCCESSFUL (recursos/AAPT2 OK).
- Pendiente: verificación en dispositivo (checklist del criterio 1 de finalización) — abrir chat, pulsar los dos botones, cambiar esfuerzo y modelo on-device, enviar, reabrir sesión histórica.

## Ampliación 2026-11-11 — Búsqueda y acciones por modelo (RF-229…RF-237, RNF-146)

- [x] **T11. `ModelSearch.kt` + test.** RF-229, RF-233, RNF-146
  - `queryMatches`, `filterOnDeviceModels`, `filterOllamaModels`, `favoritesFirst`, `favoriteOnDeviceKey`/`favoriteOllamaKey`.
  - Hecho cuando: `ModelSearchTest` en verde (filtro/orden/claves; ignora mayúsculas; favoritos primero estables).

- [x] **T12. Backup y prefs de favoritos.** RF-233
  - `AI_FAVORITE_MODELS_KEY` en `Constants.kt`; `BACKUP_SETTINGS` + `DEFAULT_SETTINGS` (TEXT, `""`) → **quince**; `BackupSettingsTest` ajustado (15 claves, fixtures).
  - `PreferencesRepository` + `SharedPreferencesRepository`: `getFavoriteModels`/`setFavoriteModels` (CSV `\n`).
  - Hecho cuando: `BackupSettingsTest` verde.

- [x] **T13. `AiViewModel`: favoritos, descarga y borrado por modelo.** RF-230…RF-232, RF-237
  - `favoriteModels` y `downloadingModelId` (StateFlows); `toggleFavoriteModel(key)`; `downloadOnDeviceModel(model)` (no toca la selección, guarda el id en descarga); `deleteOnDeviceModel(model)` (release vía `AiModelHost` si era el seleccionado + `setAiOnDeviceModelPath("")` + refresh).
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T14. `ModelSelectorSheet` rehecho.** RF-229…RF-234
  - Estado ampliado; barra de búsqueda (hoja y menú); catálogo completo + favoritos primero; filas on-device con: tap (descargado→selecciona; ausente→descarga), `Star`/`StarBorder` (`#FFB300`), `Info` (diálogo: quant/RAM/origen + «Abrir en Hugging Face» vía `LocalUriHandler`), `Download`/`Delete` (progreso `LinearWavyProgressIndicator` si es el modelo en descarga; deshabilitados mientras haya otra descarga activa); Ollama con solo favorito; sin resultados → nota.
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T15. `ModelSelectorMenu` compacto ≥600 dp.** RF-235
  - Menú con búsqueda como primer ítem (`TextField` 268 dp), filas compactas con iconos de 32 dp; Check de selección; «Probar conexión» y error `/api/tags` intactos.
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T16. Dialogos y confirmación.** RF-231
  - `ModelInfoDialog` (quant/RAM/origen + HF), `DeleteModelDialog` (con confirmación destructiva; libera pesos si era el seleccionado).
  - Hecho cuando: `:app:compileDebugKotlin` verde; la eliminación confirma antes de borrar.

- [x] **T17. `FloatingChatInputField` + `AiChatScreen`.** RF-230…RF-235
  - Campo: 3 callbacks nuevos (`onToggleFavoriteModel`, `onDownloadModel`, `onDeleteModel`); `AiChatScreen`: recolecta `favoriteModels`/`downloadingModelId`/`downloadState`, `MODEL_CATALOG` completo, wiring de callbacks.
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T18. Textos 9 locales.** RF-236
  - 10 claves nuevas en los 9 `strings.xml`; reutilizadas `close`/`cancel`; `chat_model_no_results` incluido.
  - Hecho cuando: `./gradlew assembleDebug` OK (AAPT2).

- [x] **T19. Verificación y cierre.**
  - `./gradlew test` → **890 tests, 38 fallos (plataforma), 0 omitidos** (antes 879/38; +modelsearch/backup), `:app` sin regresión; `:editor-de-codigo` intacto (2 fallos de plataforma). `assembleDebug` BUILD SUCCESSFUL.
  - Pendiente: verificación en dispositivo (checklist): buscar/favoritos/descarga/eliminación/info en hoja y en menú ≥600 dp; diálogo de borrado; «Probar conexión».

## Corrección 2026-11-12 — upload/download = cargar/descargar en memoria (como Ajustes)

- [x] **T20. Control de ciclo de vida por fila.** RF-231, RF-232, RF-237, RF-236
  - `ModelSelectorState` gana `onDeviceModelState`; filas on-device: `Download` si falta el `.gguf`, `PlayArrow` si está y no está cargado (select + backend on-device + `loadSelectedModel()`), `Stop` si está cargado y es el activo (`unloadModel()` sin confirmar), spinner mientras `LOADING`.
  - Eliminados `DeleteModelDialog` y las claves `chat_model_delete_*` de los 9 locales; añadidas `chat_model_load_desc` y `chat_model_unload_desc`; el diálogo de info y los favoritos intactos; borrar el `.gguf` solo en Ajustes → IA. `deleteOnDeviceModel` retirado (código muerto); el `@Preview` del usuario adaptado al nuevo API.
  - Hecho cuando: `:app:compileDebugKotlin` verde, `./gradlew test` → **890 tests, 38 fallos (plataforma), 0 omitidos** (sin regresión), `assembleDebug` OK.

## Ampliación 2026-11-14 — Panel de gestión de modelos (tablet/PC)

- [x] **T21. Backup con ocultos.** RF-244
  - `AI_HIDDEN_MODELS_KEY` en `Constants.kt`; `BACKUP_SETTINGS`/`DEFAULT_SETTINGS` +1 (TEXT, `""`) → **dieciséis**; `BackupSettingsTest` ajustado (16 claves).
  - Hecho cuando: `BackupSettingsTest` verde.

- [x] **T22. Preferencias de ocultos.** RF-244
  - `getHiddenModels()`/`setHiddenModels(Set<String>)` (CSV `\n`) en `PreferencesRepository` + `SharedPreferencesRepository`.
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T23. `/api/show` con contexto.** RF-241
  - `OllamaService.testConnection` extendida: tras `/api/tags`, llamada a `/api/show` por modelo con `withTimeoutOrNull(5s)`; se lee `*.context_length`; un fallo no derriba el test. Nuevo `OllamaConnectionInfo(models, contextLengths)`; `ConnectionState.Connected` lleva `contextLengths`.
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T24. `AiViewModel`: ocultos y borrado parametrico.** RF-243, RF-244
  - `hiddenModels` (StateFlow, desde pref), `toggleHiddenModel(key)`; `deleteOnDeviceModel(model)` re-añadida (release pre-borrado si es el activo).
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T25. `ModelManagerPanel.kt`.** RF-238…RF-246
  - Overlay hermano del Scaffold en assistant ≥600 dp (BackHandler, ✕); cabecera «Models» + subtítulo EN con traducción ×9; buscador; dos secciones plegadas (Local models N / Ollama N, contadores); tabla columnas (model/context/actions); filas: corazoncito si favorita, nombre negrita + chips quant/size MB; Context `8.192 tokens` (locale) / Ollama `context_length` de `/api/show` / «—»; Status power (Download→⏻/Download, encendido si READY+activo, LOADING=spinner, tap toggle load/unload); Actions `⋮` con Add/Remove favorites (heart), Delete from disk (rojo, con confirmación), Hide model; chip «Show hidden models (N)» desoculta; corazón/card en Ollama = mismo favorito.

- [x] **T26. Wire-up.** RF-238, RF-242, RF-243
  - `FloatingChatInputField`: ≥600 dp abre el panel; el menú `ModelSelectorMenu` eliminado (RF-235 superada).
  - `AiChatScreen`: recolecta `hiddenModels` y `contextLengths`; monta `ModelManagerPanel` como hermano a continuación del Scaffold (se pinta por encima); callbacks de delete/unload espejan las del sheet.
  - `ModelSelectorSheet`: filtro de ocultos en ambos `remember`; estrellas → corazones (#E91E63).

- [x] **T27. Verificación.**
  - `./gradlew test` → **890 tests, 38 fallos (plataforma), 0 omitidos** (= baseline; modelos/nueva lógica pura pasa, incluida backup=16).
  - `:app:compileDebugKotlin` y `assembleDebug` → **BUILD SUCCESSFUL**. 19 claves nuevas × 9 locales.
  - Pendiente: verificación visual en tablet/PC contra las capturas de Ollama Models (header, chip al final, iconos).

## Corrección 2026-11-15 — Context = context length del modelo

- [x] **T28. `context_length` real por modelo.** RF-241, RNF-146/147
  - `data/ai/ModelContextCache.kt` (puro): `parseContextLengths`/`serializeContextLengths` para una **caché interna** en prefs (`AI_MODEL_CONTEXT_LENGTHS_KEY`, **fuera** de la copia); cada `.gguf` se parsea **una vez** vía `GgufMetadataReader.readStructuredMetadata` y se cachea; se poda al borrar el modelo y al cambiar el catálogo.
  - `PreferencesRepository`/`SharedPreferencesRepository`: `getModelContextLengths()`/`setModelContextLengths()`.
  - `AiViewModel`: `onDeviceContextLengths: StateFlow<Map<String,Long>>`; `syncOnDeviceContextLengths()` invocada en `refreshDownloadedModels()`; borrado (`deleteOnDeviceModel`) quita la entrada y persiste.
  - Panel tablet/PC muestra `context_length` real del archivo (`gguf:<id>`); Ollama igual (`/api/show`, `ollama:<nombre>`); si falta dato, «—». Claves unificadas con favoritos.
  - `ModelContextCacheTest` (5 tests puros) en verde; sin regresión (**896 tests, 38 fallos plataforma, 0 omitidos**); `assembleDebug` OK.
  - Historial `spec.md`: RF-241 reescrita.
  - **Verificación en dispositivo pendiente** del panel completo (tamaños/jerarquía).

## Ampliación 2026-11-19 — Context del modelo cargado

- [x] **T29. Test del rótulo doble (rojo).** RF-314, RF-315, RNF-205
  - Añadir tests a `app/src/test/java/com/example/ui/chat/ContextDetailTest.kt` para `loadedContextLabel`: `8192→"8.19k"`, `32000→"32k"`, `32768→"32.77k"`, `null/0→"—"`, `Long` grande acotado, constante del motor intacta.
  - Hecho cuando: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.chat.ContextDetailTest"` falla por `loadedContextLabel` no existir (rojo esperado).

- [x] **T30. `LoadedContextLabel` puro.** RF-314, RF-315, RNF-205
  - `ui/chat/ContextDetail.kt`: `data class LoadedContextLabel(engine, own)` + `loadedContextLabel(engineWindowTokens: Int, ownContextTokens: Long?)`, reutilizando `formatTokensCompact`; `—` si dato ausente/≤0; `Long` acotado a `Int.MAX_VALUE`.
  - Hecho cuando: `ContextDetailTest` verde (sin Robolectric).

- [x] **T31. Panel: celda del modelo activo.** RF-314, RF-315
  - `ModelManagerPanel.kt`: `isEngineActive` en filas locales visibles (ON_DEVICE + seleccionado + `READY`); `stringResource(models_context_split, …)` con las dos piezas; el resto de filas y Ollama con `formatTokens` actual.
  - Hecho cuando: `:app:compileDebugKotlin` verde.

- [x] **T32. Texto en 9 locales y verificación.** RNF-205
  - `models_context_split = "%1$s / %2$s"` en `values/`, `values-es-rVE/`, `values-pt-rBR/`, `values-fr/`, `values-it/`, `values-en-rGB/`, `values-es-rES/`, `values-pt-rPT/`, `values-b+es+419/`.
  - Hecho cuando: `assembleDebug` OK (AAPT2); `:app:compileDebugUnitTestKotlin --rerun` y tests puros verdes; `MEMORY.md`/`AGENTS.md` actualizados; verificación en dispositivo completada (2026-11-22).

**Resultado (2026-11-19)**: T29 rojo (`Unresolved reference 'loadedContextLabel'`) → T30 verde; `:app:assembleDebug` y `:app:testDebugUnitTest --tests "com.example.ui.chat.ContextDetailTest"` **BUILD SUCCESSFUL**; cierre con `:app:compileDebugUnitTestKotlin --rerun` + `--tests "com.example.ui.chat.*"` **BUILD SUCCESSFUL**. Únicos warnings, preexistentes y ajenos (CrashIssueUrl/EditorLogic, los dos `createComposeRule` de screenshot y `Locale` en `ModelSearchTest`). Verificación en dispositivo completada (2026-11-22): panel ≥600 dp con modelo cargado (`motor / modelo`), sin cargar y Ollama.

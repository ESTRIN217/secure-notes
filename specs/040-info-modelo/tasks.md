# Tareas 040 — Info del modelo on-device

Una tarea cada vez; tests primero (en rojo) donde aplique; `./gradlew test` al
cerrar; marcar y comprobar el «Hecho cuando» antes de pasar a la siguiente.

- [x] **T1. Catálogo `paramsLabel` + guardia.** RF-288
  - Test JVM (`ModelInfoCatalogTest.kt`): `MODEL_CATALOG` — `paramsLabel` no
    vacío en todas las entradas; sin duplicados de id.
  - Añadir `paramsLabel: String` (obligatorio) a `OnDeviceModel` y etiquetar
    las 15 entradas (360M, 0.5B, 0.6B, 1B, 1B, 1.7B, 1.5B, 1.5B, 1.7B, 2B,
    E2B, 2B, 3B, 3.8B, 3.8B).
  - Hecho cuando: test verde y `:app:compileDebugKotlin` pasa.

- [x] **T2. Piezas puras `ui/chat/ModelInfo.kt`.** RF-283, RF-284
  - Test JVM (`ModelInfoTest.kt`) en rojo: `modelDotState` y
    `modelEngineAction` con todas sus ramas.
  - Implementar enums y funciones; `ModelState` importado de `data.ai`.
  - Hecho cuando: tests verdes.

- [x] **T3. `AiViewModel`: template y borrado.** RF-290, RF-291
  - `readChatTemplate(model): String?` con `Dispatchers.IO`, caché
    `chatTemplateCache`, `GgufMetadataReader.create(skipKeys = …sin
    «tokenizer.chat_template»)`.
  - `ModelDownloader.deleteModel(model): Boolean` (archivo ausente o borrado).
  - `deleteOnDeviceModel` → `Boolean`: conserva flujo actual, purga
    `chatTemplateCache[model.id]`, devuelve si el archivo ya no existe.
  - Hecho cuando: `:app:compileDebugKotlin` pasa; sin llamadas nuevas al
    disco desde la UI.

- [x] **T4. Strings.** RNF-176, RNF-177 · RF-281
  - 7 nuevas × 9 locales (`values*/`): `chat_model_info_context_size`,
    `chat_model_info_parameters`, `chat_model_info_quantization`,
    `chat_model_info_chat_template`, `chat_model_info_template_unavailable`,
    `chat_model_info_copy_desc`, `ai_ondevice_delete_failed`.
  - Eliminar `chat_model_quant_line` y `chat_model_open_hf` (verificado: sin
    otros usos).
  - Hecho cuando: 9 `strings.xml` sincronizados y sin literales nuevos.

- [x] **T5. `ui/ModelInfoPanel.kt` (nuevo).** RF-281…RF-292
  - `ModelInfoState` plano; `ModelInfoContent`; envolturas ≥600 dp
    (**420 dp**, scrim, `BackHandler` que cierra solo el info) y <600 dp
    (`ModalBottomSheet` hermana).
  - Cabecera: `displayName` + chips (`paramsLabel`, `quantLabel`) + ✕; punto
    de estado (`modelDotState`); botón ciclo (`modelEngineAction`) + «Nueva
    conversación» (`chat_new`, deshabilitada sin archivo).
  - Fila «Model» (`models_col_model`): `huggingface.co/<repo>` + ` : <QUANT>` si
    quant no vacío; copiar = texto visible completo (`chat_model_info_copy_desc`).
  - Filas «Context Size» (caché `contextLengths["gguf:<id>"]` → `formatTokens`
    o «—»), «Parameters» (`paramsLabel`), «Quantization» (chip, omitir si
    vacío).
  - «Chat Template» plegable: expandir → `loadingTemplate` → `loadTemplate(model)`
    → pintar text o «No disponible»; colapsable de nuevo sin releer.
  - «Eliminar del disco» (rojo, solo con archivo descargado): `AlertDialog`
    (`models_delete_title/body/disk`) → `onDelete(): Boolean` → éxito: cerrar;
    fallo: fila de error inline en la vista.
  - Hecho cuando: se pinta en preview/compila; sin lecturas de disco al abrir
    (verificado por revisión de código); funciones ≤ 20 líneas.

- [x] **T6. Sheet y campo de entrada.** RF-281, RF-285
  - `ModelSelectorSheet`: `onShowInfo` obligatorio; ⓘ lo invoca; eliminados
    `ModelInfoDialog` y restos (`LocalUriHandler`, `chat_model_quant_line`,
    `chat_model_open_hf`).
  - `FloatingChatInputField`: `onShowModelInfo: (OnDeviceModel) -> Unit`
    (obligatorio) → `ModelSelectorSheet`; `modelSheetDismissTick: Int = 0` con
    `LaunchedEffect` que cierra la hoja; preview actualizada.
  - Hecho cuando: compila y preview sin errores.

- [x] **T7. Panel ≥600 dp.** RF-281, RF-292
  - `LocalModelRow`: ⓘ `IconButton` antes del ⋮ (no en vista «Ocultos»);
    `TableHeaderRow` y filas: Actions 56 → **96 dp**; `ModelManagerPanel` y
    `LocalModelRow` aceptan `onShowInfo`; encadenado.
  - Hecho cuando: compila; hidden view sin ⓘ; filas Ollama sin ⓘ.

- [x] **T8. `AiChatScreen.kt`.** RF-282, RF-284, RF-285, RF-290, RF-291
  - `infoModel: OnDeviceModel?`; `isLargeScreen` con `isLargeScreenWidth`;
    construye `ModelInfoState` con `remember` (sin disco).
  - Overlay hermano tras `ModelManagerPanel`: ≥600 dp → lateral; <600 dp →
    hoja hermana.
  - `onNewChat`: `selectOnDeviceModel` ∧ `setBackend(ON_DEVICE)` ∧
    `chatHistoryViewModel.createSession(ON_DEVICE, modelName = displayName)`
    ∧ `showModelPanel = false` ∧ `dismissTick++` ∧ `infoModel = null`.
  - `onDelete`: `viewModel.deleteOnDeviceModel(model)` → éxito cierra;
    fallo → el contenido muestra la fila de error.
  - `loadChatTemplate` = `viewModel::readChatTemplate`.
  - Hecho cuando: atrás cierra solo el info; el panel/hoja siguen detrás.

- [x] **T9. Validación y memoria.** Criterios de finalización
  - `./gradlew test`: sin fallos nuevos (mantener 38 de plataforma).
  - `grep -n "Text(\|contentDescription" ` de los archivos tocados: sin
    literales nuevos de UI.
  - Verificación en dispositivo: ⓘ en hoja y panel; cuatro estados del botón;
    «Nueva conversación»; copiar; chat template real; borrar con y sin éxito.
  - `MEMORY.md` actualizado (estado, decisiones, errores evitados).
  - Hecho cuando: spec `039/040` actualizada; `tasks.md` marcadas.

## Resultado de la validación (2026-11-16)

- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` → 962 tests, 39 fallos: 38 de plataforma
  (`DefaultNativeRuntimeLoader.java:212`, aarch64) + 1 preexistente de spec 038
  (`ShortcutInvariantsTest`: `MainActivity.kt:209-212` llama a `chatSessionDao.`;
  ajeno a esta spec). **Cero fallos nuevos**; los tests de la 040 verdes.
- `:editor-de-codigo:testDebugUnitTest` → 2 fallos, ambos de plataforma
  (Robolectric aarch64, módulo sin tocar).
- Pendiente (usuario, dispositivo): verificación en dispositivo de la spec 040.

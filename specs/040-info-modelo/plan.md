# Plan 040 — Info del modelo on-device (vista de detalles)

Implementación de `spec.md` (v2, aprobada). RF-281…RF-292 · RNF-174…RNF-178.
Sin dependencias nuevas; sin cambios de Room ni de prefs; lectura GGUF solo al
expandir «Chat Template».

## Archivos y responsabilidades

| Archivo | Responsabilidad |
|---|---|
| `app/src/main/java/com/example/data/ai/OnDeviceModel.kt` | `OnDeviceModel` gana `paramsLabel: String` (obligatoria, sin default) con los valores declarados (RF-288). `MODEL_CATALOG` completa sus 15 entradas. |
| `app/src/main/java/com/example/ui/chat/ModelInfo.kt` | **Nuevo**. Piezas puras: `ModelDotState` (LOADED/NOT_LOADED/LOADING) y `ModelEngineAction` (DOWNLOAD, DOWNLOADING_THIS, DOWNLOAD_BUSY, LOAD, LOADING, RETRY, UNLOAD). RF-283/RF-284. |
| `app/src/main/java/com/example/ui/chat/ModelInfoTest.kt` (o en `test/…/ui/chat/`) | **Nuevo**. Tests JVM de las dos funciones puras (rama a rama). |
| `app/src/main/java/com/example/data/ai/ModelDownloader.kt` | `deleteModel(model): Boolean` — devuelve si el archivo ya no existe (cubre archivo ausente). RF-291. |
| `app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt` | `readChatTemplate(model): String?` (Dispatchers.IO, caché `chatTemplateCache` por `model.id`, lector a medida sin skip de `tokenizer.chat_template`); `deleteOnDeviceModel` pasa a `Boolean` y purga la caché del template; **ninguna** otra llamada nueva. RF-290/RF-291. |
| `app/src/main/java/com/example/ui/ModelInfoPanel.kt` | **Nuevo**. `ModelInfoState` (plano), `ModelInfoContent`, envoltura ≥600 dp (420 dp lateral + scrim + `BackHandler`) y envoltura <600 dp (`ModalBottomSheet` hermana). Chips, punto de estado, dos botones (ciclo vía T2), fila Model con copiar, filas Context Size / Parameters / Quantization, Chat Template plegable (lectura perezosa, indicador, «No disponible»), «Eliminar del disco» en rojo + confirmación (`AlertDialog` con `models_delete_title/body/disk`), fila de error inline si el borrado falla. RF-281…RF-291. |
| `app/src/main/java/com/example/ui/ModelSelectorSheet.kt` | `onShowInfo: (OnDeviceModel) -> Unit` (nuevo, obligatorio); ⓘ de `OnDeviceRow` llama al callback; **eliminados** `ModelInfoDialog` y sus `LocalUriHandler`/imports. RF-281. |
| `app/src/main/java/com/example/ui/FloatingChatInputField.kt` | Nuevo parámetro `onShowModelInfo` (baja a `ModelSelectorSheet`) y `modelSheetDismissTick: Int = 0` (`LaunchedEffect` → cierra la hoja de selección; «Nueva conversación» desde ≥600 dp no tiene hoja, pero el tick cierra también el <600 dp). Preview actualizada. RF-281/RF-285. |
| `app/src/main/java/com/example/ui/ModelManagerPanel.kt` | `LocalModelRow` gana ⓘ `IconButton` **antes** del ⋮ (no en vista de «Ocultos»); `TableHeaderRow` y filas pasan Actions de 56 a **96 dp**; nuevo `onShowInfo` (obligatorio) y encadenado desde `AiChatScreen`. RF-281/RF-292. |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | Estado `infoModel: OnDeviceModel?`; `ModelInfoState` con `remember` (sin disco); montaje hermano tras el Scaffold (orden de declaración ⇒ BackHandler propio gana al del panel); `isLargeScreen` con `isLargeScreenWidth(LocalConfiguration.current.screenWidthDp)`; `onNewChat` (select ∧ ON_DEVICE ∧ `createSession(modelName = displayName)` ∧ cierra panel ∧ `tick++` ∧ cierra info); `onDelete` (éxito → cerrar + purgar; fallo → fila de error en la vista); `loadChatTemplate` = `viewModel::readChatTemplate`; `onShowModelInfo` a los dos montajes. RF-282/RF-284/RF-285/RF-290/RF-291. |
| `app/src/main/res/values*/strings.xml` (9) | +7 nuevas (`chat_model_info_context_size`, `chat_model_info_parameters`, `chat_model_info_quantization`, `chat_model_info_chat_template`, `chat_model_info_template_unavailable`, `chat_model_info_copy_desc`, `ai_ondevice_delete_failed`); **eliminadas** `chat_model_quant_line` y `chat_model_open_hf` (sin otros usos verificados). RNF-176/RNF-177. |

## Funciones puras (con «hoy» irrelevante: sin fecha)

- `modelDotState(isSelected: Boolean, engineState: ModelState): ModelDotState`
  → LOADED si `isSelected ∧ READY`; LOADING si `isSelected ∧ LOADING`; si no,
  NOT_LOADED (ERROR incluido).
- `modelEngineAction(isDownloaded, isDownloadingThis, isAnyDownloadActive,
  isSelected, engineState): ModelEngineAction`
  → tabla exacta del RF-284: !isDownloaded → (isDownloadingThis ? DOWNLOADING_THIS
  : if (isAnyDownloadActive) DOWNLOAD_BUSY else DOWNLOAD); isDownloaded ∧
  isSelected ∧ LOADING → LOADING; isDownloaded ∧ isSelected ∧ READY → UNLOAD;
  isDownloaded ∧ isSelected ∧ ERROR → RETRY; si no → LOAD.
- Estado del Chat Template: máquina local en `ModelInfoContent`
  (`sealed: Idle → Loading → Loaded(text)|Unavailable`); la caché vive en el VM.

## Algoritmo (pseudocódigo)

```
ⓘ (sheet|panel) → infoModel = model          // AiChatScreen, estado local
render: infoModel != null →
  isLargeScreen ? SidePanel(420.dp, scrim, BackHandler{infoModel=null})
                : ModalBottomSheet(hermana, onDismiss→cerrar solo info)
  ModelInfoContent(model, …):
    dot   = modelDotState(isSelected, engineState)
    action= modelEngineAction(…)               // LOAD/UNLOAD/RETRY/…
    onClick(action): LOAD/RETRY → select+onDevice+loadSelectedModel()
                     UNLOAD → unloadModel()    DOWNLOAD(_BUSY) → downloadOnDeviceModel(model)
    «Nueva conversación»: select+onDevice+createSession(modelName=displayName)
                          tick++; showModelPanel=false; infoModel=null
    fila Model: "huggingface.co/$repo" + (quant no vacío → " : $quant") + copiar(texto visible)
    Context Size: contextLengths["gguf:$id"]?.let(formatTokens) ?: "—"
    Parameters:   model.paramsLabel            Quantization: quant ?: sin fila
    Chat Template: expandir → Loading → vm.readChatTemplate(model) ?: Unavailable
    Eliminar: solo si downloadedIds ∋ id → confirm → vm.deleteOnDeviceModel(id)
              ? (éxito → cerrar) : (fallo → fila error inline, no cerrar)
```

`readChatTemplate(model)`:
```
chatTemplateCache[model.id]?.let { return it }        // memoria
path = withContext(IO) { modelDownloader.getModelPath(model) } ?: return null
reader = GgufMetadataReader.create(skipKeys = SKIP_SIN_CHAT_TEMPLATE)  // conserva chat_template
template = reader.readStructuredMetadata(FileInputStream(path)).tokenizer?.chatTemplate
  ?.takeIf { it.isNotBlank() }
chatTemplateCache[model.id] = template                 // solo si no nulo
return template
```
`deleteOnDeviceModel(model)`: borra vía `modelDownloader.deleteModel(model)`
(Boolean), purga `chatTemplateCache[model.id]`, y devuelve `true` si el archivo
ya no existe; el resto del flujo actual (selección activa, prefs, caché de
contextos) se conserva.

## Decisiones y alternativas descartadas

- **«Nueva conversación» en ≥600 dp no tiene hoja que cerrar** (vive dentro del
  input) ⇒ el tick cierra también la hoja en <600 dp; en ≥600 dp basta
  `showModelPanel=false`. Alternativa descartada: RF-285 cambiaría para
  permitir-la hoja abierta detrás — peor UX y viola RF-285 ya aprobado.
- **BackHandler del info sin gestionar el panel**: la vista de detalles se
  declara **después** del panel en `AiChatScreen` ⇒ su `BackHandler` gana por
  orden de registro (Compose entrega al registrado más reciente). En <600 dp la
  hoja cierra por su `onDismissRequest` nativo. Alternativa descartada:
  `enabled` condicionado a «info abierto» de forma global — más frágil.
- **`errorMessage` inline en la vista** (no chat): el VM ya lo publica pero el
  chat nunca lo pinta; crear un banner nuevo en el chat = alcance fuera de la
  spec. La fila inline satisface «banner de error del ViewModel» sin tocar el
  chat.
- **`deleteOnDeviceModel` → Boolean**: el panel de gestión lo llama sin usar el
  retorno (lambda que ignora Unit→Boolean sigue compilando). Alternativa
  descartada: segundo método solo para el info — duplicaría la lógica de
  purga de contexto/selección.
- **`ModelInfoState` plano, no un StateFlow nuevo de motor**: el plano ya tiene
  descargados/estado/descarga; la vista deriva con `remember`. Cero estado
  global nuevo (RNF-178 por construcción).
- **`paramsLabel` como campo obligatorio de `OnDeviceModel`**: imposible olvidar
  uno en el catálogo (el compilador obliga). Alternativa descartada: regex
  sobre `displayName` (falla en «Phi-3.5 Mini») y lectura GGUF en apertura
  (viola RNF-178).
- **Sin cambios en Room/prefs**; la caché del template es solo memoria.
- **Nota «/props» y chip «Instruct»**: omitidos (spec). Strings `…_quant_line`
  / `…_open_hf` eliminadas (sin otros usos verificados).

## Estrategia de tests

`./gradlew test` — baseline del host aarch64: 38 fallos de plataforma; el
criterio es no añadir ningún fallo nuevo.
- Tests nuevos en `:app` JVM puro (`ui/chat/ModelInfoTest.kt`): cada rama de
  `modelDotState` (READY+activo, LOADING+activo, ERROR+activo, inactivo READY,
  inactivo) y de `modelEngineAction` (sin archivo / descargando este /
  descarga ajena / no cargado / ERROR / activo LOADING / activo READY).
- Catálogo: test puro de que `MODEL_CATALOG` tiene `paramsLabel` no vacío y
  `quantLabel` parseable para los nombres actuales (guardia de catálogo).
- El resto (Compose, GGUF, clipboard) no es testeable en esta plataforma; la
  verificación es dispositivo + criterios de la spec.

| Parte | Cubre RF |
|---|---|
| T1 catálogo `paramsLabel` | RF-288 |
| T2 piezas puras + tests | RF-283, RF-284 |
| T3 VM (template, borrado) | RF-290, RF-291 |
| T4 strings 9 locales | RF-281, RNF-176, RNF-177 |
| T5 `ModelInfoPanel.kt` | RF-281…RF-292 |
| T6 sheet + FloatingChatInputField | RF-281, RF-285 |
| T7 panel ≥600 dp | RF-281, RF-292 |
| T8 `AiChatScreen` | RF-282, RF-284, RF-285, RF-290, RF-291 |
| T9 validación | criterios de finalización |

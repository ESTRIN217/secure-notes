# Plan 033 — Razonamiento y modelo en el chat

Spec: `specs/033-razonamiento-y-modelo-chat/spec.md` (aprobada 2026-11-11, cambios tras clarificación).
Cobertura global: RF-215…RF-228 · RNF-140…RNF-145.

## Archivos y responsabilidades

| Archivo | Responsabilidad | RF |
|---|---|---|
| `data/ai/ReasoningEffort.kt` **(nuevo)** | `enum class ReasoningEffort { DEFAULT, OFF, LOW, MEDIUM, HIGH, MAX }` + `reasoningMaxTokens(pref: Int, effort: ReasoningEffort): Int` + `reasoningThink(effort: ReasoningEffort): Boolean?`. **Puro**, sin android/Compose. | RF-218, RF-219, RNF-140 |
| `ui/chat/ScreenWidth.kt` **(nuevo)** | `isLargeScreenWidth(widthDp: Int): Boolean = widthDp >= 600`. **Puro**. | RF-215, RF-223, RNF-140, RNF-144 |
| `data/ai/AiModels.kt` | `AiRequest` gana `val think: Boolean? = null` (ausente en serialización si null). | RF-219 |
| `data/ai/OllamaService.kt` | `buildChatRequest`: `if (request.think != null) jsonBody.put("think", request.think)` **top-level** (hermano de `model`/`messages`/`options`). `num_predict` sigue igual (`-1` = infinito). | RF-219 |
| `data/ai/LlamaCppEngine.kt` | `val predict = request.maxTokens.takeIf { it > 0 } ?: 1_000_000` → `engine.sendUserPrompt(user, predict)` (ambos sitios). | RF-218, RNF-143 |
| `ui/viewmodel/AiViewModel.kt` | `_reasoningEffort = MutableStateFlow(ReasoningEffort.DEFAULT)`; `val reasoningEffort: StateFlow<ReasoningEffort>`; `fun setReasoningEffort(e)`. En `execute()`: enriquece con `maxTokens = reasoningMaxTokens(_maxTokens.value, _reasoningEffort.value)` y `think = reasoningThink(_reasoningEffort.value)`. En `selectOnDeviceModel`: antes de fijar, `aiModelHost.releaseAll("model-changed")` si el modelo elegido difiere del actual — vía oficial de «soltar pesos» (como descarga/borrado). | RF-218..221, RF-224, RF-226 |
| `ui/ReasoningEffortSheet.kt` **(nuevo)** | `ReasoningEffortSheet(current: ReasoningEffort, maxTokensPref: Int, onSelect: (ReasoningEffort) -> Unit, onDismiss: () -> Unit)`: dueña de su `ModalBottomSheet` (patrón `AttachmentAiSheet`); 6 filas en orden Default→Max; icono `Check` en la seleccionada; subtítulo con `%1$d` (`Máx. N tokens`) / clave «Ilimitado» / «Usa el ajuste de la app (máx. %1$d t)» con el valor actual. | RF-215, RF-217, RNF-142, RNF-145 |
| `ui/FloatingChatInputField.kt` | Campo de texto en **línea propia encima** de la fila (`fillMaxWidth()`). Fila debajo: adjuntar (+), esfuerzo, modelo, enviar/detener; enviar anclado a la derecha con `Spacer(weight=1f)` entre esfuerzo y modelo. Nuevos parámetros: `reasoningEffort`, `onReasoningEffortChange`, `maxTokensPref`, `modelSelector: ModelSelectorState`, `onSelectOnDevice`, `onSelectOllama`, `onTestConnection`. Botón esfuerzo (Lightbulb + etiqueta corta si ≠ DEFAULT): compacto `<600 dp` → `ReasoningEffortSheet` montada aquí; `>=600 dp` → `DropdownMenu` anclado (6 `DropdownMenuItem` con check). Botón modelo (`Memory`-icon + `Text(nombre o «Sin modelo», maxLines=1, ellipsis)` + `KeyboardArrowDown`; `enabled` siempre): sheet <600 dp / `ModelSelectorMenu` anclado ≥600 dp, montados en el `Box` junto al botón. `@Preview` vacía eliminada. | RF-215..217, RF-222, RF-225, RF-227, RNF-144/145 |
| `ui/ModelSelectorSheet.kt` **(nuevo)** | `data class ModelSelectorState(...)` + `ModelSelectorSheet(state, …)` (ModalBottomSheet, <600 dp) + `ModelSelectorMenu(expanded, …)` (DropdownMenu, ≥600 dp, mismas filas). Se monta **en el campo**, junto al botón (geometría de `DropdownMenu`). | RF-223, RF-224 |
| `ui/AiChatScreen.kt` | Recolecta `maxTokens`, `reasoningEffort`, `connectionState`, `downloadedModelIds`; calcula `downloadedModels` (catálogo ∩ disco) y `ollamaModels` (de `ConnectionState.Connected`); `displayModelName` (fallback «Sin modelo»); pasa `reasoningEffort`/`onReasoningEffortChange`/`maxTokensPref`/`modelSelector`/`onSelectOnDevice`/`onSelectOllama`/`onTestConnection` al campo. | RF-222..226, RF-228 |
| `res/values*/strings.xml` ×9 | Claves nuevas (ver inventario). | RNF-142 |
| (el esfuerzo→clave de texto vive en el Composable con `when`, precedente `ChatHint`) | | |
| Tests | `test/.../data/ai/ReasoningEffortTest.kt`, `test/.../ui/chat/ScreenWidthTest.kt` (bordes 599/600, mapeos). | RNF-140 |

## Funciones puras (nuevas)

```kotlin
enum class ReasoningEffort { DEFAULT, OFF, LOW, MEDIUM, HIGH, MAX }

fun reasoningMaxTokens(pref: Int, effort: ReasoningEffort): Int = when (effort) {
    DEFAULT, OFF -> pref
    LOW -> 512; MEDIUM -> 2048; HIGH -> 8192
    MAX -> -1
}

fun reasoningThink(effort: ReasoningEffort): Boolean? = when (effort) {
    DEFAULT -> null      // campo ausente en /api/chat
    OFF -> false
    LOW, MEDIUM, HIGH, MAX -> true
}

fun isLargeScreenWidth(widthDp: Int): Boolean = widthDp >= 600
```

## Interfaz (firma del campo, parcial)

```kotlin
fun FloatingChatInputField(
    /* …existentes… */,
    reasoningEffort: ReasoningEffort,
    onReasoningEffortChange: (ReasoningEffort) -> Unit,
    maxTokensPref: Int,
    modelSelector: ModelSelectorState,   // plano; "" en modelName → «Sin modelo»
    onSelectOnDevice: (OnDeviceModel) -> Unit,
    onSelectOllama: (String) -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
)
```
El estado de apertura de hoja/menú vive **dentro** del campo (`remember`), como el pulso del botón de enviar; tanto el selector de esfuerzo como el de modelo se montan aquí (geometría de `DropdownMenu`). La pantalla posee el estado (esfuerzo en `AiViewModel`, modelo en prefs) y pasa todo por parámetros; el campo no conoce el ViewModel.

## Algoritmo de enriquecimiento (AiViewModel.execute)

```
var enriched = request.copy(customSystemPrompt=…, temperature=…, topK=…, topP=…,
    repetitionPenalty=…,
    maxTokens = reasoningMaxTokens(_maxTokens.value, _reasoningEffort.value),
    think     = reasoningThink(_reasoningEffort.value))
// in-place (summarize/FIX_GRAMMAR in-place) queda como está: coerceAtMost(512) (RF-220)
// …resto igual (historial, adjuntos, persistencia)
```

## Decisiones (y alternativas descartadas)

1. **Piezas puras en `data/ai` / `ui/chat`** en vez de en el Composable → testeo JVM sin Robolectric (constitución #3; regla de spec 032). Alt: `internal fun` en el archivo del campo — descartada (no testeable aislada).
2. **`think` top-level** en vez de dentro de `options` → Ollama lo lee; en `options` se ignora en silencio (verificado en spec/clarificación). Tests: mapeo puro; no hay test de red (ausentes hoy), se verifica en cierre manual con `adb logcat`/estado de conexión (criterio 1 de finalización).
3. **`num_predict = -1` tal cual para MAX** y tope 1 000 000 en llama.cpp → `int32` de `stop_generation_position`; RNF-143 lo fija.
4. **Esfuerzo global al proceso** (no pref, no Room, no por sesión) → RF-221; descartada persistencia por RF fuera de alcance.
5. **Hoja en archivo propio; menú inline en el botón** → la hoja se reutiliza conceptualmente; el menú es trivial y vive junto a su botón. Alt de menú como composable aparte — descartada por coste sin beneficio.
6. **Selector de modelo montado en el campo** (anclado al botón, como el de esfuerzo), en vez de en `AiChatScreen`: la geometría de `DropdownMenu` lo exige, y RNF-144 exige una sola lectura de `LocalConfiguration`. `AiChatScreen` sigue siendo el dueño de los datos y los callbacks (`ModelSelectorState` plano). Alt descartada: selector en pantalla — imposible anclar el menú al botón (decisión revisada tras implementar; la spec no cambia).
7. **Cambio on-device→on-device**: `releaseAll` en el host al seleccionar modelo distinto (vía oficial de descargar/borrar), carga perezosa por `ensureReady()` en la siguiente petición; `LOADING` ⇒ petición falla rápido con «load in progress» (error existente). Alt: recargar al instante — descartada (pesos en hilo de UI; rompe AiModelHost).
8. **`loadSession` sin cambios** (RF-228 declarado). Alt: tocar `ChatSessionEntity`/snapshot — fuera de alcance y exige migración destructiva.
9. **HU por spec** (HU-1/HU-2) y baseline de tests 868/38 de la spec 032.

## Estrategia de tests

- `ReasoningEffortTest`: 6 valores de cada mapeo; `DEFAULT/OFF → pref` (pasando 256 y otro valor); `MAX → -1`; `think: null/false/true×4`.
- `ScreenWidthTest`: `599 → false`, `600 → true`, `840 → true`.
- `./gradlew test` en este host (`linux/aarch64`): baseline **868 tests, 38 fallos de plataforma, 0 omitidos**; los nuevos tests son puros y pasan aquí. `:app:compileDebugKotlin` debe quedar verde (recursos AAPT2 incluidos en el build).
- Invariante de fuentes existente sigue verde (literales/9 locales); la sync de 9 `strings.xml` es parte del mismo commit.

## Riesgos

- **Desborde a 320 dp**: modelo con elipsis primero; etiquetas de esfuerzo cortas no se recortan (caso límite 6). Verificación manual en ancho mínimo.
- **`releaseAll` al cambiar modelo on-device**: libera también si estaba cargado (correcto); si el usuario selecciona el **mismo** modelo (descarga reciente), no se libera (condición `selected != nuevo`).
- **`/api/tags` cacheado desactualizado**: se aceptan datos stale por decisión de la spec (RF-223); refresco solo vía «Probar conexión» o Ajustes.
- **El PR incluye el stub no commiteado**: `git diff` final debe dejar el archivo limpio (RF-227).

## Mapeo RF → parte

RF-215/217 → ReasoningEffortSheet + botón compacto/menú; RF-216 → botón; RF-218/219/221 → ReasoningEffort.kt + AiViewModel.execute; RF-220 → no tocar in-place; RF-222 → botón modelo; RF-223/224 → ModelSelectorSheet + AiChatScreen wiring; RF-225 → FloatingChatInputField layout; RF-226 → AiChatScreen + release en selectOnDeviceModel; RF-227 → limpieza del stub; RF-228 → sin cambios (solo documentado).

## Inventario de claves nuevas (strings.xml ×9)

`chat_effort_title` · `chat_effort_default` · `chat_effort_off` · `chat_effort_low` · `chat_effort_medium` · `chat_effort_high` · `chat_effort_max` · `chat_effort_tokens` (`Máx. %1$d tokens`) · `chat_effort_unlimited` («Ilimitado») · `chat_effort_app_setting` (`Usa el ajuste de la app (máx. %1$d t)`) · `chat_effort_off_desc` (`Sin pensamiento; tope de la app (máx. %1$d t)`) · `chat_effort_content_desc` (`Nivel de razonamiento: %1$s`) · `chat_model_none` («Sin modelo») · `chat_model_selector_title` · `chat_model_on_device` (sección) · `chat_model_ollama` (sección) · `chat_model_none_downloaded` · `chat_model_test_connection` · `chat_model_testing` · `chat_model_picker_desc` (`Cambiar modelo, actual: %1$s`).

## Ampliación 2026-11-11 — Búsqueda y acciones por modelo (RF-229…RF-237, RNF-146)

| Archivo | Responsabilidad | RF |
|---|---|---|
| `ui/chat/ModelSearch.kt` **(nuevo)** | `queryMatches`, `filterOnDeviceModels`, `filterOllamaModels`, `favoritesFirst`, `favoriteOnDeviceKey`/`favoriteOllamaKey`. **Puro**. | RF-229, RF-233, RNF-146 |
| `ui/chat/ModelSearchTest.kt` **(nuevo)** | 12 tests JVM de filtro/orden/claves. | RNF-146 |
| `Constants.kt` | `AI_FAVORITE_MODELS_KEY = "ai_favorite_models"`. | RF-233 |
| `data/backup/BackupSettings.kt` | `+1` ajuste TEXT (`ai_favorite_models`, por defecto `""`) → **quince**; viaja con la copia. | RF-233 |
| `data/backup/BackupSettingsTest.kt` | Conteos 14→15 y fixtures actualizados para que el invariante siga verde. | RF-233 |
| `PreferencesRepository.kt` / `SharedPreferencesRepository.kt` | `getFavoriteModels()`/`setFavoriteModels(Set<String>)` (CSV `\n`). | RF-233 |
| `ui/viewmodel/AiViewModel.kt` | `favoriteModels` y `downloadingModelId` (StateFlows); `toggleFavoriteModel(key)`; `downloadOnDeviceModel(model)` (no toca la selección); `deleteOnDeviceModel(model)` (release vía `AiModelHost` + limpia pref si era el seleccionado). | RF-230..232, RF-237 |
| `ui/ModelSelectorSheet.kt` **(rehecho)** | Estado `ModelSelectorState` ampliado (`downloadedIds`, `catalogModels`, `favorites`, `downloadingModelId`, `downloadState`); **barra de búsqueda** en hoja y **menú ≥600 dp** (primer ítem; `TextField` 268 dp); **catálogo completo** con orden favoritos-primero (remember por query+favorites); filas on-device: selección por tap (si descargado) / descarga si no; 3 iconos — `Star`/`StarBorder` (`#FFB300` al marcar) favorito, `Info` (diálogo con quantización/RAM/origen + «Abrir en Hugging Face» vía `LocalUriHandler`), control de **ciclo de vida** `Download`/`PlayArrow`/`Stop`/spinner (RF-231): sin archivo→descarga; en disco y no cargado→cargar (select+backend+`loadSelectedModel()`); cargado→`unloadModel()` sin confirmar; `LOADING`→spinner deshabilitado. El borrado del `.gguf` vive solo en Ajustes → IA; confirmación de borrado `AlertDialog`; filas Ollama con solo favorito (`ollama:<nombre>`); menú ≥600 dp compacto (targets 32 dp, Check de selección, mismo comportamiento). | RF-229..236 |
| `ui/FloatingChatInputField.kt` | 3 callbacks nuevos (`onToggleFavoriteModel`, `onDownloadModel`, `onDeleteModel`) pasados al selector; layout intacto. | RF-231 |
| `ui/AiChatScreen.kt` | Recolecta `favoriteModels`, `downloadingModelId`, `downloadState`; pasa `MODEL_CATALOG` completo; wiring de los 3 callbacks. | RF-230..235 |
| `res/values*/strings.xml` ×9 | 10 claves nuevas (`chat_model_search_hint`, `chat_model_no_results`, `chat_model_info_desc`, `chat_model_favorite_desc`, `chat_model_download_desc`, `chat_model_delete_desc`, `chat_model_open_hf`, `chat_model_quant_line`, `chat_model_delete_title`, `chat_model_delete_body`). | RF-236 |

Decisiones: búsqueda client-side sin red (sobre `MODEL_CATALOG` y la caché de `/api/tags`); favoritos vía prefs CSV (sin tabla Room); favoritos también en Ollama (`ollama:<nombre>`) y viajan en la copia; borrado con confirmación; el menú se mantiene en ≥600 dp por decisión explícita del usuario tras exponer el problema con búsqueda+iconos; progreso por fila con una única descarga a la vez; info abre la página HF con `LocalUriHandler`.


## Corrección 2026-11-12 — upload/download = cargar/descargar en memoria (como Ajustes)

RF-231/RF-232/RF-236/RF-237 reescritos (ver spec «Cambio 2026-11-11» y «Historial»). Cambios de implementación: `ModelSelectorState.onDeviceModelState` (del host), filas con **control de ciclo de vida** (`Download` → `Download`; `NOT_LOADED`/`ERROR` → `PlayArrow`; `READY`+seleccionado → `Stop`; `LOADING` → spinner deshabilitado); el icono `Delete` y su diálogo (`DeleteModelDialog`) eliminados por completo; claves `chat_model_delete_*` retiradas de los 9 locales; nuevas `chat_model_load_desc`/`chat_model_unload_desc`; `deleteOnDeviceModel` retirado. `./gradlew test` → 890/38 (plataforma) sin regresión; `assembleDebug` OK.

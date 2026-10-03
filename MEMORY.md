# MEMORY.md — secure-notes 
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no 
aporte. 
## Estado actual 
- **Bloque tabla rediseñado** (2026-09-30, `:app:assembleDebug` OK, sin verificación visual) —
  casi todo en `ui/EditableTableBlock.kt`:
  - Fuera los `+` de fila/columna. Esas acciones viven en la hoja `⋮`, y ahora `activeRow`/`activeCol`
    caen a 0: con los `+` fuera, la hoja es la única vía y sus acciones no salen muertas.
  - Barra flotante en su **propia franja** (`Column { AnimatedVisibility { HorizontalFloatingToolbar }; Card }`),
    ya no `align(TopEnd)` sobre el `Card`. MD3 Expressive.
  - Gutter de 32.dp **superpuesto** al borde izquierdo (`RowGutter`) en vez de reservado: la tabla
    usa todo el ancho. Solo con la tabla activa.
  - **Selección (7 defectos)**: `cellMoveFor` no se traga `←`/`→` ni `Shift+←/→`; `Tab` respeta
    `isShiftPressed`; `key(col)` + `DisposableEffect` para los `FocusRequester` (sin `key()` todas
    las celdas de un bucle compartían requester); foco a nivel de **tabla** con `onFocusChanged` en el
    contenedor (la carrera blur(A)/focus(B) borraba el estado); el reload externo conserva `selection`;
    fuera el `ResizeHandle` intra-celda y el `clickable` de cabecera que competían con el cursor.
  - Tokens MD3 en vez de literales fijos (`Color(0xFFE5E7EB)`, `Color.White`) que rompían tema oscuro.
  - `BlockSheetAction` gana `dividerBefore: Boolean = false` (retrocompatible) para agrupar la hoja.
- **Optimización de rendimiento implementada** (2026-09-30, `:app:assembleDebug` OK, sin 
  verificación visual en dispositivo):
  - `data/ai/AiModelHost.kt`: el motor on-device es *lazy* y *ref-counted*. El `init` de 
    `AiViewModel` ya **no** carga el modelo; se materializa en `onAiSurfaceOpened()` (3 
    pantallas de chat vía `Screen.AiSurfaceLifecycle`), `execute()`, `executeInPlace()` o el 
    botón "Load". Se libera al salir del asistente + 2 min de inactividad, en background 
    (rebote 1,5 s + `hasVisibleActivity()`), en `onCleared`, al borrar el modelo y con "Unload".
  - `perf/`: `DevicePerformanceProfile` (RAM/lowRam/refresh → tier, caché de Coil, `frameBudgetMs`),
    `DisplayRefreshRate` (`preferredDisplayModeId`), `FrameMetricsMonitor` (jank, solo DEBUG).
  - `ui/drawing/StrokeGeometryCache.kt`: `Path`/`Stroke` reutilizados en el lienzo; 
    `canvasSize` pasa a `onSizeChanged`; el lienzo va en una sola `graphicsLayer` con clip.
  - Listas con `key` + `contentType`; `SimpleDateFormat` cacheado (`util/CachedDateFormatters`); 
    `TokenPacer` limita el streaming a ~1 actualización por frame; Coil con cachés dimensionadas.
  - String nuevo `ai_ondevice_lazy_note` en los 9 locales.
- Sección "Rendimiento (reglas permanentes)" añadida a `AGENTS.md`: resumen de lo anterior.

## Decisiones (y por qué) 
- El host de IA vive en la `Application`, no en el ViewModel: `AiViewModel` se destruye al salir 
  de la Activity y el host debe sobrevivir; `onCleared` solo hace `releaseAll`, nunca "close".
- `NoModelService` como `currentService` de reserva: leer `currentService` nunca puede crear el 
  motor nativo por accidente.
- Descarga por **referencias + temporizador de inactividad (2 min)** en vez de al salir de la 
  pantalla: recargar pesos re-lee el GGUF del disco y el binding solo permite `setSystemPrompt` 
  una vez por carga.
- `AiSurfaceLifecycle` (LaunchedEffect + DisposableEffect) en vez de hooks dentro de 
  `AiChatScreen`: cubre las 3 rutas de entrada al asistente con un solo sitio.
- `contentType` como `Int` calculado a partir de la nota: el slot de reutilización solo depende de 
  la *forma* (fijada/imagen/fondo/has content), no del contenido.
- Overdraw del rail: `Card(containerColor = Transparent)` → `Box` (6 formas redondeadas que se 
  dibujaban sin aportar nada) y superficie del rail opaca en vez de `alpha = 0.25f`.
- Barra de la tabla en su **propia franja** (`Column` + `AnimatedVisibility`) y no flotando con 
  `align(TopEnd)` sobre el `Card`: una superficie superpuesta tapa celdas y roba ancho.
- Foco de un grupo (tabla, lista…) con `Modifier.onFocusChanged` en el **contenedor**, no con 
  "última celda enfocada": al saltar de A a B el orden blur/focus es indeterminado.
- `remember` dentro de un `for` **exige `key()`**: sin él todas las iteraciones comparten el 
  mismo slot de memoria (FocusRequester compartido, estado corrupto al reordenar).

## Aprendizajes y errores a evitar 
- **`compileDebugUnitTestKotlin` sigue ROTO** (preexistente): `PdfExporterTest.kt` y 
  `WidgetNotesTest.kt` usan firmas viejas de `PdfExporter` → ningún test unitario corre. 
  Arreglarlos es lo siguiente de mayor valor.
- Coil 3.6.3: `MemoryCache.Builder()` **no** lleva `Context`; el tamaño se fija con 
  `maxSizeBytes(...)` + `.build()`, y `Precision` solo tiene `EXACT`/`INEXACT` (no `AUTOMATIC`).
- `Window.OnFrameMetricsAvailableListener` es un SAM de **3** parámetros (window, metrics, dropCount).
- `ThreadLocal.get()` es platform type: usar `!!` o el compilador avisa.
- AAPT2 **rechaza apóstrofos crudos** en `strings.xml` (`Invalid unicode escape sequence`) → 
  escribir `\'`. Falla en `mergeDebugResources`, después de que Kotlin ya compiló bien.
- `LoadingIndicator` (material3 1.5.0-alpha28) necesita `ExperimentalMaterial3ExpressiveApi` y 
  **no tiene `strokeWidth`**.
- `settings.gradle.kts` apunta a `/root/llama.cpp/...` y hace `include(":lib")` **solo si 
  existe**: si el checkout falta, el build sigue sin IA nativa.
- material3 1.5.0-alpha28: `FloatingToolbarDefaults.containerShape` es **internal** (no se puede
  pasar a `HorizontalFloatingToolbar`, hay que usar su `shape` por defecto) y el `DragHandle`
  horizontal **no es público** (solo `VerticalDragHandle`). Sí existen `HorizontalFloatingToolbar`,
  `ButtonGroup` y `standardFloatingToolbarColors()`.
- `TextRange` vive en `androidx.compose.ui.text`, **no** en `androidx.compose.ui.text.input`.
- `AnimatedVisibility` **no** es un `ColumnScope`: `Modifier.align` dentro no compila, hay que
  envolver en un `Box(contentAlignment = …)`.
- Ojo al auditar keys de recursos: `R.string.add_row` / `add_column` quedan **sin uso** tras sacar
  los `+`. No son error de build, pero no volver a buscarlos esperando encontrarlos en el código.

## Próximos pasos 
- **Verificar en dispositivo la tabla** (única tarea sin comprobación visual):
  - `HorizontalFloatingToolbar` impone `containerSize = 56dp` mínimo. Si la franja queda demasiado 
    alta, degradar a `Surface(shape = FloatingToolbarDefaults…` no sirve (es internal) → usar 
    `RoundedCornerShape(20.dp)` + `secondaryContainer`, como en `EditableTableBlockGutterPreview`.
  - El `RowGutter` superpuesto depende de `headerHeightPx` / `rowHeightPx` medidos con 
    `onGloballyPositioned`. Comprobar alineación con `fit_to_content`, con la cabecera seleccionada 
    (crece al mostrar el handle `⋯`) y con tipografía del sistema grande.
  - Probar `Shift+←/→` dentro de una celda y `Tab` en la última celda: debe entrar en la celda, 
    no crear fila (la hoja `⋮` es la única vía para añadir).
- **Bug conocido, fuera del alcance de la tabla**: `BlockEditor.toRenderingBlock` (`BlockEditor.kt:1011`)
  tiene un stub para `BlockType.TABLE` que ignora `meta["table"]` → la tabla sale vacía en TTS y
  previsualización. Son ~4 líneas. La selección de `EditableTextBlock` también tiene defectos
  aparte (`pendingSelection` compartido entre bloques en `BlockEditor.kt:92`, checklist con
  `onSelectionChange = {}`): otra pasada.
- Verificar en dispositivo: 90/120 Hz (`adb shell dumpsys display | grep -i fps`), jank en logcat 
  con `FrameMetrics`, y que abrir/cerrar el asistente carga/libera el modelo (`logcat -s AiModelHost`).
- Arreglar `PdfExporterTest.kt` / `WidgetNotesTest.kt` para desbloquear `./gradlew test`.
- Opcional: `FrameMetricsMonitor` → estado visible en Settings (requiere strings en 9 locales).

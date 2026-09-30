# MEMORY.md — secure-notes 
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no 
aporte. 
## Estado actual 
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

## Próximos pasos 
- Verificar en dispositivo: 90/120 Hz (`adb shell dumpsys display | grep -i fps`), jank en logcat 
  con `FrameMetrics`, y que abrir/cerrar el asistente carga/libera el modelo (`logcat -s AiModelHost`).
- Arreglar `PdfExporterTest.kt` / `WidgetNotesTest.kt` para desbloquear `./gradlew test`.
- Opcional: `FrameMetricsMonitor` → estado visible en Settings (requiere strings en 9 locales).

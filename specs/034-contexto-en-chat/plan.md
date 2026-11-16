# Plan 034 — Contexto en el chat

Spec: `specs/034-contexto-en-chat/spec.md` (estado: aprobada 2026-11-15)
Depende de: spec 006 (RF-33, campo), spec 033 (RF-241 `context_length`, `connectionState`, panel)

## Archivos y responsabilidades

| Archivo | Responsabilidad | RFs |
|---|---|---|
| `app/src/main/java/com/example/ui/chat/ContextUsage.kt` (**nuevo**, puro, sin `android`) | Modelo + lógica pura: `estimateTokens`, `contextUsageFraction`, `isContextNearLimit`, `buildContextUsage`, constante `LLAMA_CPP_CONTEXT_TOKENS = 8192`, `data class ContextUsage` | RF-248…RF-251, RF-254…RF-257, RNF-148 |
| `app/src/main/java/com/example/data/ai/OllamaService.kt` | Capturar `prompt_eval_count` / `eval_count` del trozo final (`done: true`) y exponerlos en `lastContextUsage: StateFlow<OllamaContextUsage?>`; reset al iniciar cada petición; también en la ruta no-streaming `execute()` | RF-250, RF-252, RNF-152 |
| `app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt` | Nuevo `contextUsage: StateFlow<ContextUsage?>`; `refreshContextUsage()` suspend (IO); llamadas en: fin de streaming (éxito y error), `loadSession`, restaurar sesión histórica, `setBackend`, cambio de modelo, sesión nueva. Lectura del exacto solo con backend Ollama (`ollamaService.lastContextUsage.value`) | RF-248…RF-257 |
| `app/src/main/java/com/example/ui/FloatingChatInputField.kt` | Sustituir `CircularWavyProgressIndicator()` por el anillo determinado condicionado a `contextUsage != null`, con color según umbral y `contentDescription` localizado | RF-248…RF-257, RNF-149…RNF-151 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | Recoger `viewModel.contextUsage` y pasárselo al campo | RF-248 |
| `app/src/test/java/com/example/ui/chat/ContextUsageTest.kt` (**nuevo**) | Tests JVM puros de estimación, fracción, umbral y `buildContextUsage` | RNF-148 |
| `app/src/test/java/com/example/data/ai/OllamaContextUsageParser.kt`-equivalente | El parseo vive como función pura `parseOllamaContextUsage(json: String)` en un archivo puro (nuevo `data/ai/OllamaContextUsage.kt`) + test JVM puro. El servicio hace I/O y llama al parser | RF-250, RNF-148 |

Nota: el campo **no** conoce el ViewModel ni decide nada nuevo: recibe `contextUsage: ContextUsage?` como parámetro (RF-33, regla de la casa).

## Algoritmo

```
refreshContextUsage(sessionId):            // suspend, Dispatchers.IO
  window ← if (_backend == OLLAMA) {
             (connectionState.value as? Connected)?.contextLengths[_modelName.value]?.toInt()
           } else LLAMA_CPP_CONTEXT_TOKENS          // 8192, RF-253
  turns ← _conversationHistory.value[sessionId] ?: emptyList()
  exact ← if (_backend == OLLAMA) ollamaService.lastContextUsage.value else null
  used ← exact?.sum ?: estimateTokens(join(systemPrompt, getRelevantMemories(sessionId), turns))
  // turns incluyen content + files content (los adjuntos VIAJAN en el prompt)
  _contextUsage.value ← buildContextUsage(used, window, exact != null)   // window null → null (no indicador)

contextUsageFraction(used, window): clamp(used.coerceAtLeast(0) / window.coerceAtLeast(1), 0f, 1f)
isContextNearLimit(f): f >= 0.85f
estimateTokens(t): if empty 0 else ceil(t.length / 4.0).toInt()
```

- RF-252: durante la generación no se recalcula el anillo (arrancaría «respirando» con cada pacer); el valor cambia solo al terminar la respuesta.
- RF-256: al abrir sesión histórica el exacto no está disponible → estimado.
- RF-257: conversación vacía con prompt de sistema > 0 → posible fracción pequeña; si el sistema es vacío, 0 % (anillo vacío, no ocultado).
- Memoria: se recalcula sin memorias si `getRelevantMemories` falla; el valor sigue marcándose «estimado».
- Cálculo del exacto **no** se persiste (Fuera de alcance, spec 034).

## Interfaz

```kotlin
// ui/chat/ContextUsage.kt (puro)
const val LLAMA_CPP_CONTEXT_TOKENS = 8192
const val NEAR_LIMIT_FRACTION = 0.85f
data class ContextUsage(val usedTokens: Int, val windowTokens: Int, val exact: Boolean)
fun estimateTokens(text: String): Int
fun contextUsageFraction(used: Int, window: Int): Float
fun isContextNearLimit(fraction: Float): Boolean
fun buildContextUsage(usedTokens: Int, windowTokens: Int?, exact: Boolean): ContextUsage?

// data/ai/OllamaContextUsage.kt (puro)
data class OllamaContextUsage(val promptTokens: Int, val evalTokens: Int) { val sum: Int }
fun parseOllamaContextUsage(doneLineJson: String): OllamaContextUsage?
```

Codificación cableada: `OllamaService` guarda `lastContextUsage` y lo publica como `StateFlow`; el chat es su único consumidor.

## Decisiones con su alternativa descartada

- **Parser puro + `StateFlow` en el servicio, en lugar de cambiar `AIService.executeStreaming` a un objeto con metadatos.** Alternativa: hacer que todo el flujo emita `Token`/`Done` tipados. Descartada: tocaría la interfaz, `NoModelService`, el in-place (reescritura amplia para un detalle de chat); el consumidor del exacto es el chat, y `AiViewModel` ya recibe `ollamaService` directamente.
- **Reservar 8192 on-device, no `context_length` del GGUF.** El binding reserva 8192 de verdad (AGENTS.md, horneado upstream); mostrar el GGUF (p. ej. 32 768) mentiría por 4× sobre cuánto **cabe** en la conversación. El dato GGUF sigue visible donde ya estaba (panel de modelos, RF-241).
- **Sin dato de ventana → no se pinta el indicador.** Alternativa: indeterminado infinito (lo de hoy) o estimación sin denominador. Descartadas: indeterminado es ilegible y la estimación sin ventana no dice nada (RF-253).
- **Durante la generación se congela la fracción previa**, en vez de estimar con el texto parcial. Alternativa descartada: anillo que «respira» en cada tick del `TokenPacer` (ruido visual, cálculo en cada frame, RNF-152).
- **85 % como umbral** — mismo criterio visual que «acercarse al límite» sin ser aún rojo en la práctica.
- **Functiones puras en `ui/chat`** (convención de la casa) y parseo en archivo propio `data/ai` para que ambos tests sean JVM puros: en aarch64 no corren los tests con Robolectric, los puros sí.
- **El `contentDescription` se localiza** (9 locales + 1 más de `values/` = 10 ficheros, ojo), el anillo no muestra texto (RF-255).

## Estrategia de tests

`./gradlew :app:testDebugUnitTest --tests '*ContextUsage*' --tests '*OllamaContextUsage*'` — en verde antes de cablear la UI.

Casos (rojos primero): `estimateTokens("")=0`, `"abcd"→1`, `"abcde"→2`, truncación de histórico, `fraction` clamp en 0 y 1, ventana 0/negativa no lanza, `isNearLimit` en 0.84/0.85/1.0, `buildContextUsage(window=null)=null`, parser: línea `done` válida → suma; `done:false` → null; JSON roto → null; claves ausentes → null.

La UI (anillo + umbral de color) se verifica en dispositivo; Roborazzi no corre en aarch64.

## Riesgos

- El 8192 desincroniza si el binding upstream cambia el contexto: se fija con comentario y se anota en `MEMORY.md` (el binding es externo; la conexión está en AGENTS.md).
- `contextLengths` de Ollama puede estar vacío si `/api/show` falló en el test de conexión (ya se trata como «sin dato» → indicador oculto, aceptable por RF-253).
- El estimado sobre-estima el historial cuando hay tool-calls inyectados en `messages` (no están en los turnos guardados): el exacto Ollama los cubre en el siguiente turno; antes del exacto se prefiere estimar por abajo (solo turnos + adjuntos), nunca por encima.

## Cobertura de RFs

RF-248 (field) · RF-249/RF-252/RF-253/RF-256/RF-257 (VM + puras) · RF-250 (parser + servicio) · RF-251 (VM + `estimateTokens`) · RF-254 (umbral puro + color en campo) · RF-255 (strings 9 locales) · RNF-148 (4 blancos de test) · RNF-149/150/151 ( campo ) · RNF-152 (recalculo solo en puntos discretos) · RNF-152

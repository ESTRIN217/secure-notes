# Plan 035 — Carga de modelo en el chat

Spec: `specs/035-carga-modelo-chat/spec.md` (aprobada 2026-11-15)
Depende de: spec 006 (shell del chat), spec 033 (`onDeviceModelState`)

## Archivos y responsabilidades

| Archivo | Responsabilidad | RFs |
|---|---|---|
| `app/src/main/java/com/example/ui/chat/ModelLoading.kt` (**nuevo**, puro, sin `android`) | `shouldShowModelLoadingIndicator(backend, modelState)` y `shouldShowUnavailableError(available, hasHistory, modelLoading)` | RF-258, RF-259, RNF-153 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | Sustituir el stub `LinearWavyProgressIndicator()` por la barra condicionada; `showError` excluye LOADING; `ModelState` ya llega vía `onDeviceModelState` | RF-258, RF-259 |
| `app/src/test/java/com/example/ui/chat/ModelLoadingTest.kt` (**nuevo**) | Tests JVM puros de las dos funciones | RNF-153 |

## Algoritmo

```
shouldShowModelLoadingIndicator(backend, modelState):
    backend == ON_DEVICE && modelState == LOADING     // RF-258

shouldShowUnavailableError(modelLoading, available, historyEmpty):
    !modelLoading && !available && historyEmpty        // RF-259 (banner solo fuera de la carga)
```

Nota: el código actual exige conversación **vacía** para el banner; eso se conserva — la pureza
cubre solo la lógica que cambia.

## Interfaz

```kotlin
fun shouldShowModelLoadingIndicator(backend: AiBackend, modelState: ModelState): Boolean
fun shouldShowUnavailableError(modelLoading: Boolean, available: Boolean, historyEmpty: Boolean): Boolean
```

(`AiBackend`/`ModelState` son tipos de datos del proyecto sin dependencias Android: se usan en la firma.)

## Decisiones con su alternativa descartada

- **Wavy indeterminado en vez de valor real de carga**: el host no expone fracción de lectura de
  pesos; inventarla sería peor que honesto-indeterminado.
- **Suprimir el banner durante LOADING** en vez de convivir barra+banner: dos señales sobre el
  mismo hecho; el banner es mentira en ese estado (RF-259).
- **En Ollama la barra nunca aparece**: no hay estado local de carga que mostrar; mostrarla
  siempre sería indistinguible de un indicador decorativo (lo que RF-258 evita).
- **Sin texto**: la barra no añade string; el resto del comportamiento es el de la spec 033.

## Estrategia de tests

`./gradlew :app:testDebugUnitTest --tests '*ModelLoading*'` — rojo primero (las funciones nacen
como `TODO`), verde antes del cableado UI.

## Riesgos

- Si el host cambiara cuándo marca `LOADING` (p. ej. en `ensureReady`), la barra aparece tan pronto
  como la petición dispara la carga; es exactamente la ventana que se quiere cubrir.
- El usuario con Ollama no ve nunca esta barra ni el banner — su comportamiento exige conexión/error
  ya cubiertos por `errorMessage`/`isAvailable()`.

## Cobertura de RFs

RF-258 (pantalla + pura) · RF-259 (pantalla + pura) · RNF-153 (tests) · RNF-154 (sin deps, sin strings)

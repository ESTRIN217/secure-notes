# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **2026-11-16**: Spec **039** implementada y verificada en las suites dirigidas (pendiente verificación en dispositivo) — métricas reales on-device desde el motor (`specs/039-metricas-reales-ondevice/spec.md`, RF-277…RF-280). **Binding vendorizado en `lib/`** (misma fecha): `lib/` copia la fuente del binding con el patch (3 ficheros: `ai_chat.cpp`, `InferenceEngine.kt`, `InferenceEngineImpl.kt`, +97). El **motor** sigue externo en `/root/llama.cpp` y la build nativa apunta a esa absoluta; en un host sin él, falla a propósito (CI no compila `:app`). `lib/.cxx` y `lib/build` no se commitean (ignorados). **Pendiente**: verificación en dispositivo (tokens reales tras enviar, anillo con system prompt, ambos backends, históricos con métricas 036 en el turno).
- **2026-11-16**: Spec **038** implementada (pendiente verificación en dispositivo) — chats vacíos se descartan (`specs/038-chat-vacio-se-descarta/spec.md`, RF-273…RF-276 · RNF-165…RNF-169), análoga a spec 032. Al salir del chat sin turnos ni draft ni adjuntos → borrado en cascada, silencioso (`BackHandler(enabled = !drawerState.isOpen)` + `shouldDiscardChatSession`, `ui/chat/ChatEmptiness.kt`). Al destruir la Activity (`onDestroy`, sin `isChangingConfigurations`) → purga de sesiones creadas en el proceso (`createdAt >= processStartedAt`) con `messageCount == 0` (`ChatSessionDao.emptySessionIdsSince`). Guard de draft protege con texto tecleado o adjuntos; históricas intactas; sin Room schema; 6 tests puros verdes. **Pendiente**: verificación en dispositivo (5 puntos de la spec).
- **2026-11-15**: Spec **037** implementada y verificada en dispositivo (2026-11-15) — detalle de la ventana de contexto al tocar el anillo (`specs/037-detalle-contexto/spec.md`, RF-265…RF-272 · RNF-160…RNF-164): `OutlinedCard` 200×190 dp, esquinas 28 y borde, según el diseño del usuario. Tap en el anillo: hoja `<600 dp` / menú anclado `≥600 dp` (patrón esfuerzo); cierra con tap fuera/atrás. Contenido: «Ventana de contexto» + usado/ventana compacto (`2.05k`), `LinearWavyProgressIndicator` determinada, `% usado` y `restante`, sección «Token usage details» con ACROSS ALL TURNS (suma de `promptEvalCount`/`evalCount` de la conversación abierta), THIS TURN • KV CACHE (último par), KV cache total (`contextUsage.usedTokens`) y Avg speed ponderado (`ΣevalCount/ΣevalDuration`). Números con «~» on-device. `ContextUsage` del campo sustituido por `ContextDetail` (una fuente; `buildContextDetail` en `ui/chat/ContextDetail.kt`, puro); `ContextDetailCard.kt` en `ui/chat/`; strings en 9 locales; 10 tests puros nuevos en verde; builds y suites dirigidas verdes. **Pendiente**: verificación en dispositivo (tap abre/cierra en compacto y amplio, números coherentes, «~» on-device, históricos pre-036 con 0 tok).
- **2026-11-15**: Spec **036** implementada y verificada en dispositivo (2026-11-15) — metadatos reales por burbuja según la referencia de llama.cpp Android (`specs/036-metadatos-mensaje/spec.md`, RF-260…RF-264 · RNF-155…RNF-159): usuario lleva **métrica de prompt** (`prompt_eval_count`/`prompt_eval_duration`), asistente lleva **generación** (`eval_count`/`eval_duration`); tok/s derivado (`tokensPerSecond` puro); on-device estimado con «~» (chars÷4, `processingTimeMs` compartido); persistidos en `conversations` (4 columnas anulables, **MIGRATION_9_10** aditiva, versión 9→10 — histórico conservado); rowId del turno de usuario capturado vía `viewModelScope.async` y actualizado al completar (`updatePromptMetrics`); restore pinta las métricas guardadas. Eliminados los literales fijos «• 30 tokens»/«9,20 tok/s»; `chat_msg_tokens{,_approx}` y `chat_msg_tok_per_s{,_approx}` en los 9 locales. 6 tests puros de derivados en verde; build dirigido (`:app:compileDebugKotlin` vía test + `testDebugUnitTest`) verde. **T9 pendiente**: migrar BD en dispositivo (histórico previo abierto sin pérdidas), split prompt/gen en Ollama, «~» on-device, métricas tras restaurar sesión.
- **2026-11-15**: Spec **035** implementada y verificada en dispositivo (2026-11-15) — barra de carga del motor on-device en el chat (`specs/035-carga-modelo-chat/spec.md`, RF-258…259 · RNF-153…154): `LinearWavyProgressIndicator` indeterminado, ancho completo, solo con backend ON_DEVICE + `onDeviceModelState == LOADING`; banner `ai_model_not_available` suprimido durante la carga. Piezas puras en `ui/chat/ModelLoading.kt` (`shouldShowModelLoadingIndicator`, `shouldShowUnavailableError`) con tests JVM; 14 tests puros (034+035) en verde. Reemplaza el stub suelto `LinearWavyProgressIndicator()` que estaba siempre visible arriba del contenido.
- **2026-11-15**: Arreglo mínimo en `AiChatScreen.kt` (cambios del usuario sin commitear): tres `Spacer()` sin `modifier` (obligatorio en Compose) rompían la compilación. Los literales fijos de métricas se eliminaron en spec 036; el usuario decidió conservar `title = {}` y la etiqueta de IA vacía tal cual.
- **2026-11-15**: Spec **034** implementada y verificada en dispositivo (2026-11-15) — `specs/034-contexto-en-chat/{spec,plan,tasks}.md`, RF-248…RF-257 · RNF-148…RNF-152. El `CircularWavyProgressIndicator()` del pie del chat ya muestra ocupación del contexto: exacto por Ollama (`parseOllamaContextUsage` sobre el trozo `done`, `lastContextUsage: StateFlow`) o estimado chars÷4 (`ui/chat/ContextUsage.kt`, puro); ventana = 8192 fijo on-device / `context_length` de `/api/show` (sin dato → no se pinta, RF-253); anillo determinado, rojo a ≥85 %, sin texto visible, `contentDescription` en 9 locales. Cableado en `AiViewModel` (`contextUsage`, `refreshContextUsage(justServed)`: tras respuesta, `loadConversation`, sesión nueva, `setBackend`/`setModelName`/`selectOnDeviceModel`/`testConnection`); nunca por frame ni durante streaming (RF-252). 16 tests puros en verde; `:app:compileDebugKotlin` + `testDebugUnitTest` dirigido verde. **Anotado:** el 8192 es fijo upstream (desync si el binding cambia) y el exacto no se persiste (RF-256). T11 pendiente: verificación manual + decidir `AGENTS.md`.
  - Detalle Ollama: en la ruta con tool-calls, la segunda petición `execute()` sobre-escribe `lastContextUsage` con su propio conteo (más completo); gana el último.
- **2026-11-15**: `FloatingChatInputField.kt` — arreglo menor de disposición (RF-225): la fila de botones lleva `fillMaxWidth()` explícito, el botón de enviar pasa de 30 a **32.dp** (igual al chip de modelo/esfuerzo) y el `Spacer` modelo→enviar de 4 a **8.dp** (estaba «se tocaban» a ancho compacto). `CircularWavyProgressIndicator()` se mantiene sin cambios (decisión del usuario). `:app:compileDebugKotlin` verde; sin strings nuevas ni cambio de comportamiento.
- **2026-11-12**: `FloatingChatInputField.kt` — `ChatTextField` pasa de `TextField` (Material3) a `BasicTextField` (foundation 1.13.0-alpha01): sin contenedor ni indicador, placeholder por `decorationBox`, texto en `bodyLarge` y cursor `SolidColor(primary)`; en deshabilitado el texto se atenúa a `alpha 0.38f` porque `BasicTextField` no lo hace solo. Añadido `FloatingChatInputFieldPreview` (`@Preview(showBackground = true)`) con estado plausible. `:app:compileDebugKotlin` verde; sin strings nuevas ni cambio de comportamiento.
- **Spec 033 (2026-11-14), implementada + ampliaciones; falta verificación en dispositivo**: razonamiento y modelo en el chat IA (`specs/033-…/{spec,plan,tasks}.md`). RF-215…RF-247. Esfuerzo global al proceso en `AiViewModel` (botones esfuerzo y modelo: hoja <600 dp; ≥600 dp el modelo abre un **panel a pantalla completa**, hermano del Scaffold, réplica de «Ollama Models»: Models + subtítulo EN, búsqueda, tabla Model/Context/Status/Actions con power pulsable load/unload y ⋮ con Add/Remove favorites, Delete from disk, Hide model; favoritos corazón y ocultos `ai_hidden_models` persistidos en prefs e incluidos en la copia —**dieciséis** ajustes—; catálogo on-device completo; Context = **context_length del modelo**: on-device desde GGUF (`dimensions.context_length`, leído una vez por archivo vía `GgufMetadataReader` y cacheado en prefs `ai_model_context_lengths`, fuera de la copia); Ollama desde `/api/show` (memoria); «—» sin dato. `think` top-level booleano en `/api/chat`; MAX = `num_predict -1` (Ollama) / tope 1 000 000 (llama.cpp). `./gradlew test` → **896 tests, 38 fallos (plataforma), 0 omitidos**; `assembleDebug` OK. `loadSession` restaura snapshot (RF-228, declarado).
- Specs 016–031 implementadas; spec 027 verificada en dispositivo: las notas nuevas ya no se duplican.
- **Spec 032 (2026-11-11), cerrada y verificada en dispositivo**: la nota nueva vacía se descarta. `ui/DraftEmptiness.kt` (`isNoteEmpty`) decide «vacía»; una nota existente vaciada sí se guarda (`canPersistChanges`, RF-214); la fila creada en la sesión se borra definitivo al salir (`DraftWriteGate.createdRowId`); /page, Markdown, lista, «Nuevo dibujo» e histórico intactos. `./gradlew test` → **868 tests, 38 fallos (todos de plataforma)**.
- **Spec 031 (2026-10-18)**: sitio de marca estático en `public/` (HTML/CSS planos, sin JS ni CDN) — esa es la
  raíz de Firebase Hosting, la que crea `firebase init`. `firebase.json` con `cleanUrls` + caché y **sin**
  reescritura SPA (si la hubiera, `404.html` no se serviría nunca). Proyecto: `secure-notes-502213` en
  `.firebaserc`. **Publicado en https://notes-502213.web.app** (10 ficheros, 404 propio verificado con 404 real).
  Las imágenes viven **una sola vez**, en `public/assets/`, y el `README.md` apunta ahí: la carpeta `assets/` de la
  raíz se borró para que no haya dos copias que se desfasen. Para actualizar el sitio: editar `public/` y desplegar.
- **v4.0.0 publicada** (2026-10-18): tag `v4.0.0` + release en GitHub (Latest) con los APK debug y
  release `arm64-v8a`; `versionName = "4.0.0"` / `versionCode = 4`; `CHANGELOG.md` cerrado con fecha.
- Spec 031: **Acerca de (2026-11-10)**: tarjeta de llama.cpp con logo **local**
  (`res/drawable-nodpi/llama_cpp_icon_dark.png` / `_light.png`, 1200×1200, copiados de
  `/root/llama-brand/logo/png/icon/` — asset oficial de `ggml-org/llama.brand`, CC-BY-NC-4.0 con
  permiso explícito de redistribución en software para identificar Llama.cpp; atribución en
  `THIRD-PARTY-NOTICES` y espejo en `oss-licenses.json`). Glifo según el esquema real
  (`colorScheme.background.luminance()`), tarjeta clickable → repo `ggml-org/llama.cpp`; la versión ya no es un hardcode: `app/build.gradle.kts` parsea
  `LLAMA_VERSION_MAJOR/MINOR/PATCH` (+ `LLAMA_BUILD_IS_DEV`) del `CMakeLists.txt` del checkout →
  `BuildConfig.LLAMA_CPP_VERSION` (hoy `0.9.0-dev`; fallback `"local"` si no hay checkout → CI).
  El botón web del `DeveloperCard` usa `about_website` (9 locales) y los dos botones `weight(1f)`.
- Spec 028: los ajustes de tema viajan en la copia de seguridad; `BACKUP_SETTINGS` centraliza los valores.
- Specs 029-030: pruebas dirigidas de HTML/catálogo/PDF en **26/0** (PDF con backend de test; falta validar
  PDF real en dispositivo) y captura de bloqueo desde estado explícito, sin `NotesViewModel`, Room ni Keystore.
- Spec 002 implementada (2026-10-18): T1-T5 completadas; `./gradlew test` → **BUILD SUCCESSFUL**,
  con 842 tests, 0 fallos y 0 omitidos en `:app`. Goldens de ecuaciones y bloqueo verificados;
  `roborazzi.test.verify=true`. **Verificado en un host `x86_64` (codespace)**: en `linux/aarch64`
  (host de desarrollo) siguen los 38 fallos de plataforma de Robolectric.
- Suite completa dirigida más reciente: `:editor-de-codigo` **149/0**, `:visor-pdf` **4/0**, `:lib` **1/0**.
- Limpieza de código muerto (2026-10-17): 69 archivos, –3 482 líneas; build debug y compilación de tests
  validados correctamente.
- CI (2026-10-18): `app/build.gradle.kts` referenciaba `project(":lib")` sin condicionar mientras
  `settings.gradle.kts` sí condicionaba el `include`; en CI no existía el checkout y fallaba la
  configuración. Guardado con `findProject(":lib") != null`. Verificado con `./gradlew projects` sin el
  checkout: `BUILD SUCCESSFUL`.

## Decisiones que conviene recordar
- Las reglas permanentes sobre nota nueva, tema, `AiModelHost`, `CodeEditor`, markdown, almacenamiento,
  rendimiento y seguridad viven en `AGENTS.md`; no duplicarlas aquí.
- Antes de purgar `libs.versions.toml`, revisar el módulo externo `:lib`
  (`lib/build.gradle.kts` del binding vendorizado): consume datastore-preferences y
  junit-platform-launcher.
- Robolectric 4.17 en Java 25 necesita `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` en tareas
  `Test` que inicializan estado Android.
- El módulo externo `:lib` usa JUnit Platform/Jupiter; API y launcher no bastan, también hace falta Jupiter
  Engine cuando se incluye el módulo.
- `mutableIntStateOf` funciona en tests JVM puros; decisiones que no necesitan `Context` van en piezas puras.

## Aprendizajes
- Configuration cache funciona; usar `--no-configuration-cache` solo ante síntomas de caché/KSP obsoleta.
- Buscar código muerto con rutas acotadas; `grep -r` global recorre `build/` y puede agotar tiempo.
- Un `^` sin `RegexOption.MULTILINE` puede hacer que un invariante pase por vacío.
- Kotlin no admite referencias adelantadas a funciones locales; `Color` requiere `.toInt()` en asserts
  hexadecimales; compilar Kotlin no valida recursos XML.
- Un `include(":x")` condicional en `settings.gradle.kts` no basta: **toda** referencia `project(":x")` en
  cualquier otro build script tiene que usar la misma condición, o la configuración falla donde el módulo
  no existe. Comprobarlo con `./gradlew projects`, no con una compilación.

## Próximos pasos
- Validar PDF real en dispositivo; verificar spec 020 provocando un fallo real.

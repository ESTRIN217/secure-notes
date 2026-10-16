# Secure Notes — Agent Guide

## Build & Run

```bash
./gradlew assembleDebug          # debug APK (also signs with release config)
./gradlew assembleRelease        # release (needs key.properties)
./gradlew clean --no-configuration-cache  # if build acts stale (cache bug)
```

- Secrets Gradle Plugin configured to read `.env` (fallback `.env.example`), but **no secret is currently consumed** — `GEMINI_API_KEY` is a leftover; AI runs via Ollama or on-device llama.cpp, no API key.
- Release signing: copy `key.properties.template` → `key.properties`. Debug builds also sign with release config.
- `app/google-services.json` required for Firebase (Google services plugin).
- **Native AI (llama.cpp)**: uses the official Android binding from llama.cpp's `examples/llama.android/lib`
  (`com.arm.aichat.AiChat` facade), consumed as Gradle module `:lib`. `settings.gradle.kts` points at
  `/root/llama.cpp/examples/llama.android/lib` and **conditionally** does `include(":lib")` only if that
  directory exists (`if (llamaLibDir.exists())`) — if the checkout is missing the module is silently dropped
  and the build proceeds without native AI. Native code is compiled from source by `:lib`'s CMake during
  Gradle sync — no `CMakeLists.txt` or prebuilt `.so` in this repo. Build only supports `arm64-v8a`.

## Tests

```bash
./gradlew test                   # unit + Robolectric + Roborazzi
```

- **Unit tests**: `app/src/test/java/` — JUnit 4 + Robolectric + Roborazzi.
- **Screenshot tests** (Roborazzi) output to `app/src/test/screenshots/` and require `@GraphicsMode(GraphicsMode.Mode.NATIVE)` + `@Config(sdk = [36])` (sdk = targetSdk, not compileSdk).
- Room + Moshi use KSP — annotation processing changes require a clean build.
- No instrumented tests (`connectedCheck`) usable without a device/emulator.
- La tarea de pruebas **compila y ejecuta**: `:app:compileDebugUnitTestKotlin --rerun` → `BUILD SUCCESSFUL`
  (la nota que la daba por rota, por `PdfExporterTest` y `WidgetNotesTest`, era falsa: sus llamadas
  coinciden con las firmas reales). ⚠️ La suite **con entorno de pruebas no se ejecuta en esta plataforma**:
  `./gradlew test` falla con `355 tests completed, 41 failed` en `:app` (`16 completed, 2 failed` en
  `:editor-de-codigo`), y **40** de ellos son de plataforma —
  `java.lang.AssertionError at DefaultNativeRuntimeLoader.java:212`, porque **Robolectric no tiene runtime
  nativo para `linux/aarch64`**. Los **3** restantes son aserciones preexistentes y ajenas:
  `HtmlExportMediaTest.kt:117`, `OnDeviceModelSourceTest.kt:63` y una comparación `huggingface.co` vs
  `https://huggingface.co`. Para trabajar, vuelve a las **pruebas puras** (las que no dependen de
  Robolectric): sí corren aquí. Los dos únicos avisos del compilador en `src/test` son preexistentes y
  ajenos: `EquationScreenshotTest.kt:22` y `GreetingScreenshotTest.kt:22` (`createComposeRule` v1→v2).

## Architecture

Single-module Android app (`:app`). MVVM with Jetpack Compose (MD3 Expresive), Room, `StateFlow`.

| Layer | Path | Key files |
|---|---|---|
| UI / Navigation | `com.example.ui` | `MainListScreen.kt`, `NoteEditorScreen.kt`, `AiChatScreen.kt`, `DrawingCanvasScreen.kt`, `SearchScreen.kt`, `LockScreen.kt` (media viewer vive en `:visor-media`: `com.estrin217.visormedia.ui.MediaViewerScreen`) |
| Settings UI | `com.example.ui.settings` | `SettingsScreen.kt`, `PrivacySettingsScreen.kt`, `BackupRestoreScreen.kt`, `AiSettingsScreen.kt`, `StorageManagerScreen.kt`, `AboutScreen.kt`, `LegalInfoScreen.kt`, `LicensesScreen.kt`, `SettingsWidgets.kt` |
| ViewModel | `com.example.ui.viewmodel` | `NotesViewModel.kt`, `ThemeViewModel.kt`, `BackupViewModel.kt`, `UpdaterViewModel.kt`, `AiViewModel.kt`, `StorageViewModel.kt`, `ChatHistoryViewModel.kt` |
| Data (Room) | `com.example.data.local` | `NoteDatabase.kt`, `NoteDao.kt`, `TagDao.kt` |
| Model | `com.example.data.model` | `Note.kt` (incluye `Tag`), `DecryptedNote.kt`, `NoteContentBlock.kt`, `DataBlock.kt`, `UiState.kt`, `Attachment.kt`, `NavigationSection.kt` |
| Encryption | `com.example.data.security` | `CipherService.kt` (interface), `EncryptionServiceImpl.kt` (AES-256/GCM), `KeyDerivation.kt` (PBKDF2, 200K iterations) |
| AI | `com.example.data.ai` | `AIService.kt` (interface), `OllamaService.kt` (OkHttp, default `http://localhost:11434`), `OnDeviceService.kt` (wraps `LlamaCppEngine`, official llama.cpp `llama.android` binding), `OnDeviceModel.kt` (hardcoded `MODEL_CATALOG` + `filterForDevice`/`bestForDevice` + Hugging Face `sourceLabel`/`downloadUrl`/`sourcePageUrl`/`quantLabel`), `ModelDownloader.kt`, `DeviceInfo.kt`, `ToolRegistry.kt`, `MemoryManager.kt`, `tools/` (note tools for AI) |
| Sync | `com.example.data.sync` | `CloudSyncManager.kt` (interface), `GoogleDriveSyncService.kt` (OkHttp impl), `SyncWorker.kt` (WorkManager) |
| Preferences | `com.example.data` | `PreferencesRepository.kt` (interface), `SharedPreferencesRepository.kt` |
| Crash log | `com.example.crash` | `CrashReportText.kt`, `CrashIssueUrl.kt`, `CrashStore.kt` (**puros**, sin `android`), `CrashReporter.kt` (manejador de excepciones), `CrashNotifier.kt`; pantalla en `com.example.ui.crash.CrashReportScreen.kt` + `com.example.CrashReportActivity.kt` (**Activity propia**, fuera del Navigator) |
| Utils | `com.example.util` | `RichTextParser.kt`, `ExportUtils.kt`, `BiometricAuthManager.kt`, `OssLicenses.kt` (lee `assets/oss-licenses.json`), `export/` (Txt, Markdown, Pdf, Html, Json exporters) |
| Library `:editor-de-codigo` | `com.estrin217.editordecodigo` | `CodeEditor.kt` (facade), `ui/CodeEditorApp.kt`, `ui/CodeEditorViewModel.kt`, `ui/components/` (VS Code-style tabs, welcome screen, search/replace, quick-symbol bar), `syntax/`, `formatter/`, `utils/FileStorageManager.kt` |
| Library `:visor-pdf` | `com.estrin217.pdfviewer` | `PdfViewerScreen.kt`, `PdfViewerViewModel.kt` |
| Library `:visor-media` | `com.estrin217.visormedia` | `MediaViewerScreen.kt`, `Media3VideoPlayer.kt` |

**Entrypoint**: `com.example.MainActivity` (package `com.example`, applicationId `com.estrin217.securenotes`).

El editor de código es un módulo librería (`:editor-de-codigo`, paquete `com.estrin217.editordecodigo`, ~134 strings en 8 locales) invocado desde `com.example.CodeEditorActivity` y desde la pestaña `OpenTab.TextFile` de `NoteEditorTabsHost` (`CodeEditorApp(viewModel, topTabs)`). Su contenido vive en disco (`filesDir/code_files`, SAF opcional) y Room (`code_tools_db`, v2) solo guarda metadatos — el bump de versión usa arranque limpio (`fallbackToDestructiveMigration`).

## Key Conventions

### DI
- No DI framework. ViewModels constructed via `ViewModelProvider.Factory` in `MainActivity.kt` (NotesViewModel ~line 196, AiViewModel ~line 229).
- Dependencies (`NoteDatabase`, `CipherService`, `GoogleDriveSyncService`) created manually and injected through factory.
- `AiViewModel` created with `PreferencesRepository`, `OllamaService`, `OnDeviceService`, `ModelDownloader`, Room DAOs (`conversationDao`, `chatSessionDao`, `noteDao`, `memoryDao`) injected via factory.

### UI / State
- `StateFlow` + `collectAsStateWithLifecycle()` for reactive UI. No additional reactive libraries.
- `UiState` uses separate data classes (`AuthState`, `ListState`, `SyncState`) rather than sealed Loading/Success/Error.
- Screen navigation via `Navigator` class with `Screen` sealed hierarchy and `AnimatedContent` transitions.

### Conventions
- Functions ≤ 20 lines. Guard clauses over nested `if`/`else`. No nullable chains deeper than 2.
- No repository pattern (yet) — DAOs consumed directly by ViewModels.
- `object` singletons only for companion helpers (`KeyDerivation`, `RichTextParser`). No static access to dependencies.

### Error Handling
- Encryption: `Result<String>` return type from `CipherService`.
- Sync I/O: `Result<T>` from `SyncService` suspend functions.
- `Log.e()` at minimum in `catch` blocks — never silent swallowing.

## Editor Content Format

Editor is a Notion-style **block editor** (Notion-like). Note content is stored as a JSON list of `DataBlock` (`com.example.data.model.DataBlock`) with a `BlockType` enum: `TEXT`, `HEADING1-4`, `BULLET_LIST`, `NUMBERED_LIST`, `CHECKLIST_ITEM`, `QUOTE`, `CODE_BLOCK`, `CALLOUT`, `PAGE`, `PAGE_LINK`, `IMAGE`, `VIDEO`, `AUDIO`, `DRAWING`, `VOICE`, `FILE`, `BOOKMARK`, `TABLE`, `HORIZONTAL_RULE`, `COLLAPSIBLE`. Tables use `TableData` (headers/rows/weights, serialized to JSON in `meta`).

Each block's rich-text body still uses legacy HTML-like tags parsed by `RichTextParser`/`HtmlTagParser`:

| Tag | Effect |
|---|---|
| `<b>`, `<i>`, `<u>`, `<s>` | bold, italic, underline, strikethrough |
| `<color=#RRGGBB>`, `<bg=#RRGGBB>` | text / background color |
| `<url=URL>` | clickable link |
| `<h1>`–`<h3>`, `<normal>` | heading / normal size |
| `<sub>`, `<sup>` | subscript, superscript |
| `<font=family>`, `<size=N>` | font family / size in sp |
| `<item>`, `<ol>`/`<ul>`/`<li>` | checklist / lists |
| `<img>`, `<video>`, `<audio>`, `<hr>`, `<details>` | media / rules / collapsibles |

Markdown syntax also supported: `**bold**`, `*italic*`, `` `code` ``, `~~strikethrough~~`, `[text](url)`, `# headings`, `> quote`, `- [ ]` checklists.

Legacy flat-content notes are migrated to blocks via `DataBlock.migrateLegacyContent` — keep `fromLegacyHtml`/legacy regex handling working when changing `DataBlock`.

## Gotchas

- **Room**: `.fallbackToDestructiveMigration()` — schema changes destroy data silently.
- **Configuration cache**: `org.gradle.configuration-cache=true` in `gradle.properties`. Invalidate with `--no-configuration-cache` if build acts stale.
- **Kotlin**: `kotlin.incremental=false` in `gradle.properties`. Clean builds may be required after KSP changes.
- **Robolectric**: Unit tests use `@Config(sdk = [36])` (targetSdk). Screenshot tests require `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.
- **llama.android binding semantics** (`com.arm.aichat.internal.InferenceEngineImpl`):
  - Singleton JNI engine. `setSystemPrompt()` is allowed **once per model load** and clears KV cache + chat history.
  - Conversation context accumulates across requests sharing the same system prompt — there is no public
    per-request reset without reloading the model (weights re-read from disk).
  - `LlamaCppEngine` handles isolation: reloads on system-prompt change or non-`ModelReady` state; fast path otherwise.
  - **Never call `destroy()`** — its companion caches the singleton, so destroying poisons it for the rest of the process.
    `unload()`/`cleanUp()` is the only safe teardown.
  - Context size (8192) and sampling temp (0.3) are hardcoded upstream; sampling params in `AiRequest` are ignored.
  - Known limitation (documented-only): `maxTokens` can overshoot by up to ~user-prompt-length tokens —
    stop position double-counts user tokens (`ai_chat.cpp:447`).
- **Secrets**: Secrets plugin reads `.env` (gitignored) with fallback to `.env.example`.
- **Crash log** (`com.example.crash`, spec 020) — reglas permanentes:
  - **La delegación va fuera de todo `try`.** El manejador se registra y **después** llama al manejador que
    hubiera; delegar dentro de un `try`, o en un `finally`, deja de delegar si falla la escritura. Y la
    escritura es `try/catch (Throwable)`, **no** `runCatching`: `OutOfMemoryError` no es `Exception`.
  - **`CrashStore` recibe la `File`, no un `Context`.** Es lo que hace que escribir/leer/consumir se prueben
    con `TemporaryFolder` **sin Robolectric**, que aquí no se ejecuta. Quien sabe de `filesDir` es
    `CrashReporter`. Los tres ficheros puros **no** pueden importar `android` ni `androidx.compose`.
  - **El aviso de dentro de la app es el canal que siempre funciona.** Sin `POST_NOTIFICATIONS`, `notify()`
    es un **no-op silencioso** en Android 13+: avisar y no avisar serían lo mismo. Por eso `anunciar`
    comprueba el permiso, y el canal se crea **antes** de notificar (`notify()` sobre un canal inexistente no
    suena, no rompe). **Nunca** se pide el permiso al arrancar.
  - **Se consume lo que se lleva el informe, no lo que se mira.** Copiar y cerrar **no** consumen; compartir,
    enviar y descartar **sí**; y «Ver informe» **no** consume, porque la pantalla lee el fichero al abrirse.
  - **El informe no lleva datos del usuario, así que la pantalla vive FUERA del flujo de bloqueo** y es una
    `Activity` propia: no toca `NoteDatabase` ni `NotesViewModel` por construcción.
  - El aviso de privacidad va **siempre**, sin mirar el informe: el mensaje de la excepción es el único campo
    que la app no controla, y mirar para decidir si hay que avisar es mirar justo lo que no se debe.
- **MD3 Expressive opt-ins** (material3 `1.5.0-alpha28`):
  - `LoadingIndicator` requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` — `@ExperimentalMaterial3Api` alone is NOT enough.
  - Its signature is `LoadingIndicator(modifier, color, polygons)` — there is **no `strokeWidth` param** (that is `CircularProgressIndicator`'s).
  - `LinearWavyProgressIndicator(progress = { f }, modifier = …)` works for determinate download progress.
  - `Surface(onClick = …)` and `ElevatedCard`/`CardDefaults.elevatedCardColors()` are `ExperimentalMaterial3Api`.
- **AAPT2 rejects raw apostrophes** in `strings.xml` with `Invalid unicode escape sequence`. Always write `\'`
  (e.g. `l\'application`). This fails at `mergeDebugResources`, i.e. *after* Kotlin compiles cleanly — don't
  assume a green `compileDebugKotlin` means the resources are valid.
- **New string keys must be added to all 9 locales** at once — a missing translation is not a build error, it
  just falls back to `values/` at runtime.
- **Localizations**: `values/` (en), `values-es-rVE/` (es-VE), `values-pt-rBR/` (pt-BR), `values-fr/` (fr), `values-it/` (it), `values-en-rGB/`, `values-es-rES/`, `values-pt-rPT/`, `values-b+es+419/`.
- `compileSdk = 37`, `targetSdk = 36`, `minSdk = 33`.
- Gradle 9.7.0, AGP 9.3.1, Kotlin 2.4.10.

## Memoria 
- Al empezar, lee `MEMORY.md` para conocer el estado del proyecto y las decisiones 
tomadas. 
- Al terminar una tarea, actualízalo: estado actual, decisiones importantes (con su 
porqué) y errores a evitar. 
- Mantenlo breve (máximo ~50 líneas): resume o elimina lo que ya no aporte. 
- Si algo se convierte en una regla permanente, propón moverlo a `AGENTS.md` en lugar de 
dejarlo en la memoria. 
- No guardes nunca datos sensibles (claves, tokens, datos personales). 

## Rendimiento (reglas permanentes)
- **El motor de IA solo se materializa a través de `AiModelHost`** (`data/ai/AiModelHost.kt`,
  propiedad de `SecureNotesApplication`). Prohibido instanciar `LlamaCppEngine` /
  `OnDeviceService` en otro sitio: el host crea el grafo en el primer `acquire()` y solo
  lee los pesos en `ensureReady()`. `AiViewModel` no debe volver a llamar a `loadModel()` en su
  `init` (eso fue el bug de arranque eager que se eliminó).
- Puntos de entrada del modelo: `onAiSurfaceOpened()` / `onAiSurfaceClosed()` (los llama
  `Screen.render` vía `AiSurfaceLifecycle`) y `ensureOnDeviceReady()` desde `execute` /
  `executeInPlace`. Descarga: `releaseAll()` en background, `onCleared`, "Unload" manual.
- `onAppBackgrounded()` usa rebote de 1,5 s + `SecureNotesApplication.hasVisibleActivity()`:
  una rotación pasa por `onStop` y NO debe tirar los pesos.
- **Tasa de refresco**: pedir siempre con `perf/DisplayRefreshRate.requestPeak(activity)` en
  `onCreate` y `reapplyPeak` en `onStart`. La hora de pantalla no se comparte entre Activities.
- **Decisiones de rendering según hardware**: leer `LocalPerformanceProfile.current` ( CompositionLocal
  `static`, `perf/DevicePerformanceProfile.kt`). No llamar a `ActivityManager`/`DisplayManager`
  dentro de un `@Composable`.
- **Cero allocations en la fase de dibujo**: reutilizar `Path`/`Stroke`/`Paint` con
  `ui/drawing/StrokeGeometryCache.kt`; no escribir estado dentro del lambda de `Canvas`
  (medir con `onSizeChanged`); no crear `SimpleDateFormat` en composición
  (`util/CachedDateFormatters`).
- **Listas**: toda `LazyColumn`/`LazyRow`/grid de notas lleva `key` estable **y** `contentType`;
  sin `key`, Compose reutiliza posiciones y el scroll reordena mal.
  - Única **excepción consciente** a esa regla: la fila de fichas de adjuntos del chat
    (`FloatingChatInputField.kt`) usa `chipKey(index, name)` —clave **posicional + nombre**— porque
    `FileAttachment` no tiene id estable y `removePendingAttachment` borra por índice; con dos adjuntos
    homónimos una clave por nombre lanzaría `IllegalArgumentException: Key was already used`. ⚠️ El
    mecanismo se suele decir al revés: **al cambiar la clave, Compose destruye y recompone el nodo, no lo
    reutiliza**; aquí no se pierde nada porque las fichas no tienen estado interno.
- **I/O**: nunca en el hilo principal: lecturas/escrituras en `Dispatchers.IO`, rasterizado de
  bitmaps en `Dispatchers.Default`. Estado derivado del disco → `StateFlow` en el ViewModel, no
  lectura directa desde la composición.
- **Imágenes**: el `ImageLoader` singleton (`SecureNotesApplication`) fija tamaño de caché de
  memoria/disco y `Precision` según el perfil; no crear `ImageLoader` por pantalla.
  Si se le pasa una factory `diskCache` a Coil, `.directory(...)` es **obligatorio**
  (`DiskCache.Builder.build()` hace `checkNotNull(directory)` y Coil solo aplica su directorio
  por defecto si NO le pasas factory). Como la caché se resuelve en un `Lazy`, el fallo sale en
  la primera petición: **no hay crash, las imágenes simplemente no cargan**.
- Jank: `perf/FrameMetricsMonitor` solo registra en DEBUG; para overdraw usar
  `adb shell setprop debug.hwui.overdraw show` + GPU Profiler.
- **Markdown (cinco superficies, no solo el chat)**: el motor vive en el módulo **`:markdown`**
  (`MarkdownEngine.kt` + `MarkdownText.kt`), no en `:app`, porque lo necesitan `:app` y `:editor-de-codigo` y entre
  ellos la dependencia va **en un solo sentido**. Se construye **una vez por pantalla** y se reparte con
  `CompositionLocal` (`ProvidesMarkdownEngine`, **por encima** de la `LazyColumn`, nunca dentro de cada elemento).
  Las cinco superficies: chat, texto legal, changelog, pantalla de actualización y **vista previa del editor de
  código**. El `TextView` **no** lleva `movementMethod` ni texto seleccionable: es lo que deja el menú de pulsación
  larga y el deslizamiento en la burbuja y lo que impide que el motor abra enlaces por su cuenta. Quien abre un
  enlace devuelve `true` solo si abre de verdad y solo si es web (`isWebLink`, tras `direccionParaAbrir`, que quita
  la puntuación de la frase y **conserva** los paréntesis pareados). El `AndroidView` **no** va envuelto en un
  `key(texto.hashCode())`. El texto legal y las notas de una release se entregan **literales**; solo la burbuja
  adapta el markup heredado (specs 017, 018 y 019).
- **Las imágenes solo las pinta quien pide una ruta, y el cargador no tiene red.** `ProvidesMarkdownEngine` recibe
  `imagenesLocalesDe: String?` —la ruta del **archivo**—, **no** un cargador. Consecuencia: **ninguna** superficie de
  `:app` lo pasa, así que el chat no puede mostrar ninguna imagen (RNF-50), y **ninguna** puede darle un cargador de
  red aunque quiera. El único cargador (`LocalFileImageLoader`) decodifica con `BitmapFactory` y **no** usa Coil:
  por eso la regla del `ImageLoader` único de arriba no le aplica, porque no hay ningún `ImageLoader`. La ruta se
  normaliza **antes** de comparar contra la carpeta, así que `../fuera.png` se marca como bloqueada.
- **Ningún recorrido por código fuente busca en el texto completo**: primero quita comentarios, o acabará
  comprobando el KDoc que explica por qué algo **no** está.
- **Una dependencia `runtime` no llega al classpath de compilación.** Markwon venía así por
  `compose-markdown` (librería **retirada** en la 018) y hubo que declararlo aparte; **cero jars nuevos** era
  cierto y no se notó hasta que se intentó importar (spec 017).


## Limites
- ✅ Siempre: actualizar `MEMORY.md` al terminar cada tarea.

## Reglas 
- Lee `docs/constitution.md` y la spec activa (`specs/NNN-*/`) antes de tocar código.

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
  (`com.arm.aichat.AiChat` facade), consumed as Gradle module `:lib`. `settings.gradle.kts` points at an
  app-private checkout: `/data/user/0/com.nullij.androidcodestudio/files/home/AndroidCSProjects/llama.cpp`
  (commit `3dc7285b4`, locally patched: NDK 30.0.14904198, CMake 4.3.0). A second checkout exists at
  `/storage/emulated/0/AndroidCSProjects/llama.cpp` but is NOT used by the build. Native code is built from
  source by `:lib`'s CMake during Gradle sync — no `CMakeLists.txt` or prebuilt `.so` in this repo.
  Build only supports `arm64-v8a`.

## Tests

```bash
./gradlew test                   # unit + Robolectric + Roborazzi
```

- **Unit tests**: `app/src/test/java/` — JUnit 4 + Robolectric + Roborazzi.
- **Screenshot tests** (Roborazzi) output to `app/src/test/screenshots/` and require `@GraphicsMode(GraphicsMode.Mode.NATIVE)` + `@Config(sdk = [36])` (sdk = targetSdk, not compileSdk).
- Room + Moshi use KSP — annotation processing changes require a clean build.
- No instrumented tests (`connectedCheck`) usable without a device/emulator.

## Architecture

Single-module Android app (`:app`). MVVM with Jetpack Compose (MD3 Expresive), Room, `StateFlow`.

| Layer | Path | Key files |
|---|---|---|
| UI / Navigation | `com.example.ui` | `MainListScreen.kt`, `NoteEditorScreen.kt`, `AiChatScreen.kt`, `DrawingCanvasScreen.kt`, `MediaViewerScreen.kt`, `SearchScreen.kt`, `LockScreen.kt` |
| Settings UI | `com.example.ui.settings` | `SettingsScreen.kt`, `PrivacySettingsScreen.kt`, `BackupRestoreScreen.kt`, `AiSettingsScreen.kt`, `StorageManagerScreen.kt`, `AboutScreen.kt`, `SettingsWidgets.kt` |
| ViewModel | `com.example.ui.viewmodel` | `NotesViewModel.kt`, `ThemeViewModel.kt`, `BackupViewModel.kt`, `UpdaterViewModel.kt`, `AiViewModel.kt`, `StorageViewModel.kt`, `ChatHistoryViewModel.kt` |
| Data (Room) | `com.example.data.local` | `NoteDatabase.kt`, `NoteDao.kt`, `TagDao.kt` |
| Model | `com.example.data.model` | `Note.kt`, `Tag.kt`, `DecryptedNote.kt`, `NoteContentBlock.kt`, `DataBlock.kt`, `UiState.kt`, `Attachment.kt`, `NavigationSection.kt` |
| Encryption | `com.example.data.security` | `CipherService.kt` (interface), `EncryptionServiceImpl.kt` (AES-256/GCM), `KeyDerivation.kt` (PBKDF2, 200K iterations) |
| AI | `com.example.data.ai` | `AIService.kt` (interface), `OllamaService.kt` (OkHttp, default `http://localhost:11434`), `OnDeviceService.kt` (wraps `LlamaCppEngine`, official llama.cpp `llama.android` binding), `ModelDownloader.kt`, `ToolRegistry.kt`, `MemoryManager.kt`, `tools/` (note tools for AI) |
| Sync | `com.example.data.sync` | `CloudSyncManager.kt` (interface), `GoogleDriveSyncService.kt` (OkHttp impl), `SyncWorker.kt` (WorkManager) |
| Preferences | `com.example.data` | `PreferencesRepository.kt` (interface), `SharedPreferencesRepository.kt` |
| Utils | `com.example.util` | `RichTextParser.kt`, `ExportUtils.kt`, `BiometricAuthManager.kt`, `export/` (Txt, Markdown, Pdf, Html, Json exporters) |

**Entrypoint**: `com.example.MainActivity` (package `com.example`, applicationId `com.estrin217.securenotes`).

## Key Conventions

### DI
- No DI framework. ViewModels constructed via `ViewModelProvider.Factory` in `MainActivity.kt` (NotesViewModel ~line 172, AiViewModel ~line 208).
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

Editor is a Notion-style **block editor** (Notion-like). Note content is stored as a JSON list of `DataBlock` (`com.example.data.model.DataBlock`) with a `BlockType` enum: `TEXT`, `HEADING1-4`, `BULLET_LIST`, `NUMBERED_LIST`, `CHECKLIST_ITEM`, `QUOTE`, `CODE_BLOCK`, `CALLOUT`, `PAGE`, `IMAGE`, `VIDEO`, `AUDIO`, `DRAWING`, `VOICE`, `FILE`, `TABLE`, `HORIZONTAL_RULE`, `COLLAPSIBLE`. Tables use `TableData` (headers/rows/weights, serialized to JSON in `meta`).

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
- **Localizations**: `values/` (en), `values-es-rVE/` (es-VE), `values-pt-rBR/` (pt-BR), `values-fr/` (fr), `values-it/` (it), `values-en-rGB/`, `values-es-rES/`, `values-pt-rPT/`, `values-b+es+419/`.
- `compileSdk = 37`, `targetSdk = 36`, `minSdk = 33`.
- Gradle 9.5.1, AGP 9.3.1, Kotlin 2.4.10.

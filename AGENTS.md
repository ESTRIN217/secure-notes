# Secure Notes — Agent Guide

## Build & Run

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release (needs key.properties)
```

Key signing loaded from `key.properties` (release keystore). Debug builds also sign with release config.

Secrets (`GEMINI_API_KEY`) loaded from `.env` via Secrets Gradle Plugin. See `.env.example`.

## Tests

```bash
./gradlew test                   # unit + Robolectric tests
./gradlew connectedCheck         # instrumented tests on device/emulator
```

- **Unit tests**: `app/src/test/java/` — JUnit 4 + Robolectric + Roborazzi.
- **Instrumented tests**: `app/src/androidTest/java/`.
- **Screenshot tests** (Roborazzi) output PNGs to `app/src/test/screenshots/` and require `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.
- Room + Moshi use KSP — annotation processing changes require a clean build.

## Architecture

Single-module Android app (`:app`). MVVM with Jetpack Compose (Material 3), Room, and `StateFlow`.

| Layer | Path | Key files |
|---|---|---|
| UI / Navigation | `com.example.ui` | `MainActivity.kt`, `NoteEditorScreen.kt`, `DrawingCanvasScreen.kt`, `MediaViewerScreen.kt` |
| ViewModel | `com.example.ui.viewmodel` | `NotesViewModel.kt` |
| Data (Room) | `com.example.data.local` | `NoteDatabase.kt`, `NoteDao.kt` |
| Model | `com.example.data.model` | `Note.kt`, `Tag.kt`, `ListItem.kt` |
| Encryption | `com.example.data.security` | `EncryptionUtils.kt` (AES-256/GCM, PBKDF2) |
| Sync | `com.example.data.sync` | `GoogleDriveSyncService.kt` |
| Utils | `com.example.util` | `RichTextParser.kt`, `ExportUtils.kt` |

**Entrypoint**: `com.example.MainActivity` (package `com.example`, applicationId `com.estrin217.securenotes`).

## Coding Principles

Apply these standards to all Kotlin and XML code in this project.

### DRY (Don't Repeat Yourself)
- Extract shared logic into utilities, helpers, or base classes. Avoid copy-paste across files.
- Reuse Compose components (`SettingsCardGroup`, `SettingsSwitchTile`, etc.) instead of reimplementing patterns.

### SOLID
- **SRP**: One class = one responsibility. ViewModels own screen state; DAOs own data access; Composables own rendering.
- **OCP**: Extend via parameters, lambdas, or composition — never modify a working component to add a new variant.
- **LSP**: Subtypes must honor the contracts of their base types. Override methods only to strengthen preconditions or weaken postconditions.
- **ISP**: Keep interfaces narrow. Prefer Kotlin functional types (`()->Unit`, `StateFlow<T>`) over wide interfaces.
- **DIP**: Inject `NoteDao`, `EncryptionUtils`, and sync services via constructor. No `object` singletons; no static access to dependencies.

### Clean Code
- Functions ≤ 20 lines. Name them with verbs (`saveNote`, `encryptContent`, `toggleDarkMode`).
- One level of abstraction per function. Extract inner logic to named helpers.
- Guard clauses over nested `if`/`else`. No nullable chains deeper than 2.
- Comments explain **why** (design rationale, edge-case reasoning), never **what**.
- Compose previews should be minimal; extract real UI logic into @Composable functions with explicit params.

### KISS
- Prefer a `when` branch over a strategy pattern with one implementation.
- No dependency injection framework — manual constructor DI is sufficient for a single-module app.
- `StateFlow` + `collectAsState()` is enough; no need for additional reactive layers.

### YAGNI
- Do not add generic "repository" abstractions until a second data source exists.
- Do not pre-declare interfaces for every class — introduce them only when substitution is needed (e.g., testing, alternate impl).
- No feature flags, no unused navigation routes, no commented-out code.

### Error Handling & Robustness
- Use `Result<T>` or sealed `UiState` (Loading/Success/Error) for async operations.
- Validate user input at the UI layer (`require()` / `check()`); sanitize at the data layer.
- Never `catch` an exception to continue silently. Log via `Log.e()` at minimum.
- Room queries and file I/O must be off the main thread (coroutine `Dispatchers.IO`).
- Encryption operations wrap all crypto calls in try/catch and surface failures as `EncryptionResult`.

## Key Conventions & Gotchas

- **Room DB** uses `.fallbackToDestructiveMigration()` — schema changes destroy data.
- **Configuration cache** is on (`org.gradle.configuration-cache=true`). Invalidate with `--no-configuration-cache` if build acts stale.
- **Kotlin incremental compilation disabled** (`kotlin.incremental=false` in `gradle.properties`).
- **WYSIWYG editor** stores rich text as custom HTML-like tags (`<b>`, `<color=...>`, `<cl>`, etc.). Parsed by `RichTextParser`.
- **Native localizations**: `values/` (en), `values-es-rVE/` (es-VE), `values-pt-rBR/` (pt-BR).
- **Formatting** not configured; no explicit formatter.
- `compileSdk = targetSdk = 36`, `minSdk = 24`.
- Gradle 9.5.1, AGP 9.2.1, Kotlin 2.2.10.
